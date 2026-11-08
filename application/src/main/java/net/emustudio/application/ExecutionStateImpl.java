/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application;

import net.emustudio.application.debugger.DebugSession;
import net.emustudio.emulib.runtime.ExecutionState;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Exposes the active computer's execution position to plugin views. */
public final class ExecutionStateImpl implements ExecutionState {
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final Runnable changed = () -> listeners.forEach(Runnable::run);
    private volatile DebugSession session;

    public synchronized void setSession(DebugSession next) {
        if (session != null) session.removeMetadataListener(changed);
        session = next;
        if (next != null) next.addMetadataListener(changed);
        changed.run();
    }

    public synchronized void clearSession(DebugSession old) {
        if (session == old) setSession(null);
    }

    @Override public int currentInstruction() {
        DebugSession current = session;
        return current == null ? -1 : current.cpu.getInstructionLocation();
    }

    @Override public void addListener(Runnable listener) { listeners.add(listener); }
    @Override public void removeListener(Runnable listener) { listeners.remove(listener); }
}
