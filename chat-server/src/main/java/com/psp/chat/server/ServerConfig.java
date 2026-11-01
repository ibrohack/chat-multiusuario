package com.psp.chat.server;

import com.psp.chat.core.Protocol;

/**
 * Settings of the chat server.
 *
 * @param port                   TCP port to listen on; {@code 0} picks a free port
 * @param maxClients             maximum number of connected users
 * @param monitorIntervalSeconds seconds between two monitor reports
 */
public record ServerConfig(int port, int maxClients, int monitorIntervalSeconds) {

    /** Command-line syntax shown when the arguments are not valid. */
    public static final String USAGE = "Usage: --port=5000 --max-clients=10 --monitor-interval=10";

    /** Maximum number of connected users when no other value is given. */
    public static final int DEFAULT_MAX_CLIENTS = 10;

    /** Seconds between monitor reports when no other value is given. */
    public static final int DEFAULT_MONITOR_INTERVAL_SECONDS = 10;

    /**
     * Validates the settings.
     *
     * @throws IllegalArgumentException if a value is out of range
     */
    public ServerConfig {
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("--port must be between 0 and 65535");
        }
        if (maxClients < 1) {
            throw new IllegalArgumentException("--max-clients must be at least 1");
        }
        if (monitorIntervalSeconds < 1) {
            throw new IllegalArgumentException("--monitor-interval must be at least 1");
        }
    }

    /**
     * Builds the settings from command-line options of the form {@code --name=value}.
     *
     * @param args options {@code --port}, {@code --max-clients} and {@code --monitor-interval}; all optional
     * @return the settings, with defaults for the missing options
     * @throws IllegalArgumentException if an option is unknown or its value is not valid;
     *                                  the message ends with {@link #USAGE}
     */
    public static ServerConfig fromArgs(String... args) {
        int port = Protocol.DEFAULT_PORT;
        int maxClients = DEFAULT_MAX_CLIENTS;
        int monitorInterval = DEFAULT_MONITOR_INTERVAL_SECONDS;
        for (String arg : args) {
            int separator = arg.indexOf('=');
            String name = separator < 0 ? arg : arg.substring(0, separator);
            String value = separator < 0 ? "" : arg.substring(separator + 1);
            switch (name) {
                case "--port" -> port = parseNumber(name, value);
                case "--max-clients" -> maxClients = parseNumber(name, value);
                case "--monitor-interval" -> monitorInterval = parseNumber(name, value);
                default -> throw new IllegalArgumentException("Unknown option " + arg + ". " + USAGE);
            }
        }
        try {
            return new ServerConfig(port, maxClients, monitorInterval);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() + ". " + USAGE, e);
        }
    }

    private static int parseNumber(String name, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " needs a whole number. " + USAGE, e);
        }
    }
}
