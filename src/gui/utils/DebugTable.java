/*
 * DebugTable.java
 *
 * Created on Piatok, 2007, november 9, 8:20
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
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

package gui.utils;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import plugins.cpu.ICPU;

/**
 *
 * @author vbmacher
 */
@SuppressWarnings("serial")
public class DebugTable extends JTable {
    private DebugTableModel debug_model;
    private ICPU cpu;
    
    /** Creates a new instance of DebugTable */
    public DebugTable(DebugTableModel tblModel, ICPU cpu) {
        this.debug_model = tblModel;
        this.cpu = cpu;
        setModel(tblModel);
        setDefaultRenderer(Object.class, new DebugCellRenderer());
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }
    
        /* farebnost a zobrazenie buniek v tabulke ladenia programu */
    public class DebugCellRenderer extends JLabel implements TableCellRenderer {
        public DebugCellRenderer() {  super(); setOpaque(true); }
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (debug_model.getRowAddress(row) == cpu.getInstrPosition()) {
                this.setBackground(Color.RED);
                this.setForeground(Color.WHITE);
            } else { 
                this.setBackground(Color.WHITE);
                this.setForeground(Color.BLACK);
            }
            if (value != null)  setText(" " + value.toString());
            else setText(" ");
            return this;
        }
    }

    
}
