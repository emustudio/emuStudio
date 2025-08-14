/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80.suite.injectors;

import net.emustudio.plugins.cpu.zilogZ80.suite.CpuRunnerImpl;

import java.util.function.BiConsumer;

public class RegisterPair2 implements BiConsumer<CpuRunnerImpl, Integer> {
    private final int registerPair;

    public RegisterPair2(int registerPair) {
        this.registerPair = registerPair;
    }

    @Override
    public void accept(CpuRunnerImpl cpuRunner, Integer value) {
        cpuRunner.setRegisterPair2(registerPair, value & 0xFFFF);
    }

    @Override
    public String toString() {
        return String.format("registerPair2[%04x]", registerPair);
    }

}
