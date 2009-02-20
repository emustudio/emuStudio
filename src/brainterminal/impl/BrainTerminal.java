/**
 * BrainTerminal.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 * KISS, YAGNI
 */
package brainterminal.impl;

import braincpu.interfaces.IBrainCPUContext;
import brainterminal.gui.BrainTerminalDialog;
import plugins.ISettingsHandler;
import plugins.cpu.ICPUContext;
import plugins.device.IDevice;
import plugins.device.IDeviceContext;
import plugins.memory.IMemoryContext;
import runtime.StaticDialogs;

public class BrainTerminal implements IDevice {
	private final static String BRAIN_CPU_CONTEXT = "d32e73041bf765d98eea1b8664ef6b5e"; 
	private long hash;
	private IBrainCPUContext cpu;
	private BrainTerminalContext terminal; 
	private BrainTerminalDialog gui;
	
	public BrainTerminal(Long hash) {
		this.hash = hash;
		gui = new BrainTerminalDialog();
		terminal = new BrainTerminalContext(gui);
	}

	@Override
	public String getTitle() {
		return "BrainDuck terminal";
	}

	@Override
	public String getVersion() {
		return "0.1b";
	}

	@Override
	public String getCopyright() {
		return "\u00A9 Copyright 2009, P. Jakubčo";
	}

	@Override
	public String getDescription() {
		return "Simple terminal device for BrainDuck architecture";
	}
	
	@Override
	public boolean initialize(ICPUContext cpu, IMemoryContext mem,
			ISettingsHandler settings) {
		if (!(cpu instanceof IBrainCPUContext)
				|| !cpu.getHash().equals(BRAIN_CPU_CONTEXT) ) {
			StaticDialogs.showErrorMessage("BrainTerminal doesn't support this CPU");
			return false;
		}
		this.cpu = (IBrainCPUContext)cpu;
		this.cpu.attachDevice(terminal);
		// read settings
		
	    String s = settings.readSetting(hash, "verbose");
	    if (s.toUpperCase().equals("TRUE")) {
	    	gui.setVerbose(true);
	    	gui.setVisible(true);
	    } else
	    	gui.setVerbose(false);
		return true;
	}
	
	@Override
	public void reset() {
		// zmažeme obrazovku
		gui.clearScreen();
	}

	@Override
	public void destroy() {
		gui.dispose();
		gui = null;
	}

	@Override
	public long getHash() { return hash; }
	
	@Override
	public IDeviceContext getNextContext() {
		return terminal;
	}

	@Override
	public boolean attachDevice(IDeviceContext device) {
		return true;
	}

	@Override
	public void detachAll() { }

	@Override
	public void showGUI() {
		gui.setVisible(true);
	}

	@Override
	public void showSettings() { 
		// nemáme GUI s nastaveniami
	}

}
