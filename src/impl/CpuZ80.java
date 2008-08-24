/*
 * CpuZ80.java
 *
 * Created on 23.8.2008, 12:53:21
 * hold to: KISS, YAGNI
 *
 */

package impl;

import gui.statusGUI;
import java.util.HashSet;
import javax.swing.JPanel;
import plugins.ISettingsHandler;
import plugins.cpu.ICPU;
import plugins.cpu.ICPUContext;
import plugins.cpu.IDebugColumn;
import plugins.memory.IMemoryContext;

/**
 *
 * @author vbmacher
 */
public class CpuZ80 implements ICPU {
    private statusGUI status;

    private HashSet breaks; // zoznam breakpointov (mnozina)

    // 2 sets of 6 GPR
    private short[] B; private short[] C;
    private short[] D; private short[] E;
    private short[] H; private short[] L;
    
    // accumulator and flags
    private short[] A; private short[] F;
    
    // special registers
    private int PC = 0; private int SP = 0;
    private int IX = 0; private int IY = 0;
    private short I = 0; private short R = 0; // interrupt r., refresh r.
    
    private boolean[] IFF; // interrupt enable flip-flops
    
    public CpuZ80() {
        breaks = new HashSet();
        B = new short[2];
        C = new short[2];
        D = new short[2];
        E = new short[2];
        H = new short[2];
        L = new short[2];
        A = new short[2];
        F = new short[2];
        IFF = new boolean[2];
        status = new statusGUI();
    }
    
    public boolean initialize(IMemoryContext mem, ISettingsHandler sHandler) {
        return true;
    }

    public void step() {
        
    }

    public void execute() {
        
    }

    public void pause() {
        
    }

    public void stop() {
        
    }

    public ICPUContext getContext() {
        return null;
    }

    /* GUI interaction */
    public IDebugColumn[] getDebugColumns() { return null; /*return status.getDebugColumns();*/ }
    public void setDebugValue(int index, int col, Object value) {
        //status.setDebugColVal(index, col, value);
    }
    public Object getDebugValue(int index, int col) {
        return null;
        //return status.getDebugColVal(index, col);
    }
    public JPanel getStatusGUI() { return status; }

    // breakpoints
    public boolean isBreakpointSupported() { return true; }
    public void setBreakpoint(int pos, boolean set) {
        if (set) breaks.add(pos);
        else breaks.remove(pos);
    }
    public boolean getBreakpoint(int pos) { return breaks.contains(pos); }

    public void reset() {
        PC = SP = IX = IY = 0;
        I = R = 0;
        for (int i = 0; i < 2; i++) {
            A[i] = B[i] = C[i] = D[i] = E[i] = H[i] = L[i] = 0;
            IFF[i] = false;
        }
    }

    public String getName() {
        return "Zilog Z80";
    }

    public String getCopyright() {
        return "\u00A9 Copyright 2008, Peter Jakubčo";
    }

    public String getDescription() {
        return "Implementation of Zilog Z80 8bit CPU. With its architecture"
               + " it is similar to Intel's 8080 but something is modified and"
               + " extended.";
    }

    public String getVersion() {
        return "0.1a";
    }

    public void destroy() {}

}
