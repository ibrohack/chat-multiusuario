package com.psp.chat.core;

/**
 * Packet sent by a client to the server.
 */
public sealed interface ClientPacket extends Packet permits LoginRequest, TextRequest, ImageRequest, LogoutRequest {
}
