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
package net.sf.emustudio.zilogZ80.impl;

import emulib.plugins.device.DeviceContext;
import net.sf.emustudio.intel8080.api.ExtendedContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ContextImpl implements ExtendedContext {
    public final static int DEFAULT_FREQUENCY_KHZ = 20000;

    private final static Logger LOGGER = LoggerFactory.getLogger(net.sf.emustudio.intel8080.impl.ContextImpl.class);
    private final ConcurrentMap<Integer, DeviceContext<Short>> devices = new ConcurrentHashMap<>();

    private volatile EmulatorEngine cpu;
    private volatile int clockFrequency = DEFAULT_FREQUENCY_KHZ;

    public void setCpu(EmulatorEngine cpu) {
        this.cpu = cpu;
    }

    // device mapping = only one device can be attached to one port
    @Override
    public boolean attachDevice(DeviceContext<Short> device, int port) {
        if (!device.getDataType().equals(Short.class)) {
            return false;
        }
        if (devices.containsKey(port)) {
            return false;
        }
        if (devices.putIfAbsent(port, device) == null) {
            LOGGER.info("[port={}] Attached device: {}", port, device);
        }
        return true;
    }

    @Override
    public void detachDevice(int port) {
        if (devices.remove(port) != null) {
            LOGGER.info("Detached device from port " + port);
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
    public short fireIO(int port, boolean read, int val) throws IOException {
        DeviceContext<Short> device = devices.get(port);
        if (device != null) {
            if (read) {
                return device.read();
            } else {
                device.write((short)val);
            }
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
