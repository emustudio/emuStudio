/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80.suite.injectors;

import net.emustudio.plugins.cpu.zilogZ80.suite.CpuRunnerImpl;

import java.util.function.BiConsumer;

public class RegisterPair implements BiConsumer<CpuRunnerImpl, Integer> {
    private final int registerPair;

    public RegisterPair(int registerPair) {
        this.registerPair = registerPair;
    }

    @Override
    public void accept(CpuRunnerImpl cpuRunner, Integer value) {
        cpuRunner.setRegisterPair(registerPair, value & 0xFFFF);
    }

    @Override
    public String toString() {
        return String.format("registerPair[%04x]", registerPair);
    }

}
