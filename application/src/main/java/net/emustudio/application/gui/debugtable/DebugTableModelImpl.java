/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.runtime.interaction.debugger.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class DebugTableModelImpl extends DebugTableModel {
    private DebuggerColumn<?>[] columns;
    private final CPU cpu;
    private final PaginatingDisassembler ida;

    public DebugTableModelImpl(CPU cpu, int memorySize) {
        this.cpu = Objects.requireNonNull(cpu);

        CallFlow callFlow = new CallFlow(cpu.getDisassembler());
        this.ida = new PaginatingDisassembler(callFlow, memorySize);
        setDefaultColumns();
    }

    @Override
    public int getRowCount() {
        return PaginatingDisassembler.INSTR_PER_PAGE;
    }

    @Override
    public DebuggerColumn<?> getColumnAt(int index) {
        return columns[index];
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int col) {
        return columns[col].getTitle();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columns[columnIndex].getClassType();
    }

    @Override
    public void previousPage() {
        ida.pagePrevious();
        fireTableDataChanged();
    }

    @Override
    public void seekBackwardPage(int value) {
        while (value > 0) {
            ida.pagePrevious();
            value--;
        }
        fireTableDataChanged();
    }

    @Override
    public void firstPage() {
        ida.pageFirst();
        fireTableDataChanged();
    }

    @Override
    public void nextPage() {
        ida.pageNext();
        fireTableDataChanged();
    }

    @Override
    public void seekForwardPage(int value) {
        while (value > 0) {
            ida.pageNext();
            value--;
        }
        fireTableDataChanged();
    }

    @Override
    public void lastPage() {
        ida.pageLast();
        fireTableDataChanged();
    }

    @Override
    public void currentPage() {
        ida.pageCurrent();
        fireTableDataChanged();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        int location = ida.rowToLocation(cpu.getInstructionLocation(), rowIndex);
        if (location != -1) {
            return columns[columnIndex].getValue(location);
        }
        return null;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        int location = ida.rowToLocation(cpu.getInstructionLocation(), rowIndex);
        if (location != -1) {
            DebuggerColumn<?> column = columns[columnIndex];
            if (value.getClass() == column.getClassType()) {
                try {
                    column.setValue(location, value);
                } catch (CannotSetDebuggerValueException ignored) {}
            }
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columns[columnIndex].isEditable();
    }

    @Override
    public boolean isRowAtCurrentInstruction(int rowIndex) {
        return ida.isRowAtCurrentInstruction(rowIndex);
    }

    @Override
    public void memoryChanged(int from, int to) {
        ida.flushCache(from, to + 1);
    }

    @Override
    public void setMemorySize(int memorySize) {
        ida.setMemorySize(memorySize);
        fireTableDataChanged();
    }

    @Override
    public void setColumns(List<DebuggerColumn<?>> columns) {
        this.columns = columns.toArray(new DebuggerColumn[0]);
        fireTableStructureChanged();
    }

    @Override
    public final void setDefaultColumns() {
        Disassembler dis = cpu.getDisassembler();
        if (cpu.isBreakpointSupported()) {
            setColumns(Arrays.asList(
                new BreakpointColumn(cpu), new AddressColumn(), new MnemoColumn(dis), new OpcodeColumn(dis)
            ));
        } else {
            setColumns(Arrays.asList(
                new AddressColumn(), new MnemoColumn(dis), new OpcodeColumn(dis)
            ));
        }
    }

    @Override
    public int guessPreviousInstructionLocation() {
        int location = ida.rowToLocation(cpu.getInstructionLocation(), PaginatingDisassembler.CURRENT_INSTR_ROW - 1);
        if (location < 0) {
            return Math.max(0, cpu.getInstructionLocation() - 1);
        }
        return location;
    }
}
