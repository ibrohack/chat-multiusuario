package com.psp.chat.client.model;

/**
 * User-visible texts of the client, in Spanish, kept in one place.
 */
public final class UiText {

    /** Shown when the port field is not a valid port. */
    public static final String INVALID_PORT = "El puerto debe ser un número entre 1 y 65535.";

    /** Shown when the username does not follow the protocol rules. */
    public static final String INVALID_USERNAME = "Nombre no válido: usa de 3 a 16 letras, números o _.";

    /** Notice added when the user disconnects. */
    public static final String YOU_DISCONNECTED = "Te has desconectado";

    /** Shown when the server announces its shutdown. */
    public static final String SERVER_CLOSED = "El servidor se ha cerrado.";

    /** Shown when the connection fails unexpectedly. */
    public static final String CONNECTION_LOST = "Se ha perdido la conexión con el servidor.";

    /** Shown when the server rejects the login because it is full. */
    public static final String SERVER_FULL = "El servidor está lleno. Inténtalo más tarde.";

    /** Shown when a message is empty or too long. */
    public static final String INVALID_TEXT = "El mensaje está vacío o supera 1000 caracteres.";

    /** Shown when the user tries to send a private message to themselves. */
    public static final String PRIVATE_TO_SELF = "No puedes enviarte mensajes privados a ti mismo.";

    /** Shown when the user tries to send without a session. */
    public static final String NOT_CONNECTED = "No estás conectado.";

    /** Shown when the server reports a repeated login. */
    public static final String ALREADY_LOGGED_IN = "Ya has iniciado sesión.";

    /** Shown when an image is empty or too large. */
    public static final String INVALID_IMAGE = "La imagen está vacía o supera el máximo de 2 MB.";

    /** Shown by the classic view when a private message has no recipient. */
    public static final String RECIPIENT_REQUIRED = "Indica el destinatario en «Para».";

    /** Tag of public messages in the classic view. */
    public static final String PUBLIC_TAG = "[PÚBLICO]";

    /** Tag of private messages in the classic view. */
    public static final String PRIVATE_TAG = "[PRIVADO]";

    /** Status label text without a session. */
    public static final String STATUS_DISCONNECTED = "No conectado";

    /** Status label text while logging in. */
    public static final String STATUS_CONNECTING = "Conectando…";

    /** Window title without a session. */
    public static final String APP_TITLE = "Chat multiusuario";

    /** Title of the public conversation tab. */
    public static final String GENERAL_TAB = "General";

    /** Prompt of the message field while the general tab is selected. */
    public static final String GENERAL_PROMPT = "Mensaje para General";

    private UiText() {
        // Constants and formatting helpers only.
    }

    /**
     * Builds the notice shown after logging in.
     *
     * @param host     server host
     * @param port     server port
     * @param username name the user logged in with
     * @return the notice text
     */
    public static String connectedAs(String host, int port, String username) {
        return "Conectado a " + host + ":" + port + " como " + username;
    }

    /**
     * Builds the status label text of an open session.
     *
     * @param username name the user is logged in with
     * @return the status text
     */
    public static String connectedStatus(String username) {
        return "Conectado como " + username;
    }

    /**
     * Builds the window title of an open session.
     *
     * @param username name the user is logged in with
     * @return the window title
     */
    public static String windowTitle(String username) {
        return APP_TITLE + " — " + username;
    }

    /**
     * Builds the prompt of the message field while a private tab is selected.
     *
     * @param username name of the other user of the conversation
     * @return the prompt text
     */
    public static String privatePrompt(String username) {
        return "Mensaje privado para " + username;
    }

    /**
     * Builds the title of a tab with unread messages.
     *
     * @param title  title of the tab
     * @param unread number of unread lines
     * @return the title followed by the count, for example {@code luis (2)}
     */
    public static String unreadTitle(String title, int unread) {
        return title + " (" + unread + ")";
    }

    /**
     * Builds the title of the connected users list.
     *
     * @param count number of other connected users
     * @return the list title
     */
    public static String onlineUsers(int count) {
        return "Conectados (" + count + ")";
    }

    /**
     * Builds the notice shown when a user joins.
     *
     * @param username name of the user
     * @return the notice text
     */
    public static String joined(String username) {
        return username + " se ha conectado";
    }

    /**
     * Builds the notice shown when a user leaves.
     *
     * @param username name of the user
     * @return the notice text
     */
    public static String left(String username) {
        return username + " se ha desconectado";
    }

    /**
     * Builds the error shown when the username is taken.
     *
     * @param username the requested name
     * @return the error text
     */
    public static String usernameTaken(String username) {
        return "El nombre «" + username + "» ya está en uso.";
    }

    /**
     * Builds the error shown when the server refuses the connection.
     *
     * @param host server host
     * @param port server port
     * @return the error text
     */
    public static String cannotConnect(String host, int port) {
        return "No se pudo conectar con " + host + ":" + port + ". ¿Está el servidor arrancado?";
    }

    /**
     * Builds the error shown when the server does not answer in time.
     *
     * @param host server host
     * @param port server port
     * @return the error text
     */
    public static String noResponse(String host, int port) {
        return "El servidor " + host + ":" + port + " no responde.";
    }

    /**
     * Builds the error shown when the host name cannot be resolved.
     *
     * @param host server host
     * @return the error text
     */
    public static String unknownHost(String host) {
        return "No se encuentra el host " + host + ".";
    }

    /**
     * Builds the error shown for any other communication problem.
     *
     * @param message technical detail of the problem
     * @return the error text
     */
    public static String communicationError(String message) {
        return "Error de comunicación: " + message;
    }

    /**
     * Builds the error shown when an image file cannot be read.
     *
     * @param message technical detail of the problem
     * @return the error text
     */
    public static String cannotReadImage(String message) {
        return "No se pudo leer la imagen: " + message;
    }

    /**
     * Builds the error shown when a private recipient is not connected.
     *
     * @param username name of the recipient
     * @return the error text
     */
    public static String userNotConnected(String username) {
        return "El usuario " + username + " no está conectado.";
    }
}
