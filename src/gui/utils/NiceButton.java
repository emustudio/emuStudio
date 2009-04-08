/**
 * NiceButton.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 *  KISS, YAGNI
 */
package gui.utils;

import java.awt.Dimension;
import java.awt.FontMetrics;

import javax.swing.JButton;

@SuppressWarnings("serial")
public class NiceButton extends JButton {
	private final static int WIDTH = 95;
	private static int HEIGHT = 30;

	private void setHeight() {
		FontMetrics metrics =  this.getFontMetrics(getFont());
	    HEIGHT = metrics.getHeight() + 9;
	}
	
	public NiceButton() {
		super();
		setHeight();
		Dimension d = getPreferredSize();
		d.setSize(WIDTH, HEIGHT);
		this.setPreferredSize(d);
		this.setSize(WIDTH, HEIGHT);//this.getHeight());
		this.setMinimumSize(d);
		this.setMaximumSize(d);
	}
	
}
