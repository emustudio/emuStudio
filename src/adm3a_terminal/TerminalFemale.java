/*
 * TerminalFemale.java
 *
 * Created on 28.7.2008, 21:38:02
 * hold to: KISS, YAGNI
 * 
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package adm3a_terminal;

import plugins.device.IDeviceContext;

/**
 *
 * @author vbmacher
 */
public class TerminalFemale implements IDeviceContext {

    private IDeviceContext dev;

    public void attachDevice(IDeviceContext device) {
        this.dev = device;
    }

    public void detachDevice() {
        this.dev = null;
    }

    @Override
    public Object read() {
        return (short) 0;
    }

    @Override
    public void write(Object val) {
        if (dev == null) {
            return;
        }
        dev.write(val);
    }

    @Override
    public String getID() {
        return "ADM-3A-female";
    }

    @Override
    public Class<?> getDataType() {
        return Short.class;
    }
}
