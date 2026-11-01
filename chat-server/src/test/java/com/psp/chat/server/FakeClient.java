package com.psp.chat.server;

import com.psp.chat.core.ServerPacket;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory {@link ConnectedClient} that records the packets it is sent.
 */
final class FakeClient implements ConnectedClient {

    private final String username;
    private final List<ServerPacket> received = new ArrayList<>();
    private boolean closed;

    FakeClient(String username) {
        this.username = username;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public synchronized void send(ServerPacket packet) {
        received.add(packet);
    }

    @Override
    public synchronized void close() {
        closed = true;
    }

    synchronized List<ServerPacket> received() {
        return List.copyOf(received);
    }

    synchronized boolean isClosed() {
        return closed;
    }
}
