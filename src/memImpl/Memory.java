/*
 * Memory.java
 *
 * Created on Sobota, 2007, okt�ber 27, 11:58
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package memImpl;

import gui.frmMemory;
import plugins.ISettingsHandler;
import plugins.memory.IMemory;
import plugins.memory.IMemoryContext;


/**
 *
 * @author vbmacher
 */
public class Memory implements IMemory {
    private MemoryContext memContext;
    private frmMemory memGUI;
    private ISettingsHandler settings;

    public String getDescription() {
        return "Operating memory for most CPUs. This is very simple "
                + "implementation without any support of banking, segmentation,"
                + "or pagination.";
    }

    public String getVersion() { return "0.21b"; }

    public String getName() {
        return "Standard linear-byte operating memory with variable size";
    }

    public String getCopyright() {
        return "\u00A9 Copyright 2006-2008, Peter Jakubčo";
    }

    /** Creates a new instance of Memory */
    public Memory() {
        memContext = new MemoryContext();
    }

    public void showGUI() {
        if (memGUI == null) memGUI = new frmMemory(memContext);
        memGUI.setVisible(true);
    }

    public void destroy() {
        if (this.memGUI != null) {
            memGUI.dispose();
            this.memGUI = null;
        }
    }

    public IMemoryContext getContext() {
        return memContext;
    }

    public void initialize(int size, ISettingsHandler sHandler) {
        memContext.init(size);
        this.settings = sHandler;
    }

    /**
     * Clear memory? no.. not
     */
    public void reset() {}

    public void setProgramStart(int address) {
        memContext.lastImageStart = address;
    }

}
