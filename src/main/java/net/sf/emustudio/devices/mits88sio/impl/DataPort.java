/*
 * DataPort.java
 *
 * Created on 18.6.2008, 14:30:59
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
 *
 */
package net.sf.emustudio.devices.mits88sio.impl;

import emulib.annotations.ContextType;
import emulib.plugins.device.DeviceContext;

/**
 * This is the data port of 88-SIO card.
 *
 * A read to the data port gets the buffered character, a write to the data port
 * writes the character to the device.
 *
 * @author Peter Jakubčo
 */
@ContextType(id = "Data port")
public class DataPort implements DeviceContext<Short> {

    private Mits88SIO sio;
    private DeviceContext dev;

    public DataPort(Mits88SIO sio) {
        this.sio = sio;
    }

    public void attachDevice(DeviceContext device) {
        this.dev = device;
    }

    public void detachDevice() {
        this.dev = null;
    }

    public DeviceContext getAttachedDevice() {
        return dev;
    }

    @Override
    public void write(Short data) {
        if (dev == null) {
            return;
        }
        dev.write(data);
    }

    @Override
    public Short read() {
        //    if (buffer == 0 && gui != null) {
        //      // get key from terminal (polling)
        //    buffer = gui.getChar();
        // }
        short result = 0;
        if (sio.buffer.size() > 0) {
            result = sio.buffer.remove(0);
        }

        if (sio.buffer.isEmpty()) {
            sio.status &= 0xFE;
        } else {
            sio.status |= 0x01;
        }
        return result;
    }

    /**
     * This is communication method between device and SIO. For terminal: If
     * user pressed a key, then it is sent from terminal to SIO device via this
     * method.
     */
    public void writeBuffer(short data) {
        sio.status |= 0x01;
        sio.buffer.add(data);
    }

    @Override
    public Class<?> getDataType() {
        return Short.class;
    }
}
