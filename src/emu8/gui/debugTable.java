/*
 * debugTable.java
 *
 * Created on Piatok, 2007, november 9, 8:20
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package emu8.gui;

import emu8.*;
import emu8.gui.debugTableModel;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

/**
 *
 * @author vbmacher
 */
public class debugTable extends JTable {
    private debugTableModel debug_model;
    private ArchitectureLoader emuConfig;
    
    /** Creates a new instance of debugTable */
    public debugTable(debugTableModel tblModel, ArchitectureLoader emuConfig) {
        this.debug_model = tblModel;
        this.emuConfig = emuConfig;
        setModel(tblModel);
        setDefaultRenderer(Object.class, new DebugCellRenderer());
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }
    
        /* farebnost a zobrazenie buniek v tabulke ladenia programu */
    public class DebugCellRenderer extends JLabel implements TableCellRenderer {
        public DebugCellRenderer() {  super(); setOpaque(true); }
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (debug_model.getRowAddress(row) == emuConfig.cCPU.getPC()) {
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
