package com.psp.chat.client.view;

import com.psp.chat.client.model.ChatEntry;
import com.psp.chat.client.model.ChatSession;
import com.psp.chat.client.model.ConnectionState;
import com.psp.chat.client.model.UiText;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;

/**
 * Advanced view: the connected users on the left and one tab per conversation.
 */
public final class AdvancedViewController implements ChatView {

    @FXML
    private Label onlineUsersLabel;

    @FXML
    private ListView<String> userList;

    @FXML
    private TabPane conversationTabs;

    @FXML
    private ListView<ChatEntry> generalList;

    @FXML
    private TextField messageField;

    @FXML
    private Button sendButton;

    private ChatSession session;
    private final ListChangeListener<ChatEntry> generalAutoScroll =
            change -> generalList.scrollTo(generalList.getItems().size() - 1);

    /**
     * Creates the controller; {@code FXMLLoader} instantiates it when loading the view.
     */
    public AdvancedViewController() {
        // Fields are injected by FXMLLoader.
    }

    @Override
    public void attach(ChatSession session) {
        this.session = session;
        ObservableList<String> onlineUsers = session.getOnlineUsers();
        userList.setItems(onlineUsers);
        onlineUsersLabel.textProperty().bind(Bindings.createStringBinding(
                () -> UiText.onlineUsers(onlineUsers.size()), onlineUsers));
        generalList.setCellFactory(list -> new ChatEntryCell(false));
        generalList.setItems(session.getConversation(ChatSession.GENERAL));
        generalList.getItems().addListener(generalAutoScroll);
        BooleanBinding disconnected = session.stateProperty().isNotEqualTo(ConnectionState.CONNECTED);
        messageField.disableProperty().bind(disconnected);
        sendButton.disableProperty().bind(disconnected);
    }

    @Override
    public void detach() {
        generalList.getItems().removeListener(generalAutoScroll);
        generalList.setItems(null);
        userList.setItems(null);
        onlineUsersLabel.textProperty().unbind();
        messageField.disableProperty().unbind();
        sendButton.disableProperty().unbind();
    }

    @FXML
    private void onSend() {
        String text = messageField.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        if (session.sendText(null, text)) {
            messageField.clear();
        }
    }
}
