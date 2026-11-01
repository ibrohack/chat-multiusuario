package com.psp.chat.client.view;

import static com.psp.chat.client.view.FxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.psp.chat.client.model.ChatSession;
import com.psp.chat.client.model.TestSessions;
import com.psp.chat.core.TextMessage;
import com.psp.chat.core.UserEvent;
import com.psp.chat.core.UserEventType;
import java.time.Instant;
import java.util.List;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class AdvancedViewTest {

    private static final Instant TIME = Instant.parse("2026-11-01T10:00:00Z");

    private ChatSession session;
    private AdvancedViewController controller;
    private Parent root;

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        FxTestSupport.start();
    }

    @BeforeEach
    void loadViewForAna() throws Exception {
        onFxThread(() -> {
            // Events are applied immediately; tests deliver them on the JavaFX thread.
            session = TestSessions.loggedIn("ana", List.of("ana", "eva", "luis"));
            loadView();
            return null;
        });
    }

    private void loadView() throws Exception {
        FXMLLoader loader = new FXMLLoader(MainController.class.getResource("advanced-view.fxml"));
        root = loader.load();
        controller = loader.getController();
        controller.attach(session);
    }

    private TabPane tabs() {
        return (TabPane) root.lookup("#conversationTabs");
    }

    private TextField messageField() {
        return (TextField) root.lookup("#messageField");
    }

    private List<String> tabTitles() {
        return tabs().getTabs().stream().map(Tab::getText).toList();
    }

    private Tab tab(String title) {
        return tabs().getTabs().stream()
                .filter(tab -> tab.getText().startsWith(title))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void incomingPrivateCreatesTabWithoutSelectingIt() throws Exception {
        List<String> titles = onFxThread(() -> {
            session.packetReceived(new TextMessage("luis", "ana", "hey", TIME));
            return tabTitles();
        });

        assertEquals(List.of("General", "luis (1)"), titles);
        assertEquals("General", onFxThread(() -> tabs().getSelectionModel().getSelectedItem().getText()));
    }

    @Test
    void selectingATabClearsItsUnreadCount() throws Exception {
        List<String> titles = onFxThread(() -> {
            session.packetReceived(new TextMessage("luis", "ana", "hey", TIME));
            tabs().getSelectionModel().select(tab("luis"));
            return tabTitles();
        });

        assertEquals(List.of("General", "luis"), titles);
    }

    @Test
    void generalCountsUnreadWhileAnotherTabIsSelected() throws Exception {
        List<String> titles = onFxThread(() -> {
            session.packetReceived(new TextMessage("luis", "ana", "hey", TIME));
            tabs().getSelectionModel().select(tab("luis"));
            session.packetReceived(new TextMessage("eva", null, "hola", TIME));
            return tabTitles();
        });

        assertEquals(List.of("General (1)", "luis"), titles);
    }

    @Test
    void recipientAndPromptFollowTheSelectedTab() throws Exception {
        assertNull(onFxThread(() -> controller.currentRecipient()));
        assertEquals("Mensaje para General", onFxThread(() -> messageField().getPromptText()));

        String recipient = onFxThread(() -> {
            session.packetReceived(new TextMessage("luis", "ana", "hey", TIME));
            tabs().getSelectionModel().select(tab("luis"));
            return controller.currentRecipient();
        });

        assertEquals("luis", recipient);
        assertEquals("Mensaje privado para luis", onFxThread(() -> messageField().getPromptText()));
    }

    @Test
    void offlinePeerTabIsMarkedAndDisablesSending() throws Exception {
        onFxThread(() -> {
            session.packetReceived(new TextMessage("luis", "ana", "hey", TIME));
            tabs().getSelectionModel().select(tab("luis"));
            session.packetReceived(new UserEvent(UserEventType.LEFT, "luis", List.of("ana", "eva")));
            return null;
        });

        assertTrue(onFxThread(() -> tab("luis").getStyleClass().contains("tab-offline")));
        assertTrue(onFxThread(() -> messageField().isDisabled()));

        onFxThread(() -> {
            session.packetReceived(new UserEvent(UserEventType.JOINED, "luis", List.of("ana", "eva", "luis")));
            return null;
        });

        assertFalse(onFxThread(() -> tab("luis").getStyleClass().contains("tab-offline")));
        assertFalse(onFxThread(() -> messageField().isDisabled()));
    }

    @Test
    void openingAConversationSelectsItsTab() throws Exception {
        String selected = onFxThread(() -> {
            controller.openConversation("eva");
            return tabs().getSelectionModel().getSelectedItem().getText();
        });

        assertEquals("eva", selected);
        assertEquals(List.of("General", "eva"), onFxThread(this::tabTitles));
    }

    @Test
    void existingConversationsGetTabsWhenTheViewAttaches() throws Exception {
        List<String> titles = onFxThread(() -> {
            session.packetReceived(new TextMessage("luis", "ana", "hey", TIME));
            controller.detach();
            loadView();
            return tabTitles();
        });

        assertEquals(List.of("General", "luis"), titles);
    }
}
