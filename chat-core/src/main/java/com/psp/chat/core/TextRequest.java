package com.psp.chat.core;

import java.util.Objects;

/**
 * Text a client asks the server to deliver.
 *
 * <p>The sender is not part of the request: the server stamps the name of the
 * session the request arrived on.</p>
 *
 * @param recipient name of the private recipient, or {@code null} for a public message
 * @param text      message text
 */
public record TextRequest(String recipient, String text) implements ClientPacket {

    /**
     * Validates the components.
     *
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public TextRequest {
        Objects.requireNonNull(text, "text");
    }
}
