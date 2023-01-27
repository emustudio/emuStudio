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
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.emulib.plugins.cpu.TimedEventsProcessor;
import net.emustudio.plugins.cpu.zilogZ80.api.ContextZ80;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ThreadSafe
public final class ContextZ80Impl implements ContextZ80 {
    public final static int DEFAULT_FREQUENCY_KHZ = 20000;
    private final static byte NO_DATA = (byte) 0xFF;
    private final static Logger LOGGER = LoggerFactory.getLogger(ContextZ80Impl.class);

    private final ConcurrentMap<Integer, CpuPortDevice> devices = new ConcurrentHashMap<>();
    private final TimedEventsProcessor tep = new TimedEventsProcessor();

    private volatile EmulatorEngine engine;
    private volatile int clockFrequency = DEFAULT_FREQUENCY_KHZ;

    public void setEngine(EmulatorEngine engine) {
        this.engine = engine;
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
            LOGGER.debug("Detached device from port " + port);
        }
    }

    void clearDevices() {
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

    @Override
    public void signalInterrupt(byte[] data) {
        engine.requestMaskableInterrupt(data);
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
        clockFrequency = frequency;
    }

    @Override
    public void signalNonMaskableInterrupt() {
        engine.requestNonMaskableInterrupt();
    }

    @Override
    public Optional<TimedEventsProcessor> getTimedEventsProcessor() {
        return Optional.of(tep);
    }

    public TimedEventsProcessor getTimedEventsProcessorNow() {
        // bypassing optional
        return tep;
    }
}
