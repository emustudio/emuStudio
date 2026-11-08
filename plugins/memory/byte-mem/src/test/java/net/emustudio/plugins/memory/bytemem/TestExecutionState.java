/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.ExecutionState;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Inline coordinator for isolated memory formatting/action tests, not pending-edit tests. */
public final class TestExecutionState implements ExecutionState {
    private TestExecutionState() { }
    public static ExecutionState create() { return new TestExecutionState(); }
    @Override public Snapshot snapshot() {
        return new Snapshot(CPU.RunState.STATE_STOPPED_BREAK, false, false, -1, Float.NaN);
    }
    @Override public Subscription subscribe(Consumer<Snapshot> listener) { return () -> { }; }
    @Override public void processUpdates() { }
    @Override public CompletableFuture<Void> mutate(Runnable mutation) { return update(mutation); }
    @Override public CompletableFuture<Void> update(Runnable mutation) {
        CompletableFuture<Void> completed = new CompletableFuture<>();
        try { mutation.run(); completed.complete(null); }
        catch (RuntimeException failure) { completed.completeExceptionally(failure); }
        return completed;
    }
}
