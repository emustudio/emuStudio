/*
 * DebugTable.java
 *
 * Created on Piatok, 2007, november 9, 8:20
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package emu8.gui.utils;

import emu8.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import plugins.cpu.ICPU;

/**
 *
 * @author vbmacher
 */
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
            if (debug_model.getRowAddress(row) == cpu.getPC()) {
                this.setBackground(Color.RED);
                this.setForeground(Color.WHITE);
            } else { 
                this.setBackground(Color.WHITE);
                this.setForeground(Color.BLACK);
            }
            setText(" " + value.toString());
            return this;
        }
    }

    
}
