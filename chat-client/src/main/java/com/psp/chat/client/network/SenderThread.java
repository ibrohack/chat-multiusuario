package com.psp.chat.client.network;

import com.psp.chat.core.ClientPacket;
import com.psp.chat.core.ObjectChannel;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Daemon thread that sends queued packets in order, so callers never block on the network.
 *
 * <p>Producer-consumer queue guarded with {@code synchronized}, {@code wait} and
 * {@code notifyAll}. Packets are written outside the lock.</p>
 */
final class SenderThread extends Thread {

    private final ObjectChannel channel;
    private final ServerConnection connection;
    private final Deque<ClientPacket> queue = new ArrayDeque<>();
    private boolean finishing;

    /**
     * Creates the thread; it starts sending when {@link #start()} is called.
     *
     * @param channel    channel to write to
     * @param connection connection to notify if writing fails
     */
    SenderThread(ObjectChannel channel, ServerConnection connection) {
        super("sender");
        setDaemon(true);
        this.channel = channel;
        this.connection = connection;
    }

    /**
     * Queues a packet; it is ignored once {@link #finish()} was called.
     *
     * @param packet packet to send
     */
    synchronized void enqueue(ClientPacket packet) {
        if (finishing) {
            return;
        }
        queue.addLast(packet);
        notifyAll();
    }

    /**
     * Asks the thread to send what is already queued and then stop.
     */
    synchronized void finish() {
        finishing = true;
        notifyAll();
    }

    @Override
    public void run() {
        try {
            ClientPacket packet = next();
            while (packet != null) {
                channel.send(packet);
                packet = next();
            }
        } catch (IOException e) {
            connection.communicationFailed(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Waits for the next packet.
     *
     * @return the next packet, or {@code null} once finishing and the queue is empty
     */
    private synchronized ClientPacket next() throws InterruptedException {
        while (queue.isEmpty() && !finishing) {
            wait();
        }
        return queue.pollFirst();
    }
}
