/*
 * PhysicalPort.java
 *
 * Created on 28.7.2008, 8:22:30
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
package net.sf.emustudio.devices.mits88sio.impl;

import emulib.annotations.ContextType;
import emulib.plugins.device.DeviceContext;

/**
 * Male plug, RS-232 physical port
 *
 * @author Peter Jakubčo
 */
@ContextType(id = "Physical port")
public class PhysicalPort implements DeviceContext<Short> {

    private DataPort port2;

    public PhysicalPort(DataPort port2) {
        this.port2 = port2;
    }

    @Override
    public Short read() {
        return port2.read();
    }

    @Override
    public void write(Short val) {
        port2.writeBuffer(val);
    }

    @Override
    public Class<?> getDataType() {
        return Short.class;
    }

}
