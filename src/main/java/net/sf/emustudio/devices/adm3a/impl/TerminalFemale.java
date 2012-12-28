/*
 * TerminalFemale.java
 *
 * Created on 28.7.2008, 21:38:02
 * 
 * Copyright (C) 2008-2012 Peter Jakubčo
 * KISS, YAGNI, DRY
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
package net.sf.emustudio.devices.adm3a.impl;

import emulib.annotations.ContextType;
import emulib.plugins.device.DeviceContext;

@ContextType
public class TerminalFemale implements DeviceContext {
    private DeviceContext dev;

    public void attachDevice(DeviceContext device) {
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
    public Class<?> getDataType() {
        return Short.class;
    }
}
