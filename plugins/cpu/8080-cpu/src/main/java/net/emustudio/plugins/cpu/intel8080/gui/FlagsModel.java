/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
