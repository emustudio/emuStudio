/*
 * StatusCPUPort.java
 *
 * Created on 18.6.2008, 14:27:23
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

import java.util.Objects;

/**
 * This is the status port of 88-SIO card.
 */
@ContextType(id = "Status port")
public class StatusCPUPort implements DeviceContext<Short> {
    private final SIOImpl sio;
    private short status;

    public StatusCPUPort(SIOImpl sio) {
        this.sio = Objects.requireNonNull(sio);
    }

    @Override
    public Short read() {
        return status;
    }

    @Override
    public void write(Short val) {
        if (val == 0x03) {
            sio.reset();
        }
    }

    public void reset() {
        this.status = 0x02;
    }

    public void onWriteFromAttachedDevice() {
        status = (short)(status | 0x01);
    }

    public void onReadFromAttachedDevice(boolean bufferIsEmpty) {
        if (bufferIsEmpty) {
            status = (short) (status & 0xFE);
        } else {
            status = (short) (status | 0x01);
        }
    }

    @Override
    public Class<?> getDataType() {
        return Short.class;
    }

}
