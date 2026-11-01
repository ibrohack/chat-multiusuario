package com.psp.chat.core;

import java.util.Objects;

/**
 * Error the server reports to the client that caused it.
 *
 * <p>The protocol carries a code instead of prose so that the client decides
 * how to word the error for its users.</p>
 *
 * @param code   kind of error
 * @param detail extra information, such as the unknown recipient name, or {@code null}
 */
public record ErrorNotice(ErrorCode code, String detail) implements ServerPacket {

    /**
     * Validates the components.
     *
     * @throws NullPointerException if {@code code} is {@code null}
     */
    public ErrorNotice {
        Objects.requireNonNull(code, "code");
    }
}
