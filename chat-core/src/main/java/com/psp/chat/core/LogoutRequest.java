package com.psp.chat.core;

/**
 * Packet a client sends to leave the chat voluntarily.
 */
public record LogoutRequest() implements ClientPacket {
}
