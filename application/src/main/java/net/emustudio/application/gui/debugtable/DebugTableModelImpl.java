/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.cpu.InvalidInstructionException;
import net.emustudio.emulib.runtime.ui.debugger.*;

import javax.swing.event.TableModelEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class DebugTableModelImpl extends DebugTableModel {
    private DebuggerColumn<?>[] columns = new DebuggerColumn[0];
    private CPU cpu;
    private PaginatingDisassembler ida;
    private DisassemblyCache disassemblyCache;

    public DebugTableModelImpl() {
    }

    public void setCPU(CPU cpu, Supplier<Integer> getMemorySize) {
        this.cpu = Objects.requireNonNull(cpu);
        Disassembler disassembler = cpu.getDisassembler();
        CallFlow callFlow = new CallFlow(disassembler);
        this.ida = new PaginatingDisassembler(callFlow, getMemorySize);
        this.disassemblyCache = new DisassemblyCache(disassembler);
        setDefaultColumns();
    }

    public void setMaxRows(int maxRows) {
        if (ida != null) {
            ida.setInstructionsPerPage(maxRows);
            fireTableChanged(new TableModelEvent(this));
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
        if (ida != null) {
            ida.pagePrevious();
            fireTableDataChanged();
        }
    }

    @Override
    public void seekBackwardPage(final int value) {
        if (ida != null) {
            for (int counter = 0; counter < value; counter++) {
                ida.pagePrevious();
            }
            fireTableDataChanged();
        }
    }

    @Override
    public void firstPage() {
        if (ida != null) {
            ida.pageFirst();
            fireTableDataChanged();
        }
    }

    @Override
    public void nextPage() {
        if (ida != null) {
            ida.pageNext();
            fireTableDataChanged();
        }
    }

    @Override
    public void seekForwardPage(final int value) {
        if (ida != null) {
            for (int counter = 0; counter < value; counter++) {
                ida.pageNext();
            }
            fireTableDataChanged();
        }
    }

    @Override
    public void lastPage() {
        if (ida != null) {
            ida.pageLast();
            fireTableDataChanged();
        }
    }

    @Override
    public void currentPage() {
        if (ida != null) {
            ida.pageCurrent();
            fireTableDataChanged();
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (ida == null) return null;
        int location = ida.rowToLocation(cpu.getInstructionLocation(), rowIndex);
        if (location == -1) {
            return getEmptyValue(columns[columnIndex]);
        }

        return getColumnValue(columns[columnIndex], location);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (ida == null) return;
        int location = ida.rowToLocation(cpu.getInstructionLocation(), rowIndex);
        if (location == -1) return;
        
        DebuggerColumn<?> column = columns[columnIndex];
        if (value.getClass() == column.getClassType()) {
            try {
                column.setValue(location, value);
            } catch (CannotSetDebuggerValueException ignored) {
            }
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (ida == null || !columns[columnIndex].isEditable()) {
            return false;
        }

        return ida.rowToLocation(cpu.getInstructionLocation(), rowIndex) != -1;
    }

    @Override
    public boolean isRowAtCurrentInstruction(int rowIndex) {
        return ida != null && ida.isRowAtCurrentInstruction(rowIndex);
    }

    @Override
    public void memoryChanged(int from, int to) {
        if (ida != null) {
            ida.flushCache(from, to + 1);
        }
        if (disassemblyCache != null) {
            disassemblyCache.clear();
        }
        fireTableDataChanged();
    }

    @Override
    public void memorySizeChanged(int memorySize) {
        fireTableChanged(new TableModelEvent(this));
    }

    @Override
    public final void setDefaultColumns() {
        if (cpu == null) return;
        
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

    @Override
    public void executionStateChanged() {
        if (disassemblyCache != null) {
            disassemblyCache.clear();
        }
    }

    private Object getColumnValue(DebuggerColumn<?> column, int location) {
        if (!(column instanceof MnemoColumn) && !(column instanceof OpcodeColumn)) {
            return column.getValue(location);
        }

        try {
            DisassembledInstruction instruction = disassemblyCache.get(location);
            if (column instanceof MnemoColumn) {
                return instruction.getMnemo();
            }
            return instruction.getOpCode();
        } catch (InvalidInstructionException e) {
            return (column instanceof MnemoColumn) ? "[invalid]" : "";
        } catch (IndexOutOfBoundsException e) {
            return (column instanceof MnemoColumn) ? "[incomplete]" : "";
        }
    }

    private Object getEmptyValue(DebuggerColumn<?> column) {
        return (column.getClassType() == Boolean.class) ? Boolean.FALSE : null;
    }
}
