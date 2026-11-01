package com.psp.chat.core;

/**
 * Packet sent by the server to a client.
 */
public sealed interface ServerPacket extends Packet
        permits LoginAccepted, LoginRejected, TextMessage, UserEvent, ErrorNotice, ServerShutdown {
}
