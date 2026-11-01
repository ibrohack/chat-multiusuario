package com.psp.chat.core;

/**
 * Notification that the server is stopping and is about to close the connection.
 */
public record ServerShutdown() implements ServerPacket {
}
