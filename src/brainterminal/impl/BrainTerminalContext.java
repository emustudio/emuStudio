/**
 * BrainTerminalContext.java
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
package brainterminal.impl;

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
    public Object read() {
        return (short) gui.getChar();
    }

    @Override
    public void write(Object val) {
        short s = (Short) val;
        char c = (char) s;
        gui.putChar(c);
    }

    @Override
    public String getID() {
        return "brain-terminal-context";
    }
}
