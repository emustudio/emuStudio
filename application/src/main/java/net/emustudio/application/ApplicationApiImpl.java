/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application;

import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.debugger.DebuggerTable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ApplicationApiImpl implements ApplicationApi {
    private final DebuggerTable debuggerTable;
    private final ContextPool contextPool;
    private final Dialogs dialogs;
    private final AtomicInteger programLocation = new AtomicInteger();

    public ApplicationApiImpl(DebuggerTable debuggerTable, ContextPool contextPool, Dialogs dialogs) {
        this.debuggerTable = Objects.requireNonNull(debuggerTable);
        this.contextPool = Objects.requireNonNull(contextPool);
        this.dialogs = Objects.requireNonNull(dialogs);
    }

    @Override
    public DebuggerTable getDebuggerTable() {
        return debuggerTable;
    }

    @Override
    public ContextPool getContextPool() {
        return contextPool;
    }

    @Override
    public Dialogs getDialogs() {
        return dialogs;
    }

    @Override
    public void setProgramLocation(int location) {
        programLocation.set(location);
    }

    @Override
    public int getProgramLocation() {
        return programLocation.get();
    }
}
