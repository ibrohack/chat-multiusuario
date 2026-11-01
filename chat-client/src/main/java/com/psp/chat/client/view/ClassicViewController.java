package com.psp.chat.client.view;

import com.psp.chat.client.model.ChatEntry;
import com.psp.chat.client.model.ChatSession;
import com.psp.chat.client.model.ConnectionState;
import com.psp.chat.client.model.UiText;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

/**
 * Classic view: one message list for every conversation and the «Privado» / «Para»
 * controls of the course screenshot.
 */
public final class ClassicViewController implements ChatView {

    @FXML
    private ListView<ChatEntry> messageList;

    @FXML
    private CheckBox privateCheckBox;

    @FXML
    private TextField recipientField;

    @FXML
    private TextField messageField;

    @FXML
    private Button sendButton;

    private ChatSession session;
    private final ListChangeListener<ChatEntry> autoScroll =
            change -> messageList.scrollTo(messageList.getItems().size() - 1);

    /**
     * Creates the controller; {@code FXMLLoader} instantiates it when loading the view.
     */
    public ClassicViewController() {
        // Fields are injected by FXMLLoader.
    }

    @Override
    public void attach(ChatSession session) {
        this.session = session;
        messageList.setCellFactory(list -> new ChatEntryCell(true));
        messageList.setItems(session.getAllEntries());
        messageList.getItems().addListener(autoScroll);
        BooleanBinding disconnected = session.stateProperty().isNotEqualTo(ConnectionState.CONNECTED);
        privateCheckBox.disableProperty().bind(disconnected);
        recipientField.disableProperty().bind(disconnected.or(privateCheckBox.selectedProperty().not()));
        messageField.disableProperty().bind(disconnected);
        sendButton.disableProperty().bind(disconnected);
    }

    @Override
    public void detach() {
        messageList.getItems().removeListener(autoScroll);
        messageList.setItems(null);
        privateCheckBox.disableProperty().unbind();
        recipientField.disableProperty().unbind();
        messageField.disableProperty().unbind();
        sendButton.disableProperty().unbind();
    }

    @FXML
    private void onSend() {
        String text = messageField.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        String recipient = null;
        if (privateCheckBox.isSelected()) {
            recipient = recipientField.getText() == null ? "" : recipientField.getText().trim();
            if (recipient.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, UiText.RECIPIENT_REQUIRED).show();
                return;
            }
        }
        if (session.sendText(recipient, text)) {
            messageField.clear();
        }
    }
}
