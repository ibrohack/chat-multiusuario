package com.psp.chat.server;

/**
 * Snapshot of the chat room statistics.
 *
 * @param onlineUsers   number of registered users
 * @param maxClients    maximum number of users the room accepts
 * @param lastMessage   last message recorded, or {@code null} if there is none
 * @param totalMessages number of messages recorded since the server started
 */
public record RoomStatus(int onlineUsers, int maxClients, MessageRecord lastMessage, long totalMessages) {
}
