package com.psp.chat.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.psp.chat.core.LoginAccepted;
import com.psp.chat.core.LoginRejected;
import com.psp.chat.core.LogoutRequest;
import com.psp.chat.core.ObjectChannel;
import com.psp.chat.core.RejectionReason;
import com.psp.chat.core.ServerPacket;
import com.psp.chat.core.ServerShutdown;
import com.psp.chat.core.TextRequest;
import com.psp.chat.core.UserEvent;
import com.psp.chat.core.UserEventType;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ChatServerTest {

    private ChatServer server;
    private final List<TestClient> clients = new ArrayList<>();

    @BeforeEach
    void startServer() throws IOException {
        server = new ChatServer(new ServerConfig(0, 2, 3600));
        server.start();
    }

    @AfterEach
    void stopServer() {
        clients.forEach(TestClient::close);
        server.stop();
    }

    private TestClient connect(String username) throws Exception {
        TestClient client = TestClient.connect(server.getPort(), username);
        clients.add(client);
        return client;
    }

    @Test
    void loginReturnsOnlineUsers() throws Exception {
        assertEquals(new LoginAccepted("ana", List.of("ana")), connect("ana").getLoginResponse());
        assertEquals(new LoginAccepted("luis", List.of("ana", "luis")), connect("luis").getLoginResponse());
    }

    @Test
    void othersReceiveTheJoinEvent() throws Exception {
        TestClient ana = connect("ana");
        connect("luis");

        assertEquals(new UserEvent(UserEventType.JOINED, "luis", List.of("ana", "luis")), ana.await(UserEvent.class));
    }

    @Test
    void rejectsExtraClientWhenServerIsFull() throws Exception {
        connect("ana");
        connect("luis");

        assertEquals(new LoginRejected(RejectionReason.SERVER_FULL), connect("eva").getLoginResponse());
    }

    @Test
    void rejectsTakenUsername() throws Exception {
        connect("ana");

        assertEquals(new LoginRejected(RejectionReason.USERNAME_TAKEN), connect("ANA").getLoginResponse());
    }

    @Test
    void rejectsInvalidUsername() throws Exception {
        assertEquals(new LoginRejected(RejectionReason.INVALID_USERNAME), connect("a b").getLoginResponse());
    }

    @Test
    void logoutNotifiesOthersAndFreesSlot() throws Exception {
        TestClient ana = connect("ana");
        TestClient luis = connect("luis");

        ana.send(new LogoutRequest());

        assertEquals(new UserEvent(UserEventType.LEFT, "ana", List.of("luis")), luis.await(UserEvent.class));
        assertInstanceOf(LoginAccepted.class, connect("eva").getLoginResponse());
    }

    @Test
    void abruptCloseNotifiesOthersAndFreesSlot() throws Exception {
        TestClient ana = connect("ana");
        TestClient luis = connect("luis");

        ana.closeAbruptly();

        assertEquals(new UserEvent(UserEventType.LEFT, "ana", List.of("luis")), luis.await(UserEvent.class));
        assertInstanceOf(LoginAccepted.class, connect("eva").getLoginResponse());
    }

    @Test
    void nonLoginFirstPacketClosesTheConnection() throws Exception {
        Socket socket = new Socket("localhost", server.getPort());
        socket.setSoTimeout(2_000);
        try (ObjectChannel channel = new ObjectChannel(socket)) {
            channel.send(new TextRequest(null, "hola"));

            IOException error = assertThrows(IOException.class, () -> channel.receive(ServerPacket.class));
            assertFalse(error instanceof SocketTimeoutException, "the server should close, not stay silent");
        }
    }

    @Test
    void stopSendsServerShutdownToEveryone() throws Exception {
        TestClient ana = connect("ana");
        TestClient luis = connect("luis");

        server.stop();

        assertEquals(new ServerShutdown(), ana.await(ServerShutdown.class));
        assertEquals(new ServerShutdown(), luis.await(ServerShutdown.class));
    }
}
