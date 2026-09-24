/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
        int flagsValue;
        if (registersSet == 0) {
            flagsValue = cpu.flags;
        } else {
            flagsValue = cpu.flags2;
        }
        update(flagsValue);
    }

    void update(int flagsValue) {
        flagsI[0] = ((flagsValue & EmulatorEngine.FLAG_S) != 0) ? 1 : 0;
        flagsI[1] = ((flagsValue & EmulatorEngine.FLAG_Z) != 0) ? 1 : 0;
        flagsI[2] = ((flagsValue & EmulatorEngine.FLAG_H) != 0) ? 1 : 0;
        flagsI[3] = ((flagsValue & EmulatorEngine.FLAG_PV) != 0) ? 1 : 0;
        flagsI[4] = ((flagsValue & EmulatorEngine.FLAG_N) != 0) ? 1 : 0;
        flagsI[5] = ((flagsValue & EmulatorEngine.FLAG_C) != 0) ? 1 : 0;
        super.fireTableDataChanged();
    }

}
