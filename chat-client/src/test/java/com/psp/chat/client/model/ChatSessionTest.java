package com.psp.chat.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.psp.chat.client.network.LoginRejectedException;
import com.psp.chat.core.ErrorCode;
import com.psp.chat.core.ErrorNotice;
import com.psp.chat.core.LoginAccepted;
import com.psp.chat.core.RejectionReason;
import com.psp.chat.core.ServerShutdown;
import com.psp.chat.core.TextMessage;
import com.psp.chat.core.UserEvent;
import com.psp.chat.core.UserEventType;
import java.net.ConnectException;
import java.net.SocketException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatSessionTest {

    private static final Instant TIME = Instant.parse("2026-11-01T10:00:00Z");

    private ChatSession session;
    private final List<String> errors = new ArrayList<>();

    @BeforeEach
    void createSession() {
        session = new ChatSession(Runnable::run);
        session.setOnError(errors::add);
    }

    private void loginAsAna() {
        session.onConnected(new LoginAccepted("ana", List.of("ana", "luis")));
    }

    private static ChatEntry last(List<ChatEntry> entries) {
        return entries.get(entries.size() - 1);
    }

    @Test
    void onConnectedSetsUsernameAndExcludesSelf() {
        loginAsAna();

        assertEquals("ana", session.getUsername());
        assertEquals(ConnectionState.CONNECTED, session.getState());
        assertEquals(List.of("luis"), session.getOnlineUsers());
    }

    @Test
    void publicMessageGoesToTimelineAndGeneral() {
        loginAsAna();

        session.packetReceived(new TextMessage("luis", null, "hola", TIME));

        assertEquals(1, session.getAllEntries().size());
        assertEquals(EntryType.PUBLIC, last(session.getAllEntries()).type());
        assertEquals(1, session.getConversation(ChatSession.GENERAL).size());
    }

    @Test
    void incomingPrivateOpensConversationWithSender() {
        loginAsAna();

        session.packetReceived(new TextMessage("luis", "ana", "hey", TIME));

        assertEquals(1, session.getConversation("luis").size());
        assertEquals(List.of("luis"), session.getPrivateConversations());
        assertEquals(EntryType.PRIVATE, last(session.getAllEntries()).type());
    }

    @Test
    void ownPrivateIsStoredWithRecipient() {
        loginAsAna();

        session.packetReceived(new TextMessage("ana", "luis", "hey", TIME));

        assertEquals(1, session.getConversation("luis").size());
    }

    @Test
    void joinEventUpdatesUsersAndGeneral() {
        loginAsAna();

        session.packetReceived(new UserEvent(UserEventType.JOINED, "eva", List.of("ana", "eva", "luis")));

        assertEquals(List.of("eva", "luis"), session.getOnlineUsers());
        assertEquals("eva se ha conectado", last(session.getConversation(ChatSession.GENERAL)).text());
    }

    @Test
    void peerLeavingIsShownInItsConversation() {
        loginAsAna();
        session.packetReceived(new TextMessage("luis", "ana", "hey", TIME));

        session.packetReceived(new UserEvent(UserEventType.LEFT, "luis", List.of("ana")));

        assertEquals("luis se ha desconectado", last(session.getConversation("luis")).text());
    }

    @Test
    void errorNoticeIsReportedAndShownAsError() {
        loginAsAna();

        session.packetReceived(new ErrorNotice(ErrorCode.USER_NOT_CONNECTED, "luis"));

        assertEquals(List.of("El usuario luis no está conectado."), errors);
        assertEquals(EntryType.ERROR, last(session.getAllEntries()).type());
    }

    @Test
    void connectionLostSwitchesToDisconnected() {
        loginAsAna();

        session.connectionLost(new SocketException("Connection reset"));

        assertEquals(ConnectionState.DISCONNECTED, session.getState());
        assertTrue(session.getOnlineUsers().isEmpty());
        assertEquals(List.of("Se ha perdido la conexión con el servidor."), errors);
    }

    @Test
    void serverShutdownSwitchesToDisconnected() {
        loginAsAna();

        session.packetReceived(new ServerShutdown());

        assertEquals(ConnectionState.DISCONNECTED, session.getState());
        assertEquals(List.of("El servidor se ha cerrado."), errors);
    }

    @Test
    void sendTextValidatesBeforeUsingTheConnection() {
        session.sendText(null, "   ");
        session.sendText(null, "hola");
        loginAsAna();
        session.sendText("ANA", "hola");

        assertEquals(List.of(
                "El mensaje está vacío o supera 1000 caracteres.",
                "No estás conectado.",
                "No puedes enviarte mensajes privados a ti mismo."), errors);
    }

    @Test
    void describeErrorTranslatesCauses() {
        ConnectionSettings settings = new ConnectionSettings("127.0.0.1", 5000, "ana");

        assertEquals("El servidor está lleno. Inténtalo más tarde.",
                ChatSession.describeError(new LoginRejectedException(RejectionReason.SERVER_FULL), settings));
        assertEquals("El nombre «ana» ya está en uso.",
                ChatSession.describeError(new LoginRejectedException(RejectionReason.USERNAME_TAKEN), settings));
        assertEquals("No se pudo conectar con 127.0.0.1:5000. ¿Está el servidor arrancado?",
                ChatSession.describeError(new ConnectException("Connection refused"), settings));
    }
}
