/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80.api;

/**
 * Optional hook for machines that need to observe passive Z80 memory-bus cycles.
 * <p>
 * During refresh, indexed-address setup, taken branches, and similar internal T-states, the Z80 places an address
 * on the bus without performing a real memory transfer. Machines such as the ZX Spectrum can use that information
 * to add machine-specific wait states without the CPU core faking reads.
 */
public interface PassiveMemoryCycleContext {

    /**
     * Advances passive memory-bus cycles at {@code address}.
     * <p>
     * The CPU core already advances the base Z80 T-states for these cycles. Implementations may add only
     * machine-specific extra delay and must not perform a real memory read or write as part of this callback.
     *
     * @param address address driven on the bus
     * @param cycles  number of passive T-states to execute
     */
    void passiveMemoryCycles(int address, int cycles);
}
