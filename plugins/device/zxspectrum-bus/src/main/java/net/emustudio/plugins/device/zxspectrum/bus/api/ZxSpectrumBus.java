/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.bus.api;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.cpu.CPUContext;
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
public interface ZxSpectrumBus extends DeviceContext<Byte>, MemoryContext<Byte>, ContextZ80, CPUContext.PassedCyclesListener {
    /** Visible bitmap width in pixels. */
    int SCREEN_WIDTH_PIXELS = 256;
    /** Visible bitmap height in pixels. */
    int SCREEN_HEIGHT_PIXELS = 192;
    /** Number of attribute cells per display row. */
    int ATTRIBUTES_WIDTH = SCREEN_WIDTH_PIXELS / 8;
    /** Number of attribute-cell rows on screen. */
    int ATTRIBUTE_HEIGHT = SCREEN_HEIGHT_PIXELS / 8;
    /** Border thickness on each horizontal side in pixels. */
    int BORDER_WIDTH_PIXELS = 48;
    /** Base address of the ZX Spectrum pixel bitmap. */
    int SCREEN_MEMORY_BASE = 0x4000;
    /** Base address of the ZX Spectrum attribute area. */
    int ATTRIBUTE_MEMORY_BASE = 0x5800;
    /** ULA I/O port address. */
    int ULA_PORT_ADDRESS = 0xFE;
    /** Value observed when the bus is effectively undriven. */
    int UNDRIVEN_BUS_DATA_BYTE = 0xFF;
    /** Power-on border color used by the 48K model. */
    int DEFAULT_BORDER_COLOR = 7;
    /** Frames between flash attribute swaps. */
    int FLASH_SWAP_FRAME_COUNT = 16;

    TimingProfile getProfile();

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
