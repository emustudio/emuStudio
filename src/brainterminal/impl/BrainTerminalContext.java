/**
 * BrainTerminalContext.java
 * 
 * (c) Copyright 2009, P.Jakubčo
 * 
 * KISS, YAGNI
 */
package brainterminal.impl;

import java.util.EventObject;
import brainterminal.gui.BrainTerminalDialog;
import plugins.device.IDeviceContext;

public class BrainTerminalContext implements IDeviceContext {
	private BrainTerminalDialog gui;
	
	public BrainTerminalContext(BrainTerminalDialog gui) {
		this.gui = gui;
	}

	@Override
	public Class<?> getDataType() {
		return Short.class;
	}

	@Override
	public Object in(EventObject evt) {
	    return (short)gui.getChar();
	}

	@Override
	public void out(EventObject evt, Object val) {
		short s = (Short)val;
		char c = (char)s;
		gui.putChar(c);
	}

	@Override
	public String getHash() {
		return "4a0411686e1560c765c1d6ea903a9c5f";
	}

	@Override
	public String getID() {
		return "brain-terminal-context";
	}

}
