package com.psp.chat.client;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Entry point of the JavaFX chat client.
 */
public final class ChatClientApp extends Application {

    /**
     * Creates the application; JavaFX instantiates it when the client starts.
     */
    public ChatClientApp() {
        // Nothing to initialize: the window is built in start(Stage).
    }

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(ChatClientApp.class.getResource("view/chat.fxml"));
        stage.setScene(new Scene(root, 640, 480));
        stage.show();
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
