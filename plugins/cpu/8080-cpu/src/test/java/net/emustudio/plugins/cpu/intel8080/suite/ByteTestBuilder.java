/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080.suite;

import net.emustudio.cpu.testsuite.RunnerContext;
import net.emustudio.cpu.testsuite.TestBuilder;
import net.emustudio.plugins.cpu.intel8080.suite.injectors.Register;

import java.util.function.Function;

public class ByteTestBuilder extends TestBuilder<Byte, ByteTestBuilder, CpuRunnerImpl, CpuVerifierImpl> {

    public ByteTestBuilder(CpuRunnerImpl cpuRunner, CpuVerifierImpl cpuVerifier) {
        super(cpuRunner, cpuVerifier);
    }

    public ByteTestBuilder firstIsRegister(int register) {
        runner.injectFirst(new Register(register));
        return this;
    }

    public ByteTestBuilder secondIsRegister(int register) {
        runner.injectSecond(new Register(register));
        return this;
    }

    public ByteTestBuilder setPair(int registerPair, int value) {
        runner.injectFirst(
                (tmpRunner, argument) -> tmpRunner.ensureProgramSize(value + 1),
                (tmpRunner, argument) -> cpuRunner.setRegisterPair(registerPair, value)
        );
        return this;
    }

    public ByteTestBuilder verifyRegister(int register, Function<RunnerContext<Byte>, Integer> operator) {
        runner.verifyAfterTest(context -> cpuVerifier.checkRegister(register, operator.apply(context)));
        return this;
    }

}
