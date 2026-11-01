package com.psp.chat.server;

import com.psp.chat.core.RejectionReason;

/**
 * Outcome of {@link ChatRoom#register(String, ConnectedClient)}.
 */
public enum RegistrationResult {

    /** The user joined the room. */
    ACCEPTED(null),

    /** The room already holds the maximum number of clients. */
    SERVER_FULL(RejectionReason.SERVER_FULL),

    /** Another user already has the name, ignoring case. */
    USERNAME_TAKEN(RejectionReason.USERNAME_TAKEN),

    /** The name does not follow the protocol rules. */
    INVALID_USERNAME(RejectionReason.INVALID_USERNAME);

    private final RejectionReason rejectionReason;

    RegistrationResult(RejectionReason rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    /**
     * Returns the protocol reason to send to a rejected client.
     *
     * @return the rejection reason, or {@code null} for {@link #ACCEPTED}
     */
    public RejectionReason toRejectionReason() {
        return rejectionReason;
    }
}
