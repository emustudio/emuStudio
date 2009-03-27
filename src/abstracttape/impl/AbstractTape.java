/**
 * AbstractTape.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 *   KISS, YAGNI
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
	public String getVersion() { return "0.1b"; }
 
	@Override
	public String getCopyright() {
		return "\u00A9 Copyright 2009, P. Jakubčo";
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
		context.clear();
	}

	@Override
	public void showSettings() {
		new SettingsDialog(settings,hash,gui).setVisible(true);
	}

}
