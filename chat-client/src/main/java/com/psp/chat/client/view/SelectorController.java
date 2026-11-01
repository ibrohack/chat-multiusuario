package com.psp.chat.client.view;

import java.util.Objects;
import java.util.function.Consumer;
import javafx.fxml.FXML;

/**
 * Controller of the start-up window where the user picks the classic or the advanced view.
 */
public final class SelectorController {

    private Consumer<ViewType> onSelect = view -> { };

    /**
     * Creates the controller; {@code FXMLLoader} instantiates it when loading the window.
     */
    public SelectorController() {
        // Buttons call the handlers below through FXML.
    }

    /**
     * Sets the action run when the user picks a view.
     *
     * @param action receives the chosen view
     */
    public void setOnSelect(Consumer<ViewType> action) {
        onSelect = Objects.requireNonNull(action, "action");
    }

    @FXML
    private void onClassicSelected() {
        onSelect.accept(ViewType.CLASSIC);
    }

    @FXML
    private void onAdvancedSelected() {
        onSelect.accept(ViewType.ADVANCED);
    }
}
