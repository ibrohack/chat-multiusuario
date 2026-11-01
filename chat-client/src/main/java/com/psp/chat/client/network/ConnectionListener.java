package com.psp.chat.client.network;

import com.psp.chat.core.ServerPacket;

/**
 * Receives the events of a {@link ServerConnection}.
 *
 * <p>Both methods are called from background threads, never from the JavaFX
 * application thread.</p>
 */
public interface ConnectionListener {

    /**
     * Called for every packet the server sends after the login.
     *
     * @param packet the received packet
     */
    void packetReceived(ServerPacket packet);

    /**
     * Called once if the connection fails without a voluntary disconnection or a server shutdown.
     *
     * @param cause the error that ended the connection
     */
    void connectionLost(Exception cause);
}
