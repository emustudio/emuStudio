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
package net.emustudio.plugins.cpu.intel8080.api;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.cpu.CPUContext;

/**
 * Extended CPU context for 8080 processor.
 */
@PluginContext
public interface Context8080 extends CPUContext {

    /**
     * Attach a device into the CPU.
     *
     * @param port   CPU port where the device should be attached
     * @param device the device
     * @return true on success, false otherwise
     */
    boolean attachDevice(int port, CpuPortDevice device);

    /**
     * Detach a device from the CPU.
     *
     * @param port the CPU port number which will be freed.
     */
    void detachDevice(int port);

    /**
     * Set CPU frequency in kHZ
     *
     * @param freq new frequency in kHZ
     */
    void setCPUFrequency(int freq);

    /**
     * Device attachable to CPU port. It's not a DeviceContext because some machines need port address (low + high byte)
     * for being able to respond (e.g. ZX-spectrum port 0xFE).
     */
    interface CpuPortDevice {

        /**
         * Read a byte data from device
         *
         * @param portAddress port address. Low 8 bits is the port number.
         * @return byte data from the port
         */
        byte read(int portAddress);

        /**
         * Write data to the device
         *
         * @param portAddress port address. Low 8 bits is the port number.
         * @param data        byte data to be written
         */
        void write(int portAddress, byte data);

        /**
         * Get device port name
         *
         * @return device port name
         */
        String getName();
    }
}
