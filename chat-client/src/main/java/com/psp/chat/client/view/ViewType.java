package com.psp.chat.client.view;

/**
 * Chat views the user can choose between.
 */
public enum ViewType {

    /** View of the course screenshot: one list and the «Privado» / «Para» controls. */
    CLASSIC("classic-view.fxml"),

    /** View with the connected users and one tab per conversation. */
    ADVANCED("advanced-view.fxml");

    private final String fxml;

    ViewType(String fxml) {
        this.fxml = fxml;
    }

    /**
     * Returns the FXML file of the view, relative to this package.
     *
     * @return the FXML file name
     */
    public String getFxml() {
        return fxml;
    }
}
