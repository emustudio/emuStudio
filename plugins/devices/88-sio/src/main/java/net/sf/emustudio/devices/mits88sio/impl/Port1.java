/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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

import java.util.Objects;

/**
 * This is the status port of 88-SIO card.
 */
@ContextType(id = "Status port")
class Port1 implements DeviceContext<Short> {
    private final Transmitter transmitter;

    Port1(Transmitter transmitter) {
        this.transmitter = Objects.requireNonNull(transmitter);
    }

    @Override
    public Short read() {
        return transmitter.readStatus();
    }

    @Override
    public void write(Short val) {
        transmitter.writeToStatus(val);
    }

    @Override
    public Class<Short> getDataType() {
        return Short.class;
    }

    @Override
    public String toString() {
        return "88-SIO Status Port";
    }
}
