package com.psp.chat.server;

import com.psp.chat.core.ServerPacket;

/**
 * A user connected to the chat room, seen from the room's point of view.
 *
 * <p>Implementations must be safe to call from several threads, because any
 * client thread may broadcast to every other client.</p>
 */
public interface ConnectedClient {

    /**
     * Returns the name the user logged in with.
     *
     * @return the username
     */
    String getUsername();

    /**
     * Sends a packet to the user. Failures close the connection instead of being thrown.
     *
     * @param packet packet to send
     */
    void send(ServerPacket packet);

    /**
     * Closes the connection with the user.
     */
    void close();
}
