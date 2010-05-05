/*
 * TerminalImpl.java
 *
 * Created on 28.7.2008, 19:12:19
 * hold to: KISS, YAGNI
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
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

package terminal;

import plugins.ISettingsHandler;
import plugins.cpu.ICPUContext;
import plugins.device.IDevice;
import plugins.device.IDeviceContext;
import plugins.memory.IMemoryContext;
import adm3a_terminal.gui.ConfigDialog;
import adm3a_terminal.gui.TerminalWindow;

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
        return "0.15-rc1";
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
