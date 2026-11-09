/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.emulib.plugins.cpu.AbstractCPUContext;

final class CpuContextImpl extends AbstractCPUContext {

    private volatile EmulatorEngine engine;

    void setEngine(EmulatorEngine engine) {
        this.engine = engine;
    }

    @Override
    public int getInstructionLocation() {
        return engine == null ? -1 : Math.max(0, engine.CI.get() + 4);
    }

    @Override public boolean isPassedCyclesSupported() { return false; }
    void publishState(net.emustudio.emulib.plugins.cpu.CPU.RunState state, int instructionLocation, float frequencyKhz) {
        fireStateChanged(state, instructionLocation, frequencyKhz);
    }
}
