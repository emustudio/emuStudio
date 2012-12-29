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

import emulib.plugins.device.DeviceContext;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the data port of 88-SIO card.
 * It's a male plug, RS-232 physical port.
 * 
 * A read to the data port gets the buffered character, a write to the data port
 * writes the character to the device.
 *
 * @author Peter Jakubčo
 */
public class DataPort implements DeviceContext<Short> {
    private SIOImpl sio;
    private List<Short> buffer = new ArrayList<Short>();

    public DataPort(SIOImpl sio) {
        this.sio = sio;
    }

    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    /**
     * This is communication method between device and SIO. For terminal: If
     * user pressed a key, then it is sent from terminal to SIO device via this
     * method.
     */
    @Override
    public void write(Short data) {
        sio.setStatus((short) (sio.getStatus() | 0x01));
        buffer.add(data);
    }

    @Override
    public Short read() {
        short result = 0;
        if (buffer.size() > 0) {
            result = buffer.remove(0);
        }

        if (buffer.isEmpty()) {
            sio.setStatus((short) (sio.getStatus() & 0xFE));
        } else {
            sio.setStatus((short) (sio.getStatus() | 0x01));
        }
        return result;
    }

    @Override
    public Class<?> getDataType() {
        return Short.class;
    }
}
