package com.psp.chat.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ServerConfigTest {

    @Test
    void usesDefaults() {
        assertEquals(new ServerConfig(5000, 10, 10), ServerConfig.fromArgs());
    }

    @Test
    void readsAllOptions() {
        assertEquals(new ServerConfig(6000, 3, 5),
                ServerConfig.fromArgs("--port=6000", "--max-clients=3", "--monitor-interval=5"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"--max-clients=0", "--port=70000", "--monitor-interval=0", "--port=abc", "--other=1"})
    void rejectsInvalidOptionsShowingTheUsage(String arg) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> ServerConfig.fromArgs(arg));

        assertTrue(error.getMessage().contains("Usage: --port=5000 --max-clients=10 --monitor-interval=10"),
                error.getMessage());
    }
}
