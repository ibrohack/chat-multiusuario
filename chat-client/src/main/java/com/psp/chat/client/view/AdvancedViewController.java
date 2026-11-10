package com.psp.chat.client.view;

import com.psp.chat.client.model.ChatEntry;
import com.psp.chat.client.model.ChatSession;
import com.psp.chat.client.model.ConnectionState;
import com.psp.chat.client.model.UiText;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.Popup;

/**
 * Advanced view: the connected users on the left and one tab per conversation.
 *
 * <p>The recipient of a message is the user of the selected tab, so this view
 * needs no «Privado» or «Para» controls. Tabs of other conversations count
 * their unread lines, and tabs of users who left are greyed out.</p>
 */
public final class AdvancedViewController implements ChatView {

    private static final String OFFLINE_STYLE = "tab-offline";

    @FXML
    private Label onlineUsersLabel;

    @FXML
    private ListView<String> userList;

    @FXML
    private TabPane conversationTabs;

    @FXML
    private Tab generalTab;

    @FXML
    private ListView<ChatEntry> generalList;

    @FXML
    private Button attachButton;

    @FXML
    private TextArea messageField;

    @FXML
    private Button sendButton;

    @FXML private Button emojiButton;

    private Popup emojiPopup;

    private static final String[] EMOJIS = {
        "😀","😂","😍","😎","😛",
        "😢","😡","👍","👎","🙏",
        "👏","🔥","💓","💔","🎉",
        "🤔","😴","🙌","💯","✅",
        "🧙‍♂️","🤓","🤑","😘","❎"
    };



    private ChatSession session;
    private final Map<String, Tab> privateTabs = new HashMap<>();
    private final Map<Tab, Integer> unreadCounts = new HashMap<>();
    private final Map<Tab, Runnable> tabCleanups = new HashMap<>();
    private final List<Runnable> cleanups = new ArrayList<>();
    private final BooleanProperty selectedPeerOffline = new SimpleBooleanProperty(false);

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
        userList.setOnMouseClicked(event -> {
            String selected = userList.getSelectionModel().getSelectedItem();
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && selected != null) {
                openConversation(selected);
            }
        });
        onlineUsersLabel.textProperty().bind(Bindings.createStringBinding(
                () -> UiText.onlineUsers(onlineUsers.size()), onlineUsers));

        tabCleanups.put(generalTab, watchConversation(generalTab, generalList, ChatSession.GENERAL));
        for (String peer : session.getPrivateConversations()) {
            createTab(peer);
        }
        ListChangeListener<String> conversationsListener = change -> {
            while (change.next()) {
                change.getRemoved().forEach(this::removeTab);
                change.getAddedSubList().forEach(this::createTab);
            }
        };
        session.getPrivateConversations().addListener(conversationsListener);
        cleanups.add(() -> session.getPrivateConversations().removeListener(conversationsListener));

        ListChangeListener<String> usersListener = change -> refreshOfflineState();
        onlineUsers.addListener(usersListener);
        cleanups.add(() -> onlineUsers.removeListener(usersListener));

        ChangeListener<Tab> selectionListener = (property, previous, selected) -> onTabSelected(selected);
        conversationTabs.getSelectionModel().selectedItemProperty().addListener(selectionListener);
        cleanups.add(() -> conversationTabs.getSelectionModel().selectedItemProperty()
                .removeListener(selectionListener));

        BooleanBinding cannotSend = session.stateProperty().isNotEqualTo(ConnectionState.CONNECTED)
                .or(selectedPeerOffline);
        messageField.disableProperty().bind(cannotSend);
        sendButton.disableProperty().bind(cannotSend);
        attachButton.disableProperty().bind(cannotSend);
		SendKeyHandler.install(messageField, this::onSend);
        onTabSelected(conversationTabs.getSelectionModel().getSelectedItem());
    }

    @Override
    public void detach() {
        cleanups.forEach(Runnable::run);
        cleanups.clear();
        tabCleanups.values().forEach(Runnable::run);
        tabCleanups.clear();
        privateTabs.clear();
        unreadCounts.clear();
        userList.setItems(null);
        userList.setOnMouseClicked(null);
        onlineUsersLabel.textProperty().unbind();
        messageField.disableProperty().unbind();
        sendButton.disableProperty().unbind();
        attachButton.disableProperty().unbind();
    }

    /**
     * Opens the private conversation with a user, creating its tab if needed, and selects it.
     *
     * @param peer name of the other user
     */
    void openConversation(String peer) {
        Tab tab = createTab(peer);
        conversationTabs.getSelectionModel().select(tab);
    }

    /**
     * Returns the recipient of the next message.
     *
     * @return the user of the selected private tab, or {@code null} for the general tab
     */
    String currentRecipient() {
        Tab selected = conversationTabs.getSelectionModel().getSelectedItem();
        return selected == null || selected == generalTab ? null : (String) selected.getUserData();
    }

    @FXML
    private void onSend() {
        String text = messageField.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        if (session.sendText(currentRecipient(), text)) {
            messageField.clear();
        }
    }

    @FXML
    private void onAttach() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(UiText.CHOOSE_IMAGE);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(UiText.IMAGE_FILTER, "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = chooser.showOpenDialog(attachButton.getScene().getWindow());
        if (file != null) {
            session.sendImage(currentRecipient(), file.toPath());
        }
    }

    private Tab createTab(String peer) {
        Tab existing = privateTabs.get(peer);
        if (existing != null) {
            return existing;
        }
        ListView<ChatEntry> list = new ListView<>();
        Tab tab = new Tab(peer, list);
        tab.setUserData(peer);
        // Register the tab before touching the session: getConversation may announce a new conversation.
        privateTabs.put(peer, tab);
        tabCleanups.put(tab, watchConversation(tab, list, peer));
        tab.setOnClosed(event -> forgetTab(peer, tab));
        conversationTabs.getTabs().add(tab);
        refreshOfflineState();
        return tab;
    }

    private void removeTab(String peer) {
        Tab tab = privateTabs.get(peer);
        if (tab != null) {
            conversationTabs.getTabs().remove(tab);
            forgetTab(peer, tab);
        }
    }

    private void forgetTab(String peer, Tab tab) {
        privateTabs.remove(peer);
        unreadCounts.remove(tab);
        Runnable cleanup = tabCleanups.remove(tab);
        if (cleanup != null) {
            cleanup.run();
        }
    }

    /**
     * Shows a conversation in a list, scrolls to new lines and counts them while the tab is not selected.
     *
     * @return the action that stops watching the conversation
     */
    private Runnable watchConversation(Tab tab, ListView<ChatEntry> list, String conversation) {
        list.setCellFactory(view -> new ChatEntryCell(false));
        ObservableList<ChatEntry> entries = session.getConversation(conversation);
        list.setItems(entries);
        ListChangeListener<ChatEntry> listener = change -> {
            int added = 0;
            while (change.next()) {
                added += change.getAddedSize();
            }
            if (added > 0) {
                list.scrollTo(entries.size() - 1);
                if (conversationTabs.getSelectionModel().getSelectedItem() != tab) {
                    unreadCounts.merge(tab, added, Integer::sum);
                    tab.setText(UiText.unreadTitle(titleOf(tab), unreadCounts.get(tab)));
                }
            }
        };
        entries.addListener(listener);
        return () -> {
            entries.removeListener(listener);
            list.setItems(null);
        };
    }

    private void onTabSelected(Tab selected) {
        if (selected == null) {
            return;
        }
        unreadCounts.remove(selected);
        selected.setText(titleOf(selected));
        String recipient = currentRecipient();
        messageField.setPromptText(recipient == null ? UiText.GENERAL_PROMPT : UiText.privatePrompt(recipient));
        refreshOfflineState();
    }

    private void refreshOfflineState() {
        for (Map.Entry<String, Tab> entry : privateTabs.entrySet()) {
            boolean offline = !isOnline(entry.getKey());
            List<String> styles = entry.getValue().getStyleClass();
            if (offline && !styles.contains(OFFLINE_STYLE)) {
                styles.add(OFFLINE_STYLE);
            } else if (!offline) {
                styles.remove(OFFLINE_STYLE);
            }
        }
        String recipient = currentRecipient();
        selectedPeerOffline.set(recipient != null && !isOnline(recipient));
    }

    private boolean isOnline(String user) {
        for (String online : session.getOnlineUsers()) {
            if (online.equalsIgnoreCase(user)) {
                return true;
            }
        }
        return false;
    }

    private String titleOf(Tab tab) {
        return tab == generalTab ? UiText.GENERAL_TAB : (String) tab.getUserData();
    }
    
    @FXML
    private void onEmoji() {
        if (emojiPopup == null) 
            emojiPopup = buildEmojiPopup();
        if (emojiPopup.isShowing()) {
            emojiPopup.hide(); return; 
        }

        Bounds b = emojiButton.localToScreen(emojiButton.getBoundsInLocal());
        emojiPopup.show(emojiButton, b.getMinX(), b.getMinY());
        emojiPopup.setY(b.getMinY() - emojiPopup.getHeight() - 4);
    }

    private Popup buildEmojiPopup() {
        FlowPane grid = new FlowPane(4, 4);
        grid.setPrefWrapLength(240);
        grid.setStyle("-fx-background-color: -fx-base; -fx-padding: 8; "
                    + "-fx-border-color: gray; -fx-border-radius: 4; -fx-background-radius: 4;");

        for (String emoji : EMOJIS) {
            Button b = new Button(emoji);
            b.setStyle("-fx-font-size: 18; -fx-background-color: transparent;");
            b.setOnAction(e -> {
                messageField.insertText(messageField.getLength(), emoji);
                emojiPopup.hide();
            });
            grid.getChildren().add(b);
        }

        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(grid);
        return popup;
    }
}
