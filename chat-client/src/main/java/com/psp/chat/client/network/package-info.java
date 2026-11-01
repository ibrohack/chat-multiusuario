/**
 * Client side of the network connection: login handshake, a receiver thread and a sender thread.
 *
 * <p>Nothing in this package touches JavaFX: listeners are called from the
 * background threads and must hand the work to the UI thread themselves.</p>
 */
package com.psp.chat.client.network;
