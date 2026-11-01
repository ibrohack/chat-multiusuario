package com.psp.chat.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.util.Objects;

/**
 * Bidirectional channel of {@link Packet}s over a socket, built on object streams.
 *
 * <p>The output stream is created and flushed before the input stream is opened,
 * because an {@link ObjectInputStream} blocks until it reads the peer's stream
 * header; opening the input first on both ends would deadlock.</p>
 *
 * <p>Thread-safety: {@link #send(Packet)} is {@code synchronized}, so several
 * threads may send through the same channel. {@link #receive(Class)} must be
 * called from a single reader thread. {@link #close()} may be called from any
 * thread and unblocks a pending {@code receive}.</p>
 */
public final class ObjectChannel implements Closeable {

    private final Socket socket;
    private final ObjectOutputStream output;
    private final ObjectInputStream input;

    /**
     * Opens the object streams of a connected socket.
     *
     * <p>If construction fails, the caller remains responsible for closing the socket.</p>
     *
     * @param socket connected socket; the channel owns it from now on
     * @throws IOException if the streams cannot be opened or the peer's header cannot be read
     */
    public ObjectChannel(Socket socket) throws IOException {
        this.socket = Objects.requireNonNull(socket, "socket");
        this.output = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.output.flush();
        this.input = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
    }

    /**
     * Sends a packet and flushes it to the network.
     *
     * <p>The stream is reset after every write so that it does not keep references
     * to the packets already sent.</p>
     *
     * @param packet packet to send
     * @throws IOException if the connection fails
     */
    public synchronized void send(Packet packet) throws IOException {
        output.writeObject(packet);
        output.reset();
        output.flush();
    }

    /**
     * Blocks until the next packet arrives and checks its type.
     *
     * @param <T>  expected packet type
     * @param type class of the expected packet type
     * @return the received packet
     * @throws StreamCorruptedException if the received object is not of the expected type
     * @throws IOException              if the connection fails or is closed
     * @throws ClassNotFoundException   if the received class is unknown
     */
    public <T extends Packet> T receive(Class<T> type) throws IOException, ClassNotFoundException {
        Object value = input.readObject();
        if (!type.isInstance(value)) {
            String received = value == null ? "null" : value.getClass().getName();
            throw new StreamCorruptedException("Expected " + type.getSimpleName() + " but received " + received);
        }
        return type.cast(value);
    }

    /**
     * Returns the underlying socket, for instance to change its read timeout.
     *
     * @return the socket this channel wraps
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Tells whether the channel has been closed.
     *
     * @return {@code true} once the socket is closed
     */
    public boolean isClosed() {
        return socket.isClosed();
    }

    /**
     * Closes the socket and its streams. Calling it again has no effect.
     */
    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            // The socket is unusable either way; there is nothing left to release.
        }
    }
}
