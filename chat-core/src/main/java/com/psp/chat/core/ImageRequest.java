package com.psp.chat.core;

import java.util.Objects;

/**
 * Image a client asks the server to deliver.
 *
 * <p>The size is checked by the server with {@link Protocol#isValidImage(byte[])}.
 * The byte array is not copied; treat it as read-only.</p>
 *
 * @param recipient name of the private recipient, or {@code null} for a public image
 * @param fileName  name of the image file, shown to the receivers
 * @param data      image bytes
 */
public record ImageRequest(String recipient, String fileName, byte[] data) implements ClientPacket {

    /**
     * Validates the components.
     *
     * @throws NullPointerException if {@code fileName} or {@code data} is {@code null}
     */
    public ImageRequest {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(data, "data");
    }
}
