package com.psp.chat.core;

/**
 * Reason why the server rejects a {@link LoginRequest}.
 */
public enum RejectionReason {

    /** The server already has the maximum number of connected clients. */
    SERVER_FULL,

    /** Another connected user already has that name, ignoring case. */
    USERNAME_TAKEN,

    /** The name does not match {@link Protocol#USERNAME_PATTERN}. */
    INVALID_USERNAME
}
