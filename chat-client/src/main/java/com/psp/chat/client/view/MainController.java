package com.psp.chat.client.view;

import com.psp.chat.client.model.ChatSession;
import com.psp.chat.client.model.ConnectionSettings;
import com.psp.chat.client.model.ConnectionState;
import com.psp.chat.client.model.UiText;
import java.io.IOException;
import java.io.UncheckedIOException;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Controller of the main window: the connection bar shared by both views and the
 * area where the current chat view is shown.
 */
public final class MainController {

    @FXML
    private RadioMenuItem classicViewItem;

    @FXML
    private RadioMenuItem advancedViewItem;

    @FXML
    private TextField ipField;

    @FXML
    private TextField portField;

    @FXML
    private TextField usernameField;

    @FXML
    private Button connectButton;

    @FXML
    private Button disconnectButton;

    @FXML
    private Label statusLabel;

    @FXML
    private BorderPane contentPane;

    private ChatSession session;
    private ChatView currentView;

    /**
     * Creates the controller; {@code FXMLLoader} instantiates it when loading the window.
     */
    public MainController() {
        // Fields are injected by FXMLLoader.
    }

    /**
     * Binds the window to the session and shows the first view.
     *
     * @param session     session shared by every view
     * @param stage       window whose title shows the logged-in user
     * @param initialView view chosen in the start-up selector
     */
    public void configure(ChatSession session, Stage stage, ViewType initialView) {
        this.session = session;
        BooleanBinding disconnected = session.stateProperty().isEqualTo(ConnectionState.DISCONNECTED);
        BooleanBinding connected = session.stateProperty().isEqualTo(ConnectionState.CONNECTED);
        ipField.disableProperty().bind(disconnected.not());
        portField.disableProperty().bind(disconnected.not());
        usernameField.disableProperty().bind(disconnected.not());
        connectButton.disableProperty().bind(disconnected.not());
        disconnectButton.disableProperty().bind(connected.not());
        statusLabel.textProperty().bind(Bindings.createStringBinding(
                this::statusText, session.stateProperty(), session.usernameProperty()));
        stage.titleProperty().bind(Bindings.createStringBinding(
                this::windowTitle, session.stateProperty(), session.usernameProperty()));
        session.usernameProperty().addListener((property, oldName, newName) -> usernameField.setText(newName));
        session.setOnError(text -> new Alert(Alert.AlertType.ERROR, text).show());
        showView(initialView);
    }

    /**
     * Replaces the current view, keeping the session and its history.
     *
     * @param view view to show
     */
    public void showView(ViewType view) {
        if (currentView != null) {
            currentView.detach();
        }
        FXMLLoader loader = new FXMLLoader(MainController.class.getResource(view.getFxml()));
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load the view " + view.getFxml(), e);
        }
        currentView = loader.getController();
        currentView.attach(session);
        contentPane.setCenter(root);
        (view == ViewType.CLASSIC ? classicViewItem : advancedViewItem).setSelected(true);
    }

    @FXML
    private void onClassicView() {
        showView(ViewType.CLASSIC);
    }

    @FXML
    private void onAdvancedView() {
        showView(ViewType.ADVANCED);
    }

    @FXML
    private void onConnect() {
        ConnectionSettings settings;
        try {
            settings = ConnectionSettings.parse(ipField.getText(), portField.getText(), usernameField.getText());
        } catch (IllegalArgumentException e) {
            new Alert(Alert.AlertType.WARNING, e.getMessage()).show();
            return;
        }
        session.connect(settings);
    }

    @FXML
    private void onDisconnect() {
        session.disconnect();
    }

    private String statusText() {
        return switch (session.getState()) {
            case DISCONNECTED -> UiText.STATUS_DISCONNECTED;
            case CONNECTING -> UiText.STATUS_CONNECTING;
            case CONNECTED -> UiText.connectedStatus(session.getUsername());
        };
    }

    private String windowTitle() {
        return session.getState() == ConnectionState.CONNECTED
                ? UiText.windowTitle(session.getUsername())
                : UiText.APP_TITLE;
    }
}
