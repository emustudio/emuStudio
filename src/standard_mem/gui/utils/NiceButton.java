/**
 * NiceButton.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 *  KISS, YAGNI
 */
package standard_mem.gui.utils;

import java.awt.Dimension;

import javax.swing.JButton;

@SuppressWarnings("serial")
public class NiceButton extends JButton {
	private final static int WIDTH = 125;
	private final static int HEIGHT = 30;

	public NiceButton() {
		super();
		Dimension d = getPreferredSize();
		d.setSize(WIDTH,HEIGHT); //d.getHeight());
		this.setPreferredSize(d);
		this.setSize(WIDTH, HEIGHT);//this.getHeight());
		this.setMinimumSize(d);
		this.setMaximumSize(d);
	}

	public NiceButton(String text) {
		this();
		setText(text);
	}
}
