/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application;

import net.emustudio.application.emulation.EmulationController;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.ExecutionState;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/** The single execution publication store, available to plugins before their machine is initialized. */
public final class ExecutionStateImpl implements ExecutionState {
    private final CopyOnWriteArraySet<Consumer<Snapshot>> listeners = new CopyOnWriteArraySet<>();
    private volatile Snapshot snapshot = new Snapshot(CPU.RunState.STATE_STOPPED_NORMAL, true, false, -1, Float.NaN);
    private volatile EmulationController execution;

    @Override public Snapshot snapshot() { return snapshot; }

    @Override public Subscription subscribe(Consumer<Snapshot> listener) {
        listeners.add(Objects.requireNonNull(listener));
        return () -> listeners.remove(listener);
    }

    @Override public CompletableFuture<Void> update(Runnable change) {
        EmulationController current = execution;
        return current == null ? CompletableFuture.failedFuture(new IllegalStateException("No active machine"))
                : current.update(change);
    }

    public synchronized void bind(EmulationController execution) { this.execution = Objects.requireNonNull(execution); }

    /** Only the currently bound command scheduler publishes; old machines cannot overwrite its state. */
    public void publish(EmulationController owner, Snapshot next) {
        synchronized (this) {
            if (execution != owner || next.equals(snapshot)) return;
            snapshot = next;
        }
        for (Consumer<Snapshot> listener : listeners) {
            try { listener.accept(next); }
            catch (RuntimeException failure) {
                LoggerFactory.getLogger(ExecutionStateImpl.class).warn("Execution observer failed", failure);
            }
        }
    }

    public synchronized void unbind(EmulationController owner) {
        if (execution == owner) execution = null;
    }
}
