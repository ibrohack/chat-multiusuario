package com.psp.chat.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.psp.chat.core.ErrorCode;
import com.psp.chat.core.ErrorNotice;
import com.psp.chat.core.TextMessage;
import com.psp.chat.core.TextRequest;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ChatServerMessagingTest {

    private ChatServer server;
    private TestClient ana;
    private TestClient luis;
    private TestClient eva;

    @BeforeEach
    void connectThreeUsers() throws Exception {
        server = new ChatServer(new ServerConfig(0, 5, 3600));
        server.start();
        ana = TestClient.connect(server.getPort(), "ana");
        luis = TestClient.connect(server.getPort(), "luis");
        eva = TestClient.connect(server.getPort(), "eva");
    }

    @AfterEach
    void stopServer() {
        ana.close();
        luis.close();
        eva.close();
        server.stop();
    }

    @Test
    void publicMessageReachesEveryoneIncludingSender() throws Exception {
        ana.send(new TextRequest(null, "hola"));

        for (TestClient client : List.of(ana, luis, eva)) {
            TextMessage message = client.await(TextMessage.class);
            assertEquals("ana", message.sender());
            assertNull(message.recipient());
            assertEquals("hola", message.text());
        }
    }

    @Test
    void privateMessageReachesOnlyRecipientAndSender() throws Exception {
        ana.send(new TextRequest("luis", "secreto"));

        for (TestClient client : List.of(luis, ana)) {
            TextMessage message = client.await(TextMessage.class);
            assertEquals(List.of("ana", "luis", "secreto"), List.of(message.sender(), message.recipient(), message.text()));
        }
        eva.assertNoMessages(Duration.ofMillis(300));
    }

    @Test
    void privateRecipientMatchesIgnoringCase() throws Exception {
        ana.send(new TextRequest("LUIS", "hola"));

        assertEquals("luis", luis.await(TextMessage.class).recipient());
    }

    @Test
    void privateToOfflineUserNotifiesSender() throws Exception {
        ana.send(new TextRequest("nobody", "hola"));

        assertEquals(new ErrorNotice(ErrorCode.USER_NOT_CONNECTED, "nobody"), ana.await(ErrorNotice.class));
    }

    @Test
    void privateToSelfNotifiesSender() throws Exception {
        ana.send(new TextRequest("ANA", "hola"));

        assertEquals(new ErrorNotice(ErrorCode.PRIVATE_TO_SELF, null), ana.await(ErrorNotice.class));
    }

    @Test
    void invalidTextNotifiesSender() throws Exception {
        ana.send(new TextRequest(null, "x".repeat(1001)));

        assertEquals(new ErrorNotice(ErrorCode.INVALID_TEXT, null), ana.await(ErrorNotice.class));
    }

    @Test
    void messagesAreKeptInHistory() throws Exception {
        ana.send(new TextRequest(null, "hola"));
        ana.send(new TextRequest("luis", "secreto"));
        luis.await(TextMessage.class);
        luis.await(TextMessage.class);

        assertEquals(2, server.getRoom().getHistory().size());
        assertEquals("secreto", server.getRoom().getStatus().lastMessage().content());
        assertEquals(2, server.getRoom().getStatus().totalMessages());
    }
}
