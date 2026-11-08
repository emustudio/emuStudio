/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public abstract class DebugTableModel extends AbstractTableModel {
    // Titles of columns created by DebuggerColumn.address/mnemo/opcode factories
    static final String TITLE_ADDRESS = "address";
    static final String TITLE_MNEMO = "instruction";
    static final String TITLE_OPCODE = "opcode";

    public abstract DebuggerColumn<?> getColumnAt(int index);

    public abstract void previousPage();

    public abstract void seekBackwardPage(int value);

    public abstract void firstPage();

    public abstract void nextPage();

    public abstract void seekForwardPage(int value);

    public abstract void lastPage();

    public abstract void currentPage();

    public abstract void showAddress(int address);

    public abstract boolean isRowAtCurrentInstruction(int rowIndex);

    public abstract int getLocationAt(int rowIndex);

    public abstract void memoryChanged(int from, int to);

    public abstract int guessPreviousInstructionLocation();

    public abstract void setMaxRows(int maxRows);

    public abstract void setDebuggerColumns(List<DebuggerColumn<?>> columns);
}
