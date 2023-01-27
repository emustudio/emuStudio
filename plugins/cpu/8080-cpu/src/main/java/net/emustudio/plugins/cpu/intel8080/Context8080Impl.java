/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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

import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ThreadSafe
public class Context8080Impl implements Context8080 {
    private final static Logger LOGGER = LoggerFactory.getLogger(Context8080Impl.class);
    private final static byte NO_DATA = (byte) 0xFF; // ha! from survey.mac in cpm2.dsk: "inactive port could return 0xFF or echo port#"

    private final ConcurrentMap<Integer, CpuPortDevice> devices = new ConcurrentHashMap<>();

    private volatile EmulatorEngine cpu;
    private volatile int clockFrequency = 2000; // kHz

    public void setCpu(EmulatorEngine cpu) {
        this.cpu = cpu;
    }

    // device mapping = only one device can be attached to one port
    @Override
    public boolean attachDevice(int port, CpuPortDevice device) {
        CpuPortDevice oldDevice = devices.get(port);
        if (oldDevice != null) {
            LOGGER.debug("[port={}, device={}] Could not attach device to given port. The port is already taken by: {}", port, device.getName(), oldDevice.getName());
            return false;
        }
        if (devices.putIfAbsent(port, device) != null) {
            LOGGER.debug("[port={}, device={}] Could not attach device to given port. The port is already taken.", port, device.getName());
            return false;
        }
        LOGGER.debug("[port={},device={}] Device was attached to CPU", port, device.getName());
        return true;
    }

    @Override
    public void detachDevice(int port) {
        if (devices.remove(port) != null) {
            LOGGER.debug("[port={}] Device was detached from CPU", port);
        }
    }

    public void clearDevices() {
        LOGGER.info("Detaching all devices from CPU");
        devices.clear();
    }

    void writeIO(int portAddress, byte data) {
        CpuPortDevice device = devices.get(portAddress & 0xFF);
        if (device != null) {
            device.write(portAddress, data);
        }
    }

    byte readIO(int portAddress) {
        CpuPortDevice device = devices.get(portAddress & 0xFF);
        if (device != null) {
            return device.read(portAddress);
        }
        return NO_DATA;
    }

    @Override
    public boolean isInterruptSupported() {
        return true;
    }

    /**
     * Signals raw interrupt to the CPU.
     * <p>
     * The interrupting device can insert any instruction on the data bus for
     * execution by the CPU. The first byte of a multibyte instruction is read
     * during the interrupt acknowledge cycle. Subsequent bytes are read in by a
     * normal memory read sequence.
     *
     * @param data instruction signaled by this interrupt
     */
    @Override
    public void signalInterrupt(byte[] data) {
        short b1 = (data.length >= 1) ? data[0] : 0;
        short b2 = (data.length >= 2) ? data[1] : 0;
        short b3 = (data.length >= 3) ? data[2] : 0;
        cpu.interrupt(b1, b2, b3);
    }

    @Override
    public int getCPUFrequency() {
        return clockFrequency;
    }

    @Override
    public void setCPUFrequency(int frequency) {
        if (frequency <= 0) {
            throw new IllegalArgumentException("Invalid CPU frequency (expected > 0): " + frequency);
        }
        this.clockFrequency = frequency;
    }

}
