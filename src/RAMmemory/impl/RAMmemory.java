/**
 * RAMmemory.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 *  KISS, YAGNI
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
	public String getVersion() { return "0.1b"; }

	@Override
	public String getCopyright() {
		return "\u00A9 Copyright 2009, P. Jakubčo"; 
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
	public void reset() { }

	@Override
	public void showSettings() {
		// TODO Auto-generated method stub
	}

}
