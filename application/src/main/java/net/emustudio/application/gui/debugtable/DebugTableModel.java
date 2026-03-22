/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;
import net.emustudio.emulib.runtime.ui.debugger.DebuggerTable;

import javax.swing.table.AbstractTableModel;

public abstract class DebugTableModel extends AbstractTableModel implements DebuggerTable {

    public abstract DebuggerColumn<?> getColumnAt(int index);

    public abstract void previousPage();

    public abstract void seekBackwardPage(int value);

    public abstract void firstPage();

    public abstract void nextPage();

    public abstract void seekForwardPage(int value);

    public abstract void lastPage();

    public abstract void currentPage();

    public abstract boolean isRowAtCurrentInstruction(int rowIndex);

    public abstract void memoryChanged(int from, int to);

    public abstract void memorySizeChanged(int memorySize);

    public abstract void setDefaultColumns();

    public abstract int guessPreviousInstructionLocation();

    public abstract void setMaxRows(int maxRows);

    public abstract void executionStateChanged();
}
