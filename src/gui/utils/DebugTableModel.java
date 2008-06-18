/*
 * DebugTableModel.java
 *
 * Created on Pondelok, 2007, marec 26, 16:29
 *
 * KEEP IT SIMPLY STUPID
 * YOU AREN'T GONNA NEED IT
 */

package gui.utils;

import javax.swing.table.*;

import plugins.memory.IMemory;
import plugins.cpu.ICPU;
import plugins.compiler.ICompiler;

/**
 *
 * @author vbmacher
 */
public class DebugTableModel extends AbstractTableModel {
    private ICPU cpu;
    private ICompiler compiler;
    private IMemory mem;
    
    private int nextAddress;
    private int lastRow;
    private int firstRowAddress;
    
    private final int MAX_ROW_COUNT = 25;
    
    /** Creates a new instance of DebugTableModel */
    public DebugTableModel(ICPU cpu, ICompiler comp, IMemory mem) {
        this.cpu = cpu;
        this.compiler = comp;
        this.mem = mem;
        lastRow = 0;
        nextAddress = this.getFirstRowAddress();
        firstRowAddress = 0;
    }

    // pocet riadkov
    public int getRowCount() {
        int a = mem.getContext().getSize() - cpu.getContext().getInstrPosition();
        if (a < MAX_ROW_COUNT) return a;
        else return MAX_ROW_COUNT;
    }

    public int getColumnCount() {
        return cpu.getDebugColumns().length;
    }

    public String getColumnName(int col) {
        return cpu.getDebugColumns()[col].getName();
    }
    
    public Class getColumnClass(int columnIndex) {
        return cpu.getDebugColumns()[columnIndex].getType();
    }
    
    // zisti adresu prveho riadku v tabulke, ktory sa zobrazi
    // urychlena verzia
    private int getFirstRowAddress() {
        int pc = cpu.getContext().getInstrPosition();
        // max 10 operacnych kodov sa zobrazi pred aktualnou instrukciou
        firstRowAddress = 0;
        while (pc > firstRowAddress+10)
            firstRowAddress = cpu.getContext().getNextInstrPos(firstRowAddress);
        return firstRowAddress;
    }
    
    public int getRowAddress(int rowIndex) {
        if (rowIndex == 0) return this.getFirstRowAddress();
        else if (rowIndex == (lastRow +1)) return nextAddress;
        else { int i = 0, a = 0;
            a = this.getFirstRowAddress();
            for (i = 0; i < rowIndex; i++) a = cpu.getContext().getNextInstrPos(a);
            return a;
        }
    }
    
    public Object getValueAt(int rowIndex, int columnIndex) {
        int addr;
        // get position of first row - user can see 8 instruction before actual
        if (rowIndex == 0) { nextAddress = this.getFirstRowAddress(); lastRow = 0; }
        else if (rowIndex != (lastRow +1)) {
            // move nextAddress pointer to address for current row
            int i = 0;
            nextAddress = this.getFirstRowAddress();
            for (i = 0; i < rowIndex; i++) 
                nextAddress = cpu.getContext().getNextInstrPos(nextAddress);
            lastRow = i;
        }
        addr = nextAddress;
        nextAddress = cpu.getContext().getNextInstrPos(nextAddress);
        lastRow = rowIndex;
        return cpu.getDebugValue(addr, columnIndex);
    }
    
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        int a = getRowAddress(rowIndex);
        cpu.setDebugValue(a,columnIndex, aValue);
    }
    
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return cpu.getDebugColumns()[columnIndex].isEditable();
    }
    
}
