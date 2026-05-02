/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck;

import net.emustudio.emulib.plugins.device.DeviceContext;

import java.util.Objects;

class BrainCPUContextImpl implements BrainCPUContext {

    private DeviceContext<Byte> device;

    BrainCPUContextImpl() {
        device = null;
    }

    @Override
    public void attachDevice(DeviceContext<Byte> device) {
        this.device = Objects.requireNonNull(device);
    }

    @Override
    public void detachDevice() {
        device = null;
    }

    /**
     * Write a value into attached device.
     *
     * @param data value that will be written into the device
     */
    public void writeToDevice(byte data) {
        DeviceContext<Byte> tmp = device;
        if (tmp == null) {
            return;
        }
        tmp.writeData(data);
    }

    /**
     * Read a value from the attached device.
     * <p>
     * If the device doesn't have anything to send, a zero (0) might be considered
     * as the signal.
     *
     * @return value from the device, or 0 if the device is null or there's anything
     */
    public byte readFromDevice() {
        DeviceContext<Byte> tmp = device;
        if (tmp == null) {
            return 0;
        }
        Byte value = tmp.readData();
        return (value == null) ? 0 : value;
    }

    @Override
    public boolean isPassedCyclesSupported() {
        return false;
    }

    @Override
    public void addPassedCyclesListener(PassedCyclesListener passedCyclesListener) {

    }

    @Override
    public void removePassedCyclesListener(PassedCyclesListener passedCyclesListener) {

    }
}
