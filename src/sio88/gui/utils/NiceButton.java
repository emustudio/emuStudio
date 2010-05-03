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
package sio88.gui.utils;

import java.awt.Dimension;

import javax.swing.JButton;

@SuppressWarnings("serial")
public class NiceButton extends JButton {
	private final static int x_WIDTH = 95;
	private final static int x_HEIGHT = 30;

	public NiceButton() {
		super();
		Dimension d = getPreferredSize();
		d.setSize(x_WIDTH,x_HEIGHT); //d.getHeight());
		this.setPreferredSize(d);
		this.setSize(x_WIDTH, x_HEIGHT);//this.getHeight());
		this.setMinimumSize(d);
		this.setMaximumSize(d);
	}

	public NiceButton(String text) {
		this();
		this.setText(text);
	}
}
