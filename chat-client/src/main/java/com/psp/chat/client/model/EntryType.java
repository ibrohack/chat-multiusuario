package com.psp.chat.client.model;

/**
 * Kind of line shown in a chat view.
 */
public enum EntryType {

    /** Message addressed to everyone. */
    PUBLIC,

    /** Message between two users. */
    PRIVATE,

    /** Notice from the client, such as a user joining or leaving. */
    SYSTEM,

    /** Error reported to the user. */
    ERROR
}
