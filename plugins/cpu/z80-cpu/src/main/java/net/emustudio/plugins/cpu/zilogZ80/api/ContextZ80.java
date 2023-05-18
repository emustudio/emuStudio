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
