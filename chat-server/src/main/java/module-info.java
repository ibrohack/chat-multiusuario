/**
 * Console server of the multiuser chat.
 *
 * <p>Accepts clients over TCP, serves each one on its own thread, keeps the
 * connected users and the message history in a synchronized room, and reports
 * statistics periodically from a monitor thread.</p>
 */
module com.psp.chat.server {
    requires com.psp.chat.core;
    requires org.slf4j;
}
