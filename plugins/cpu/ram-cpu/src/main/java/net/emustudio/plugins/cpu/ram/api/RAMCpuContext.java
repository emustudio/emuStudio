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

package net.emustudio.plugins.cpu.ram.api;

import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;

public interface RAMCpuContext extends CPUContext {

    AbstractTapeContext getStorageTape();

    AbstractTapeContext getInputTape();

    AbstractTapeContext getOutputTape();

    @Override
    default boolean isInterruptSupported() {
        return false;
    }

    @Override
    default void signalInterrupt(DeviceContext device, int mask) {

    }

    @Override
    default void clearInterrupt(DeviceContext device, int mask) {

    }

    @Override
    default boolean isRawInterruptSupported() {
        return false;
    }

    @Override
    default void signalRawInterrupt(DeviceContext device, byte[] data) {

    }

    @Override
    default int getCPUFrequency() {
        return 0;
    }
}
