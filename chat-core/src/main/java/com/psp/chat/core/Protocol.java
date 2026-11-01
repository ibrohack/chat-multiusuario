package com.psp.chat.core;

import java.util.regex.Pattern;

/**
 * Constants and validation rules shared by the server and the client.
 */
public final class Protocol {

    /** Port the server listens on when no other port is configured. */
    public static final int DEFAULT_PORT = 5000;

    /** Maximum number of characters of a text message. */
    public static final int MAX_TEXT_LENGTH = 1000;

    /** Maximum size of an image, in bytes (2 MiB). */
    public static final int MAX_IMAGE_BYTES = 2 * 1024 * 1024;

    /** Valid usernames: 3 to 16 letters, digits or underscores. */
    public static final Pattern USERNAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}_]{3,16}$");

    private Protocol() {
        // Utility class: not instantiable.
    }

    /**
     * Tells whether a username follows {@link #USERNAME_PATTERN}.
     *
     * @param username name to check, may be {@code null}
     * @return {@code true} if the name is valid
     */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * Tells whether an image can be sent.
     *
     * @param data image bytes to check, may be {@code null}
     * @return {@code true} if the image has between 1 and {@link #MAX_IMAGE_BYTES} bytes
     */
    public static boolean isValidImage(byte[] data) {
        return data != null && data.length > 0 && data.length <= MAX_IMAGE_BYTES;
    }

    /**
     * Tells whether a text can be sent as a chat message.
     *
     * @param text text to check, may be {@code null}
     * @return {@code true} if the text is not blank and has at most {@link #MAX_TEXT_LENGTH} characters
     */
    public static boolean isValidText(String text) {
        return text != null && !text.isBlank() && text.length() <= MAX_TEXT_LENGTH;
    }
}
