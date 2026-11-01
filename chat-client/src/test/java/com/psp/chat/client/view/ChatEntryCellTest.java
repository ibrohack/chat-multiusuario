package com.psp.chat.client.view;

import static com.psp.chat.client.view.FxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.psp.chat.client.model.ChatEntry;
import com.psp.chat.client.model.EntryType;
import java.time.LocalTime;
import java.util.Base64;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ChatEntryCellTest {

    /** A valid 1x1 PNG image. */
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    private static final LocalTime TIME = LocalTime.of(12, 1);

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        FxTestSupport.start();
    }

    private static ChatEntryCell render(ChatEntry entry) {
        ChatEntryCell cell = new ChatEntryCell(true);
        cell.updateItem(entry, false);
        return cell;
    }

    private static boolean contains(Node node, Class<? extends Node> type) {
        if (type.isInstance(node)) {
            return true;
        }
        return node instanceof Parent parent
                && parent.getChildrenUnmodifiable().stream().anyMatch(child -> contains(child, type));
    }

    private static boolean containsLabel(Node node, String text) {
        if (node instanceof Label label && text.equals(label.getText())) {
            return true;
        }
        return node instanceof Parent parent
                && parent.getChildrenUnmodifiable().stream().anyMatch(child -> containsLabel(child, text));
    }

    @Test
    void textEntryHasTextAndNoGraphic() throws Exception {
        ChatEntryCell cell = onFxThread(() ->
                render(new ChatEntry(EntryType.PUBLIC, "ana", null, "hola", null, TIME)));

        assertEquals("[12:01] [PÚBLICO] ana: hola", cell.getText());
        assertNull(cell.getGraphic());
    }

    @Test
    void imageEntryShowsItsHeaderAndAThumbnail() throws Exception {
        ChatEntryCell cell = onFxThread(() ->
                render(new ChatEntry(EntryType.PUBLIC, "luis", null, "foto.png", PNG, TIME)));

        assertNull(cell.getText());
        assertTrue(containsLabel(cell.getGraphic(), "[12:01] [PÚBLICO] luis: foto.png"));
        assertTrue(contains(cell.getGraphic(), ImageView.class));
    }

    @Test
    void brokenImageShowsAPlaceholder() throws Exception {
        ChatEntryCell cell = onFxThread(() ->
                render(new ChatEntry(EntryType.PUBLIC, "luis", null, "roto.png", new byte[] {1, 2, 3}, TIME)));

        assertTrue(containsLabel(cell.getGraphic(), "[imagen no válida]"));
        assertFalse(contains(cell.getGraphic(), ImageView.class));
    }
}
