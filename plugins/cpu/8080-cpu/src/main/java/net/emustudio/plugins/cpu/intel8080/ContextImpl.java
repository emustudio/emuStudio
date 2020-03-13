/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.cpu.intel8080;

import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.cpu.intel8080.api.ExtendedContext;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ThreadSafe
public class ContextImpl implements ExtendedContext {
    private final static Logger LOGGER = LoggerFactory.getLogger(ContextImpl.class);

    private final ConcurrentMap<Integer, DeviceContext<Short>> devices = new ConcurrentHashMap<>();

    private volatile EmulatorEngine cpu;
    private volatile int clockFrequency = 2000; // kHz

    public void setCpu(EmulatorEngine cpu) {
        this.cpu = cpu;
    }

    // device mapping = only one device can be attached to one port
    @Override
    public boolean attachDevice(DeviceContext<Short> device, int port) {
        if (devices.containsKey(port)) {
            return false;
        }
        if (devices.putIfAbsent(port, device) == null) {
            LOGGER.info("[port={},device={}] Device was attached to CPU", port, device);
        }
        return true;
    }

    @Override
    public void detachDevice(int port) {
        if (devices.remove(port) != null) {
            LOGGER.info("[port={}] Device was detached from CPU", port);
        }
    }

    public void clearDevices() {
        LOGGER.info("Detaching all devices from CPU");
        devices.clear();
    }

    /**
     * Performs I/O operation.
     *
     * @param port I/O port
     * @param read whether method should read or write to the port
     * @param data data to be written to the port. if parameter read is set to true, then val is ignored.
     * @return value from the port if read is true, otherwise 0
     */
    public short fireIO(int port, boolean read, short data) throws IOException {
        DeviceContext<Short> device = devices.get(port);
        if (device != null) {
            if (read) {
                return device.readData();
            } else {
                device.writeData(data);
            }
        }
        return 0;
    }

    @Override
    public boolean isRawInterruptSupported() {
        return true;
    }

    /**
     * Signals raw interrupt to the CPU.
     * <p>
     * The interrupting device can insert any instruction on the data bus for
     * execution by the CPU. The first byte of a multi-byte instruction is read
     * during the interrupt acknowledge cycle. Subsequent bytes are read in by a
     * normal memory read sequence.
     *
     * @param device      the device which signals the interrupt
     * @param instruction instruction signaled by this interrupt
     */
    @Override
    public void signalRawInterrupt(DeviceContext device, byte[] instruction) {
        EmulatorEngine tmpCpu = cpu;
        if (tmpCpu != null) {
            short b1 = (instruction.length >= 1) ? instruction[0] : 0;
            short b2 = (instruction.length >= 2) ? instruction[1] : 0;
            short b3 = (instruction.length >= 3) ? instruction[2] : 0;
            tmpCpu.interrupt(b1, b2, b3);
        }
    }

    @Override
    public boolean isInterruptSupported() {
        return false;
    }

    @Override
    public void signalInterrupt(DeviceContext device, int mask) {
    }

    @Override
    public void clearInterrupt(DeviceContext device, int mask) {
    }

    @Override
    public int getCPUFrequency() {
        return clockFrequency;
    }

    @Override
    public void setCPUFrequency(int freq) {
        this.clockFrequency = freq;
    }

}
