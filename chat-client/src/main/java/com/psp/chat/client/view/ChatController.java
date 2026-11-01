package com.psp.chat.client.view;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Controller of the chat window defined in {@code chat.fxml}.
 */
public class ChatController {

    @FXML
    private Button connectButton;

    @FXML
    private Button disconnectButton;

    @FXML
    private Button sendButton;

    @FXML
    private Label statusLabel;

    @FXML
    private TextField ipField;

    @FXML
    private TextField messageField;

    @FXML
    private TextField recipientField;

    @FXML
    private CheckBox privateCheckBox;

    @FXML
    private TextField portField;

    @FXML
    private TextField usernameField;

    /**
     * Creates the controller; {@code FXMLLoader} instantiates it when loading the view.
     */
    public ChatController() {
        // Fields are injected by FXMLLoader.
    }
}
