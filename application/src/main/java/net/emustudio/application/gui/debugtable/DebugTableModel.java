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

package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.runtime.interaction.debugger.DebuggerColumn;
import net.emustudio.emulib.runtime.interaction.debugger.DebuggerTable;

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

    public abstract void setMemorySize(int memorySize);

    public abstract void setDefaultColumns();

    public abstract int guessPreviousInstructionLocation();

    public abstract void setMaxRows(int maxRows);
}
