package com.psp.chat.client.view;

import static com.psp.chat.client.view.FxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.psp.chat.client.model.ChatSession;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Smoke test of the FXML files and their controllers on a real JavaFX toolkit.
 */
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ViewLoadingTest {

    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        FxTestSupport.start();
    }

    @ParameterizedTest
    @ValueSource(strings = {"selector-view.fxml", "main-view.fxml", "classic-view.fxml", "advanced-view.fxml"})
    void everyFxmlLoadsWithItsController(String fxml) throws Exception {
        Object controller = onFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(MainController.class.getResource(fxml));
            loader.load();
            return loader.getController();
        });

        assertNotNull(controller);
    }

    @Test
    void mainWindowSwitchesViewsWithoutLosingTheSession() throws Exception {
        ChatSession session = new ChatSession(Platform::runLater);

        String title = onFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(MainController.class.getResource("main-view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            MainController controller = loader.getController();
            controller.configure(session, stage, ViewType.CLASSIC);
            controller.showView(ViewType.ADVANCED);
            controller.showView(ViewType.CLASSIC);
            return stage.getTitle();
        });

        assertEquals("Chat multiusuario", title);
    }
}
