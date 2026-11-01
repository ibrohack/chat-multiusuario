package com.psp.chat.client.model;

import com.psp.chat.core.Protocol;

/**
 * Validated connection settings typed by the user.
 *
 * @param host     server host name or IP address
 * @param port     server port
 * @param username name to log in with
 */
public record ConnectionSettings(String host, int port, String username) {

    /** Host used when the IP field is left empty. */
    public static final String DEFAULT_HOST = "127.0.0.1";

    /**
     * Parses the text fields of the connection bar.
     *
     * @param host     IP field; blank means {@link #DEFAULT_HOST}
     * @param port     port field; blank means {@link Protocol#DEFAULT_PORT}
     * @param username username field
     * @return the validated settings, with surrounding spaces removed
     * @throws IllegalArgumentException with a message for the user if the port or the name is not valid
     */
    public static ConnectionSettings parse(String host, String port, String username) {
        String trimmedHost = trim(host);
        String trimmedPort = trim(port);
        String trimmedUsername = trim(username);
        int portNumber = trimmedPort.isEmpty() ? Protocol.DEFAULT_PORT : parsePort(trimmedPort);
        if (!Protocol.isValidUsername(trimmedUsername)) {
            throw new IllegalArgumentException(UiText.INVALID_USERNAME);
        }
        return new ConnectionSettings(trimmedHost.isEmpty() ? DEFAULT_HOST : trimmedHost, portNumber, trimmedUsername);
    }

    private static int parsePort(String port) {
        int number;
        try {
            number = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(UiText.INVALID_PORT, e);
        }
        if (number < 1 || number > 65_535) {
            throw new IllegalArgumentException(UiText.INVALID_PORT);
        }
        return number;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
