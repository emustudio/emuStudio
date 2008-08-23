/*
 * CpuZ80.java
 *
 * Created on 23.8.2008, 12:53:21
 * hold to: KISS, YAGNI
 *
 */

package impl;

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

    public JPanel getStatusGUI() {
        return null;
    }

    public IDebugColumn[] getDebugColumns() {
        return null;
    }

    public void setDebugValue(int row, int col, Object value) {
        
    }

    public Object getDebugValue(int row, int col) {
        return null;
    }

    public boolean isBreakpointSupported() {
        return false;
    }

    public void setBreakpoint(int pos, boolean set) {
        
    }

    public boolean getBreakpoint(int pos) {
        return false;
    }

    public void reset() {}

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
