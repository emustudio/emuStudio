/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package net.sf.emustudio.devices.mits88sio.gui;

import java.util.Objects;
import javax.swing.table.AbstractTableModel;
import net.sf.emustudio.devices.mits88sio.impl.CPUPorts;

public class CPUPortsTableModel extends AbstractTableModel {
    private final CPUPorts cpuPorts;

    public CPUPortsTableModel(CPUPorts cpuPorts) {
        this.cpuPorts = Objects.requireNonNull(cpuPorts);
    }
        
    @Override
    public int getRowCount() {
        return cpuPorts.getDataPortsCount() + cpuPorts.getStatusPortsCount();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int column) {
        return column == 0 ? "Type" : "Attached to";
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        int statusCount = cpuPorts.getStatusPortsCount();
        
        if (rowIndex < statusCount) {
            return columnIndex == 0 ? "Status" : cpuPorts.getStatusPort(rowIndex);
        }
        return columnIndex == 0 ? "Data" : cpuPorts.getDataPort(rowIndex - statusCount);
    }
    
}
