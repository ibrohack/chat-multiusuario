package com.psp.chat.client.network;

import com.psp.chat.core.ClientPacket;
import com.psp.chat.core.LoginAccepted;
import com.psp.chat.core.LoginRejected;
import com.psp.chat.core.LoginRequest;
import com.psp.chat.core.LogoutRequest;
import com.psp.chat.core.ObjectChannel;
import com.psp.chat.core.ServerPacket;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;

/**
 * Connection of the chat client to the server.
 *
 * <p>{@link #connect(String, int, String)} performs the blocking login handshake
 * and must run off the JavaFX thread. Afterwards a {@code receiver} thread reads
 * packets and a {@code sender} thread writes them, so neither reading nor
 * sending ever blocks the caller.</p>
 *
 * <p>Thread-safety: every method may be called from any thread; the state shared
 * with the two threads is guarded by {@code synchronized}.</p>
 */
public final class ServerConnection {

    /** Maximum time, in milliseconds, to establish the TCP connection. */
    public static final int CONNECT_TIMEOUT_MS = 5_000;

    private static final int LOGIN_TIMEOUT_MS = 10_000;
    private static final long SENDER_DRAIN_MS = 500;

    private final ConnectionListener listener;
    private ObjectChannel channel;
    private ReceiverThread receiver;
    private SenderThread sender;
    private boolean voluntaryDisconnect;
    private boolean closedByServer;
    private boolean lossNotified;

    /**
     * Creates an unconnected connection.
     *
     * @param listener receiver of the packets and of the connection loss
     */
    public ServerConnection(ConnectionListener listener) {
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    /**
     * Connects to the server and logs in. Blocks until the server answers.
     *
     * <p>Packets are not delivered to the listener until {@link #startReceiving()} is called.</p>
     *
     * @param host     server host name or IP address
     * @param port     server port
     * @param username name to log in with
     * @return the server's acceptance, with the users already connected
     * @throws LoginRejectedException  if the server rejects the login
     * @throws IOException             if the server cannot be reached or the connection fails
     * @throws ClassNotFoundException  if the server sends an unknown class
     */
    public LoginAccepted connect(String host, int port, String username)
            throws IOException, ClassNotFoundException, LoginRejectedException {
        Socket socket = openSocket(host, port);
        ObjectChannel opened;
        try {
            socket.setSoTimeout(LOGIN_TIMEOUT_MS);
            opened = new ObjectChannel(socket);
        } catch (IOException e) {
            socket.close();
            throw e;
        }
        try {
            opened.send(new LoginRequest(username));
            ServerPacket reply = opened.receive(ServerPacket.class);
            if (reply instanceof LoginRejected rejected) {
                opened.close();
                throw new LoginRejectedException(rejected.reason());
            }
            if (!(reply instanceof LoginAccepted accepted)) {
                throw new StreamCorruptedException("Unexpected answer to the login: " + reply);
            }
            socket.setSoTimeout(0);
            SenderThread newSender = new SenderThread(opened, this);
            synchronized (this) {
                channel = opened;
                receiver = new ReceiverThread(opened, listener, this);
                sender = newSender;
            }
            newSender.start();
            return accepted;
        } catch (IOException | ClassNotFoundException | RuntimeException e) {
            opened.close();
            throw e;
        }
    }

    /**
     * Starts delivering the server's packets to the listener.
     *
     * @throws IllegalStateException if the connection was not established
     */
    public synchronized void startReceiving() {
        if (receiver == null) {
            throw new IllegalStateException("Not connected");
        }
        receiver.start();
    }

    /**
     * Queues a packet for the sender thread. Never blocks.
     *
     * @param packet packet to send
     * @throws IllegalStateException if the connection was not established
     */
    public void send(ClientPacket packet) {
        SenderThread target;
        synchronized (this) {
            target = sender;
        }
        if (target == null) {
            throw new IllegalStateException("Not connected");
        }
        target.enqueue(packet);
    }

    /**
     * Logs out and closes the connection. Waits at most half a second for pending packets.
     * Calling it again, or on a connection that never connected, has no effect.
     */
    public void disconnect() {
        SenderThread senderToFinish;
        ObjectChannel channelToClose;
        synchronized (this) {
            if (channel == null || voluntaryDisconnect) {
                return;
            }
            voluntaryDisconnect = true;
            senderToFinish = sender;
            channelToClose = channel;
        }
        senderToFinish.enqueue(new LogoutRequest());
        senderToFinish.finish();
        try {
            senderToFinish.join(SENDER_DRAIN_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        channelToClose.close();
    }

    /**
     * Tells whether the session is still open.
     *
     * @return {@code true} after a successful login and until the session ends
     */
    public synchronized boolean isConnected() {
        return channel != null && !voluntaryDisconnect && !closedByServer && !lossNotified;
    }

    /**
     * Ends the session after a communication error and notifies the listener,
     * unless the session already ended voluntarily or by a server shutdown.
     *
     * @param cause the error
     */
    void communicationFailed(Exception cause) {
        ObjectChannel channelToClose;
        SenderThread senderToFinish;
        synchronized (this) {
            if (voluntaryDisconnect || closedByServer || lossNotified) {
                return;
            }
            lossNotified = true;
            channelToClose = channel;
            senderToFinish = sender;
        }
        senderToFinish.finish();
        channelToClose.close();
        listener.connectionLost(cause);
    }

    /**
     * Ends the session after the server announced its shutdown.
     */
    void closedByServer() {
        ObjectChannel channelToClose;
        SenderThread senderToFinish;
        synchronized (this) {
            closedByServer = true;
            channelToClose = channel;
            senderToFinish = sender;
        }
        senderToFinish.finish();
        channelToClose.close();
    }

    private Socket openSocket(String host, int port) throws IOException {
        // The only place where the client creates sockets: TLS can be plugged in here later.
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        } catch (IOException e) {
            socket.close();
            throw e;
        }
        return socket;
    }
}
