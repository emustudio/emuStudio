/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80.suite.injectors;

import net.emustudio.plugins.cpu.zilogZ80.suite.CpuRunnerImpl;

import java.util.function.BiConsumer;

public class Register implements BiConsumer<CpuRunnerImpl, Byte> {
    private final int register;

    public Register(int register) {
        this.register = register;
    }

    @Override
    public void accept(CpuRunnerImpl cpuRunner, Byte value) {
        cpuRunner.setRegister(register, value);
    }

    @Override
    public String toString() {
        return String.format("register[%02x]", register);
    }
}
