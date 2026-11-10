package com.psp.chat.client.view;

import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Keyboard rule of the message box in both views: Enter sends the message and
 * Shift+Enter adds a line break.
 */
final class SendKeyHandler {

    private SendKeyHandler() {
        // Static helpers only.
    }

    /**
     * Applies the rule to a message box.
     *
     * <p>A filter runs before the default handling of the box, where Enter adds a line
     * break and Shift+Enter does nothing, so both cases are decided here.</p>
     *
     * @param area message box
     * @param send action that sends the message
     */
    static void install(TextArea area, Runnable send) {
        area.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.ENTER) {
                return;
            }
            event.consume();
            if (event.isShiftDown()) {
                area.replaceSelection("\n");
            } else {
                send.run();
            }
        });
    }
}