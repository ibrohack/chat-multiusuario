package com.psp.chat.core;

import java.time.Instant;
import java.util.Objects;

/**
 * Image delivered by the server, routed like a {@link TextMessage}.
 *
 * <p>The byte array is not copied; treat it as read-only.</p>
 *
 * @param sender    name of the user who sent the image
 * @param recipient name of the private recipient, or {@code null} for a public image
 * @param fileName  name of the image file
 * @param data      image bytes
 * @param timestamp moment the server accepted the image
 */
public record ImageMessage(String sender, String recipient, String fileName, byte[] data, Instant timestamp)
        implements ServerPacket {

    /**
     * Validates the components.
     *
     * @throws NullPointerException if any component other than {@code recipient} is {@code null}
     */
    public ImageMessage {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(timestamp, "timestamp");
    }

    /**
     * Tells whether the image was addressed to a single user.
     *
     * @return {@code true} if the image has a recipient
     */
    public boolean isPrivate() {
        return recipient != null;
    }
}
