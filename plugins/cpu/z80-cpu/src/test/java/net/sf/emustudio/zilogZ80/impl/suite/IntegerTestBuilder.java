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
import net.sf.emustudio.cpu.testsuite.injectors.MemoryByte;
import net.sf.emustudio.cpu.testsuite.injectors.MemoryExpand;
import net.sf.emustudio.cpu.testsuite.runners.RunnerContext;
import net.sf.emustudio.zilogZ80.impl.suite.injectors.RegisterPair;
import net.sf.emustudio.zilogZ80.impl.suite.injectors.RegisterPair2;
import net.sf.emustudio.zilogZ80.impl.suite.injectors.RegisterPairPSW;

import java.util.Objects;
import java.util.function.Function;

import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_A;
import static net.sf.emustudio.zilogZ80.impl.suite.Utils.get8MSBplus8LSB;

public class IntegerTestBuilder extends TestBuilder<Integer, IntegerTestBuilder, CpuRunnerImpl, CpuVerifierImpl>  {

    public IntegerTestBuilder(CpuRunnerImpl cpuRunner, CpuVerifierImpl cpuVerifier) {
        super(cpuRunner, cpuVerifier);
    }

    public IntegerTestBuilder firstIsPair(int registerPair) {
        runner.injectFirst(new MemoryExpand(), new RegisterPair(registerPair));
        return this;
    }

    public IntegerTestBuilder firstIsPair2(int registerPair) {
        runner.injectFirst(new MemoryExpand(), new RegisterPair2(registerPair));
        return this;
    }

    public IntegerTestBuilder secondIsPair(int registerPair) {
        runner.injectSecond(new MemoryExpand(), new RegisterPair(registerPair));
        return this;
    }

    public IntegerTestBuilder secondIsPair2(int registerPair) {
        runner.injectSecond(new MemoryExpand(), new RegisterPair2(registerPair));
        return this;
    }

    public IntegerTestBuilder secondIsIX() {
        runner.injectSecond((tmpRunner, argument) -> cpuRunner.setIX(argument.intValue()));
        return this;
    }

    public IntegerTestBuilder secondIsIY() {
        runner.injectSecond((tmpRunner, argument) -> cpuRunner.setIY(argument.intValue()));
        return this;
    }

    public IntegerTestBuilder firstIsPSW() {
        runner.injectFirst(new MemoryExpand(), new RegisterPairPSW(3));
        return this;
    }

    public IntegerTestBuilder firstIsIX() {
        runner.injectFirst((tmpRunner, argument) -> cpuRunner.setIX(argument.intValue()));
        return this;
    }

    public IntegerTestBuilder firstIsIY() {
        runner.injectFirst((tmpRunner, argument) -> cpuRunner.setIY(argument.intValue()));
        return this;
    }

    public IntegerTestBuilder first8MSBisIX() {
        runner.injectFirst((tmpRunner, argument) -> cpuRunner.setIX(argument.intValue() & 0xFF00));
        return this;
    }

    public IntegerTestBuilder first8LSBisRegister(int register) {
        runner.injectFirst((tmpRunner, argument) -> cpuRunner.setRegister(register, argument.intValue() & 0xFF));
        return this;
    }

    public IntegerTestBuilder first8MSBisRegister(int register) {
        runner.injectFirst((tmpRunner, argument) -> cpuRunner.setRegister(register, (argument.intValue() >>> 8) & 0xFF));
        return this;
    }

    public IntegerTestBuilder first8MSBisIY() {
        runner.injectFirst((tmpRunner, argument) -> cpuRunner.setIY(argument.intValue() & 0xFF00));
        return this;
    }

    public IntegerTestBuilder first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte() {
        runner.injectTwoOperands((tmpRunner, first, second) -> {
                    new MemoryByte(get8MSBplus8LSB(first.intValue())).inject(tmpRunner, (byte) (second.intValue() & 0xFF));
                }
        );
        return this;
    }

    public IntegerTestBuilder secondIsPSW() {
        runner.injectSecond(new MemoryExpand(), new RegisterPairPSW(3));
        return this;
    }

    public IntegerTestBuilder firstIsAF() {
        runner.injectFirst((tmpRunner, argument) -> {
            cpuRunner.resetFlags();
            cpuRunner.setFlags(argument.intValue() & 0xFF);
            cpuRunner.setRegister(REG_A, (argument.intValue() >>> 8) & 0xFF);
        });
        return this;
    }

    public IntegerTestBuilder secondIsAF2() {
        runner.injectSecond((tmpRunner, argument) -> {
            cpuRunner.resetFlags2();
            cpuRunner.setFlags2(argument.intValue() & 0xFF);
            cpuRunner.setRegister2(REG_A, (argument.intValue() >>> 8) & 0xFF);
        });
        return this;
    }

    public IntegerTestBuilder first8MSBisDeviceAndFirst8LSBIsPort() {
        runner.injectFirst((tmpRunner, first) ->
                cpuRunner.getDevice(first.intValue() & 0xFF).setValue((byte)((first.intValue() >>> 8) & 0xFF)));
        return this;
    }

    public IntegerTestBuilder setRegister(int register, int value) {
        runner.injectFirst((tmpRunner, argument) -> cpuRunner.setRegister(register, value));
        return this;
    }

    public IntegerTestBuilder disableIFF1() {
        runner.injectFirst((tmpRunner, argument) -> cpuRunner.disableIFF1());
        return this;
    }

    public IntegerTestBuilder enableIFF2() {
        runner.injectFirst((tmpRunner, argument) -> cpuRunner.enableIFF2());
        return this;
    }

    public IntegerTestBuilder setPair(int registerPair, int value) {
        runner.injectFirst((tmpRunner, argument) -> cpuRunner.setRegisterPair(registerPair, value));
        return this;
    }

    public IntegerTestBuilder verifyPSW(Function<RunnerContext<Integer>, Integer> operation) {
        lastOperation = Objects.requireNonNull(operation);
        addVerifier(context -> cpuVerifier.checkRegisterPairPSW(3, operation.apply(context)));
        return this;
    }

    public IntegerTestBuilder verifyRegister(int register, Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        return verifyRegister(register);
    }

    public IntegerTestBuilder verifyRegister(int register) {
        if (lastOperation == null) {
            throw new IllegalStateException("Last operation is not set!");
        }
        Function<RunnerContext<Integer>, Integer> operation = lastOperation;
        addVerifier(context -> cpuVerifier.checkRegister(register, operation.apply(context)));
        return this;
    }

    public IntegerTestBuilder verifyPair(int registerPair, Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        addVerifier(context -> cpuVerifier.checkRegisterPair(registerPair, operator.apply(context)));
        return this;
    }

    public IntegerTestBuilder verifyPair2(int registerPair, Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        addVerifier(context -> cpuVerifier.checkRegisterPair2(registerPair, operator.apply(context)));
        return this;
    }

    public IntegerTestBuilder verifyIX(Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        addVerifier(context -> cpuVerifier.checkIX(operator.apply(context)));
        return this;
    }

    public IntegerTestBuilder verifyIY(Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        addVerifier(context -> cpuVerifier.checkIY(operator.apply(context)));
        return this;
    }

    public IntegerTestBuilder verifyPC(Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        addVerifier(context -> cpuVerifier.checkPC(operator.apply(context)));
        return this;
    }

    public IntegerTestBuilder verifyIFF1isEnabled() {
        addVerifier(context -> cpuVerifier.checkInterruptsAreEnabled(0));
        return this;
    }

    public IntegerTestBuilder verifyAF(Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        addVerifier(context -> cpuVerifier.checkAF(operator.apply(context)));
        return this;
    }

    public IntegerTestBuilder verifyAF2(Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        addVerifier(context -> cpuVerifier.checkAF2(operator.apply(context)));
        return this;
    }

}
