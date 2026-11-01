package com.psp.chat.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class MonitorThreadTest {

    @Test
    void formatsStatusWithLastMessage() {
        RoomStatus status = new RoomStatus(3, 10,
                new MessageRecord(LocalTime.of(12, 1, 22), "ana", null, "hola"), 5);

        assertEquals("Online users: 3/10 | Uptime: 01:02:05 | Last message: [12:01:22] ana -> all: hola",
                MonitorThread.format(status, Duration.ofSeconds(3725)));
    }

    @Test
    void formatsStatusWithoutMessages() {
        assertEquals("Online users: 0/10 | Uptime: 00:00:00 | Last message: (none)",
                MonitorThread.format(new RoomStatus(0, 10, null, 0), Duration.ZERO));
    }

    @Test
    void formatsUptimeBeyond24Hours() {
        assertTrue(MonitorThread.format(new RoomStatus(0, 10, null, 0), Duration.ofHours(25))
                .contains("Uptime: 25:00:00"));
    }

    @Test
    void stopsWhenInterrupted() throws InterruptedException {
        MonitorThread monitor = new MonitorThread(new ChatRoom(1), Instant.now(), Duration.ofHours(1));
        monitor.start();

        monitor.interrupt();
        monitor.join(2_000);

        assertFalse(monitor.isAlive());
    }
}
