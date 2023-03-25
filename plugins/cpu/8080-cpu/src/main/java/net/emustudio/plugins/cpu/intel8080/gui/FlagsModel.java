/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.cpu.intel8080.gui;

import net.emustudio.plugins.cpu.intel8080.EmulatorEngine;

import javax.swing.table.AbstractTableModel;
import java.util.Objects;

class FlagsModel extends AbstractTableModel {
    private final String[] flags = {"S", "Z", "A", "P", "C"};
    private final int[] flagsI = {0, 0, 0, 0, 0};
    private final EmulatorEngine engine;

    FlagsModel(EmulatorEngine engine) {
        this.engine = Objects.requireNonNull(engine);
    }

    @Override
    public int getRowCount() {
        return 2;
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return flags[columnIndex];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (rowIndex) {
            case 0:
                return flags[columnIndex];
            case 1:
                return flagsI[columnIndex];
        }
        return null;
    }

    @Override
    public void fireTableDataChanged() {
        short F = engine.flags;
        flagsI[0] = ((F & EmulatorEngine.FLAG_S) != 0) ? 1 : 0;
        flagsI[1] = ((F & EmulatorEngine.FLAG_Z) != 0) ? 1 : 0;
        flagsI[2] = ((F & EmulatorEngine.FLAG_AC) != 0) ? 1 : 0;
        flagsI[3] = ((F & EmulatorEngine.FLAG_P) != 0) ? 1 : 0;
        flagsI[4] = ((F & EmulatorEngine.FLAG_C) != 0) ? 1 : 0;
        super.fireTableDataChanged();
    }
}
