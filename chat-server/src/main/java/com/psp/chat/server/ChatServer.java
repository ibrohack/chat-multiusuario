package com.psp.chat.server;

import com.psp.chat.core.ServerShutdown;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Console chat server: accepts clients and serves each one on its own thread.
 *
 * <p>Threads: an {@code acceptor} thread waits for connections and starts one
 * {@code client-<n>} thread per connection, renamed {@code client-<username>}
 * after the login.</p>
 *
 * <p>Thread-safety: {@link #start()}, {@link #getPort()} and {@link #stop()}
 * may be called from any thread.</p>
 */
public final class ChatServer {

    private static final Logger LOG = LoggerFactory.getLogger(ChatServer.class);

    private final ServerConfig config;
    private final ChatRoom room;
    private ServerSocket serverSocket;
    private boolean stopped;

    /**
     * Creates a server that is not listening yet.
     *
     * @param config server settings
     */
    public ChatServer(ServerConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.room = new ChatRoom(config.maxClients());
    }

    /**
     * Starts listening and accepting clients on a background thread.
     *
     * @throws IOException           if the port cannot be opened
     * @throws IllegalStateException if the server was already started
     */
    public synchronized void start() throws IOException {
        if (serverSocket != null) {
            throw new IllegalStateException("The server was already started");
        }
        serverSocket = createServerSocket(config.port());
        LOG.info("Server listening on port {} (max {} clients)", serverSocket.getLocalPort(), config.maxClients());
        Thread acceptor = new Thread(this::acceptClients, "acceptor");
        acceptor.start();
    }

    /**
     * Returns the port the server listens on.
     *
     * @return the bound port, useful when the configured port is {@code 0}
     * @throws IllegalStateException if the server was not started
     */
    public synchronized int getPort() {
        if (serverSocket == null) {
            throw new IllegalStateException("The server was not started");
        }
        return serverSocket.getLocalPort();
    }

    /**
     * Returns the chat room shared by every client thread.
     *
     * @return the room
     */
    ChatRoom getRoom() {
        return room;
    }

    /**
     * Stops the server: no new clients are accepted, every connected user is told
     * about the shutdown and every connection is closed. Calling it again has no effect.
     */
    public void stop() {
        ServerSocket socketToClose;
        synchronized (this) {
            if (stopped) {
                return;
            }
            stopped = true;
            socketToClose = serverSocket;
        }
        if (socketToClose == null) {
            return;
        }
        try {
            socketToClose.close();
        } catch (IOException e) {
            LOG.warn("Error closing the server socket: {}", e.getMessage());
        }
        room.broadcast(new ServerShutdown());
        for (ConnectedClient client : room.getClients()) {
            client.close();
        }
        LOG.info("Server stopped");
    }

    /**
     * Starts the server from the command line.
     *
     * @param args options {@code --port}, {@code --max-clients} and {@code --monitor-interval}
     */
    public static void main(String[] args) {
        ServerConfig config;
        try {
            config = ServerConfig.fromArgs(args);
        } catch (IllegalArgumentException e) {
            LOG.error(e.getMessage());
            return;
        }
        ChatServer server = new ChatServer(config);
        try {
            server.start();
        } catch (IOException e) {
            LOG.error("Cannot listen on port {}: {}", config.port(), e.getMessage());
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "shutdown-hook"));
    }

    private synchronized boolean isStopped() {
        return stopped;
    }

    private synchronized ServerSocket getServerSocket() {
        return serverSocket;
    }

    private void acceptClients() {
        ServerSocket listener = getServerSocket();
        int connectionCount = 0;
        while (true) {
            Socket socket;
            try {
                socket = listener.accept();
            } catch (IOException e) {
                if (!isStopped()) {
                    LOG.error("Stopped accepting clients", e);
                }
                return;
            }
            connectionCount++;
            LOG.info("Incoming connection from {}", socket.getRemoteSocketAddress());
            Thread clientThread = new Thread(new ClientHandler(socket, room), "client-" + connectionCount);
            clientThread.setDaemon(true);
            clientThread.start();
        }
    }

    private ServerSocket createServerSocket(int port) throws IOException {
        // The only place where the server creates sockets: TLS can be plugged in here later.
        return new ServerSocket(port);
    }
}
