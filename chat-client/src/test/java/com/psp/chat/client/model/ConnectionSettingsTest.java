package com.psp.chat.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConnectionSettingsTest {

    @Test
    void appliesDefaultsAndTrims() {
        assertEquals(new ConnectionSettings("127.0.0.1", 5000, "ana"), ConnectionSettings.parse(" ", "", " ana "));
    }

    @Test
    void acceptsExplicitValues() {
        assertEquals(new ConnectionSettings("192.168.1.10", 6000, "Duxon"),
                ConnectionSettings.parse("192.168.1.10", "6000", "Duxon"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "70000"})
    void rejectsInvalidPort(String port) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ConnectionSettings.parse("", port, "ana"));

        assertEquals("El puerto debe ser un número entre 1 y 65535.", error.getMessage());
    }

    @Test
    void rejectsInvalidUsername() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ConnectionSettings.parse("", "", "a b"));

        assertEquals("Nombre no válido: usa de 3 a 16 letras, números o _.", error.getMessage());
    }
}
