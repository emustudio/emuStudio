/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application;

import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.debugger.DebuggerTable;

import java.util.Objects;

public class ApplicationApiImpl implements ApplicationApi {
    private final DebuggerTable debuggerTable;
    private final ContextPool contextPool;
    private final Dialogs dialogs;

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
}
