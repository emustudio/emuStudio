/**
 * NiceButton.java
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
package emustudio.gui.utils;

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
