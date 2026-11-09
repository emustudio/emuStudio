/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem;

import net.emustudio.emulib.plugins.cpu.CPUSnapshot;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.cpu.AbstractCPUContext;

import net.emustudio.emulib.plugins.cpu.CPU;

/** Execution snapshots for isolated memory formatting and action tests. */
public final class TestCPUContext extends AbstractCPUContext {
    private TestCPUContext() { }
    public static CPUContext create() { return new TestCPUContext(); }
    @Override public boolean isPassedCyclesSupported() { return false; }
    @Override public CPUSnapshot snapshot() {
        return new CPUSnapshot(CPU.RunState.STATE_STOPPED_BREAK, false, false, -1, Float.NaN);
    }
}
