package com.psp.chat.core;

/**
 * Kind of error reported by an {@link ErrorNotice}.
 */
public enum ErrorCode {

    /** The client sent a second {@link LoginRequest} on an open session. */
    ALREADY_LOGGED_IN,

    /** The text is empty, blank or longer than {@link Protocol#MAX_TEXT_LENGTH}. */
    INVALID_TEXT,

    /** The image is empty or larger than the maximum size accepted by the server. */
    INVALID_IMAGE,

    /** The client tried to send a private message to itself. */
    PRIVATE_TO_SELF,

    /** The private recipient, given in {@link ErrorNotice#detail()}, is not connected. */
    USER_NOT_CONNECTED
}
