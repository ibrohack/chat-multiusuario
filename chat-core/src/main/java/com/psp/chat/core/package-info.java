/**
 * Wire protocol of the multiuser chat.
 *
 * <p>Every message is a {@link com.psp.chat.core.Packet} sent with an
 * {@link java.io.ObjectOutputStream}. Packets sent by clients implement
 * {@link com.psp.chat.core.ClientPacket} and packets sent by the server
 * implement {@link com.psp.chat.core.ServerPacket}. A session starts with a
 * {@link com.psp.chat.core.LoginRequest} answered by either
 * {@link com.psp.chat.core.LoginAccepted} or
 * {@link com.psp.chat.core.LoginRejected}.</p>
 */
package com.psp.chat.core;
