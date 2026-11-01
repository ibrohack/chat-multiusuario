package com.psp.chat.client.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.psp.chat.core.ClientPacket;
import com.psp.chat.core.LoginAccepted;
import com.psp.chat.core.ObjectChannel;
import com.psp.chat.core.RejectionReason;
import com.psp.chat.core.ServerShutdown;
import com.psp.chat.core.TextMessage;
import com.psp.chat.core.TextRequest;
import com.psp.chat.core.UserEvent;
import com.psp.chat.core.UserEventType;
import com.psp.chat.server.ChatServer;
import com.psp.chat.server.ServerConfig;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ServerConnectionTest {

    private ChatServer server;
    private final List<ServerConnection> connections = new ArrayList<>();

    @BeforeEach
    void startServer() throws IOException {
        server = new ChatServer(new ServerConfig(0, 3, 3600));
        server.start();
    }

    @AfterEach
    void stopEverything() {
        connections.forEach(ServerConnection::disconnect);
        server.stop();
    }

    private ServerConnection connection(RecordingListener listener) {
        ServerConnection connection = new ServerConnection(listener);
        connections.add(connection);
        return connection;
    }

    private ServerConnection connectAndReceive(String username, RecordingListener listener) throws Exception {
        ServerConnection connection = connection(listener);
        connection.connect("localhost", server.getPort(), username);
        connection.startReceiving();
        return connection;
    }

    @Test
    void connectReturnsLoginAccepted() throws Exception {
        LoginAccepted accepted = connection(new RecordingListener()).connect("localhost", server.getPort(), "ana");

        assertEquals(new LoginAccepted("ana", List.of("ana")), accepted);
    }

    @Test
    void takenUsernameThrowsLoginRejected() throws Exception {
        connection(new RecordingListener()).connect("localhost", server.getPort(), "ana");

        LoginRejectedException error = assertThrows(LoginRejectedException.class,
                () -> connection(new RecordingListener()).connect("localhost", server.getPort(), "ana"));
        assertEquals(RejectionReason.USERNAME_TAKEN, error.getReason());
    }

    @Test
    void noServerThrowsConnectException() throws Exception {
        int closedPort;
        try (ServerSocket probe = new ServerSocket(0)) {
            closedPort = probe.getLocalPort();
        }

        assertThrows(ConnectException.class,
                () -> connection(new RecordingListener()).connect("localhost", closedPort, "ana"));
    }

    @Test
    void deliversReceivedPacketsToListener() throws Exception {
        RecordingListener anaListener = new RecordingListener();
        connectAndReceive("ana", anaListener);
        ServerConnection luis = connectAndReceive("luis", new RecordingListener());

        luis.send(new TextRequest(null, "hola"));

        assertEquals("hola", anaListener.await(TextMessage.class).text());
    }

    @Test
    void sendsPacketsInOrder() throws Exception {
        ServerConnection ana = connectAndReceive("ana", new RecordingListener());
        RecordingListener luisListener = new RecordingListener();
        connectAndReceive("luis", luisListener);

        for (int i = 0; i < 50; i++) {
            ana.send(new TextRequest(null, Integer.toString(i)));
        }

        for (int i = 0; i < 50; i++) {
            assertEquals(Integer.toString(i), luisListener.await(TextMessage.class).text());
        }
    }

    @Test
    void disconnectNotifiesServer() throws Exception {
        RecordingListener anaListener = new RecordingListener();
        ServerConnection ana = connectAndReceive("ana", anaListener);
        RecordingListener luisListener = new RecordingListener();
        connectAndReceive("luis", luisListener);

        ana.disconnect();

        assertEquals(new UserEvent(UserEventType.LEFT, "ana", List.of("luis")), luisListener.await(UserEvent.class));
        assertFalse(anaListener.connectionLostWithin(Duration.ofMillis(300)));
    }

    @Test
    void serverShutdownIsDeliveredWithoutConnectionLost() throws Exception {
        RecordingListener anaListener = new RecordingListener();
        connectAndReceive("ana", anaListener);

        server.stop();

        assertEquals(new ServerShutdown(), anaListener.await(ServerShutdown.class));
        assertFalse(anaListener.connectionLostWithin(Duration.ofMillis(500)));
    }

    @Test
    void serverCrashNotifiesConnectionLost() throws Exception {
        try (ServerSocket fakeServer = new ServerSocket(0)) {
            CompletableFuture<Void> crash = CompletableFuture.runAsync(() -> {
                try (Socket socket = fakeServer.accept(); ObjectChannel channel = new ObjectChannel(socket)) {
                    channel.receive(ClientPacket.class);
                    channel.send(new LoginAccepted("ana", List.of("ana")));
                } catch (IOException | ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            });
            RecordingListener listener = new RecordingListener();
            ServerConnection ana = connection(listener);

            ana.connect("localhost", fakeServer.getLocalPort(), "ana");
            ana.startReceiving();
            crash.get(5, TimeUnit.SECONDS);

            assertTrue(listener.connectionLostWithin(Duration.ofSeconds(2)));
        }
    }
}
