/*
 * KISS, YAGNI, DRY
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

import emulib.plugins.cpu.CPU;
import emulib.plugins.cpu.DebugColumn;
import emulib.plugins.cpu.Disassembler;
import javax.swing.table.AbstractTableModel;
import java.util.Objects;

public class DebugTableModel extends AbstractTableModel {
    private final DebugColumn[] columns;
    private final CPU cpu;
    private final InteractiveDisassembler ida;
    private final int breakpointColumnIndex;

    public DebugTableModel(CPU cpu, int memorySize) {
        this.cpu = Objects.requireNonNull(cpu);

        Disassembler dis = cpu.getDisassembler();
        this.ida = new InteractiveDisassembler(dis, memorySize);

        if (cpu.isBreakpointSupported()) {
            columns = new DebugColumn[4];
            columns[0] = new BreakpointColumn(cpu);
            columns[1] = new AddressColumn();
            columns[2] = new MnemoColumn(dis);
            columns[3] = new OpcodeColumn(dis);
            breakpointColumnIndex = 0;
        } else {
            columns = new DebugColumn[3];
            columns[0] = new AddressColumn();
            columns[1] = new MnemoColumn(dis);
            columns[2] = new OpcodeColumn(dis);
            breakpointColumnIndex = -1;
        }
    }

    @Override
    public int getRowCount() {
        return InteractiveDisassembler.INSTRUCTIONS_IN_GAP * 2 + 1;
    }

    public int getBreakpointColumnIndex() {
        return breakpointColumnIndex;
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

    /**
     * Determine if the instruction at rowIndex is current
     * instruction.
     *
     * @param rowIndex The row in the debug table
     * @return true if the row represents current instruction
     */
    public boolean isCurrent(int rowIndex) {
        int location = ida.rowToLocation(cpu.getInstructionPosition(), rowIndex);
        return (cpu.getInstructionPosition() == location);
    }

}
