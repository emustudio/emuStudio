/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.ExecutionState;
import java.util.concurrent.CompletableFuture;

/** Inline host edit adapter for focused memory GUI tests. */
public final class TestExecutionState implements ExecutionState {
    private TestExecutionState() { }
    public static ExecutionState create() { return new TestExecutionState(); }
    @Override public int currentInstruction() { return -1; }
    @Override public CPU.RunState runState() { return CPU.RunState.STATE_STOPPED_BREAK; }
    @Override public boolean isBusy() { return false; }
    @Override public void addCPUListener(CPU.CPUListener listener) { }
    @Override public void removeCPUListener(CPU.CPUListener listener) { }
    @Override public void addListener(Runnable listener) { }
    @Override public void removeListener(Runnable listener) { }

    @Override public CompletableFuture<Void> mutate(Runnable mutation) {
        CompletableFuture<Void> completed = new CompletableFuture<>();
        try { mutation.run(); completed.complete(null); }
        catch (RuntimeException failure) { completed.completeExceptionally(failure); }
        return completed;
    }
}
