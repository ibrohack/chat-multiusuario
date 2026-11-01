package com.psp.chat.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PacketTest {

    private static final Instant TIME = Instant.parse("2026-11-01T10:00:00Z");

    static Object roundTrip(Object value) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return in.readObject();
        }
    }

    @Test
    void everyPacketSurvivesSerialization() throws Exception {
        List<Packet> packets = List.of(
                new LoginRequest("Duxon"),
                new TextRequest(null, "hola"),
                new TextRequest("admin", "secreto"),
                new LogoutRequest(),
                new LoginAccepted("Duxon", List.of("Duxon", "admin")),
                new LoginRejected(RejectionReason.SERVER_FULL),
                new TextMessage("Duxon", null, "Ñandú áéí 🙂", TIME),
                new UserEvent(UserEventType.JOINED, "admin", List.of("Duxon", "admin")),
                new ErrorNotice(ErrorCode.USER_NOT_CONNECTED, "luis"),
                new ServerShutdown());

        for (Packet packet : packets) {
            assertEquals(packet, roundTrip(packet));
        }
    }

    @Test
    void isPrivateOnlyWithRecipient() {
        assertFalse(new TextMessage("a", null, "t", TIME).isPrivate());
        assertTrue(new TextMessage("a", "b", "t", TIME).isPrivate());
    }

    @Test
    void loginAcceptedKeepsAnImmutableCopy() {
        List<String> source = new ArrayList<>(List.of("ana"));
        LoginAccepted accepted = new LoginAccepted("ana", source);

        source.add("luis");

        assertEquals(List.of("ana"), accepted.onlineUsers());
        assertThrows(UnsupportedOperationException.class, () -> accepted.onlineUsers().add("eva"));
    }

    @Test
    void mandatoryComponentsRejectNull() {
        assertThrows(NullPointerException.class, () -> new LoginRequest(null));
        assertThrows(NullPointerException.class, () -> new TextRequest("a", null));
        assertThrows(NullPointerException.class, () -> new ErrorNotice(null, "x"));
    }
}
