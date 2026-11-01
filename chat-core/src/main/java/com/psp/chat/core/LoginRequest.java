package com.psp.chat.core;

import java.util.Objects;

/**
 * First packet a client sends after connecting, asking to join the chat.
 *
 * @param username name the user wants to appear with
 */
public record LoginRequest(String username) implements ClientPacket {

    /**
     * Validates the components.
     *
     * @throws NullPointerException if {@code username} is {@code null}
     */
    public LoginRequest {
        Objects.requireNonNull(username, "username");
    }
}
