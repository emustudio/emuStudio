/**
 * AbstractTape.java
 * 
 *   KISS, YAGNI
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
package abstracttape.impl;

import abstracttape.gui.SettingsDialog;
import abstracttape.gui.TapeDialog;
import interfaces.IRAMCPUContext;
import plugins.ISettingsHandler;
import plugins.cpu.ICPUContext;
import plugins.device.IDevice;
import plugins.device.IDeviceContext;
import plugins.memory.IMemoryContext;
import runtime.StaticDialogs;

public class AbstractTape implements IDevice {
	private final String KNOWN_CPU = "ce861f51295b912a76a7df538655ab0f";  
	private long hash;
	private TapeContext context;
	private ISettingsHandler settings;
	private IRAMCPUContext cpu;
	private String title = "Abstract tape"; // can change
	private TapeDialog gui;
	
	public AbstractTape(Long hash) {
		this.hash = hash;
		context = new TapeContext();
	}
	
	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public String getVersion() { return "0.2-rc1"; }
 
	@Override
	public String getCopyright() {
		return "\u00A9 Copyright 2009-2010, P. Jakubčo";
	}

	@Override
	public String getDescription() {
		return "Abstract tape device is used by abstract machines" +
				"such as RAM or Turing machine. The mean and purpose" +
				"of the tape is given by the machine itself. Properties" +
				"such as read only tape or one-way direction tape is" +
				"also given by chosen machine. Therefore the tape is" +
				"universal.";
	}
	
	/**
	 * Nothing will be connected to the tape.
	 */
	@Override
	public boolean attachDevice(IDeviceContext arg0) {
		return false;
	}

	@Override
	public void detachAll() {}

	@Override
	public IDeviceContext getNextContext() {
		return context;
	}

	@Override
	public boolean initialize(ICPUContext cpu, IMemoryContext mem,
			ISettingsHandler settings) {
		if (!cpu.getHash().equals(KNOWN_CPU) ||
				!(cpu instanceof IRAMCPUContext)) {
			StaticDialogs.showErrorMessage("This device (abstract tape) doesn't support" +
					"this kind of CPU !");
			return false;
		}
		this.cpu = (IRAMCPUContext)cpu;
		this.settings = settings;
		
        String s = this.cpu.attachTape(context);
        if (s == null) return false;
        title = s;
        
        // show GUI at startup?        
		s = settings.readSetting(hash, "showAtStartup");
		if (s != null && s.toLowerCase().equals("true"))
			showGUI();
		return true;
	}

	@Override
	public void showGUI() {
		if (gui == null) gui = new TapeDialog(this,settings,hash);
		gui.setVisible(true);
	}

	@Override
	public void destroy() {
		if (gui != null) gui.dispose();
		gui = null;
        context = null;
		settings = null;
	}

	@Override
	public long getHash() {	return hash; }


	@Override
	public void reset() {
		context.reset();
	}

	@Override
	public void showSettings() {
		new SettingsDialog(settings,hash,gui).setVisible(true);
	}

}
