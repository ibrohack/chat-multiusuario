package com.psp.chat.client.view;

import com.psp.chat.client.model.ChatEntry;
import com.psp.chat.client.model.UiText;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

/**
 * List cell that renders a {@link ChatEntry} with a style that depends on its type.
 *
 * <p>Image entries show their header line and a thumbnail; clicking the thumbnail
 * opens the full image.</p>
 */
public final class ChatEntryCell extends ListCell<ChatEntry> {

    private static final List<String> ENTRY_STYLES =
            List.of("entry-public", "entry-private", "entry-system", "entry-error");
    private static final double THUMBNAIL_SIZE = 240;

    /** Decoded thumbnails per image; cells only run on the JavaFX thread, so no locking is needed. */
    private static final Map<byte[], Image> THUMBNAILS = new WeakHashMap<>();

    private final boolean showScope;

    /**
     * Creates a cell.
     *
     * @param showScope whether messages show the {@code [PÚBLICO]} or {@code [PRIVADO]} tag
     */
    public ChatEntryCell(boolean showScope) {
        this.showScope = showScope;
        setWrapText(true);
		setPrefWidth(0);
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
        getStyleClass().add(switch (entry.type()) {
            case PUBLIC -> "entry-public";
            case PRIVATE -> "entry-private";
            case SYSTEM -> "entry-system";
            case ERROR -> "entry-error";
        });
        String text = EntryFormatter.format(entry, showScope);
        if (!entry.isImage()) {
            setText(text);
            return;
        }
        setText(null);
        Label header = new Label(text);
        header.setWrapText(true);
        setGraphic(new VBox(4, header, imageNode(entry)));
    }

    private Node imageNode(ChatEntry entry) {
        Image thumbnail = THUMBNAILS.computeIfAbsent(entry.image(),
                data -> new Image(new ByteArrayInputStream(data), THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, true));
        if (thumbnail.isError()) {
            return new Label(UiText.INVALID_IMAGE_PLACEHOLDER);
        }
        ImageView view = new ImageView(thumbnail);
        view.setCursor(Cursor.HAND);
        view.setOnMouseClicked(event -> ImageViewer.show(getScene().getWindow(), entry));
        return view;
    }
}
