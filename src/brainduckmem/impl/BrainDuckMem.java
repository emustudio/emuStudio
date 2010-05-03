/**
 * BrainDuckMem.java
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
package brainduckmem.impl;

import plugins.ISettingsHandler;
import plugins.memory.IMemory;
import plugins.memory.IMemoryContext;
import runtime.StaticDialogs;

public class BrainDuckMem implements IMemory {
    private BrainMemContext memContext;
    private long hash;
    @SuppressWarnings("unused")
	private ISettingsHandler settings;
    private int programStart;
    private int size;

    public BrainDuckMem(Long hash) {
    	this.hash = hash;
        memContext = new BrainMemContext();
    }
    
    @Override
    public String getTitle() { return "BrainDuck OM"; }

    @Override
    public String getVersion() { return "0.1-rc1"; }
    
    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2009, P. Jakubčo";
    }
    @Override
    public String getDescription() {
        return "BrainDuck operating memory. Don't even have a GUI.";
    }

    @Override
    public long getHash() { return hash; }

    /**
     * Initialize memory:
     *     1. load settings as: banks count, common boundary
     *     2. create memory context (create memory with loaded settings)
     *     3. load images from settings
     *     4. load these images into memory in order as they appear in config file
     *     5. load rom ranges from settings
     *     6. set rom ranges to memory
     */
    @Override
    public boolean initialize(int size, ISettingsHandler sHandler) {
        this.settings = sHandler;
        this.size = size;
        memContext.init(size);
        return true;
    }

    @Override
    public void showGUI() {
    	// my nemáme GUI
    	StaticDialogs.showMessage("BrainDuck memory doesn't support GUI.");
    }

    @Override
    public void destroy() { }

    @Override
    public IMemoryContext getContext() {
        return memContext;
    }

    @Override
    public int getProgramStart() {
    	return programStart;
    }
    
    @Override
    public int getSize() {	return size; }

    @Override
    public void reset() {
    	//memContext.clearMemory();
    }

    @Override
    public void setProgramStart(int address) {
        programStart = address;
    }

	@Override
	public void showSettings() {
		// nemáme ani GUI pre nastavenia
	}

}
