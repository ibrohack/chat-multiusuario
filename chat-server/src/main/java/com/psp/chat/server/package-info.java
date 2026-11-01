/**
 * Chat server: connection handling, the shared chat room and monitoring.
 *
 * <p>Lock rules: {@link com.psp.chat.server.ChatRoom} guards the registry, the
 * history and the statistics; each connection guards its own writes. No thread
 * sends a packet while holding the room lock, and the only allowed nesting is
 * connection then room, during login.</p>
 */
package com.psp.chat.server;
