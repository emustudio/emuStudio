/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
package net.sf.emustudio.brainduck.cpu.impl;

import emulib.plugins.device.DeviceContext;
import net.sf.emustudio.brainduck.cpu.BrainCPUContext;

import java.io.IOException;
import java.util.Objects;

class BrainCPUContextImpl implements BrainCPUContext {

    private DeviceContext<Short> device;

    BrainCPUContextImpl() {
        device = null;
    }

    @Override
    public boolean attachDevice(DeviceContext<Short> device) {
        this.device = Objects.requireNonNull(device);
        return true;
    }

    @Override
    public void detachDevice() {
        device = null;
    }

    /**
     * Write a value into attached device. 
     *
     * @param val value that will be written into the device
     */
    public void writeToDevice(short val) throws IOException {
        if (device == null) {
            return;
        }
        device.write(val);
    }

    /**
     * Read a value from the attached device.
     * 
     * If the device doesn't have anything to send, a zero (0) might be considered
     * as the signal.
     *
     * @return value from the device, or 0 if the device is null or there's anything
     */
    public short readFromDevice() throws IOException {
        if (device == null) {
            return 0;
        }
        Short value = device.read();
        return (value == null) ? 0 : value;
    }

    @Override
    public boolean isInterruptSupported() {
        return false;
    }

    @Override
    public void clearInterrupt(DeviceContext device, int mask) {
    }

    @Override
    public boolean isRawInterruptSupported() {
        return false;
    }

    @Override
    public void signalRawInterrupt(DeviceContext device, byte[] data) {
    }

    @Override
    public void signalInterrupt(DeviceContext device, int mask) {
    }

    @Override
    public int getCPUFrequency() {
        return 0;
    }
}
