/*
 * DataCPUPort.java
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;

/**
 * This is the data port of 88-SIO card.
 * 
 * This port is attached to a CPU.
 * 
 * A read to the data port gets the buffered character, a write to the data port
 * writes the character to the device.
 *
 * @author Peter Jakubčo
 */
public class DataCPUPort implements DeviceContext<Short> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataCPUPort.class);
    private SIOImpl sio;
    private Queue<Short> buffer = new LinkedList<Short>();
    private DeviceContext<Short> device;

    public DataCPUPort(SIOImpl sio) {
        this.sio = sio;
    }

    public void attachDevice(DeviceContext<Short> device) {
        LOGGER.info("Attaching device to the data port: " + device);
        this.device = device;
    }
    
    public String getAttachedDeviceID() {
        return (device == null) ? "unknown" : device.toString();
    }
    
    public void detachDevice() {
        this.device = null;
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
        if (device != null) {
            device.write(data);
        }
    }
    
    /**
     * A device (usually the attached one) writes data.
     * 
     * We save it into a buffer for further reading by CPU.
     * 
     * @param data data
     */
    public void writeFromDevice(Short data) {
        sio.setStatus((short) (sio.getStatus() | 0x01));
        buffer.add(data);
    }

    @Override
    public Short read() {
        Short result;

        result = buffer.poll();
        if (result == null) {
            result = 0;
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
