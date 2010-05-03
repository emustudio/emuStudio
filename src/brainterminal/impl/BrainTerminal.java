/**
 * BrainTerminal.java
 * 
 * KISS, YAGNI
 *
 * Copyright (C) 2009-2010 Peter Jakubčo <pjakubco at gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
		return "0.11-rc1";
	}

	@Override
	public String getCopyright() {
		return "\u00A9 Copyright 2009-2010, P. Jakubčo";
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
