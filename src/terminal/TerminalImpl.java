/*
 * TerminalImpl.java
 *
 * Created on 28.7.2008, 19:12:19
 * hold to: KISS, YAGNI
 *
 */

package terminal;

import plugins.ISettingsHandler;
import plugins.cpu.ICPUContext;
import plugins.device.IDevice;
import plugins.device.IDeviceContext;
import plugins.memory.IMemoryContext;
import terminal.gui.ConfigDialog;
import terminal.gui.TerminalWindow;

/**
 *
 * @author vbmacher
 */
public class TerminalImpl implements IDevice {
	private long hash;
	private ISettingsHandler settings;
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
        this.settings = sHandler;
        terminal = new TerminalDisplay(80,25);
        female = new TerminalFemale();
        terminalGUI = new TerminalWindow(terminal,female);
        readSettings();
        return true;
    }

    private void readSettings() {
    	String s;
    	
    	s = settings.readSetting(hash, "verbose");
        if (s != null && s.toUpperCase().equals("TRUE")) {
        	terminal.setVerbose(true);
        	terminalGUI.setVisible(true);
        } else
        	terminal.setVerbose(false);
    	
    	s = settings.readSetting(hash, "duplex_mode");
        if (s != null && s.toUpperCase().equals("HALF"))
        	terminalGUI.setHalfDuplex(true);
        else terminalGUI.setHalfDuplex(false);
        
        s = settings.readSetting(hash, "always_on_top");
        if (s != null && s.toUpperCase().equals("TRUE"))
        	terminalGUI.setAlwaysOnTop(true);
        else terminalGUI.setAlwaysOnTop(false);
        
        s = settings.readSetting(hash, "anti_aliasing");
        if (s != null && s.toUpperCase().equals("TRUE"))
        	terminal.setAntiAliasing(true);
        else terminal.setAntiAliasing(false); 
    }
    
    
    @Override
    public void showGUI() {
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
        return "0.15b";
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
		new ConfigDialog(settings,hash, terminalGUI, terminal).setVisible(true);
	}

}
