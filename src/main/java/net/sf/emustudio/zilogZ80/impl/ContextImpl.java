/*
 * ContextImpl.java
 *
 * Created on 18.6.2008, 8:50:11
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
package net.sf.emustudio.zilogZ80.impl;

import emulib.plugins.device.DeviceContext;
import java.util.HashMap;
import java.util.Map;
import net.sf.emustudio.intel8080.ExtendedContext;

/**
 * Context of Z80 CPU emulator.
 * 
 */
public final class ContextImpl implements ExtendedContext {
    private Map<Integer, DeviceContext<Short>> devices;
    private EmulatorImpl cpu;
    private int clockFrequency = 20000; // kHz

    public ContextImpl(EmulatorImpl cpu) {
        devices = new HashMap<Integer, DeviceContext<Short>>();
        this.cpu = cpu;
    }

    // device mapping = only one device can be attached to one port
    @Override
    public boolean attachDevice(DeviceContext<Short> device, int port) {
        if (devices.containsKey(port)) {
            return false;
        }
        if (!device.getDataType().equals(Short.class)) {
            return false;
        }
        devices.put(port, device);
        return true;
    }

    @Override
    public void detachDevice(int port) {
        if (devices.containsKey(port)) {
            devices.remove(port);
        }
    }

    public void clearDevices() {
        devices.clear();
    }

    /**
     * Performs I/O operation.
     * @param port I/O port
     * @param read whether method should read or write to the port
     * @param val value to be written to the port. if parameter read is set to
     *            true, then val is ignored.
     * @return value from the port if read is true, otherwise 0
     */
    public short fireIO(int port, boolean read, short val) {
        if (devices.containsKey(port) == false) {
            // this behavior isn't constant for all situations...
            // on ALTAIR computer it depends on setting of one switch on front
            // panel (called IR or what..)
            return 0;
        }
        if (read == true) {
            return (Short) devices.get(port).read();
        } else {
            devices.get(port).write(val);
        }
        return 0;
    }

    @Override
    public boolean isInterruptSupported() {
        return true;
    }

    @Override
    public void setCPUFrequency(int frequency) {
        clockFrequency = frequency;
    }

    @Override
    public boolean isRawInterruptSupported() {
        return true;
    }

    @Override
    public void signalRawInterrupt(DeviceContext device, byte[] data) {
        cpu.setInterruptVector(data);
    }

    @Override
    public void signalInterrupt(DeviceContext device, int mask) {
        cpu.setInterrupt(device, mask);
    }

    @Override
    public void clearInterrupt(DeviceContext device, int mask) {
        cpu.clearInterrupt(device, mask);
    }

    @Override
    public int getCPUFrequency() {
        return clockFrequency;
    }
}
