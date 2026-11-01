package com.psp.chat.client;

import com.psp.chat.client.model.ChatSession;
import com.psp.chat.client.model.ConnectionState;
import com.psp.chat.client.model.UiText;
import com.psp.chat.client.view.MainController;
import com.psp.chat.client.view.SelectorController;
import com.psp.chat.client.view.ViewType;
import java.io.IOException;
import java.io.UncheckedIOException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point of the JavaFX chat client.
 *
 * <p>Shows the start-up selector first; the chosen view then opens in the main
 * window, where the "Vista" menu switches views without losing the session.</p>
 */
public final class ChatClientApp extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(ChatClientApp.class);
    private static final String STYLESHEET = "view/styles.css";

    private ChatSession session;

    /**
     * Creates the application; JavaFX instantiates it when the client starts.
     */
    public ChatClientApp() {
        // The windows are built in start(Stage).
    }

    @Override
    public void start(Stage stage) throws IOException {
        session = new ChatSession(Platform::runLater);
        FXMLLoader loader = new FXMLLoader(ChatClientApp.class.getResource("view/selector-view.fxml"));
        Parent selector = loader.load();
        SelectorController controller = loader.getController();
        controller.setOnSelect(view -> showMainWindow(stage, view));
        stage.setTitle(UiText.APP_TITLE);
        stage.setScene(styledScene(selector, 460, 240));
        stage.show();
        LOG.info("Client started");
    }

    @Override
    public void stop() {
        if (session != null && session.getState() == ConnectionState.CONNECTED) {
            session.disconnect();
        }
        LOG.info("Client stopped");
    }

    /**
     * Launches the chat client.
     *
     * @param args command-line arguments forwarded to JavaFX
     */
    public static void main(String[] args) {
        launch(args);
    }

    private void showMainWindow(Stage stage, ViewType view) {
        FXMLLoader loader = new FXMLLoader(ChatClientApp.class.getResource("view/main-view.fxml"));
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load the main window", e);
        }
        stage.setScene(styledScene(root, 800, 560));
        MainController controller = loader.getController();
        controller.configure(session, stage, view);
        stage.centerOnScreen();
        LOG.info("Opened the {} view", view);
    }

    private static Scene styledScene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(ChatClientApp.class.getResource(STYLESHEET).toExternalForm());
        return scene;
    }
}
