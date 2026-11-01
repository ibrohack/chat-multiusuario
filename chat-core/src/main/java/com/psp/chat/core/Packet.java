package com.psp.chat.core;

import java.io.Serializable;

/**
 * Unit of information exchanged between the chat server and its clients.
 *
 * <p>The hierarchy is sealed so that a {@code switch} over the packets a peer
 * can receive is checked for exhaustiveness by the compiler. All
 * implementations are immutable records and therefore thread-safe.</p>
 */
public sealed interface Packet extends Serializable permits ClientPacket, ServerPacket {
}
