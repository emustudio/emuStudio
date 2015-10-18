/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2015, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package emustudio.gui.debugTable;

import emulib.emustudio.debugtable.AddressColumn;
import emulib.emustudio.debugtable.BreakpointColumn;
import emulib.emustudio.debugtable.MnemoColumn;
import emulib.emustudio.debugtable.OpcodeColumn;
import emulib.plugins.cpu.CPU;
import emulib.plugins.cpu.DebugColumn;
import emulib.plugins.cpu.Disassembler;

import javax.swing.table.AbstractTableModel;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class DebugTableModel extends AbstractTableModel {
    private DebugColumn[] columns;
    private final CPU cpu;
    private final InteractiveDisassembler ida;

    public DebugTableModel(CPU cpu, int memorySize) {
        this.cpu = Objects.requireNonNull(cpu);

        Disassembler dis = cpu.getDisassembler();
        this.ida = new InteractiveDisassembler(dis, memorySize);
        setDefaultColumns();
    }

    @Override
    public int getRowCount() {
        return InteractiveDisassembler.INSTRUCTIONS_PER_PAGE;
    }

    public DebugColumn getColumnAt(int index) {
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

    public void previousPage() {
        ida.pagePrevious();
        fireTableDataChanged();
    }

    public void seekBackwardPage(int value) {
        while (value > 0) {
            ida.pagePrevious();
            value--;
        }
    }

    public void firstPage() {
        ida.pageFirst();
        fireTableDataChanged();
    }

    public void nextPage() {
        ida.pageNext();
        fireTableDataChanged();
    }

    public void seekForwardPage(int value) {
        while (value > 0) {
            ida.pageNext();
            value--;
        }
    }

    public void lastPage() {
        ida.pageLast();
        fireTableDataChanged();
    }

    public void currentPage() {
        ida.pageCurrent();
        fireTableDataChanged();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        int location = ida.rowToLocation(cpu.getInstructionPosition(), rowIndex);
        if (location != -1) {
            return columns[columnIndex].getDebugValue(location);
        }
        return null;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        int location = ida.rowToLocation(cpu.getInstructionPosition(), rowIndex);
        if (location != -1) {
            columns[columnIndex].setDebugValue(location, value);
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columns[columnIndex].isEditable();
    }

    public boolean isRowAtCurrentInstruction(int rowIndex) {
        return InteractiveDisassembler.CURRENT_INSTRUCTION == rowIndex;
    }

    public void memoryChanged(int from, int to) {
        ida.flushCache(from, to + 1);
    }

    public void setMemorySize(int memorySize) {
        ida.setMemorySize(memorySize);
        fireTableDataChanged();
    }

    public void setColumns(List<DebugColumn> columns) {
        this.columns = columns.toArray(new DebugColumn[0]);
        fireTableStructureChanged();
    }

    public void setDefaultColumns() {
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

}
