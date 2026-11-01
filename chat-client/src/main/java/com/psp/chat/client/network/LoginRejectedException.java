package com.psp.chat.client.network;

import com.psp.chat.core.RejectionReason;
import java.util.Objects;

/**
 * Thrown when the server answers the login request with a rejection.
 */
public final class LoginRejectedException extends Exception {

    private static final long serialVersionUID = 1L;

    /** Why the server rejected the login. */
    private final RejectionReason reason;

    /**
     * Creates the exception.
     *
     * @param reason why the server rejected the login
     */
    public LoginRejectedException(RejectionReason reason) {
        super("Login rejected: " + reason);
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    /**
     * Returns why the server rejected the login.
     *
     * @return the rejection reason
     */
    public RejectionReason getReason() {
        return reason;
    }
}
