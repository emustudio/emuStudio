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
package net.emustudio.plugins.cpu.zilogZ80.gui;

import net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine;

import javax.swing.table.AbstractTableModel;

class FlagsModel extends AbstractTableModel {
    private static final String[] FLAG_NAMES = {"S", "Z", "H", "P/V", "N", "C"};
    private final int[] flagsI = {0, 0, 0, 0, 0, 0};
    private final int registersSet;
    private final EmulatorEngine cpu;

    public FlagsModel(int registersSet, final EmulatorEngine cpu) {
        this.cpu = cpu;
        this.registersSet = registersSet;
    }

    @Override
    public int getRowCount() {
        return 1;
    }

    @Override
    public int getColumnCount() {
        return 6;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return FLAG_NAMES[columnIndex];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return flagsI[columnIndex];
    }

    @Override
    public void fireTableDataChanged() {
        int F;
        if (registersSet == 0) {
            F = cpu.flags;
        } else {
            F = cpu.flags2;
        }
        flagsI[0] = ((F & EmulatorEngine.FLAG_S) != 0) ? 1 : 0;
        flagsI[1] = ((F & EmulatorEngine.FLAG_Z) != 0) ? 1 : 0;
        flagsI[2] = ((F & EmulatorEngine.FLAG_H) != 0) ? 1 : 0;
        flagsI[3] = ((F & EmulatorEngine.FLAG_PV) != 0) ? 1 : 0;
        flagsI[4] = ((F & EmulatorEngine.FLAG_N) != 0) ? 1 : 0;
        flagsI[5] = ((F & EmulatorEngine.FLAG_C) != 0) ? 1 : 0;
        super.fireTableDataChanged();
    }

}
