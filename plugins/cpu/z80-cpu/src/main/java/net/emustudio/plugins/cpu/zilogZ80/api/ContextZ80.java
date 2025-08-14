/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
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
}
