/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80.api;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;

@SuppressWarnings("unused")
@PluginContext
public interface ContextZ80 extends Context8080 {

    /**
     * Signals a non-maskable interrupt.
     * <p>
     * On the interrupt execution, CPU ignores the next instruction and instead performs a restart
     * at address 0066h. Routines should exit with RETN instruction.
     */
    void signalNonMaskableInterrupt();

    /**
     * Explicitly adds machine cycles (slows down CPU).
     * <p>
     * Used primarily in contention implementation.
     *
     * @param tStates number of t-states (machine cycles) to add
     */
    void addCycles(long tStates);

    /**
     * Sets the duration (in T-states) for which the INT signal remains active after signalInterrupt() is called.
     * <p>
     * When set to a positive value, the interrupt model switches from edge-triggered (one-shot, default)
     * to level-triggered. The INT signal will persist for the specified number of T-states, allowing
     * the interrupt to be taken even if interrupts are re-enabled (EI) after the signal was initially asserted.
     * <p>
     * For ZX Spectrum 48K, this should be set to 32 T-states.
     *
     * @param tStates duration in T-states (0 = edge-triggered/default behavior)
     */
    default void setInterruptDuration(int tStates) {
    }

    /**
     * Clears any pending maskable interrupt.
     */
    default void clearInterrupt() {
    }
}
