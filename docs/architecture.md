# Architecture and technical decisions

This document explains how the chat is built and why. It also records the alternatives we evaluated, which the course asks every team member to be able to defend.

## Modules

```
chat-parent (POM: versions, plugins, Checkstyle rules)
├── chat-core    module com.psp.chat.core    packets, Protocol rules, ObjectChannel
├── chat-server  module com.psp.chat.server  console server (depends on chat-core)
└── chat-client  module com.psp.chat.client  JavaFX client (depends on chat-core)
```

| Package | Responsibility |
|---|---|
| `com.psp.chat.core` | Serializable packets, protocol constants and validation, `ObjectChannel` |
| `com.psp.chat.server` | `ChatServer` (acceptor), `ClientHandler` (one per client), `ChatRoom` (shared state), `MonitorThread` |
| `com.psp.chat.client.network` | `ServerConnection` (login handshake), `ReceiverThread`, `SenderThread` |
| `com.psp.chat.client.model` | `ChatSession`, the observable model shared by both views, plus `UiText` (Spanish copy) |
| `com.psp.chat.client.view` | FXML controllers and list cells |

## Protocol

Every message is a `Packet`, an immutable `record` sent with `ObjectOutputStream`. The hierarchy is sealed:

- `ClientPacket` covers what clients send.
- `ServerPacket` covers what the server sends.

Because the hierarchy is sealed, the compiler checks that every `switch` handles every packet type.

| Packet | Direction | Content | Notes |
|---|---|---|---|
| `LoginRequest` | C→S | `username` | First packet after connecting |
| `LoginAccepted` | S→C | `username`, `onlineUsers` | Always the first packet an accepted client receives |
| `LoginRejected` | S→C | `reason`: `SERVER_FULL`, `USERNAME_TAKEN`, `INVALID_USERNAME` | The server then closes the connection |
| `TextRequest` | C→S | `recipient` (`null` = public), `text` | The client cannot set the sender |
| `TextMessage` | S→C | `sender`, `recipient`, `text`, `timestamp` | Public: everyone, sender included. Private: recipient plus an echo to the sender |
| `UserEvent` | S→C | `JOINED` or `LEFT`, `username`, `onlineUsers` | Join: sent to the others. Leave: sent to everyone left |
| `ErrorNotice` | S→C | `code`, `detail` | Codes: invalid text, private to self, unknown recipient, repeated login |
| `LogoutRequest` | C→S | — | Voluntary leave |
| `ServerShutdown` | S→C | — | The server is stopping |

A session looks like this:

```
client                         server
  | -- LoginRequest("ana") -->   |   check name and capacity atomically
  | <-- LoginAccepted ---------  |   others receive UserEvent(JOINED, "ana")
  | -- TextRequest(...) ------>  |   validated, logged, recorded in the history
  | <-- TextMessage -----------  |   broadcast, or private + echo
  | -- LogoutRequest --------->  |   others receive UserEvent(LEFT, "ana")
```

`ObjectChannel` wraps a socket. It creates and flushes the `ObjectOutputStream` before opening the `ObjectInputStream`: each input stream waits for the peer's header, so opening the inputs first on both ends would deadlock. It calls `reset()` after every write so the stream does not keep a reference to every packet ever sent.

## Threads

| Side | Thread | Job |
|---|---|---|
| Server | `main` | Parses options, starts the server, registers the shutdown hook |
| Server | `acceptor` | `accept()` loop; starts one thread per connection |
| Server | `client-<user>` (×N) | Login, then a read loop that routes the client's packets |
| Server | `monitor` | Every *X* seconds logs online users, uptime and last message |
| Server | `shutdown-hook` | On Ctrl+C: notifies clients and closes everything |
| Client | JavaFX Application Thread | Views and the `ChatSession` model |
| Client | `connector` | JavaFX `Task` running the blocking login handshake |
| Client | `receiver` | `readObject()` loop; hands each packet to the UI thread with `Platform.runLater` |
| Client | `sender` | Writes queued packets in order, so the UI never blocks on the network |

## Synchronization

The statement requires manual synchronization with `synchronized`, so the code uses no concurrent collections, atomics, `volatile` or `Lock`s.

- **`ChatRoom`.** Every method that reads or changes the registry, the history or the statistics is `synchronized`.
  - `register` checks the name and the capacity in the same critical section, so two clients can never both take the last slot (`ChatRoomTest.concurrentRegistrationRespectsTheMaximum`).
  - `broadcast` copies the member list under the lock and sends **outside** it, so a slow client cannot block the whole room.
- **`ObjectChannel.send`** is `synchronized`: several threads may broadcast to the same client, and writes must not interleave. A test that removes the keyword corrupts the stream.
- **Login ordering.** `ClientHandler` registers the user and sends `LoginAccepted` inside `synchronized (channel)`. A broadcast arriving at the same moment waits on that lock, so `LoginAccepted` is always the first packet the client sees.
- **Lock order.** Code may hold the channel lock and then take the room lock (login). It never holds the room lock while sending. Locks are always taken in the order channel → room, so no thread waits in a cycle and there is no deadlock.
- **Client `SenderThread`.** A producer-consumer queue built on `synchronized`, `wait()` and `notifyAll()`.
- **`ServerConnection`.** Flags shared by the UI and network threads (voluntary disconnect, server shutdown, loss notified) are read and written in `synchronized` blocks. This guarantees the user is told about a lost connection at most once.
- **`ChatSession`.** Confined to the JavaFX thread: network threads only post work to it through `Platform.runLater`.

## Error handling and orderly close

| Situation | What happens |
|---|---|
| User presses Desconectar or closes the window | `LogoutRequest` is sent and the socket closed. The server unregisters the user, broadcasts `LEFT` and logs `(logout)`. |
| Client crashes or its network drops | The server's read fails, and the same cleanup runs in `finally`, logged as `(connection lost)`. |
| Server stopped with Ctrl+C | The shutdown hook sends `ServerShutdown` to everyone, closes every socket and stops the monitor. Clients show "El servidor se ha cerrado." |
| Server crashes | The client's receiver gets EOF and reports one connection loss: "Se ha perdido la conexión con el servidor." |
| Server full, name taken, invalid name | `LoginRejected` with the reason; the client shows a dialog |
| Invalid text, private to self, unknown recipient | `ErrorNotice` with a code; the client shows a red line and a dialog |
| Silent peer | Connect timeout 5 s and login timeout 10 s, so no thread waits forever |

## Alternatives considered

1. **Object streams** vs `DataInputStream`/`DataOutputStream` with text commands vs JSON.
   - **Chose object streams**, which the rubric names explicitly.
   - Packets are typed objects, so there is no hand-written parsing.
2. **Records + sealed interfaces** vs a single `Message` class with a `type` enum.
   - **Chose records.** They are immutable and validated in their constructors, records skip the `serialVersionUID` check, and the compiler checks every `switch` for exhaustiveness.
3. **Thread per client** vs a thread pool vs Java 21 virtual threads vs NIO selectors.
   - **Chose platform threads, one per client.** This is what the statement asks for, and it is the easiest model to explain.
   - Virtual threads were rejected: on Java 21 a virtual thread blocked inside `synchronized` pins its carrier thread (fixed in JDK 24, JEP 491).
   - NIO would replace threads with a selector loop, which is not the goal of the challenge.
4. **`synchronized`** (mandatory) vs `ConcurrentHashMap`/locks.
   - **Chose `synchronized` with snapshot broadcasting.** The room never holds its lock during network I/O.
5. **Capacity check at registration** vs rejecting right after `accept()`.
   - **Chose registration.** The check is atomic with the name check, and the rejected client receives the reason.
   - Closing right after `accept()` can reset the connection on Windows before the client reads anything.
6. **Server echo** vs the client adding its own messages locally.
   - **Chose the echo.** Everybody sees the same order and the server's timestamp, and the sender knows the server accepted the message.
7. **JavaFX `Task` + `Platform.runLater`** vs Swing's `SwingWorker` + `invokeLater`.
   - **Chose the JavaFX tools**, the equivalents for a JavaFX client: `Task` runs the login off the UI thread, and `runLater` applies network events on it.
8. **`SenderThread` with `wait`/`notifyAll`** vs an `ExecutorService`.
   - **Chose `SenderThread`.** It keeps the sending queue visible and synchronized by hand, as the statement requires.
9. **Monitor `Thread` with `sleep`** vs `ScheduledExecutorService`.
   - **Chose a dedicated thread**, as the rubric describes ("hilo monitor").
10. **Shared `chat-core` module** vs two independent projects with a copied protocol vs the client depending on the server.
    - **Chose a shared module.** There is a single definition of the packets, and both sides need identical classes to deserialize them.
11. **Error codes in the protocol** vs error sentences sent by the server.
    - **Chose codes.** The protocol stays language-neutral and the client words the errors in Spanish.
12. **Two UIs in one client.** We compared four ways to choose a view:
    - a chooser at start-up only;
    - a live menu toggle only;
    - a launch flag (two run configurations);
    - a chooser **plus** a live switch.

    **Chose the chooser plus the live switch.** The network layer and the observable `ChatSession` are shared, so switching a view only reloads one FXML and rebinds it. The connection and the whole history survive the switch (`ViewLoadingTest`).
13. **Images inside packets** vs a separate transfer channel vs chunked transfers.
    - **Chose a byte array inside the packet**, capped at 2 MiB.
    - The same `ObjectChannel` and routing serve text and images.
    - `reset()` after each write keeps sent images from piling up in the stream's memory.
    - The server history stores a description (`[image foto.png, 34 KB]`), never the bytes.
14. **SLF4J + Logback.**
    - Console output plus a rolling `logs/server.log`.
    - A separate `monitor` logger, so the statistics appear on the console only.
    - The MDC adds the username to every line written by a client thread.

## Security: pending

The current version sends packets in plain text and trusts the bytes it deserializes. The next session will cover:

- **TLS 1.3 with JSSE.** `SSLServerSocket` / `SSLSocket`, plugged in at the only two places that create sockets: `ChatServer.createServerSocket` and `ServerConnection.openSocket`.
  - A keytool script will generate a PKCS12 keystore.
  - The client truststore will pin the server certificate.
  - `ObjectChannel` works unchanged on an `SSLSocket`.
- **Deserialization filter.** An `ObjectInputFilter` allow-list on every `ObjectInputStream`: only the `com.psp.chat.core` packets and the JDK types they use, plus size and depth limits. This closes the classic risk of deserializing untrusted input.
- **Optional:** password login with PBKDF2 hashes.
