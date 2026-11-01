package com.psp.chat.core;

import java.time.Instant;
import java.util.Objects;

/**
 * Text message delivered by the server.
 *
 * <p>Public messages go to every connected user, including the sender. Private
 * messages go to the recipient and are echoed back to the sender.</p>
 *
 * @param sender    name of the user who wrote the message
 * @param recipient name of the private recipient, or {@code null} for a public message
 * @param text      message text
 * @param timestamp moment the server accepted the message
 */
public record TextMessage(String sender, String recipient, String text, Instant timestamp)
        implements ServerPacket {

    /**
     * Validates the components.
     *
     * @throws NullPointerException if {@code sender}, {@code text} or {@code timestamp} is {@code null}
     */
    public TextMessage {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(timestamp, "timestamp");
    }

    /**
     * Tells whether the message was addressed to a single user.
     *
     * @return {@code true} if the message has a recipient
     */
    public boolean isPrivate() {
        return recipient != null;
    }
}
