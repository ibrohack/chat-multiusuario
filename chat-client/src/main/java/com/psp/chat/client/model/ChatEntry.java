package com.psp.chat.client.model;

import com.psp.chat.core.ImageMessage;
import com.psp.chat.core.TextMessage;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * One line of a chat view.
 *
 * @param type      kind of line
 * @param sender    author of a message, or {@code null} for notices
 * @param recipient private recipient, or {@code null} for public messages and notices
 * @param text      message text, notice text, or file name for images
 * @param image     image bytes, or {@code null} if the line is not an image
 * @param time      local time to display
 */
public record ChatEntry(EntryType type, String sender, String recipient, String text, byte[] image, LocalTime time) {

    /**
     * Validates the components.
     *
     * @throws NullPointerException if {@code type}, {@code text} or {@code time} is {@code null}
     */
    public ChatEntry {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(time, "time");
    }

    /**
     * Creates the line for a text message.
     *
     * @param message message received from the server
     * @return a public or private line, with the server time converted to local time
     */
    public static ChatEntry of(TextMessage message) {
        EntryType type = message.isPrivate() ? EntryType.PRIVATE : EntryType.PUBLIC;
        LocalTime time = LocalTime.ofInstant(message.timestamp(), ZoneId.systemDefault());
        return new ChatEntry(type, message.sender(), message.recipient(), message.text(), null, time);
    }

    /**
     * Creates the line for an image.
     *
     * @param message image received from the server
     * @return a public or private line whose text is the file name
     */
    public static ChatEntry of(ImageMessage message) {
        EntryType type = message.isPrivate() ? EntryType.PRIVATE : EntryType.PUBLIC;
        LocalTime time = LocalTime.ofInstant(message.timestamp(), ZoneId.systemDefault());
        return new ChatEntry(type, message.sender(), message.recipient(), message.fileName(), message.data(), time);
    }

    /**
     * Creates a notice line.
     *
     * @param text notice text
     * @return a system line stamped with the current time
     */
    public static ChatEntry system(String text) {
        return new ChatEntry(EntryType.SYSTEM, null, null, text, null, LocalTime.now());
    }

    /**
     * Creates an error line.
     *
     * @param text error text
     * @return an error line stamped with the current time
     */
    public static ChatEntry error(String text) {
        return new ChatEntry(EntryType.ERROR, null, null, text, null, LocalTime.now());
    }

    /**
     * Tells whether the line carries an image.
     *
     * @return {@code true} if {@link #image()} is not {@code null}
     */
    public boolean isImage() {
        return image != null;
    }
}
