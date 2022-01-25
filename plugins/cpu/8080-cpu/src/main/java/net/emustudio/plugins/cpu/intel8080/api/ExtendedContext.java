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
package net.emustudio.plugins.cpu.intel8080.api;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.device.DeviceContext;

/**
 * Extended CPU context for 8080 processor.
 */
@PluginContext
public interface ExtendedContext extends CPUContext {

    /**
     * Attach a device into the CPU.
     *
     * @param device the device
     * @param port   CPU port where the device should be attached
     * @return true on success, false otherwise
     */
    boolean attachDevice(DeviceContext<Byte> device, int port);

    /**
     * Detach a device from the CPU.
     *
     * @param port the CPU port number which will be freed.
     */
    void detachDevice(int port);

    void setCPUFrequency(int freq);
}
