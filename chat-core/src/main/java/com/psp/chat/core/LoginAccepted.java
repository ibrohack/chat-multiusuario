package com.psp.chat.core;

import java.util.List;
import java.util.Objects;

/**
 * Server answer to a successful {@link LoginRequest}.
 *
 * <p>It is always the first packet an accepted client receives.</p>
 *
 * @param username    name the user was registered with
 * @param onlineUsers names of every connected user, including {@code username}
 */
public record LoginAccepted(String username, List<String> onlineUsers) implements ServerPacket {

    /**
     * Validates the components and keeps an immutable copy of the user list.
     *
     * @throws NullPointerException if any component is {@code null}
     */
    public LoginAccepted {
        Objects.requireNonNull(username, "username");
        onlineUsers = List.copyOf(onlineUsers);
    }
}
