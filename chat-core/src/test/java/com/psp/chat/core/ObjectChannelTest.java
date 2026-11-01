package com.psp.chat.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ObjectChannelTest {

    private ServerSocket serverSocket;
    private ObjectChannel clientSide;
    private ObjectChannel serverSide;

    @BeforeEach
    void connectChannelPair() throws Exception {
        serverSocket = new ServerSocket(0);
        // Each constructor waits for the peer's stream header, so one end is built on another thread.
        CompletableFuture<ObjectChannel> accepted = CompletableFuture.supplyAsync(() -> {
            try {
                return new ObjectChannel(serverSocket.accept());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        clientSide = new ObjectChannel(new Socket("localhost", serverSocket.getLocalPort()));
        serverSide = accepted.get(5, TimeUnit.SECONDS);
    }

    @AfterEach
    void closeEverything() throws IOException {
        clientSide.close();
        serverSide.close();
        serverSocket.close();
    }

    @Test
    void sendsAndReceivesAPacket() throws Exception {
        clientSide.send(new LoginRequest("Duxon"));

        assertEquals(new LoginRequest("Duxon"), serverSide.receive(ClientPacket.class));
    }

    @Test
    void unexpectedTypeThrowsStreamCorruptedException() throws Exception {
        clientSide.send(new LoginRequest("x"));

        assertThrows(StreamCorruptedException.class, () -> serverSide.receive(ServerPacket.class));
    }

    @Test
    void concurrentSendsKeepTheStreamIntact() throws Exception {
        int threadCount = 10;
        int packetsPerThread = 100;
        List<Thread> senders = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            String threadName = "t" + t;
            Thread sender = new Thread(() -> {
                for (int i = 0; i < packetsPerThread; i++) {
                    try {
                        clientSide.send(new TextRequest(threadName, Integer.toString(i)));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
            sender.setDaemon(true);
            senders.add(sender);
            sender.start();
        }

        Map<String, Integer> lastIndexByThread = new HashMap<>();
        for (int n = 0; n < threadCount * packetsPerThread; n++) {
            TextRequest received = serverSide.receive(TextRequest.class);
            int index = Integer.parseInt(received.text());
            Integer previous = lastIndexByThread.put(received.recipient(), index);
            assertEquals(previous == null ? 0 : previous + 1, index, "out of order: " + received);
        }
        for (Thread sender : senders) {
            sender.join();
        }

        assertEquals(threadCount, lastIndexByThread.size());
        lastIndexByThread.values().forEach(last -> assertEquals(packetsPerThread - 1, last));
    }

    @Test
    void closeIsIdempotentAndPeerFails() {
        clientSide.close();
        clientSide.close();

        assertTrue(clientSide.isClosed());
        assertThrows(IOException.class, () -> serverSide.receive(Packet.class));
    }
}
