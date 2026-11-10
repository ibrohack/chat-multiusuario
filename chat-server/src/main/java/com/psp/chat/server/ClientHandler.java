package com.psp.chat.server;

import com.psp.chat.core.ClientPacket;
import com.psp.chat.core.ErrorCode;
import com.psp.chat.core.ErrorNotice;
import com.psp.chat.core.ImageMessage;
import com.psp.chat.core.ImageRequest;
import com.psp.chat.core.LoginAccepted;
import com.psp.chat.core.LoginRejected;
import com.psp.chat.core.LoginRequest;
import com.psp.chat.core.LogoutRequest;
import com.psp.chat.core.ObjectChannel;
import com.psp.chat.core.Protocol;
import com.psp.chat.core.RejectionReason;
import com.psp.chat.core.ServerPacket;
import com.psp.chat.core.TextMessage;
import com.psp.chat.core.TextRequest;
import com.psp.chat.core.UserEvent;
import com.psp.chat.core.UserEventType;
import java.io.IOException;
import java.net.Socket;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Serves one client connection on its own thread: login, packet loop and cleanup.
 *
 * <p>Thread-safety: {@link #send(ServerPacket)} and {@link #close()} may be
 * called from any thread; the read loop runs only on the handler's thread.</p>
 */
final class ClientHandler implements Runnable, ConnectedClient {

    private static final Logger LOG = LoggerFactory.getLogger(ClientHandler.class);
    private static final int LOGIN_TIMEOUT_MS = 10_000;

    private final Socket socket;
    private final ChatRoom room;
    private ObjectChannel channel;
    private String username;

    /**
     * Creates the handler of an accepted connection.
     *
     * @param socket accepted socket; the handler closes it when the session ends
     * @param room   chat room shared by every client
     */
    ClientHandler(Socket socket, ChatRoom room) {
        this.socket = socket;
        this.room = room;
    }

    @Override
    public void run() {
        String name = null;
        String endReason = "connection lost";
        try {
            ObjectChannel opened = new ObjectChannel(socket);
            setChannel(opened);
            socket.setSoTimeout(LOGIN_TIMEOUT_MS);
            name = login(opened);
            if (name != null) {
                endReason = serve(opened);
            }
        } catch (IOException | ClassNotFoundException e) {
            LOG.debug("Connection ended: {}", e.toString());
        } catch (RuntimeException e) {
            LOG.error("Unexpected error serving {}", socket.getRemoteSocketAddress(), e);
        } finally {
            if (name != null) {
                room.unregister(name, this);
                room.broadcast(new UserEvent(UserEventType.LEFT, name, room.getUsernames()));
                LOG.info("User disconnected: {} ({})", name, endReason);
            }
            close();
            MDC.clear();
        }
    }

    /**
     * Reads the login request and registers the user.
     *
     * @return the registered username, or {@code null} if the login failed
     */
    private String login(ObjectChannel opened) throws IOException, ClassNotFoundException {
        ClientPacket first = opened.receive(ClientPacket.class);
        if (!(first instanceof LoginRequest request)) {
            LOG.warn("Unexpected first packet from {}: {}", socket.getRemoteSocketAddress(), first);
            return null;
        }
        setUsername(request.username());
        RegistrationResult result;
        // Holding the channel lock makes LoginAccepted the first packet this client gets,
        // even if another thread broadcasts right after the registration.
        synchronized (opened) {
            result = room.register(request.username(), this);
            if (result == RegistrationResult.ACCEPTED) {
                opened.send(new LoginAccepted(request.username(), room.getUsernames()));
            } else {
                opened.send(new LoginRejected(result.toRejectionReason()));
            }
        }
        if (result != RegistrationResult.ACCEPTED) {
            RejectionReason reason = result.toRejectionReason();
            LOG.warn("Connection rejected from {}: {}", socket.getRemoteSocketAddress(), reason);
            return null;
        }
        socket.setSoTimeout(0);
        String name = request.username();
        Thread.currentThread().setName("client-" + name);
        MDC.put("user", name);
        RoomStatus status = room.getStatus();
        LOG.info("User connected: {} ({}/{})", name, status.onlineUsers(), status.maxClients());
        room.broadcastExcept(new UserEvent(UserEventType.JOINED, name, room.getUsernames()), name);
        return name;
    }

    /**
     * Handles packets until the client logs out.
     *
     * @return the reason the session ended
     */
    private String serve(ObjectChannel opened) throws IOException, ClassNotFoundException {
        while (true) {
            ClientPacket packet = opened.receive(ClientPacket.class);
            switch (packet) {
                case LogoutRequest logout -> {
                    return "logout";
                }
                case LoginRequest repeated -> send(new ErrorNotice(ErrorCode.ALREADY_LOGGED_IN, null));
                case TextRequest request -> deliverText(request);
                case ImageRequest request -> deliverImage(request);
            }
        }
    }

    /**
     * Validates a text request and delivers it.
     */
    private void deliverText(TextRequest request) {
        if (!Protocol.isValidText(request.text())) {
            send(new ErrorNotice(ErrorCode.INVALID_TEXT, null));
            return;
        }
        String sender = getUsername();
        String text = request.text();
        route(request.recipient(), text,
                recipientName -> new TextMessage(sender, recipientName, text, Instant.now()),
                recipientName -> LOG.info(logLine(sender, recipientName, text)));
    }

    /**
     * Formats the log entry of a text message, always on one line.
     *
     * @param sender    name of the sender
     * @param recipient name of the private recipient, or {@code null} for a public message
     * @param text      message text
     * @return a line like {@code [PUBLIC] ana: hola} or {@code [PRIVATE] ana -> luis: hola}
     */
    static String logLine(String sender, String recipient, String text) {
        String line = MessageRecord.singleLine(text);
        return recipient == null
                ? "[PUBLIC] " + sender + ": " + line
                : "[PRIVATE] " + sender + " -> " + recipient + ": " + line;
    }

    /**
     * Validates an image request and delivers it; the history keeps a description, not the bytes.
     */
    private void deliverImage(ImageRequest request) {
        if (!Protocol.isValidImage(request.data())) {
            send(new ErrorNotice(ErrorCode.INVALID_IMAGE, null));
            return;
        }
        String sender = getUsername();
        long kilobytes = (request.data().length + 1023L) / 1024L;
        String description = "[image " + request.fileName() + ", " + kilobytes + " KB]";
        route(request.recipient(), description,
                recipientName -> new ImageMessage(sender, recipientName, request.fileName(), request.data(),
                        Instant.now()),
                recipientName -> LOG.info("[IMAGE] {} -> {}: {} ({} KB)", sender,
                        recipientName == null ? "all" : recipientName, request.fileName(), kilobytes));
    }

    /**
     * Delivers a message to everyone if it is public, or to the recipient plus an echo
     * to the sender if it is private, recording it in the history.
     *
     * @param recipient      requested recipient, or {@code null} for everyone
     * @param historyContent text recorded in the history
     * @param packetFor      builds the packet for the canonical recipient name ({@code null} if public)
     * @param logDelivery    logs the delivery for the canonical recipient name ({@code null} if public)
     */
    private void route(String recipient, String historyContent, Function<String, ServerPacket> packetFor,
            Consumer<String> logDelivery) {
        String sender = getUsername();
        if (recipient == null) {
            room.addToHistory(new MessageRecord(LocalTime.now(), sender, null, historyContent));
            logDelivery.accept(null);
            room.broadcast(packetFor.apply(null));
            return;
        }
        if (recipient.equalsIgnoreCase(sender)) {
            send(new ErrorNotice(ErrorCode.PRIVATE_TO_SELF, null));
            return;
        }
        Optional<ConnectedClient> target = room.findClient(recipient);
        if (target.isEmpty()) {
            send(new ErrorNotice(ErrorCode.USER_NOT_CONNECTED, recipient));
            return;
        }
        String recipientName = target.get().getUsername();
        room.addToHistory(new MessageRecord(LocalTime.now(), sender, recipientName, historyContent));
        logDelivery.accept(recipientName);
        ServerPacket packet = packetFor.apply(recipientName);
        target.get().send(packet);
        send(packet);
    }

    @Override
    public synchronized String getUsername() {
        return username;
    }

    @Override
    public void send(ServerPacket packet) {
        ObjectChannel target = getChannel();
        if (target == null) {
            return;
        }
        try {
            target.send(packet);
        } catch (IOException e) {
            LOG.debug("Cannot send to {}: {}", getUsername(), e.toString());
            close();
        }
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            // Closing a broken socket can fail; there is nothing else to release.
        }
    }

    private synchronized void setUsername(String username) {
        this.username = username;
    }

    private synchronized ObjectChannel getChannel() {
        return channel;
    }

    private synchronized void setChannel(ObjectChannel channel) {
        this.channel = channel;
    }
}
