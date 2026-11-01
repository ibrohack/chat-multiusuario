package com.psp.chat.client.view;

import com.psp.chat.client.model.ChatEntry;
import java.io.ByteArrayInputStream;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Window that shows an image of the chat at full size.
 */
final class ImageViewer {

    private static final double MARGIN = 20;
    private static final double MAX_SCREEN_FRACTION = 0.9;

    private ImageViewer() {
        // Static helper only.
    }

    /**
     * Opens a window with the image of an entry, at most 90% of the screen; larger images scroll.
     *
     * @param owner window that owns the viewer
     * @param entry image entry to show
     */
    static void show(Window owner, ChatEntry entry) {
        Image image = new Image(new ByteArrayInputStream(entry.image()));
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double width = Math.min(image.getWidth() + MARGIN, screen.getWidth() * MAX_SCREEN_FRACTION);
        double height = Math.min(image.getHeight() + MARGIN, screen.getHeight() * MAX_SCREEN_FRACTION);
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle(entry.text() + " — " + entry.sender());
        stage.setScene(new Scene(new ScrollPane(new ImageView(image)), width, height));
        stage.show();
    }
}
