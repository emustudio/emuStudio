/*
 * Copyright (C) 2015 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.emustudio.zilogZ80.impl.suite;

import net.sf.emustudio.cpu.testsuite.TestBuilder;
import net.sf.emustudio.cpu.testsuite.runners.RunnerContext;
import net.sf.emustudio.zilogZ80.impl.suite.injectors.Register;

import java.util.Objects;
import java.util.function.Function;

public class ByteTestBuilder extends TestBuilder<Byte, ByteTestBuilder, CpuRunnerImpl, CpuVerifierImpl>  {

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

    public ByteTestBuilder firstIsDeviceAndSecondIsPort() {
        runner.injectTwoOperands((tmpRunner, first, second) ->
                cpuRunner.getDevice(second.intValue() & 0xFF).setValue(first.byteValue()));
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

    public ByteTestBuilder verifyRegister(int register, Function<RunnerContext<Byte>, Integer> operation) {
        lastOperation = Objects.requireNonNull(operation);
        return verifyRegister(register);
    }

    public ByteTestBuilder verifyRegisterI(Function<RunnerContext<Byte>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        addVerifier(context -> cpuVerifier.checkI(operator.apply(context)));
        return this;
    }

    public ByteTestBuilder verifyRegisterR(Function<RunnerContext<Byte>, Integer> operation) {
        lastOperation = Objects.requireNonNull(operation);
        addVerifier(context -> cpuVerifier.checkR(operation.apply(context)));
        return this;
    }

    public ByteTestBuilder verifyRegister(int register) {
        if (lastOperation == null) {
            throw new IllegalStateException("Last operation is not set!");
        }
        Function<RunnerContext<Byte>, Integer> operation = lastOperation;
        addVerifier(context -> cpuVerifier.checkRegister(register, operation.apply(context)));
        return this;
    }

    public ByteTestBuilder verifyPair(int registerPair, Function<RunnerContext<Byte>, Integer> operation) {
        lastOperation = Objects.requireNonNull(operation);
        addVerifier(context -> cpuVerifier.checkRegisterPair(registerPair, operation.apply(context)));
        return this;
    }

    public ByteTestBuilder verifyPC(Function<RunnerContext<Byte>, Integer> operation) {
        lastOperation = Objects.requireNonNull(operation);
        addVerifier(context -> cpuVerifier.checkPC(operation.apply(context)));
        return this;
    }

    public ByteTestBuilder verifyDeviceWhenSecondIsPort(Function<RunnerContext<Byte>, Integer> operation) {
        lastOperation = Objects.requireNonNull(operation);
        addVerifier(context -> cpuVerifier.checkDeviceValue(context.second, operation.apply(context)));
        return this;
    }
}
