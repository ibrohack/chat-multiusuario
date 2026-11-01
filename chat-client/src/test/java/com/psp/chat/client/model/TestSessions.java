package com.psp.chat.client.model;

import com.psp.chat.core.LoginAccepted;
import java.util.List;

/**
 * Builds sessions in a known state for tests of other packages.
 */
public final class TestSessions {

    private TestSessions() {
    }

    /**
     * Returns a session that applies events immediately and is logged in without a network connection.
     */
    public static ChatSession loggedIn(String username, List<String> onlineUsers) {
        ChatSession session = new ChatSession(Runnable::run);
        session.onConnected(new LoginAccepted(username, onlineUsers));
        return session;
    }
}
