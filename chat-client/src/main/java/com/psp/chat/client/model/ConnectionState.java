package com.psp.chat.client.model;

/**
 * State of the client's session with the server.
 */
public enum ConnectionState {

    /** No session: the user can edit the connection settings. */
    DISCONNECTED,

    /** The login handshake is in progress. */
    CONNECTING,

    /** The user is logged in and can chat. */
    CONNECTED
}
