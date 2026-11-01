package com.psp.chat.core;

import java.util.Objects;

/**
 * Server answer to a {@link LoginRequest} that cannot be accepted.
 *
 * <p>The server closes the connection right after sending it.</p>
 *
 * @param reason why the login was rejected
 */
public record LoginRejected(RejectionReason reason) implements ServerPacket {

    /**
     * Validates the components.
     *
     * @throws NullPointerException if {@code reason} is {@code null}
     */
    public LoginRejected {
        Objects.requireNonNull(reason, "reason");
    }
}
