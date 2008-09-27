/*
 * TerminalImpl.java
 *
 * Created on 28.7.2008, 19:12:19
 * hold to: KISS, YAGNI
 *
 */

package terminal;

import gui.TerminalWindow;
import plugins.ISettingsHandler;
import plugins.cpu.ICPUContext;
import plugins.device.IDevice;
import plugins.device.IDeviceContext;
import plugins.memory.IMemoryContext;

/**
 *
 * @author vbmacher
 */
public class TerminalImpl implements IDevice {
    private ISettingsHandler sHandler;
    private TerminalWindow terminalGUI;
    private TerminalDisplay terminal; // male
    private TerminalFemale female;
    private boolean femaleAttached = false;
    
    public boolean initialize(ICPUContext cpu, IMemoryContext mem, 
            ISettingsHandler sHandler) {
        this.sHandler = sHandler;
        terminal = new TerminalDisplay(80,25);
        female = new TerminalFemale(terminal);
        return true;
    }

    public void showGUI() {
        if (terminalGUI == null) terminalGUI = new TerminalWindow(terminal,female);
        else terminalGUI.initTerminalLabel();
        terminalGUI.setVisible(true);
    }

    /**
     * Return female plug.
     */
    public IDeviceContext getFreeFemale() { return female; }

    /**
     * This terminal can be connected to any serial RS232 device.
     */
    public IDeviceContext getFreeMale() { return terminal; }

    public boolean attachDevice(IDeviceContext female, IDeviceContext male) {
        if (female == this.female && !femaleAttached) {
            this.female.attachDevice(male);
            femaleAttached = true;
            return true;
        }
        return false;
    }

    public void detachDevice(IDeviceContext device, boolean male) {
        if (!femaleAttached) return;
        if (male) {
            female.detachDevice();
            femaleAttached = false;
        }
    }

    public void reset() {
        terminal.clear_screen();
    }

    public String getName() { return "Terminal ADM-3A"; }
    public String getCopyright() {
        return "\u00A9 Copyright 2007-2008, Peter Jakubčo";
    }
    public String getDescription() {
        return "Implementation of virtual terminal LSI ADM-3A";
    }

    public String getVersion() {
        return "0.12b1";
    }

    public void destroy() {
        if (terminalGUI != null) terminalGUI.destroyMe();
        female.detachDevice();
        femaleAttached = false;
    }

}
