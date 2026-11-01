package com.psp.chat.core;

/**
 * Kind of change reported by a {@link UserEvent}.
 */
public enum UserEventType {

    /** The user has just joined the chat. */
    JOINED,

    /** The user has left the chat, voluntarily or because the connection was lost. */
    LEFT
}
