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
	private long hash;
    @SuppressWarnings("unused")
	private ISettingsHandler sHandler;
    private TerminalWindow terminalGUI;
    private TerminalDisplay terminal; // male
    private TerminalFemale female;
    private boolean femaleAttached = false;
    
    public TerminalImpl(Long hash) {
    	this.hash = hash;
    }
    
    @Override
    public boolean initialize(ICPUContext cpu, IMemoryContext mem, 
            ISettingsHandler sHandler) {
        this.sHandler = sHandler;
        terminal = new TerminalDisplay(80,25);
        female = new TerminalFemale();
        return true;
    }

    @Override
    public void showGUI() {
        if (terminalGUI == null) terminalGUI = new TerminalWindow(terminal,female);
        else terminalGUI.initTerminalLabel();
        terminalGUI.setVisible(true);
    }

    /**
     * This terminal can be connected to any serial RS232 device.
     */
    public IDeviceContext getNextContext() { return terminal; }

    @Override
    public boolean attachDevice(IDeviceContext male) {
        if (!femaleAttached) {
            female.attachDevice(male);
            femaleAttached = true;
            return true;
        }
        return false;
    }

    @Override
    public void detachAll() {
        if (!femaleAttached) return;
        female.detachDevice();
        femaleAttached = false;
    }

    @Override
    public void reset() {
        terminal.clear_screen();
    }

    @Override
    public String getTitle() { return "Terminal ADM-3A"; }
    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2007-2009, Peter Jakubčo";
    }
    @Override
    public String getDescription() {
        return "Implementation of virtual terminal LSI ADM-3A";
    }

    @Override
    public String getVersion() {
        return "0.12b1";
    }

    @Override
    public void destroy() {
        if (terminalGUI != null) terminalGUI.destroyMe();
    	detachAll();
    }

	@Override
	public long getHash() { return hash; }

	@Override
	public void showSettings() {
		// TODO Auto-generated method stub
		
	}

}
