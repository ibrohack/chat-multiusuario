package com.psp.chat.server;

import com.psp.chat.core.Protocol;
import com.psp.chat.core.ServerPacket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Shared state of the chat: the connected users, the message history and the statistics.
 *
 * <p>Thread-safety: every method that reads or changes the state is
 * {@code synchronized} on the room. The broadcast methods are deliberately not
 * synchronized: they take a snapshot of the members under the lock and send
 * outside it, so a slow client never blocks the whole room.</p>
 */
public final class ChatRoom {

    /** Maximum number of messages kept in the history. */
    public static final int MAX_HISTORY = 500;

    private final int maxClients;
    private final Map<String, Member> members = new LinkedHashMap<>();
    private final Deque<MessageRecord> history = new ArrayDeque<>();
    private MessageRecord lastMessage;
    private long totalMessages;

    /**
     * Creates an empty room.
     *
     * @param maxClients maximum number of users the room accepts
     * @throws IllegalArgumentException if {@code maxClients} is lower than 1
     */
    public ChatRoom(int maxClients) {
        if (maxClients < 1) {
            throw new IllegalArgumentException("maxClients must be at least 1");
        }
        this.maxClients = maxClients;
    }

    /**
     * Registers a user, checking the name and the capacity atomically.
     *
     * @param username name the user wants to use
     * @param client   connection of the user
     * @return {@link RegistrationResult#ACCEPTED} or the reason for the rejection
     */
    public synchronized RegistrationResult register(String username, ConnectedClient client) {
        Objects.requireNonNull(client, "client");
        if (!Protocol.isValidUsername(username)) {
            return RegistrationResult.INVALID_USERNAME;
        }
        if (members.size() >= maxClients) {
            return RegistrationResult.SERVER_FULL;
        }
        String key = keyOf(username);
        if (members.containsKey(key)) {
            return RegistrationResult.USERNAME_TAKEN;
        }
        members.put(key, new Member(username, client));
        return RegistrationResult.ACCEPTED;
    }

    /**
     * Removes a user, but only if the name is still registered to the same connection.
     *
     * @param username name of the user to remove
     * @param client   connection that registered the name
     * @return {@code true} if the user was removed
     */
    public synchronized boolean unregister(String username, ConnectedClient client) {
        String key = keyOf(username);
        Member member = members.get(key);
        if (member == null || member.client() != client) {
            return false;
        }
        members.remove(key);
        return true;
    }

    /**
     * Returns the names of the connected users.
     *
     * @return the names sorted alphabetically, ignoring case
     */
    public synchronized List<String> getUsernames() {
        List<String> usernames = new ArrayList<>();
        for (Member member : members.values()) {
            usernames.add(member.username());
        }
        usernames.sort(String.CASE_INSENSITIVE_ORDER);
        return usernames;
    }

    /**
     * Looks up a connected user by name, ignoring case.
     *
     * @param username name to look for, may be {@code null}
     * @return the user's connection, or an empty optional if nobody has that name
     */
    public synchronized Optional<ConnectedClient> findClient(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(members.get(keyOf(username))).map(Member::client);
    }

    /**
     * Returns a snapshot of the connected users' connections.
     *
     * @return a copy that later registrations do not change
     */
    public synchronized List<ConnectedClient> getClients() {
        List<ConnectedClient> clients = new ArrayList<>();
        for (Member member : members.values()) {
            clients.add(member.client());
        }
        return clients;
    }

    /**
     * Records a message, dropping the oldest one beyond {@link #MAX_HISTORY}.
     *
     * @param message message to record
     */
    public synchronized void addToHistory(MessageRecord message) {
        Objects.requireNonNull(message, "message");
        history.addLast(message);
        if (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }
        lastMessage = message;
        totalMessages++;
    }

    /**
     * Returns the recorded messages, oldest first.
     *
     * @return a copy of the history
     */
    public synchronized List<MessageRecord> getHistory() {
        return List.copyOf(history);
    }

    /**
     * Returns the current statistics of the room.
     *
     * @return a consistent snapshot of the statistics
     */
    public synchronized RoomStatus getStatus() {
        return new RoomStatus(members.size(), maxClients, lastMessage, totalMessages);
    }

    /**
     * Sends a packet to every connected user.
     *
     * @param packet packet to send
     */
    public void broadcast(ServerPacket packet) {
        for (Member member : snapshot()) {
            member.client().send(packet);
        }
    }

    /**
     * Sends a packet to every connected user except one.
     *
     * @param packet   packet to send
     * @param username name of the user to skip, ignoring case
     */
    public void broadcastExcept(ServerPacket packet, String username) {
        String skipped = keyOf(username);
        for (Member member : snapshot()) {
            if (!keyOf(member.username()).equals(skipped)) {
                member.client().send(packet);
            }
        }
    }

    private synchronized List<Member> snapshot() {
        return List.copyOf(members.values());
    }

    private static String keyOf(String username) {
        return username.toLowerCase(Locale.ROOT);
    }

    /**
     * A registered user.
     *
     * @param username display name, with the case the user chose
     * @param client   connection of the user
     */
    private record Member(String username, ConnectedClient client) {
    }
}
