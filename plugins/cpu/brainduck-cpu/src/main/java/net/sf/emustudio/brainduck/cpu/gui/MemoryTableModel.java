/*
 * Copyright (C) 2014 Peter Jakubčo
 *
 * KISS, YAGNI, DRY
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.emustudio.brainduck.cpu.gui;

import emulib.plugins.memory.MemoryContext;
import javax.swing.table.AbstractTableModel;

public class MemoryTableModel extends AbstractTableModel {
    private final MemoryContext memory;
    private volatile int P;

    public MemoryTableModel(MemoryContext memory) {
        this.memory = memory;
    }
    
    @Override
    public int getRowCount() {
        return 1;
    }

    @Override
    public int getColumnCount() {
        return 5;
    }
    
    @Override
    public String getColumnName(int columnIndex) {
        int index = P + (columnIndex - 2);
        if (index >= 0) {
            return String.format("%02Xh", index);
        }
        return "N/A";
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        int index = P + (columnIndex - 2);
        if (index >= 0) {
            return String.format("%02Xh", memory.read(index));
        }
        return "";
    }

    public void setP(int P) {
        this.P = P;
        fireTableDataChanged();
    }
}
