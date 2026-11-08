/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

// Benchmark-only baseline from 184f8d5a49fd40d99e553a80496c508e2e699618; class names only changed.

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;

import javax.swing.event.TableModelEvent;
import java.util.List;
import java.util.Objects;

public class BaselineDebugTableModelImpl extends DebugTableModel {
    private DebuggerColumn<?>[] columns = new DebuggerColumn[0];
    private CPU cpu;
    private BaselinePaginatingDisassembler ida;
    private Integer inspectedLocation;

    public BaselineDebugTableModelImpl() {
    }

    public void setCPU(CPU cpu) {
        this.cpu = Objects.requireNonNull(cpu);
        inspectedLocation = null;
        BaselineCallFlow callFlow = new BaselineCallFlow(cpu.getDisassembler());
        this.ida = new BaselinePaginatingDisassembler(callFlow, cpu.getAddressSpaceSize());
        List<DebuggerColumn<?>> cpuColumns = cpu.getDebuggerColumns();
        setDebuggerColumns(cpuColumns.isEmpty() ? DebugTableModelImpl.defaultColumns(cpu.getDisassembler()) : cpuColumns);
    }

    public void setMaxRows(int maxRows) {
        if (ida != null) {
            int rows = Math.max(1, maxRows);
            if (cpu != null && ida.getInstructionsPerPage() != rows && ida.getPageIndex() != 0) {
                ida.rowToLocation(viewLocation(), 0);
                int anchor = ida.rowToLocation(viewLocation(), ida.getCurrentInstructionRow());
                if (anchor >= 0) inspectedLocation = anchor;
            }
            ida.setInstructionsPerPage(rows);
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
            inspectedLocation = null;
            ida.pageCurrent();
            fireTableDataChanged();
        }
    }

    @Override
    public void showAddress(int address) {
        if (cpu == null) return;
        if (address < 0 || address >= cpu.getAddressSpaceSize()) {
            throw new IllegalArgumentException("Address outside CPU");
        }
        inspectedLocation = address;
        ida.pageCurrent();
        ida.rowToLocation(address, 0);
        ida.rowToLocation(address, ida.getInstructionsPerPage() - 1);
        fireTableDataChanged();
    }

    private int viewLocation() {
        return inspectedLocation == null ? cpu.getInstructionLocation() : inspectedLocation;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (ida == null) return null;
        int location = ida.rowToLocation(viewLocation(), rowIndex);
        if (location == -1) {
            return getEmptyValue(columns[columnIndex]);
        }
        return columns[columnIndex].getValue(location);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (ida == null) return;
        int location = ida.rowToLocation(viewLocation(), rowIndex);
        if (location == -1) return;
        
        DebuggerColumn<?> column = columns[columnIndex];
        if (column.getClassType().isInstance(value)) {
            try {
                column.setValue(location, value);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (ida == null || !columns[columnIndex].isEditable()) {
            return false;
        }

        return ida.rowToLocation(viewLocation(), rowIndex) != -1;
    }

    @Override
    public boolean isRowAtCurrentInstruction(int rowIndex) {
        return ida != null && getLocationAt(rowIndex) == cpu.getInstructionLocation();
    }

    @Override
    public int getLocationAt(int rowIndex) {
        return ida == null ? -1 : ida.rowToLocation(viewLocation(), rowIndex);
    }

    @Override
    public void memoryChanged(int from, int to) {
        if (ida != null) {
            ida.flushCache(from, to + 1);
        }
        fireTableDataChanged();
    }

    @Override
    public int guessPreviousInstructionLocation() {
        BaselinePaginatingDisassembler i = ida;
        CPU cpu = this.cpu;
        if (i != null && cpu != null) {
            i.rowToLocation(cpu.getInstructionLocation(), 0);
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

    private Object getEmptyValue(DebuggerColumn<?> column) {
        return (column.getClassType() == Boolean.class) ? Boolean.FALSE : null;
    }
}
