/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.bus.api;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.plugins.cpu.zilogZ80.api.ContextZ80;

/**
 * ZX Spectrum bus.
 * <p>
 * It's a proxy between CPU, memory and devices. Mostly due to contention, but also for CPU cycles synchronization
 * (devices are usually not connected to CPU directly).
 */
@PluginContext
public interface ZxSpectrumBus extends DeviceContext<Byte>, MemoryContext<Byte>, ContextZ80 {

    default boolean isInterruptSupported() {
        return true;
    }

    default boolean isPassedCyclesSupported() {
        return true;
    }

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

}
