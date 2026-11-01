package com.psp.chat.server;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Daemon thread that periodically reports the server statistics on the console.
 *
 * <p>Each report shows the connected users, the uptime and the last message.
 * The thread ends when it is interrupted.</p>
 */
final class MonitorThread extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger("monitor");

    private final ChatRoom room;
    private final Instant startTime;
    private final Duration interval;

    /**
     * Creates the monitor; call {@link #start()} to begin reporting.
     *
     * @param room      room whose statistics are reported
     * @param startTime moment the server started, used to compute the uptime
     * @param interval  time between two reports
     */
    MonitorThread(ChatRoom room, Instant startTime, Duration interval) {
        super("monitor");
        setDaemon(true);
        this.room = room;
        this.startTime = startTime;
        this.interval = interval;
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                Thread.sleep(interval.toMillis());
                LOG.info(format(room.getStatus(), Duration.between(startTime, Instant.now())));
            }
        } catch (InterruptedException e) {
            // Interruption is the stop signal: end the thread.
        }
    }

    /**
     * Formats one report line.
     *
     * @param status room statistics
     * @param uptime time since the server started
     * @return a line like {@code Online users: 3/10 | Uptime: 01:02:05 | Last message: ...}
     */
    static String format(RoomStatus status, Duration uptime) {
        long seconds = uptime.getSeconds();
        String time = String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
        String lastMessage = status.lastMessage() == null ? "(none)" : status.lastMessage().toString();
        return "Online users: " + status.onlineUsers() + "/" + status.maxClients()
                + " | Uptime: " + time
                + " | Last message: " + lastMessage;
    }
}
