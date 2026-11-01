package com.psp.chat.client.view;

import com.psp.chat.client.model.ChatEntry;
import com.psp.chat.client.model.UiText;
import java.time.format.DateTimeFormatter;

/**
 * Turns chat lines into the text shown by the views.
 */
public final class EntryFormatter {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private EntryFormatter() {
        // Static helpers only.
    }

    /**
     * Formats a line, for instance {@code [12:01] [PRIVADO] ana → luis: hola}.
     *
     * @param entry     line to format
     * @param showScope whether to add the {@code [PÚBLICO]} or {@code [PRIVADO]} tag
     * @return the text to display
     */
    public static String format(ChatEntry entry, boolean showScope) {
        return switch (entry.type()) {
            case SYSTEM -> "— " + entry.text() + " —";
            case ERROR -> "⚠ " + entry.text();
            case PUBLIC -> message(entry, showScope ? UiText.PUBLIC_TAG + " " : "", entry.sender());
            case PRIVATE -> message(entry, showScope ? UiText.PRIVATE_TAG + " " : "",
                    entry.sender() + " → " + entry.recipient());
        };
    }

    private static String message(ChatEntry entry, String scope, String participants) {
        return "[" + TIME_FORMAT.format(entry.time()) + "] " + scope + participants + ": " + entry.text();
    }
}
