/*
 * FlagsModel.java
 *
 * Copyright (C) 2008-2012 Peter Jakubčo
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
package net.sf.emustudio.zilogZ80.gui;

import javax.swing.table.AbstractTableModel;
import net.sf.emustudio.zilogZ80.impl.EmulatorImpl;

/**
 *
 * @author vbmacher
 */
class FlagsModel extends AbstractTableModel {
    private String[] flags = {"S", "Z", "H", "P/V", "N", "C"};
    private int[] flagsI = {0, 0, 0, 0, 0, 0};
    private int set;
    private final EmulatorImpl cpu;

    public FlagsModel(int set, final EmulatorImpl cpu) {
        this.cpu = cpu;
        this.set = set;
    }

    @Override
    public int getRowCount() {
        return 2;
    }

    @Override
    public int getColumnCount() {
        return 6;
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
        short F;
        if (set == 0) {
            F = cpu.F;
        } else {
            F = cpu.F1;
        }
        flagsI[0] = ((F & EmulatorImpl.flagS) != 0) ? 1 : 0;
        flagsI[1] = ((F & EmulatorImpl.flagZ) != 0) ? 1 : 0;
        flagsI[2] = ((F & EmulatorImpl.flagH) != 0) ? 1 : 0;
        flagsI[3] = ((F & EmulatorImpl.flagPV) != 0) ? 1 : 0;
        flagsI[4] = ((F & EmulatorImpl.flagN) != 0) ? 1 : 0;
        flagsI[5] = ((F & EmulatorImpl.flagC) != 0) ? 1 : 0;
        super.fireTableDataChanged();
    }
    
}
