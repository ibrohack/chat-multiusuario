package com.psp.chat.client;

import com.psp.chat.client.model.ChatSession;
import com.psp.chat.client.model.ConnectionState;
import com.psp.chat.client.view.MainController;
import java.io.IOException;
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
 */
public final class ChatClientApp extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(ChatClientApp.class);

    private ChatSession session;

    /**
     * Creates the application; JavaFX instantiates it when the client starts.
     */
    public ChatClientApp() {
        // The window is built in start(Stage).
    }

    @Override
    public void start(Stage stage) throws IOException {
        session = new ChatSession(Platform::runLater);
        FXMLLoader loader = new FXMLLoader(ChatClientApp.class.getResource("view/main-view.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 800, 560);
        scene.getStylesheets().add(ChatClientApp.class.getResource("view/styles.css").toExternalForm());
        stage.setScene(scene);
        MainController controller = loader.getController();
        controller.configure(session, stage);
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
}
