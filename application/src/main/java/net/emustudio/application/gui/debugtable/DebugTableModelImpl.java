/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.application.gui.debugtable.PaginatingDisassembler.BoundaryDecoder;

import net.emustudio.application.debugger.InstructionIndex;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;

import javax.swing.event.TableModelEvent;
import java.util.List;
import java.util.Objects;

public class DebugTableModelImpl extends DebugTableModel {
    private DebuggerColumn<?>[] columns = new DebuggerColumn[0];
    private CPU cpu;
    private PaginatingDisassembler ida;
    private BoundaryDecoder boundaryDecoder;
    private Integer inspectedLocation;
    private InstructionIndex instructionIndex;
    private InstructionIndex.View indexedView;
    private int[] rowLocations;
    private int cachedAnchor = -1, cachedPage;

    public DebugTableModelImpl() {
    }

    public void setCPU(CPU cpu) {
        this.cpu = Objects.requireNonNull(cpu);
        inspectedLocation = null;
        boundaryDecoder = new BoundaryDecoder(cpu.getDisassembler());
        this.ida = new PaginatingDisassembler(boundaryDecoder, cpu.getAddressSpaceSize());
        rowLocations = null;
        indexedView = null;
        List<DebuggerColumn<?>> cpuColumns = cpu.getDebuggerColumns();
        setDebuggerColumns(cpuColumns.isEmpty() ? defaultColumns(cpu.getDisassembler()) : cpuColumns);
    }

    /** Optional logical instruction view; no analysis is performed here. */
    public void setInstructionIndex(InstructionIndex instructionIndex) {
        this.instructionIndex = Objects.requireNonNull(instructionIndex);
        indexedView = null;
        resetPagination();
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
            if (rowLocations != null && rowLocations.length != rows) rowLocations = null;
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
            materializePage();
            ida.pagePrevious();
            rowLocations = null;
            fireTableDataChanged();
        }
    }

    @Override
    public void seekBackwardPage(final int value) {
        if (ida != null) {
            materializePage();
            for (int counter = 0; counter < value; counter++) {
                ida.pagePrevious();
            }
            rowLocations = null;
            fireTableDataChanged();
        }
    }

    @Override
    public void firstPage() {
        if (ida != null) {
            materializePage();
            ida.pageFirst();
            rowLocations = null;
            fireTableDataChanged();
        }
    }

    @Override
    public void nextPage() {
        if (ida != null) {
            materializePage();
            ida.pageNext();
            rowLocations = null;
            fireTableDataChanged();
        }
    }

    @Override
    public void seekForwardPage(final int value) {
        if (ida != null) {
            materializePage();
            for (int counter = 0; counter < value; counter++) {
                ida.pageNext();
            }
            rowLocations = null;
            fireTableDataChanged();
        }
    }

    @Override
    public void lastPage() {
        if (ida != null) {
            materializePage();
            ida.pageLast();
            rowLocations = null;
            fireTableDataChanged();
        }
    }

    @Override
    public void currentPage() {
        if (ida != null) {
            inspectedLocation = null;
            ida.pageCurrent();
            rowLocations = null;
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
        rowLocations = null;
        materializePage();
        fireTableDataChanged();
    }

    private int viewLocation() {
        return inspectedLocation == null ? cpu.getInstructionLocation() : inspectedLocation;
    }

    private void resetPagination() {
        if (ida == null || boundaryDecoder == null) { rowLocations = null; return; }
        int rows = ida.getInstructionsPerPage();
        if (ida.getPageIndex() != 0 && rowLocations != null && rowLocations.length > 0) {
            int center = rowLocations[rowLocations.length / 2];
            if (center >= 0) inspectedLocation = center;
        }
        boundaryDecoder = new BoundaryDecoder(cpu.getDisassembler());
        if (indexedView != null) boundaryDecoder.useIndex(indexedView);
        ida = new PaginatingDisassembler(boundaryDecoder, cpu.getAddressSpaceSize());
        ida.setInstructionsPerPage(rows);
        rowLocations = null;
    }

    private void materializePage() {
        if (ida == null || cpu == null) return;
        InstructionIndex.View view = instructionIndex == null ? null : instructionIndex.snapshot();
        if (view != indexedView) {
            indexedView = view;
            resetPagination();
        }
        int rows = ida.getInstructionsPerPage(), anchor = viewLocation(), page = ida.getPageIndex();
        if (rows <= 0 || (rowLocations != null && rowLocations.length == rows
                && cachedAnchor == anchor && cachedPage == page)) return;
        int[] locations = new int[rows];
        for (int row = 0; row < rows; row++) locations[row] = ida.rowToLocation(anchor, row);
        rowLocations = locations;
        cachedAnchor = anchor;
        cachedPage = page;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (ida == null) return null;
        int location = getLocationAt(rowIndex);
        if (location == -1) {
            return getEmptyValue(columns[columnIndex]);
        }
        return columns[columnIndex].getValue(location);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (ida == null) return;
        int location = getLocationAt(rowIndex);
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

        return getLocationAt(rowIndex) != -1;
    }

    @Override
    public boolean isRowAtCurrentInstruction(int rowIndex) {
        return ida != null && getLocationAt(rowIndex) == cpu.getInstructionLocation();
    }

    @Override
    public int getLocationAt(int rowIndex) {
        if (ida == null) return -1;
        materializePage();
        return rowLocations == null || rowIndex < 0 || rowIndex >= rowLocations.length ? -1 : rowLocations[rowIndex];
    }

    @Override
    public void memoryChanged(int from, int to) {
        if (ida != null) {
            ida.flushCache(from, to + 1);
            resetPagination();
        }
        fireTableDataChanged();
    }

    static List<DebuggerColumn<?>> defaultColumns(Disassembler disassembler) {
        return List.of(
                DebuggerColumn.address("%04X"), DebuggerColumn.mnemo(disassembler), DebuggerColumn.opcode(disassembler)
        );
    }

    @Override
    public int guessPreviousInstructionLocation() {
        materializePage();
        PaginatingDisassembler i = ida;
        CPU cpu = this.cpu;
        if (i != null && cpu != null) {
            try {
                i.rowToLocation(cpu.getInstructionLocation(), 0);
                int location = i.rowToLocation(cpu.getInstructionLocation(), i.getCurrentInstructionRow() - 1);
                if (location < 0) return Math.max(0, cpu.getInstructionLocation() - 1);
                return location;
            } finally {
                // This CPU-relative lookup may have moved a separately browsed paginator.
                rowLocations = null;
            }
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
