package com.psp.chat.client.network;

import com.psp.chat.core.ServerPacket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link ConnectionListener} that records what it receives and lets tests wait for it.
 */
final class RecordingListener implements ConnectionListener {

    private static final long WAIT_MS = 2_000;

    private final List<ServerPacket> packets = new ArrayList<>();
    private int cursor;
    private boolean lost;

    @Override
    public synchronized void packetReceived(ServerPacket packet) {
        packets.add(packet);
        notifyAll();
    }

    @Override
    public synchronized void connectionLost(Exception cause) {
        lost = true;
        notifyAll();
    }

    /** Returns the next packet of the given type, skipping others; fails after two seconds. */
    synchronized <T extends ServerPacket> T await(Class<T> type) throws InterruptedException {
        long deadline = System.currentTimeMillis() + WAIT_MS;
        while (true) {
            while (cursor < packets.size()) {
                ServerPacket packet = packets.get(cursor++);
                if (type.isInstance(packet)) {
                    return type.cast(packet);
                }
            }
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                throw new AssertionError("No " + type.getSimpleName() + " received; got " + packets);
            }
            wait(remaining);
        }
    }

    /** Tells whether connectionLost was called within the given time. */
    synchronized boolean connectionLostWithin(Duration wait) throws InterruptedException {
        long deadline = System.currentTimeMillis() + wait.toMillis();
        while (!lost) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                return false;
            }
            wait(remaining);
        }
        return true;
    }
}
