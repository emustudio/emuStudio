/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application;

import net.emustudio.application.debugger.DebugSession;
import net.emustudio.emulib.runtime.ExecutionState;
import net.emustudio.emulib.plugins.cpu.CPU;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletableFuture;

/** Exposes the active computer's execution position to plugin views. */
public final class ExecutionStateImpl implements ExecutionState {
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final Runnable changed = () -> listeners.forEach(Runnable::run);
    private volatile DebugSession session;
    private volatile net.emustudio.application.emulation.EmulationController execution;
    private final List<CPU.CPUListener> cpuListeners = new CopyOnWriteArrayList<>();
    private final CPU.CPUListener stateChanged = new CPU.CPUListener() {
        @Override public void runStateChanged(CPU.RunState state) {
            cpuListeners.forEach(listener -> {
                try { listener.runStateChanged(state); }
                catch (RuntimeException failure) { reportListenerFailure(failure); }
            });
        }
        @Override public void internalStateChanged() {
            cpuListeners.forEach(listener -> {
                try { listener.internalStateChanged(); }
                catch (RuntimeException failure) { reportListenerFailure(failure); }
            });
        }
    };

    private static void reportListenerFailure(RuntimeException failure) {
        org.slf4j.LoggerFactory.getLogger(ExecutionStateImpl.class).warn("Execution observer failed", failure);
    }

    public void setSession(DebugSession next) {
        synchronized (this) {
            replaceSession(next);
        }
        changed.run();
    }

    private void replaceSession(DebugSession next) {
        if (session != null) session.removeMetadataListener(changed);
        session = next;
        if (next != null) next.addMetadataListener(changed);
        setExecution(next == null ? null : next.execution());
    }

    public void clearSession(DebugSession old) {
        synchronized (this) {
            if (session != old) return;
            replaceSession(null);
        }
        changed.run();
    }

    @Override public int currentInstruction() {
        DebugSession current = session;
        return current == null ? -1 : current.cpu.getInstructionLocation();
    }

    @Override public void addListener(Runnable listener) { listeners.add(listener); }
    @Override public void removeListener(Runnable listener) { listeners.remove(listener); }

    @Override public CPU.RunState runState() {
        net.emustudio.application.emulation.EmulationController current = execution;
        return current == null ? CPU.RunState.STATE_STOPPED_NORMAL : current.runState();
    }

    @Override public boolean isBusy() {
        net.emustudio.application.emulation.EmulationController current = execution;
        return current == null || current.isBusy();
    }

    @Override public CompletableFuture<Void> mutate(Runnable mutation) {
        net.emustudio.application.emulation.EmulationController current = execution;
        return current == null ? CompletableFuture.failedFuture(new IllegalStateException("No active machine"))
                : current.mutate(mutation);
    }

    public synchronized void setExecution(net.emustudio.application.emulation.EmulationController next) {
        if (execution != null) execution.removeCPUListener(stateChanged);
        execution = next;
        if (next != null) next.addCPUListener(stateChanged);
    }

    public synchronized void clearExecution(net.emustudio.application.emulation.EmulationController old) {
        if (execution == old) setExecution(null);
    }

    @Override public void addCPUListener(CPU.CPUListener listener) { cpuListeners.add(listener); }
    @Override public void removeCPUListener(CPU.CPUListener listener) { cpuListeners.remove(listener); }
}
