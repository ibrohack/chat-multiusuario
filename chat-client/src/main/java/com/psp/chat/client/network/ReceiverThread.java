package com.psp.chat.client.network;

import com.psp.chat.core.ObjectChannel;
import com.psp.chat.core.ServerPacket;
import com.psp.chat.core.ServerShutdown;
import java.io.IOException;

/**
 * Daemon thread that reads packets from the server and hands them to the listener.
 *
 * <p>It is the only reader of the channel. It stops after a
 * {@link ServerShutdown} packet or when the connection fails.</p>
 */
final class ReceiverThread extends Thread {

    private final ObjectChannel channel;
    private final ConnectionListener listener;
    private final ServerConnection connection;

    /**
     * Creates the thread; it starts reading when {@link #start()} is called.
     *
     * @param channel    channel to read from
     * @param listener   receiver of the packets
     * @param connection connection to notify about the end of the session
     */
    ReceiverThread(ObjectChannel channel, ConnectionListener listener, ServerConnection connection) {
        super("receiver");
        setDaemon(true);
        this.channel = channel;
        this.listener = listener;
        this.connection = connection;
    }

    @Override
    public void run() {
        try {
            while (true) {
                ServerPacket packet = channel.receive(ServerPacket.class);
                if (packet instanceof ServerShutdown) {
                    // Mark the end first, so the closing socket is not reported as a lost connection.
                    connection.closedByServer();
                    listener.packetReceived(packet);
                    return;
                }
                listener.packetReceived(packet);
            }
        } catch (IOException | ClassNotFoundException e) {
            connection.communicationFailed(e);
        }
    }
}
