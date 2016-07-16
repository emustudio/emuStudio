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

import net.sf.emustudio.cpu.testsuite.injectors.InstructionNoOperands;
import net.sf.emustudio.cpu.testsuite.injectors.InstructionSingleOperand;
import net.sf.emustudio.cpu.testsuite.injectors.InstructionTwoOperands;
import net.sf.emustudio.cpu.testsuite.injectors.MemoryAddress;
import net.sf.emustudio.cpu.testsuite.injectors.MemoryByte;
import net.sf.emustudio.cpu.testsuite.injectors.MemoryWord;
import net.sf.emustudio.cpu.testsuite.verifiers.FlagsVerifier;
import net.sf.emustudio.cpu.testsuite.verifiers.MemoryByteVerifier;
import net.sf.emustudio.cpu.testsuite.verifiers.MemoryWordVerifier;

import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public abstract class TestBuilder<OperandType extends Number, SpecificTestBuilder extends TestBuilder,
        T extends CpuRunner, CpuVerifierType extends CpuVerifier> {
    protected final T cpuRunner;
    protected final CpuVerifierType cpuVerifier;
    protected final TestRunner<T, OperandType> runner;

    protected Function<RunnerContext<OperandType>, Integer> lastOperation;

    protected TestBuilder(T cpuRunner, CpuVerifierType cpuVerifier) {
        this.cpuRunner = Objects.requireNonNull(cpuRunner);
        this.cpuVerifier = Objects.requireNonNull(cpuVerifier);
        this.runner = new TestRunner<>(cpuRunner);
    }

    public SpecificTestBuilder clearAllVerifiers() {
        runner.clearAllVerifiers();
        return (SpecificTestBuilder)this;
    }

    @SuppressWarnings("unused")
    public SpecificTestBuilder verifyAll(Consumer<RunnerContext<OperandType>>... verifiers) {
        runner.verifyAfterTest(verifiers);
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder registerIsRandom(int register, int maxValue) {
        Random random = new Random();
        runner.injectFirst((tmpRunner, argument) -> cpuRunner.setRegister(register, random.nextInt(maxValue + 1)));
        return (SpecificTestBuilder)this;
    }

    @SuppressWarnings("unused")
    public SpecificTestBuilder printRegister(int register) {
        runner.injectTwoOperands((runner, first, second) ->
                        System.out.println(String.format("REG_%d=%x", register, runner.getRegisters().get(register)))
        );
        return (SpecificTestBuilder)this;
    }

    @SuppressWarnings("unused")
    public SpecificTestBuilder printOperands() {
        runner.injectTwoOperands((runner, first, second) ->
                        System.out.println(String.format("first=%x, second=%x", first, second))
        );
        return (SpecificTestBuilder)this;
    }

    @SuppressWarnings("unused")
    public SpecificTestBuilder printInjectingProcess() {
        runner.printInjectingProcess();
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder verifyFlags(FlagsCheck flagsCheck, Function<RunnerContext<OperandType>, Integer> operator) {
        lastOperation = operator;
        return verifyFlagsOfLastOp(flagsCheck);
    }

    public SpecificTestBuilder verifyFlagsOfLastOp(FlagsCheck flagsCheck) {
        if (lastOperation == null) {
            throw new IllegalStateException("Last operation is not set!");
        }
        Function<RunnerContext<OperandType>, Integer> operation = lastOperation;
        runner.verifyAfterTest(new FlagsVerifier<>(cpuVerifier, operation, flagsCheck));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder verifyByte(int address, Function<RunnerContext<OperandType>, Integer> operator) {
        lastOperation = operator;
        return verifyByte(address);
    }

    public SpecificTestBuilder verifyWord(Function<RunnerContext<OperandType>, Integer> addressOperator,
                                          Function<RunnerContext<OperandType>, Integer> operator) {
        lastOperation = operator;
        runner.verifyAfterTest(new MemoryWordVerifier(cpuVerifier, operator, addressOperator));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder verifyByte(int address) {
        return verifyByte(context -> address);
    }

    public SpecificTestBuilder verifyByte(Function<RunnerContext<OperandType>, Integer> addressOperator,
                                          Function<RunnerContext<OperandType>, Integer> operator) {
        lastOperation = operator;
        return verifyByte(addressOperator);
    }

    public SpecificTestBuilder verifyByte(Function<RunnerContext<OperandType>, Integer> addressOperator) {
        if (lastOperation == null) {
            throw new IllegalStateException("Last operation is not set!");
        }
        runner.verifyAfterTest(new MemoryByteVerifier<>(cpuVerifier, lastOperation, addressOperator));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder keepCurrentInjectorsAfterRun() {
        runner.keepCurrentInjectorsAfterClear();
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder clearOtherVerifiersAfterRun() {
        runner.keepCurrentVerifiersAfterClear();
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder firstIsMemoryByteAt(int address) {
        runner.injectFirst(new MemoryByte<>(address));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder secondIsMemoryByteAt(int address) {
        runner.injectSecond(new MemoryByte<>(address));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder firstIsMemoryWordAt(int address) {
        runner.injectFirst(new MemoryWord<>(address));
        return (SpecificTestBuilder)this;
    }

    @SuppressWarnings("unused")
    public SpecificTestBuilder secondIsMemoryWordAt(int address) {
        runner.injectSecond(new MemoryWord<>(address));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder firstIsMemoryAddressByte(int value) {
        runner.injectFirst(new MemoryAddress<>((byte)value));
        return (SpecificTestBuilder)this;
    }

    @SuppressWarnings("unused")
    public SpecificTestBuilder secondIsMemoryAddressByte(int value) {
        runner.injectSecond(new MemoryAddress<>((byte) value));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder firstIsMemoryAddressWord(int value) {
        runner.injectFirst(new MemoryAddress<>(value));
        return (SpecificTestBuilder)this;
    }

    @SuppressWarnings("unused")
    public SpecificTestBuilder secondIsMemoryAddressWord(int value) {
        runner.injectSecond(new MemoryAddress<>(value));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder firstIsAddressAndSecondIsMemoryWord() {
        runner.injectTwoOperands((runner, first, second) -> {
            runner.ensureProgramSize(first.intValue() + 4);
            runner.setByte(first.intValue(), second.intValue() & 0xFF);
            runner.setByte(first.intValue() + 1, (second.intValue() >>> 8) & 0xFF);
        });
        return (SpecificTestBuilder)this;
    }

    @SuppressWarnings("unused")
    public SpecificTestBuilder secondIsAddressAndFirstIsMemoryWord() {
        runner.injectTwoOperands((runner, first, second) -> {
            runner.ensureProgramSize(second.intValue() + 4);
            runner.setByte(second.intValue(), first.intValue() & 0xFF);
            runner.setByte(second.intValue() + 1, (first.intValue() >>> 8) & 0xFF);
        });
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder firstIsAddressAndSecondIsMemoryByte() {
        runner.injectTwoOperands((runner, first, second) -> {
            runner.ensureProgramSize(first.intValue() + 4);
            runner.setByte(first.intValue(), second.intValue() & 0xFF);
        });
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder secondIsAddressAndFirstIsMemoryByte() {
        runner.injectTwoOperands((runner, first, second) -> {
            runner.ensureProgramSize(second.intValue() + 4);
            runner.setByte(second.intValue(), first.intValue() & 0xFF);
        });
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder setFlags(int flags) {
        runner.injectFirst((tmpRunner, argument) -> tmpRunner.setFlags(flags));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder expandMemory(Function<Number, Integer> address) {
        runner.injectFirst((tmpRunner, argument) -> tmpRunner.ensureProgramSize(address.apply(argument)));
        return (SpecificTestBuilder)this;
    }

    public TestRunner<T, OperandType> run(int... instruction) {
        return prepareTest().injectNoOperand(new InstructionNoOperands<>(instruction));
    }

    public TestRunner<T, OperandType> runWithFirstOperand(int... instruction) {
        return prepareTest().injectFirst(new InstructionSingleOperand<>(instruction));
    }

    public TestRunner<T, OperandType> runWithSecondOperand(int... instruction) {
        return prepareTest().injectSecond(new InstructionSingleOperand<>(instruction));
    }

    public TestRunner<T, OperandType> runWithFirst8bitOperandWithOpcodeAfter(int opcodeAfterOperand, int... instruction) {
        return prepareTest().injectFirst((tmpRunner, first) ->
            new InstructionSingleOperand<T, Byte>(instruction)
                .placeOpcodesAfterOperand(opcodeAfterOperand)
                .accept(cpuRunner, first.byteValue())
        );
    }

    public TestRunner<T, OperandType> runWithFirst8bitOperand(int... instruction) {
        return prepareTest().injectFirst((tmpRunner, first) ->
            new InstructionSingleOperand<T, Byte>(instruction)
                .accept(tmpRunner, first.byteValue())
        );
    }

    public TestRunner<T, OperandType> runWithFirst8bitOperandTwoTimes(int... instruction) {
        return prepareTest().injectFirst((tmpRunner, first) ->
            new InstructionTwoOperands<T, Byte>(instruction)
                .inject(tmpRunner, first.byteValue(), first.byteValue())
        );
    }

    @SuppressWarnings("unused")
    public TestRunner<T, OperandType> runWithBothOperandsWithOpcodeAfter(int opcodeAfter, int... instruction) {
        return prepareTest().injectTwoOperands(
            new InstructionTwoOperands<T, OperandType>(instruction).placeOpcodesAfterOperands(opcodeAfter)
        );
    }

    private TestRunner<T, OperandType> prepareTest() {
        TestRunner<T, OperandType> tmpRunner = runner.clone();

        runner.clearInjectors();
        runner.clearVerifiers();
        return tmpRunner;
    }
}
