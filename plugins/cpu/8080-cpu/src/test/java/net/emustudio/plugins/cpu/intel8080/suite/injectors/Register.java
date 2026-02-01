/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080.suite.injectors;

import net.emustudio.plugins.cpu.intel8080.suite.CpuRunnerImpl;

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
        return String.format("reg[%02x]", register);
    }
}
