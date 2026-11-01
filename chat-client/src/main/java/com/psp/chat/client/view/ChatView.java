package com.psp.chat.client.view;

import com.psp.chat.client.model.ChatSession;

/**
 * Controller of a chat view that can be swapped inside the main window.
 */
public interface ChatView {

    /**
     * Binds the view to the session; called right after the view is loaded.
     *
     * @param session session shared by every view
     */
    void attach(ChatSession session);

    /**
     * Releases the bindings and listeners before the view is replaced.
     */
    void detach();
}
