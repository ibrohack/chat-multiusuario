package com.psp.chat.core;

import java.util.List;
import java.util.Objects;

/**
 * Notification that a user joined or left the chat.
 *
 * @param type        whether the user joined or left
 * @param username    name of the user the event is about
 * @param onlineUsers names of every user connected after the event
 */
public record UserEvent(UserEventType type, String username, List<String> onlineUsers)
        implements ServerPacket {

    /**
     * Validates the components and keeps an immutable copy of the user list.
     *
     * @throws NullPointerException if any component is {@code null}
     */
    public UserEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(username, "username");
        onlineUsers = List.copyOf(onlineUsers);
    }
}
