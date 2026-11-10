package com.psp.chat.server;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Entry of the server's message history.
 *
 * @param time      time the server accepted the message
 * @param sender    name of the user who sent the message
 * @param recipient name of the private recipient, or {@code null} for a public message
 * @param content   message text, or a description for non-text messages
 */
public record MessageRecord(LocalTime time, String sender, String recipient, String content) {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Validates the components.
     *
     * @throws NullPointerException if {@code time}, {@code sender} or {@code content} is {@code null}
     */
    public MessageRecord {
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(content, "content");
    }

    /**
     * Formats the record as {@code [HH:mm:ss] sender -> all: content}, or with
     * {@code recipient (private)} for private messages.
     *
     * @return the formatted record
     */
    @Override
    public String toString() {
        String target = recipient == null ? "all" : recipient + " (private)";
        return "[" + TIME_FORMAT.format(time) + "] " + sender + " -> " + target + ": " + singleLine(content);
    }

    /**
     * Writes the line breaks of a text as the visible sequences {@code \r} and {@code \n},
     * so that a multi-line message stays on one line of the log.
     *
     * @param text text to convert
     * @return the text on a single line
     */
    static String singleLine(String text) {
        return text.replace("\r", "\\r").replace("\n", "\\n");
    }
}
