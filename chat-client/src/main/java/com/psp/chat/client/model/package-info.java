/**
 * Observable chat session shared by both user interfaces.
 *
 * <p>The model lives on the JavaFX application thread: network events are
 * handed to it through an {@link java.util.concurrent.Executor} such as
 * {@code Platform::runLater}, and views bind to its properties and lists.</p>
 */
package com.psp.chat.client.model;
