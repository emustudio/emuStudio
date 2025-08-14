/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80.suite.injectors;

import net.emustudio.plugins.cpu.zilogZ80.suite.CpuRunnerImpl;

import java.util.function.BiConsumer;

public class RegisterPairPSW implements BiConsumer<CpuRunnerImpl, Integer> {
    private final int registerPairPSW;

    public RegisterPairPSW(int registerPairPSW) {
        this.registerPairPSW = registerPairPSW;
    }

    @Override
    public void accept(CpuRunnerImpl cpuRunner, Integer value) {
        cpuRunner.setRegisterPairPSW(registerPairPSW, value & 0xFFFF);
    }

    @Override
    public String toString() {
        return String.format("registerPairPSW[%x]", registerPairPSW);
    }

}
