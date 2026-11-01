package com.psp.chat.server;

import static org.junit.jupiter.api.Assertions.fail;

import com.psp.chat.core.ClientPacket;
import com.psp.chat.core.ImageMessage;
import com.psp.chat.core.LoginRequest;
import com.psp.chat.core.ObjectChannel;
import com.psp.chat.core.ServerPacket;
import com.psp.chat.core.TextMessage;
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Duration;

/**
 * Headless chat client used by the server integration tests.
 */
final class TestClient implements Closeable {

    private static final int READ_TIMEOUT_MS = 2_000;

    private final ObjectChannel channel;
    private final ServerPacket loginResponse;

    private TestClient(ObjectChannel channel, ServerPacket loginResponse) {
        this.channel = channel;
        this.loginResponse = loginResponse;
    }

    /** Connects, sends the login request and keeps the server's first answer. */
    static TestClient connect(int port, String username) throws Exception {
        Socket socket = new Socket("localhost", port);
        socket.setSoTimeout(READ_TIMEOUT_MS);
        ObjectChannel channel = new ObjectChannel(socket);
        channel.send(new LoginRequest(username));
        return new TestClient(channel, channel.receive(ServerPacket.class));
    }

    ServerPacket getLoginResponse() {
        return loginResponse;
    }

    void send(ClientPacket packet) throws IOException {
        channel.send(packet);
    }

    /** Reads packets until one of the given type arrives, skipping the others. */
    <T extends ServerPacket> T await(Class<T> type) throws Exception {
        while (true) {
            ServerPacket packet = channel.receive(ServerPacket.class);
            if (type.isInstance(packet)) {
                return type.cast(packet);
            }
        }
    }

    /** Fails if a chat message arrives within the given time. */
    void assertNoMessages(Duration wait) throws Exception {
        channel.getSocket().setSoTimeout((int) wait.toMillis());
        try {
            while (true) {
                ServerPacket packet = channel.receive(ServerPacket.class);
                if (packet instanceof TextMessage || packet instanceof ImageMessage) {
                    fail("Unexpected message: " + packet);
                }
            }
        } catch (SocketTimeoutException expected) {
            // Nothing arrived in time, as expected.
        } finally {
            channel.getSocket().setSoTimeout(READ_TIMEOUT_MS);
        }
    }

    /** Closes the socket without saying goodbye, like a crashed client. */
    void closeAbruptly() {
        channel.close();
    }

    @Override
    public void close() {
        channel.close();
    }
}
