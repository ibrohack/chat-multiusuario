package com.psp.chat.client.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.psp.chat.client.model.ChatEntry;
import com.psp.chat.client.model.EntryType;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class EntryFormatterTest {

    private static final LocalTime TIME = LocalTime.of(12, 1);

    @Test
    void publicMessageShowsScopeAndSender() {
        ChatEntry entry = new ChatEntry(EntryType.PUBLIC, "ana", null, "hola", null, TIME);

        assertEquals("[12:01] [PÚBLICO] ana: hola", EntryFormatter.format(entry, true));
    }

    @Test
    void privateMessageShowsScopeSenderAndRecipient() {
        ChatEntry entry = new ChatEntry(EntryType.PRIVATE, "ana", "luis", "hola", null, TIME);

        assertEquals("[12:01] [PRIVADO] ana → luis: hola", EntryFormatter.format(entry, true));
    }

    @Test
    void scopeCanBeHidden() {
        ChatEntry entry = new ChatEntry(EntryType.PUBLIC, "ana", null, "hola", null, TIME);

        assertEquals("[12:01] ana: hola", EntryFormatter.format(entry, false));
    }

    @Test
    void systemLineIsFramed() {
        ChatEntry entry = new ChatEntry(EntryType.SYSTEM, null, null, "luis se ha conectado", null, TIME);

        assertEquals("— luis se ha conectado —", EntryFormatter.format(entry, true));
    }

    @Test
    void errorLineHasWarningSign() {
        ChatEntry entry = new ChatEntry(EntryType.ERROR, null, null, "Se ha perdido la conexión con el servidor.", null, TIME);

        assertEquals("⚠ Se ha perdido la conexión con el servidor.", EntryFormatter.format(entry, true));
    }
}
