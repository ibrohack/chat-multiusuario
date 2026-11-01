package com.psp.chat.client.view;

import com.psp.chat.client.model.ChatEntry;
import java.util.List;
import javafx.scene.control.ListCell;

/**
 * List cell that renders a {@link ChatEntry} with a style that depends on its type.
 */
public final class ChatEntryCell extends ListCell<ChatEntry> {

    private static final List<String> ENTRY_STYLES =
            List.of("entry-public", "entry-private", "entry-system", "entry-error");

    private final boolean showScope;

    /**
     * Creates a cell.
     *
     * @param showScope whether messages show the {@code [PÚBLICO]} or {@code [PRIVADO]} tag
     */
    public ChatEntryCell(boolean showScope) {
        this.showScope = showScope;
        setWrapText(true);
    }

    @Override
    protected void updateItem(ChatEntry entry, boolean empty) {
        super.updateItem(entry, empty);
        getStyleClass().removeAll(ENTRY_STYLES);
        setGraphic(null);
        if (empty || entry == null) {
            setText(null);
            return;
        }
        setText(EntryFormatter.format(entry, showScope));
        getStyleClass().add(switch (entry.type()) {
            case PUBLIC -> "entry-public";
            case PRIVATE -> "entry-private";
            case SYSTEM -> "entry-system";
            case ERROR -> "entry-error";
        });
    }
}
