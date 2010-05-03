/*
 * SIMHpseudo.java
 *
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

package simh;

import interfaces.ACpuContext;
import interfaces.SMemoryContext;
import plugins.ISettingsHandler;
import plugins.cpu.ICPUContext;
import plugins.device.IDevice;
import plugins.device.IDeviceContext;
import plugins.memory.IMemoryContext;
import runtime.StaticDialogs;

/**
 * SIMH emulator's pseudo device
 * 
 * @author vbmacher
 */
public class SIMHpseudo implements IDevice {
	private final static String KNOWN_CPU_CONTEXT_HASH = "4bb574accc0ed96b5ed84b5832127289";
	private final static String KNOWN_MEM_CONTEXT_HASH = "a93730cef0f15c6ea9d6b5e9e5d7f05f";
	
	private long hash;
    private PseudoContext male;
    private ACpuContext cpu;
    private SMemoryContext mem;

    public SIMHpseudo(Long hash) {
    	this.hash = hash;
    }
    
    @Override
    public boolean initialize(ICPUContext cpu, IMemoryContext mem, ISettingsHandler sHandler) {
        if (cpu == null) {
            StaticDialogs.showErrorMessage("SIMH-pseudo device has to be attached"
                    + " to a CPU");
            return false;
        }
    	
        if (mem == null) {
            StaticDialogs.showErrorMessage("SIMH-pseudo device has to be attached"
                    + " to a Memory");
            return false;
        }
    	
        // ID of cpu can be ofcourse also else.
        if (!cpu.getHash().equals(KNOWN_CPU_CONTEXT_HASH)) {
            StaticDialogs.showErrorMessage("SIMH-pseudo device can not be attached"
                    + " to this kind of CPU");
            return false;
        }

        if (!mem.getHash().equals(KNOWN_MEM_CONTEXT_HASH)) {
            StaticDialogs.showErrorMessage("SIMH-pseudo device can't collaborate "
                    + "with this kind of MEMORY");
            return false;
        }

        this.cpu = (ACpuContext) cpu;
        this.mem = (SMemoryContext) mem;
        
        male = new PseudoContext(this.mem);
        
        // attach IO port
        if (this.cpu.attachDevice(male, 0xFE) == false) {
            StaticDialogs.showErrorMessage("Error: SIMH device can't be"
                    + " attached to CPU (maybe there is a hardware conflict)");
            return false;
        }
        reset();
        return true;
    }

    @Override
    public void showGUI() {
        StaticDialogs.showMessage("GUI not supported");
    }

    @Override
    public IDeviceContext getNextContext() { return male; }

    @Override
    public boolean attachDevice(IDeviceContext male) { return false; }

    @Override
    public void detachAll() {}

    @Override
    public void reset() {
        male.reset();
    }

    @Override
    public String getTitle() { return "SIMH pseudo device"; }
    @Override
    public String getCopyright() {
        return "Copyright (c) 2002-2007, Peter Schorn\n"
            + "\u00A9 Copyright 2007-2009, Peter Jakubčo";
    }
    @Override
    public String getDescription() {
        return "Some of the images taken from SIMH emulator contain Z80 or 8080" +
                " programs that communicate with the SIMH pseudo device "
                + "via port 0xfe. The version of the interface is: SIMH003";
    }
    @Override
    public String getVersion() { return "0.11b"; }

    @Override
    public void destroy() {}

	@Override
	public long getHash() { return hash; }

	@Override
	public void showSettings() {
		// TODO Auto-generated method stub
		
	}

}
