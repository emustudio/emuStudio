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

import emulib.plugins.device.DeviceContext;

import java.util.Objects;

/**
 * This port is a physical port which is used to device-device connection.
 * 
 * For example, a terminal would use this port for communication.
 */
class PhysicalPort implements DeviceContext<Short> {
    private final Transmitter transmitter;
    
    PhysicalPort(Transmitter transmitter) {
        this.transmitter = Objects.requireNonNull(transmitter);
    }

    @Override
    public Short read() {
        return 0; // Attached device cannot read back what it already wrote
    }

    @Override
    public void write(Short val) {
        transmitter.writeFromDevice(val);
    }

    @Override
    public Class getDataType() {
        return Short.class;
    }
    
}
