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
package net.sf.emustudio.cpu.testsuite;

import net.sf.emustudio.cpu.testsuite.injectors.MemoryAddress;
import net.sf.emustudio.cpu.testsuite.injectors.MemoryByte;
import net.sf.emustudio.cpu.testsuite.injectors.MemoryWord;
import net.sf.emustudio.cpu.testsuite.injectors.NoOperInstr;
import net.sf.emustudio.cpu.testsuite.injectors.OneOperInstr;
import net.sf.emustudio.cpu.testsuite.injectors.TwoOperInstr;
import net.sf.emustudio.cpu.testsuite.verifiers.FlagsVerifier;
import net.sf.emustudio.cpu.testsuite.verifiers.MemoryByteVerifier;
import net.sf.emustudio.cpu.testsuite.verifiers.MemoryWordVerifier;

import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public abstract class TestBuilder<OperandT extends Number, TestBuilderT extends TestBuilder,
        RunnerT extends CpuRunner, VerifierT extends CpuVerifier> {
    protected final RunnerT cpuRunner;
    protected final VerifierT cpuVerifier;
    protected final TestRunner<RunnerT, OperandT> runner;

    protected Function<RunnerContext<OperandT>, Integer> lastOperation;

    protected TestBuilder(RunnerT cpuRunner, VerifierT cpuVerifier) {
        this.cpuRunner = Objects.requireNonNull(cpuRunner);
        this.cpuVerifier = Objects.requireNonNull(cpuVerifier);
        this.runner = new TestRunner<>(cpuRunner);
    }

    public TestBuilderT clearAllVerifiers() {
        runner.clearAllVerifiers();
        return (TestBuilderT)this;
    }

    @SuppressWarnings("unused")
    public TestBuilderT verifyAll(Consumer<RunnerContext<OperandT>>... verifiers) {
        runner.verifyAfterTest(verifiers);
        return (TestBuilderT)this;
    }

    public TestBuilderT registerIsRandom(int register, int maxValue) {
        Random random = new Random();
        runner.injectFirst((tmpRunner, argument) -> cpuRunner.setRegister(register, random.nextInt(maxValue + 1)));
        return (TestBuilderT)this;
    }

    @SuppressWarnings("unused")
    public TestBuilderT printRegister(int register) {
        runner.injectTwoOperands((runner, first, second) ->
                        System.out.println(String.format("REG_%d=%x", register, runner.getRegisters().get(register)))
        );
        return (TestBuilderT)this;
    }

    @SuppressWarnings("unused")
    public TestBuilderT printOperands() {
        runner.injectTwoOperands((runner, first, second) ->
                        System.out.println(String.format("first=%x, second=%x", first, second))
        );
        return (TestBuilderT)this;
    }

    @SuppressWarnings("unused")
    public TestBuilderT printInjectingProcess() {
        runner.printInjectingProcess();
        return (TestBuilderT)this;
    }

    public TestBuilderT verifyFlags(FlagsCheck flagsCheck, Function<RunnerContext<OperandT>, Integer> operator) {
        lastOperation = operator;
        return verifyFlagsOfLastOp(flagsCheck);
    }

    public TestBuilderT verifyFlagsOfLastOp(FlagsCheck flagsCheck) {
        if (lastOperation == null) {
            throw new IllegalStateException("Last operation is not set!");
        }
        Function<RunnerContext<OperandT>, Integer> operation = lastOperation;
        runner.verifyAfterTest(new FlagsVerifier<>(cpuVerifier, operation, flagsCheck));
        return (TestBuilderT)this;
    }

    public TestBuilderT verifyByte(int address, Function<RunnerContext<OperandT>, Integer> operator) {
        lastOperation = operator;
        return verifyByte(address);
    }

    public TestBuilderT verifyWord(Function<RunnerContext<OperandT>, Integer> addressOperator,
                                   Function<RunnerContext<OperandT>, Integer> operator) {
        lastOperation = operator;
        runner.verifyAfterTest(new MemoryWordVerifier(cpuVerifier, operator, addressOperator));
        return (TestBuilderT)this;
    }

    public TestBuilderT verifyByte(int address) {
        return verifyByte(context -> address);
    }

    public TestBuilderT verifyByte(Function<RunnerContext<OperandT>, Integer> addressOperator,
                                   Function<RunnerContext<OperandT>, Integer> operator) {
        lastOperation = operator;
        return verifyByte(addressOperator);
    }

    public TestBuilderT verifyByte(Function<RunnerContext<OperandT>, Integer> addressOperator) {
        if (lastOperation == null) {
            throw new IllegalStateException("Last operation is not set!");
        }
        runner.verifyAfterTest(new MemoryByteVerifier<>(cpuVerifier, lastOperation, addressOperator));
        return (TestBuilderT)this;
    }

    public TestBuilderT keepCurrentInjectorsAfterRun() {
        runner.keepCurrentInjectorsAfterClear();
        return (TestBuilderT)this;
    }

    public TestBuilderT clearOtherVerifiersAfterRun() {
        runner.keepCurrentVerifiersAfterClear();
        return (TestBuilderT)this;
    }

    public TestBuilderT firstIsMemoryByteAt(int address) {
        runner.injectFirst(new MemoryByte<>(address));
        return (TestBuilderT)this;
    }

    public TestBuilderT secondIsMemoryByteAt(int address) {
        runner.injectSecond(new MemoryByte<>(address));
        return (TestBuilderT)this;
    }

    public TestBuilderT firstIsMemoryWordAt(int address) {
        runner.injectFirst(new MemoryWord<>(address));
        return (TestBuilderT)this;
    }

    @SuppressWarnings("unused")
    public TestBuilderT secondIsMemoryWordAt(int address) {
        runner.injectSecond(new MemoryWord<>(address));
        return (TestBuilderT)this;
    }

    public TestBuilderT firstIsMemoryAddressByte(int value) {
        runner.injectFirst(new MemoryAddress<>((byte)value));
        return (TestBuilderT)this;
    }

    @SuppressWarnings("unused")
    public TestBuilderT secondIsMemoryAddressByte(int value) {
        runner.injectSecond(new MemoryAddress<>((byte) value));
        return (TestBuilderT)this;
    }

    public TestBuilderT firstIsMemoryAddressWord(int value) {
        runner.injectFirst(new MemoryAddress<>(value));
        return (TestBuilderT)this;
    }

    @SuppressWarnings("unused")
    public TestBuilderT secondIsMemoryAddressWord(int value) {
        runner.injectSecond(new MemoryAddress<>(value));
        return (TestBuilderT)this;
    }

    public TestBuilderT firstIsAddressAndSecondIsMemoryWord() {
        runner.injectTwoOperands((runner, first, second) -> {
            runner.ensureProgramSize(first.intValue() + 4);
            runner.setByte(first.intValue(), second.intValue() & 0xFF);
            runner.setByte(first.intValue() + 1, (second.intValue() >>> 8) & 0xFF);
        });
        return (TestBuilderT)this;
    }

    @SuppressWarnings("unused")
    public TestBuilderT secondIsAddressAndFirstIsMemoryWord() {
        runner.injectTwoOperands((runner, first, second) -> {
            runner.ensureProgramSize(second.intValue() + 4);
            runner.setByte(second.intValue(), first.intValue() & 0xFF);
            runner.setByte(second.intValue() + 1, (first.intValue() >>> 8) & 0xFF);
        });
        return (TestBuilderT)this;
    }

    public TestBuilderT firstIsAddressAndSecondIsMemoryByte() {
        runner.injectTwoOperands((runner, first, second) -> {
            runner.ensureProgramSize(first.intValue() + 4);
            runner.setByte(first.intValue(), second.intValue() & 0xFF);
        });
        return (TestBuilderT)this;
    }

    public TestBuilderT secondIsAddressAndFirstIsMemoryByte() {
        runner.injectTwoOperands((runner, first, second) -> {
            runner.ensureProgramSize(second.intValue() + 4);
            runner.setByte(second.intValue(), first.intValue() & 0xFF);
        });
        return (TestBuilderT)this;
    }

    public TestBuilderT setFlags(int flags) {
        runner.injectFirst((tmpRunner, argument) -> tmpRunner.setFlags(flags));
        return (TestBuilderT)this;
    }

    public TestBuilderT expandMemory(Function<Number, Integer> address) {
        runner.injectFirst((tmpRunner, argument) -> tmpRunner.ensureProgramSize(address.apply(argument)));
        return (TestBuilderT)this;
    }

    public TestRunner<RunnerT, OperandT> run(int... instruction) {
        return prepareTest().injectNoOperand(new NoOperInstr<>(instruction));
    }

    public TestRunner<RunnerT, OperandT> runWithFirstOperand(int... instruction) {
        return prepareTest().injectFirst(new OneOperInstr<>(instruction));
    }

    public TestRunner<RunnerT, OperandT> runWithSecondOperand(int... instruction) {
        return prepareTest().injectSecond(new OneOperInstr<>(instruction));
    }

    public TestRunner<RunnerT, OperandT> runWithFirst8bitOperandWithOpcodeAfter(int opcodeAfterOperand, int... instruction) {
        return prepareTest().injectFirst((tmpRunner, first) ->
            new OneOperInstr<RunnerT, Byte>(instruction)
                .placeOpcodesAfterOperand(opcodeAfterOperand)
                .accept(cpuRunner, first.byteValue())
        );
    }

    public TestRunner<RunnerT, OperandT> runWithFirst8bitOperand(int... instruction) {
        return prepareTest().injectFirst((tmpRunner, first) ->
            new OneOperInstr<RunnerT, Byte>(instruction).accept(tmpRunner, first.byteValue())
        );
    }

    public TestRunner<RunnerT, OperandT> runWithFirst8bitOperandTwoTimes(int... instruction) {
        return prepareTest().injectFirst((tmpRunner, first) ->
            new TwoOperInstr<RunnerT, Byte>(instruction)
                .inject(tmpRunner, first.byteValue(), first.byteValue())
        );
    }

    @SuppressWarnings("unused")
    public TestRunner<RunnerT, OperandT> runWithBothOperandsWithOpcodeAfter(int opcodeAfter, int... instruction) {
        return prepareTest().injectTwoOperands(
            new TwoOperInstr<RunnerT, OperandT>(instruction).placeOpcodesAfterOperands(opcodeAfter)
        );
    }

    private TestRunner<RunnerT, OperandT> prepareTest() {
        TestRunner<RunnerT, OperandT> tmpRunner = runner.clone();

        runner.clearInjectors();
        runner.clearVerifiers();
        return tmpRunner;
    }
}
