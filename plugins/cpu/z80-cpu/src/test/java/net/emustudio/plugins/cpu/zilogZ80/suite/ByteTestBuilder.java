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
package net.emustudio.plugins.cpu.zilogZ80.suite;

import net.emustudio.cpu.testsuite.RunnerContext;
import net.emustudio.cpu.testsuite.TestBuilder;
import net.emustudio.plugins.cpu.zilogZ80.suite.injectors.Register;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class ByteTestBuilder extends TestBuilder<Byte, ByteTestBuilder, CpuRunnerImpl, CpuVerifierImpl> {

    public ByteTestBuilder(CpuRunnerImpl cpuRunner, CpuVerifierImpl cpuVerifier) {
        super(cpuRunner, cpuVerifier);
    }

    public ByteTestBuilder firstIsFlags() {
        runner.injectFirst((runner, argument) -> {
            runner.resetFlags();
            runner.setFlags(argument);
        });
        return this;
    }

    public ByteTestBuilder firstIsRegister(int register) {
        runner.injectFirst(new Register(register));
        return this;
    }

    public ByteTestBuilder secondIsRegister(int register) {
        runner.injectSecond(new Register(register));
        return this;
    }

    public ByteTestBuilder firstIsRegisterI() {
        runner.injectFirst((tmpRunner, argument) -> cpuRunner.setI(argument.intValue()));
        return this;
    }

    public ByteTestBuilder firstIsRegisterR() {
        runner.injectFirst((tmpRunner, argument) -> cpuRunner.setR(argument.intValue()));
        return this;
    }

    public ByteTestBuilder secondIsRegisterI() {
        runner.injectSecond((tmpRunner, argument) -> cpuRunner.setI(argument.intValue()));
        return this;
    }

    public ByteTestBuilder secondIsRegisterR() {
        runner.injectSecond((tmpRunner, argument) -> cpuRunner.setR(argument.intValue()));
        return this;
    }

    public ByteTestBuilder secondIsFlags() {
        runner.injectSecond((tmpRunner, argument) -> cpuRunner.setFlags(argument.intValue()));
        return this;
    }

    public ByteTestBuilder firstIsDeviceAndSecondIsPort() {
        runner.injectTwoOperands((tmpRunner, first, second) ->
                cpuRunner.getDevice(second.intValue() & 0xFF).setValue(first));
        return this;
    }

    public ByteTestBuilder setRegister(int register, int value) {
        runner.injectFirst((tmpRunner, argument) -> cpuRunner.setRegister(register, value));
        return this;
    }

    public ByteTestBuilder setPair(int registerPair, int value) {
        runner.injectFirst(
                (tmpRunner, argument) -> tmpRunner.ensureProgramSize(value + 1),
                (tmpRunner, argument) -> cpuRunner.setRegisterPair(registerPair, value)
        );
        return this;
    }

    public ByteTestBuilder setIX(int ix) {
        runner.injectFirst((runner, argument) -> runner.setIX(ix));
        return this;
    }

    public ByteTestBuilder setIY(int iy) {
        runner.injectFirst((runner, argument) -> runner.setIY(iy));
        return this;
    }

    public ByteTestBuilder setSP(int sp) {
        runner.injectFirst((runner, argument) -> runner.setSP(sp));
        return this;
    }

    public ByteTestBuilder setMemoryByteAt(int address, byte value) {
        runner.injectFirst((runner, argument) -> runner.setByte(address, value));
        return this;
    }

    public ByteTestBuilder verifyRegister(int register, Function<RunnerContext<Byte>, Integer> operation) {
        lastOperation = Objects.requireNonNull(operation);
        return verifyRegister(register);
    }

    public ByteTestBuilder verifyRegisterI(Function<RunnerContext<Byte>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        runner.verifyAfterTest(context -> cpuVerifier.checkI(operator.apply(context)));
        return this;
    }

    public ByteTestBuilder verifyRegisterR(Function<RunnerContext<Byte>, Integer> operation) {
        lastOperation = Objects.requireNonNull(operation);
        runner.verifyAfterTest(context -> cpuVerifier.checkR(operation.apply(context)));
        return this;
    }

    public ByteTestBuilder verifyRegister(int register) {
        if (lastOperation == null) {
            throw new IllegalStateException("Last operation is not set!");
        }
        Function<RunnerContext<Byte>, Integer> operation = lastOperation;
        runner.verifyAfterTest(context -> cpuVerifier.checkRegister(register, operation.apply(context)));
        return this;
    }

    public ByteTestBuilder verifyPC(Function<RunnerContext<Byte>, Integer> operation) {
        lastOperation = Objects.requireNonNull(operation);
        runner.verifyAfterTest(context -> cpuVerifier.checkPC(operation.apply(context)));
        return this;
    }

    public ByteTestBuilder verifyDeviceWhenSecondIsPort(Function<RunnerContext<Byte>, Integer> operation) {
        lastOperation = Objects.requireNonNull(operation);
        runner.verifyAfterTest(context -> cpuVerifier.checkDeviceValue(context.second, operation.apply(context)));
        return this;
    }

    public ByteTestBuilder verify(Consumer<RunnerContext<Byte>> verifier) {
        runner.verifyAfterTest(verifier);
        return this;
    }
}
