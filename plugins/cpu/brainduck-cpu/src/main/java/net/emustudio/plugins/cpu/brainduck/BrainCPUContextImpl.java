/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
}
