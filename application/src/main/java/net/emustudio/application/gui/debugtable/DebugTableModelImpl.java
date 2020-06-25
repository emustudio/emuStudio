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
import java.util.Optional;

public class DebugTableModelImpl extends DebugTableModel {
    private DebuggerColumn<?>[] columns = new DebuggerColumn[0];
    private CPU cpu;
    private PaginatingDisassembler ida;

    public DebugTableModelImpl() {
    }

    public void setCPU(CPU cpu, int memorySize) {
        this.cpu = Objects.requireNonNull(cpu);
        CallFlow callFlow = new CallFlow(cpu.getDisassembler());
        this.ida = new PaginatingDisassembler(callFlow, memorySize);
        setDefaultColumns();
    }

    public void setMaxRows(int maxRows) {
        if (ida != null) {
            ida.setInstructionsPerPage(maxRows);
        }
    }

    @Override
    public int getRowCount() {
        return (ida != null) ? ida.getInstructionsPerPage() : 0;
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
        Optional.ofNullable(ida).ifPresent(i -> {
            i.pagePrevious();
            fireTableDataChanged();
        });
    }

    @Override
    public void seekBackwardPage(final int value) {
        Optional.ofNullable(ida).ifPresent(i -> {
            for (int counter = 0; counter < value; counter++) {
                i.pagePrevious();
            }
            fireTableDataChanged();
        });
    }

    @Override
    public void firstPage() {
        Optional.ofNullable(ida).ifPresent(i -> {
            i.pageFirst();
            fireTableDataChanged();
        });
    }

    @Override
    public void nextPage() {
        Optional.ofNullable(ida).ifPresent(i -> {
            i.pageNext();
            fireTableDataChanged();
        });
    }

    @Override
    public void seekForwardPage(final int value) {
        Optional.ofNullable(ida).ifPresent(i -> {
            for (int counter = 0; counter < value; counter++) {
                ida.pageNext();
            }
            fireTableDataChanged();
        });
    }

    @Override
    public void lastPage() {
        Optional.ofNullable(ida).ifPresent(i -> {
            i.pageLast();
            fireTableDataChanged();
        });
    }

    @Override
    public void currentPage() {
        Optional.ofNullable(ida).ifPresent(i -> {
            i.pageCurrent();
            fireTableDataChanged();
        });
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return Optional.ofNullable(ida).map(i -> {
            int location = i.rowToLocation(cpu.getInstructionLocation(), rowIndex);
            if (location != -1) {
                return columns[columnIndex].getValue(location);
            }
            return null;
        }).orElse(null);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        Optional.ofNullable(ida).ifPresent(i -> {
            int location = i.rowToLocation(cpu.getInstructionLocation(), rowIndex);
            if (location != -1) {
                DebuggerColumn<?> column = columns[columnIndex];
                if (value.getClass() == column.getClassType()) {
                    try {
                        column.setValue(location, value);
                    } catch (CannotSetDebuggerValueException ignored) {
                    }
                }
            }
        });
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columns[columnIndex].isEditable();
    }

    @Override
    public boolean isRowAtCurrentInstruction(int rowIndex) {
        return Optional.ofNullable(ida).map(i -> i.isRowAtCurrentInstruction(rowIndex)).orElse(false);
    }

    @Override
    public void memoryChanged(int from, int to) {
        Optional.ofNullable(ida).ifPresent(i -> i.flushCache(from, to + 1));
    }

    @Override
    public void setMemorySize(int memorySize) {
        Optional.ofNullable(ida).ifPresent(i -> i.setMemorySize(memorySize));
    }

    @Override
    public final void setDefaultColumns() {
        Optional.ofNullable(cpu).ifPresent(cpu -> {
            Disassembler dis = cpu.getDisassembler();
            if (cpu.isBreakpointSupported()) {
                setDebuggerColumns(Arrays.asList(
                    new BreakpointColumn(cpu), new AddressColumn(), new MnemoColumn(dis), new OpcodeColumn(dis)
                ));
            } else {
                setDebuggerColumns(Arrays.asList(
                    new AddressColumn(), new MnemoColumn(dis), new OpcodeColumn(dis)
                ));
            }
        });
    }

    @Override
    public int guessPreviousInstructionLocation() {
        PaginatingDisassembler i = ida;
        CPU cpu = this.cpu;
        if (i != null && cpu != null) {
            int location = i.rowToLocation(cpu.getInstructionLocation(), i.getCurrentInstructionRow() - 1);
            if (location < 0) {
                return Math.max(0, cpu.getInstructionLocation() - 1);
            }
            return location;
        }
        return 0;
    }

    @Override
    public void setDebuggerColumns(List<DebuggerColumn<?>> columns) {
        this.columns = columns.toArray(new DebuggerColumn[0]);
        fireTableStructureChanged();
    }
}
