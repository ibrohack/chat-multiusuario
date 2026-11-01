package com.psp.chat.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.psp.chat.core.ErrorCode;
import com.psp.chat.core.ErrorNotice;
import com.psp.chat.core.ImageMessage;
import com.psp.chat.core.ImageRequest;
import com.psp.chat.core.Protocol;
import com.psp.chat.core.TextMessage;
import com.psp.chat.core.TextRequest;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ChatServerMessagingTest {

    private ChatServer server;
    private TestClient ana;
    private TestClient luis;
    private TestClient eva;

    @BeforeEach
    void connectThreeUsers() throws Exception {
        server = new ChatServer(new ServerConfig(0, 5, 3600));
        server.start();
        ana = TestClient.connect(server.getPort(), "ana");
        luis = TestClient.connect(server.getPort(), "luis");
        eva = TestClient.connect(server.getPort(), "eva");
    }

    @AfterEach
    void stopServer() {
        ana.close();
        luis.close();
        eva.close();
        server.stop();
    }

    @Test
    void publicMessageReachesEveryoneIncludingSender() throws Exception {
        ana.send(new TextRequest(null, "hola"));

        for (TestClient client : List.of(ana, luis, eva)) {
            TextMessage message = client.await(TextMessage.class);
            assertEquals("ana", message.sender());
            assertNull(message.recipient());
            assertEquals("hola", message.text());
        }
    }

    @Test
    void privateMessageReachesOnlyRecipientAndSender() throws Exception {
        ana.send(new TextRequest("luis", "secreto"));

        for (TestClient client : List.of(luis, ana)) {
            TextMessage message = client.await(TextMessage.class);
            assertEquals(List.of("ana", "luis", "secreto"), List.of(message.sender(), message.recipient(), message.text()));
        }
        eva.assertNoMessages(Duration.ofMillis(300));
    }

    @Test
    void privateRecipientMatchesIgnoringCase() throws Exception {
        ana.send(new TextRequest("LUIS", "hola"));

        assertEquals("luis", luis.await(TextMessage.class).recipient());
    }

    @Test
    void privateToOfflineUserNotifiesSender() throws Exception {
        ana.send(new TextRequest("nobody", "hola"));

        assertEquals(new ErrorNotice(ErrorCode.USER_NOT_CONNECTED, "nobody"), ana.await(ErrorNotice.class));
    }

    @Test
    void privateToSelfNotifiesSender() throws Exception {
        ana.send(new TextRequest("ANA", "hola"));

        assertEquals(new ErrorNotice(ErrorCode.PRIVATE_TO_SELF, null), ana.await(ErrorNotice.class));
    }

    @Test
    void invalidTextNotifiesSender() throws Exception {
        ana.send(new TextRequest(null, "x".repeat(1001)));

        assertEquals(new ErrorNotice(ErrorCode.INVALID_TEXT, null), ana.await(ErrorNotice.class));
    }

    @Test
    void publicImageReachesEveryone() throws Exception {
        byte[] bytes = {1, 2, 3, 4};

        ana.send(new ImageRequest(null, "foto.png", bytes));

        for (TestClient client : List.of(ana, luis, eva)) {
            ImageMessage message = client.await(ImageMessage.class);
            assertEquals("ana", message.sender());
            assertNull(message.recipient());
            assertEquals("foto.png", message.fileName());
            assertArrayEquals(bytes, message.data());
        }
    }

    @Test
    void privateImageReachesOnlyRecipientAndSender() throws Exception {
        ana.send(new ImageRequest("luis", "foto.png", new byte[] {1}));

        assertEquals("luis", luis.await(ImageMessage.class).recipient());
        assertEquals("luis", ana.await(ImageMessage.class).recipient());
        eva.assertNoMessages(Duration.ofMillis(300));
    }

    @Test
    void oversizedImageNotifiesSender() throws Exception {
        ana.send(new ImageRequest(null, "big.png", new byte[Protocol.MAX_IMAGE_BYTES + 1]));

        assertEquals(new ErrorNotice(ErrorCode.INVALID_IMAGE, null), ana.await(ErrorNotice.class));
    }

    @Test
    void imageHistoryOmitsBytes() throws Exception {
        ana.send(new ImageRequest(null, "foto.png", new byte[1024]));
        luis.await(ImageMessage.class);

        assertEquals("[image foto.png, 1 KB]", server.getRoom().getStatus().lastMessage().content());
    }

    @Test
    void messagesAreKeptInHistory() throws Exception {
        ana.send(new TextRequest(null, "hola"));
        ana.send(new TextRequest("luis", "secreto"));
        luis.await(TextMessage.class);
        luis.await(TextMessage.class);

        assertEquals(2, server.getRoom().getHistory().size());
        assertEquals("secreto", server.getRoom().getStatus().lastMessage().content());
        assertEquals(2, server.getRoom().getStatus().totalMessages());
    }
}
