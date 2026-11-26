/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotapeplayer;

import java.util.concurrent.CompletableFuture;

/** A tape worker with separate readiness and stop acknowledgements. The owning controller locks its lifecycle. */
final class TapeTask {
    final CompletableFuture<Void> ready = new CompletableFuture<>();
    final CompletableFuture<Void> completion = new CompletableFuture<>();
    private Thread worker;

    void start(String name, Runnable action) {
        worker = new Thread(action, name);
        worker.start();
    }

    boolean isRunning() {
        return worker != null;
    }

    void requestStop() {
        if (worker != null) worker.interrupt();
    }

    void finish(Throwable failure) {
        worker = null;
        if (failure == null) {
            ready.complete(null);
            completion.complete(null);
        } else {
            ready.completeExceptionally(failure);
            completion.completeExceptionally(failure);
        }
    }
}
