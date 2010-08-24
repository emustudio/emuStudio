/**
 * RAMmemory.java
 * 
 *  KISS, YAGNI
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
package RAMmemory.impl;

import RAMmemory.gui.MemoryWindow;
import plugins.ISettingsHandler;
import plugins.memory.IMemory;
import plugins.memory.IMemoryContext;

public class RAMmemory implements IMemory {
	private long hash;
	@SuppressWarnings("unused")
	private ISettingsHandler settings;
	private RAMContext context;
	private MemoryWindow gui = null;

	public RAMmemory(Long hash) {
		this.hash = hash;
		context = new RAMContext();
	}
	
	@Override
	public String getTitle() { return "RAM Memory"; }

	@Override
	public String getVersion() { return "0.11-rc1"; }

	@Override
	public String getCopyright() {
		return "\u00A9 Copyright 2009-2010, P. Jakubčo"; 
	}

	@Override
	public String getDescription() {
		return "Operating memory for RAM machine. Contains program -" +
				"tape. User can not manually edit the tape - it is" +
				"a read only memory (filled once).";
	}
	
	@Override
	public IMemoryContext getContext() {
		return context;
	}

	@Override
	public int getProgramStart() { return 0; }

	@Override
	public int getSize() {	return context.getSize(); }

	@Override
	public boolean initialize(int size, ISettingsHandler settings) {
		this.settings = settings;
		return true;
	}

	@Override
	// Program start is always 0
	public void setProgramStart(int pos) { }

	@Override
	public void showGUI() {
		if (gui == null) gui = new MemoryWindow(context);
		gui.setVisible(true);
	}

	@Override
	public void destroy() {
		context.destroy();
		context = null;
		if (gui != null) {
			gui.dispose();
			gui = null;
		}
	}

	@Override
	public long getHash() { return hash; }

	@Override
	public void reset() { 
		context.clearInputs();
	}

	@Override
	public void showSettings() {
		// TODO Auto-generated method stub
	}

}
