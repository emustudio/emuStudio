/*
 * PhysicalPort.java
 *
 * Created on 28.7.2008, 8:22:30
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
package sio88;

import emuLib8.plugins.device.IDeviceContext;

/**
 * Male plug, RS-232 physical port
 *
 * @author vbmacher
 */
public class PhysicalPort implements IDeviceContext {

    private CpuPort2 port2;

    public PhysicalPort(CpuPort2 port2) {
        this.port2 = port2;
    }

    @Override
    public Object read() {
        return port2.read();
    }

    @Override
    public void write(Object val) {
        short v = (Short) val;
        port2.writeBuffer(v);
    }

    @Override
    public Class<?> getDataType() {
        return Short.class;
    }

    @Override
    public String getID() {
        return "RS232";
    }
}
