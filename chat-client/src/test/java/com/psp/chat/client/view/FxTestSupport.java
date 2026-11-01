package com.psp.chat.client.view;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;

/**
 * Starts the JavaFX toolkit once and runs test code on its thread.
 */
final class FxTestSupport {

    private FxTestSupport() {
    }

    /** Starts the toolkit if it is not running yet; safe to call from every test class. */
    static void start() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyRunning) {
            started.countDown();
        }
        Platform.setImplicitExit(false);
        if (!started.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("JavaFX did not start");
        }
    }

    /** Runs the action on the JavaFX thread and returns its result, rethrowing its failure. */
    static <T> T onFxThread(Callable<T> action) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(action.call());
            } catch (Throwable e) {
                result.completeExceptionally(e);
            }
        });
        return result.get(10, TimeUnit.SECONDS);
    }
}
