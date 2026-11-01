package com.psp.chat.client.model;

import com.psp.chat.client.network.ConnectionListener;
import com.psp.chat.client.network.LoginRejectedException;
import com.psp.chat.client.network.ServerConnection;
import com.psp.chat.core.ErrorNotice;
import com.psp.chat.core.LoginAccepted;
import com.psp.chat.core.LoginRejected;
import com.psp.chat.core.Protocol;
import com.psp.chat.core.ServerPacket;
import com.psp.chat.core.ServerShutdown;
import com.psp.chat.core.TextMessage;
import com.psp.chat.core.TextRequest;
import com.psp.chat.core.UserEvent;
import com.psp.chat.core.UserEventType;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Observable chat session shared by the classic and the advanced views.
 *
 * <p>Thread confinement: the state is only read and changed on the UI thread.
 * The network threads call {@link #packetReceived(ServerPacket)} and
 * {@link #connectionLost(Exception)}, which hand the work to the UI executor
 * without touching the state. The login handshake runs in a JavaFX
 * {@link Task} on a background thread.</p>
 */
public final class ChatSession implements ConnectionListener {

    /** Conversation key of the public chat. */
    public static final String GENERAL = "#general";

    private static final Logger LOG = LoggerFactory.getLogger(ChatSession.class);

    private final Executor uiExecutor;
    private final ReadOnlyObjectWrapper<ConnectionState> state =
            new ReadOnlyObjectWrapper<>(this, "state", ConnectionState.DISCONNECTED);
    private final ReadOnlyStringWrapper username = new ReadOnlyStringWrapper(this, "username", "");
    private final ObservableList<String> onlineUsers = FXCollections.observableArrayList();
    private final ObservableList<ChatEntry> allEntries = FXCollections.observableArrayList();
    private final ObservableList<String> privateConversations = FXCollections.observableArrayList();
    private final Map<String, ObservableList<ChatEntry>> conversations = new HashMap<>();
    private final Map<String, ObservableList<ChatEntry>> conversationViews = new HashMap<>();
    private Consumer<String> onError = message -> { };
    private ServerConnection connection;

    /**
     * Creates a disconnected session.
     *
     * @param uiExecutor runs model updates on the UI thread, typically {@code Platform::runLater}
     */
    public ChatSession(Executor uiExecutor) {
        this.uiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor");
    }

    /**
     * Returns the connection state property.
     *
     * @return the read-only state property
     */
    public ReadOnlyObjectProperty<ConnectionState> stateProperty() {
        return state.getReadOnlyProperty();
    }

    /**
     * Returns the connection state.
     *
     * @return the current state
     */
    public ConnectionState getState() {
        return state.get();
    }

    /**
     * Returns the property with the name the user is logged in with.
     *
     * @return the read-only username property; empty before the first login
     */
    public ReadOnlyStringProperty usernameProperty() {
        return username.getReadOnlyProperty();
    }

    /**
     * Returns the name the user is logged in with.
     *
     * @return the username, or an empty string before the first login
     */
    public String getUsername() {
        return username.get();
    }

    /**
     * Returns the other connected users.
     *
     * @return a read-only list kept in the server's order
     */
    public ObservableList<String> getOnlineUsers() {
        return FXCollections.unmodifiableObservableList(onlineUsers);
    }

    /**
     * Returns every line of the session in arrival order, used by the classic view.
     *
     * @return a read-only list
     */
    public ObservableList<ChatEntry> getAllEntries() {
        return FXCollections.unmodifiableObservableList(allEntries);
    }

    /**
     * Returns the lines of one conversation, creating it if needed.
     *
     * @param key {@link #GENERAL} or the name of the other user of a private conversation
     * @return a read-only list that stays the same for the whole session
     */
    public ObservableList<ChatEntry> getConversation(String key) {
        conversationList(key);
        return conversationViews.get(key);
    }

    /**
     * Returns the users with a private conversation, in the order they started.
     *
     * @return a read-only list of user names
     */
    public ObservableList<String> getPrivateConversations() {
        return FXCollections.unmodifiableObservableList(privateConversations);
    }

    /**
     * Sets the action that shows errors to the user.
     *
     * @param handler receives the Spanish error text on the UI thread
     */
    public void setOnError(Consumer<String> handler) {
        onError = Objects.requireNonNull(handler, "handler");
    }

    /**
     * Starts connecting in the background; the state changes to {@link ConnectionState#CONNECTING}.
     * Does nothing unless the session is disconnected.
     *
     * @param settings validated connection settings
     */
    public void connect(ConnectionSettings settings) {
        if (getState() != ConnectionState.DISCONNECTED) {
            return;
        }
        clearConversations();
        state.set(ConnectionState.CONNECTING);
        ServerConnection newConnection = new ServerConnection(this);
        connection = newConnection;
        Task<LoginAccepted> login = new Task<>() {
            @Override
            protected LoginAccepted call() throws Exception {
                return newConnection.connect(settings.host(), settings.port(), settings.username());
            }
        };
        login.setOnSucceeded(event -> {
            onConnected(login.getValue());
            addToGeneral(ChatEntry.system(UiText.connectedAs(settings.host(), settings.port(), getUsername())));
            LOG.info("Connected to {}:{} as {}", settings.host(), settings.port(), getUsername());
            newConnection.startReceiving();
        });
        login.setOnFailed(event -> {
            connection = null;
            state.set(ConnectionState.DISCONNECTED);
            LOG.warn("Cannot connect to {}:{}: {}", settings.host(), settings.port(), login.getException().toString());
            onError.accept(describeError(login.getException(), settings));
        });
        Thread connector = new Thread(login, "connector");
        connector.setDaemon(true);
        connector.start();
    }

    /**
     * Sends a text message after checking it; problems are reported through the error handler.
     *
     * @param recipient name of the private recipient, or {@code null} for the general chat
     * @param text      message text
     * @return {@code true} if the message was queued for sending
     */
    public boolean sendText(String recipient, String text) {
        if (!Protocol.isValidText(text)) {
            onError.accept(UiText.INVALID_TEXT);
            return false;
        }
        if (recipient != null && recipient.equalsIgnoreCase(getUsername())) {
            onError.accept(UiText.PRIVATE_TO_SELF);
            return false;
        }
        if (getState() != ConnectionState.CONNECTED || connection == null) {
            onError.accept(UiText.NOT_CONNECTED);
            return false;
        }
        connection.send(new TextRequest(recipient, text));
        return true;
    }

    /**
     * Logs out and closes the connection. Does nothing if there is no session.
     */
    public void disconnect() {
        boolean wasConnected = getState() == ConnectionState.CONNECTED;
        if (connection != null) {
            connection.disconnect();
        }
        endSession();
        if (wasConnected) {
            addToGeneral(ChatEntry.system(UiText.YOU_DISCONNECTED));
        }
    }

    @Override
    public void packetReceived(ServerPacket packet) {
        uiExecutor.execute(() -> handle(packet));
    }

    @Override
    public void connectionLost(Exception cause) {
        uiExecutor.execute(() -> {
            LOG.warn("Connection lost: {}", cause.toString());
            endSession();
            addToGeneral(ChatEntry.error(UiText.CONNECTION_LOST));
            onError.accept(UiText.CONNECTION_LOST);
        });
    }

    /**
     * Applies a successful login to the model.
     *
     * @param accepted the server's acceptance
     */
    void onConnected(LoginAccepted accepted) {
        username.set(accepted.username());
        state.set(ConnectionState.CONNECTED);
        onlineUsers.setAll(othersIn(accepted.onlineUsers()));
    }

    /**
     * Turns a connection failure into a message for the user.
     *
     * @param cause    the failure
     * @param settings settings used to connect
     * @return the Spanish error text
     */
    static String describeError(Throwable cause, ConnectionSettings settings) {
        if (cause instanceof LoginRejectedException rejected) {
            return switch (rejected.getReason()) {
                case SERVER_FULL -> UiText.SERVER_FULL;
                case USERNAME_TAKEN -> UiText.usernameTaken(settings.username());
                case INVALID_USERNAME -> UiText.INVALID_USERNAME;
            };
        }
        if (cause instanceof ConnectException) {
            return UiText.cannotConnect(settings.host(), settings.port());
        }
        if (cause instanceof SocketTimeoutException) {
            return UiText.noResponse(settings.host(), settings.port());
        }
        if (cause instanceof UnknownHostException) {
            return UiText.unknownHost(settings.host());
        }
        return UiText.communicationError(String.valueOf(cause.getMessage()));
    }

    /**
     * Turns an error reported by the server into a message for the user.
     *
     * @param notice the server's error
     * @return the Spanish error text
     */
    static String describe(ErrorNotice notice) {
        return switch (notice.code()) {
            case ALREADY_LOGGED_IN -> UiText.ALREADY_LOGGED_IN;
            case INVALID_TEXT -> UiText.INVALID_TEXT;
            case INVALID_IMAGE -> UiText.INVALID_IMAGE;
            case PRIVATE_TO_SELF -> UiText.PRIVATE_TO_SELF;
            case USER_NOT_CONNECTED -> UiText.userNotConnected(notice.detail());
        };
    }

    private void handle(ServerPacket packet) {
        switch (packet) {
            case TextMessage message -> addMessage(ChatEntry.of(message), message.sender(), message.recipient());
            case UserEvent event -> handleUserEvent(event);
            case ErrorNotice notice -> {
                String text = describe(notice);
                addToGeneral(ChatEntry.error(text));
                onError.accept(text);
            }
            case ServerShutdown shutdown -> {
                endSession();
                addToGeneral(ChatEntry.system(UiText.SERVER_CLOSED));
                onError.accept(UiText.SERVER_CLOSED);
            }
            case LoginAccepted accepted -> LOG.warn("Unexpected packet after the login: {}", accepted);
            case LoginRejected rejected -> LOG.warn("Unexpected packet after the login: {}", rejected);
        }
    }

    private void addMessage(ChatEntry entry, String sender, String recipient) {
        allEntries.add(entry);
        if (recipient == null) {
            conversationList(GENERAL).add(entry);
            return;
        }
        String peer = sender.equalsIgnoreCase(getUsername()) ? recipient : sender;
        conversationList(peer).add(entry);
    }

    private void handleUserEvent(UserEvent event) {
        onlineUsers.setAll(othersIn(event.onlineUsers()));
        String text = event.type() == UserEventType.JOINED
                ? UiText.joined(event.username())
                : UiText.left(event.username());
        ChatEntry entry = ChatEntry.system(text);
        addToGeneral(entry);
        ObservableList<ChatEntry> privateConversation = conversations.get(event.username());
        if (privateConversation != null) {
            privateConversation.add(entry);
        }
    }

    private void addToGeneral(ChatEntry entry) {
        allEntries.add(entry);
        conversationList(GENERAL).add(entry);
    }

    private ObservableList<ChatEntry> conversationList(String key) {
        ObservableList<ChatEntry> list = conversations.get(key);
        if (list == null) {
            list = FXCollections.observableArrayList();
            conversations.put(key, list);
            conversationViews.put(key, FXCollections.unmodifiableObservableList(list));
            if (!GENERAL.equals(key)) {
                privateConversations.add(key);
            }
        }
        return list;
    }

    private List<String> othersIn(List<String> users) {
        List<String> others = new ArrayList<>();
        for (String user : users) {
            if (!user.equalsIgnoreCase(getUsername())) {
                others.add(user);
            }
        }
        return others;
    }

    private void endSession() {
        connection = null;
        state.set(ConnectionState.DISCONNECTED);
        onlineUsers.clear();
    }

    private void clearConversations() {
        allEntries.clear();
        privateConversations.clear();
        for (ObservableList<ChatEntry> list : conversations.values()) {
            list.clear();
        }
    }
}
