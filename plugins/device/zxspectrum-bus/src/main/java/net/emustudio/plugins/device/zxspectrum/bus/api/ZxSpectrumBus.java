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
package net.emustudio.plugins.device.zxspectrum.bus.api;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;

/**
 * ZX Spectrum bus.
 * <p>
 * It's a proxy between CPU, memory and devices. Mostly due to contention, but also for CPU cycles synchronization
 * (devices are usually not connected to CPU directly).
 */
@PluginContext
public interface ZxSpectrumBus extends DeviceContext<Byte>, MemoryContext<Byte> {
    long LINE_CYCLES = 224;

    /**
     * Attach a device on the bus.
     * <p>
     * Under the hood, it will be attached to the CPU on given port. If the port adheres to contention,
     * device access will be contended.
     *
     * @param port   CPU port where the device should be attached
     * @param device the device
     */
    void attachDevice(int port, Context8080.CpuPortDevice device);

    /**
     * Signals a NMI to the CPU
     */
    void signalNonMaskableInterrupt();

    /**
     * Signals an interrupt to the CPU
     *
     * @param data interrupt data
     */
    void signalInterrupt(byte[] data);

    /**
     * Read data from memory, a non-contended variant.
     * <p>
     * Under the hood it uses existing byte-memory. The reason for this method is the default readMemory() applies
     * contention on specific location.
     *
     * @param location memory location
     * @return data read by memory
     */
    byte readMemoryNotContended(int location);

    /**
     * Write data from memory, a non-contended variant.
     * <p>
     * Under the hood it uses existing byte-memory. The reason for this method is the default writeMemory() applies
     * contention on specific memory location.
     *
     * @param location memory location
     * @param data     data to write
     */
    void writeMemoryNotContended(int location, byte data);

    void addPassedCyclesListener(CPUContext.PassedCyclesListener passedCyclesListener);

    void removePassedCyclesListener(CPUContext.PassedCyclesListener passedCyclesListener);
}
