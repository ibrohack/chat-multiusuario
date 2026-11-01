package com.psp.chat.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ProtocolTest {

    @ParameterizedTest
    @ValueSource(strings = {"Duxon", "admin", "abc", "José_1"})
    void acceptsValidUsernames(String username) {
        assertTrue(Protocol.isValidUsername(username));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ab", "a b", "ana!", "xxxxxxxxxxxxxxxxx"})
    void rejectsInvalidUsernames(String username) {
        assertFalse(Protocol.isValidUsername(username));
    }

    @Test
    void validatesImages() {
        assertFalse(Protocol.isValidImage(null));
        assertFalse(Protocol.isValidImage(new byte[0]));
        assertTrue(Protocol.isValidImage(new byte[Protocol.MAX_IMAGE_BYTES]));
        assertFalse(Protocol.isValidImage(new byte[Protocol.MAX_IMAGE_BYTES + 1]));
    }

    @Test
    void validatesText() {
        assertFalse(Protocol.isValidText(null));
        assertFalse(Protocol.isValidText(""));
        assertFalse(Protocol.isValidText("   "));
        assertTrue(Protocol.isValidText("x".repeat(1000)));
        assertFalse(Protocol.isValidText("x".repeat(1001)));
    }
}
