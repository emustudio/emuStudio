/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application;

import net.emustudio.application.debugger.DebugSession;
import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.emulib.runtime.DebuggerMemory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Connects plugin views to the currently loaded computer's debugger. */
public final class DebuggerMemoryImpl implements DebuggerMemory {
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final Runnable changed = () -> listeners.forEach(Runnable::run);
    private volatile DebugSession session;

    public synchronized void setSession(DebugSession next) {
        if (session != null) session.removeMetadataListener(changed);
        if (next != null) {
            try { next.annotations(); }
            catch (IOException e) {
                org.slf4j.LoggerFactory.getLogger(DebuggerMemoryImpl.class).warn("Could not load memory annotations", e);
            }
        }
        session = next;
        if (next != null) next.addMetadataListener(changed);
        changed.run();
    }

    public synchronized void clearSession(DebugSession old) {
        if (session == old) setSession(null);
    }

    @Override public String label(int bank, int address) { return lookup(bank, address, true); }
    @Override public String comment(int bank, int address) { return lookup(bank, address, false); }

    private String lookup(int bank, int address, boolean label) {
        DebugSession current = session;
        if (current == null) return "";
        try {
            ProgramIndex.Snapshot snapshot = current.programIndex().snapshot();
            int physicalBank = snapshot.layout().physicalBank(bank, address);
            StringBuilder result = new StringBuilder();
            snapshot.forEachAnnotationStartingAt(physicalBank, address, annotation -> {
                boolean matches = label ? annotation.kind().equals(ProgramIndex.Kinds.LABEL)
                        : annotation.kind().startsWith("emuStudio.user/");
                if (matches && !annotation.text().isEmpty()) {
                    if (result.length() > 0) result.append(", ");
                    result.append(annotation.text());
                }
            });
            return result.toString();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return "";
        }
    }

    @Override public boolean isCurrentInstruction(int bank, int address) {
        DebugSession current = session;
        if (current == null || current.memory == null || current.cpu.getInstructionLocation() != address) return false;
        try {
            ProgramIndex.Layout layout = current.programIndex().snapshot().layout();
            return layout.physicalBank(bank, address) == layout.physicalBank(current.memory.getBank(address), address);
        } catch (IllegalArgumentException | IllegalStateException e) { return false; }
    }

    @Override public void addListener(Runnable listener) { listeners.add(listener); }
    @Override public void removeListener(Runnable listener) { listeners.remove(listener); }
}
