/**
 * Core of the multiuser chat shared by the server and the client.
 *
 * <p>Defines the serializable packets exchanged over the network, the protocol
 * rules (default port, limits and username format) and the object channel that
 * wraps a socket with object streams.</p>
 */
module com.psp.chat.core {
    exports com.psp.chat.core;
}
