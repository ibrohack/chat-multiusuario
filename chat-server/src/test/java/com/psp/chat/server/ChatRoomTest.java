package com.psp.chat.server;

import static com.psp.chat.server.RegistrationResult.ACCEPTED;
import static com.psp.chat.server.RegistrationResult.INVALID_USERNAME;
import static com.psp.chat.server.RegistrationResult.SERVER_FULL;
import static com.psp.chat.server.RegistrationResult.USERNAME_TAKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.psp.chat.core.ServerPacket;
import com.psp.chat.core.ServerShutdown;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ChatRoomTest {

    @Test
    void acceptsUpToMaxClientsAndRejectsTheNext() {
        ChatRoom room = new ChatRoom(2);

        assertEquals(ACCEPTED, room.register("ana", new FakeClient("ana")));
        assertEquals(ACCEPTED, room.register("luis", new FakeClient("luis")));
        assertEquals(SERVER_FULL, room.register("eva", new FakeClient("eva")));
        assertEquals(2, room.getStatus().onlineUsers());
    }

    @Test
    void rejectsTakenUsernameIgnoringCase() {
        ChatRoom room = new ChatRoom(5);

        assertEquals(ACCEPTED, room.register("Ana", new FakeClient("Ana")));
        assertEquals(USERNAME_TAKEN, room.register("ana", new FakeClient("ana")));
    }

    @Test
    void rejectsInvalidUsernameEvenWithFreeSlots() {
        assertEquals(INVALID_USERNAME, new ChatRoom(5).register("a b", new FakeClient("a b")));
    }

    @Test
    void unregisterFreesTheSlot() {
        ChatRoom room = new ChatRoom(1);
        FakeClient ana = new FakeClient("ana");
        room.register("ana", ana);

        assertTrue(room.unregister("ana", ana));
        assertEquals(ACCEPTED, room.register("luis", new FakeClient("luis")));
    }

    @Test
    void unregisterIgnoresADifferentClientWithSameName() {
        ChatRoom room = new ChatRoom(5);
        room.register("ana", new FakeClient("ana"));

        assertFalse(room.unregister("ana", new FakeClient("ana")));
        assertEquals(1, room.getStatus().onlineUsers());
    }

    @Test
    void usernamesAreSortedIgnoringCase() {
        ChatRoom room = new ChatRoom(5);
        for (String name : List.of("luis", "Ana", "eva")) {
            room.register(name, new FakeClient(name));
        }

        assertEquals(List.of("Ana", "eva", "luis"), room.getUsernames());
    }

    @Test
    void findClientIgnoresCase() {
        ChatRoom room = new ChatRoom(5);
        FakeClient luis = new FakeClient("luis");
        room.register("luis", luis);

        assertEquals(Optional.of(luis), room.findClient("LUIS"));
    }

    @Test
    void broadcastExceptSkipsTheGivenUser() {
        ChatRoom room = new ChatRoom(5);
        FakeClient ana = new FakeClient("ana");
        FakeClient luis = new FakeClient("luis");
        room.register("ana", ana);
        room.register("luis", luis);
        ServerPacket packet = new ServerShutdown();

        room.broadcastExcept(packet, "ana");

        assertEquals(List.of(packet), luis.received());
        assertEquals(List.of(), ana.received());
    }

    @Test
    void historyKeepsTheLast500() {
        ChatRoom room = new ChatRoom(5);
        for (int i = 0; i <= 500; i++) {
            room.addToHistory(new MessageRecord(LocalTime.NOON, "ana", null, Integer.toString(i)));
        }

        List<MessageRecord> history = room.getHistory();
        assertEquals(500, history.size());
        assertEquals("1", history.get(0).content());
        assertEquals("500", room.getStatus().lastMessage().content());
        assertEquals(501, room.getStatus().totalMessages());
    }

    @Test
    void messageRecordFormatsItself() {
        LocalTime time = LocalTime.of(12, 1, 22);

        assertEquals("[12:01:22] ana -> all: hola", new MessageRecord(time, "ana", null, "hola").toString());
        assertEquals("[12:01:22] ana -> luis (private): hola",
                new MessageRecord(time, "ana", "luis", "hola").toString());
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void concurrentRegistrationRespectsTheMaximum() throws Exception {
        ChatRoom room = new ChatRoom(10);
        CountDownLatch start = new CountDownLatch(1);
        List<RegistrationResult> results = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String name = "user" + i;
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                results.add(room.register(name, new FakeClient(name)));
            });
            thread.setDaemon(true);
            threads.add(thread);
            thread.start();
        }

        start.countDown();
        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(10, results.stream().filter(result -> result == ACCEPTED).count());
        assertEquals(10, room.getStatus().onlineUsers());
    }
}
