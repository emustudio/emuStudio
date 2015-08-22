package net.sf.emustudio.zilogZ80.impl.suite;

import net.sf.emustudio.cpu.testsuite.Test;
import net.sf.emustudio.cpu.testsuite.TestBuilder;
import net.sf.emustudio.cpu.testsuite.injectors.InstructionOperand;
import net.sf.emustudio.cpu.testsuite.injectors.MemoryByte;
import net.sf.emustudio.cpu.testsuite.injectors.MemoryExpand;
import net.sf.emustudio.cpu.testsuite.runners.RunnerContext;
import net.sf.emustudio.zilogZ80.impl.suite.injectors.RegisterPair;
import net.sf.emustudio.zilogZ80.impl.suite.injectors.RegisterPairPSW;
import net.sf.emustudio.zilogZ80.impl.suite.verifiers.IX_Verifier;
import net.sf.emustudio.zilogZ80.impl.suite.verifiers.IY_Verifier;
import net.sf.emustudio.zilogZ80.impl.suite.verifiers.PC_Verifier;
import net.sf.emustudio.zilogZ80.impl.suite.verifiers.RegisterPair_PSW_Verifier;
import net.sf.emustudio.zilogZ80.impl.suite.verifiers.RegisterPair_SP_Verifier;
import net.sf.emustudio.zilogZ80.impl.suite.verifiers.RegisterVerifier;

import java.util.function.Function;

import static net.sf.emustudio.zilogZ80.impl.suite.Utils.get8MSBplus8LSB;

public class IntegerTestBuilder extends TestBuilder<Integer, IntegerTestBuilder, CpuRunnerImpl, CpuVerifierImpl>  {

    public IntegerTestBuilder(CpuRunnerImpl cpuRunner, CpuVerifierImpl cpuVerifier) {
        super(cpuRunner, cpuVerifier);
    }

    public IntegerTestBuilder firstIsPair(int registerPair) {
        runner.injectFirst(new MemoryExpand(), new RegisterPair(registerPair));
        return this;
    }

    public IntegerTestBuilder secondIsPair(int registerPair) {
        runner.injectSecond(new MemoryExpand(), new RegisterPair(registerPair));
        return this;
    }

    public IntegerTestBuilder secondIsIX() {
        runner.injectSecond((tmpRunner, argument) -> cpuRunner.setIX(argument.intValue()));
        return this;
    }

    public IntegerTestBuilder firstIsRegisterPairPSW(int registerPairPSW) {
        runner.injectFirst(new MemoryExpand(), new RegisterPairPSW(registerPairPSW));
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

    public IntegerTestBuilder first8MSBisIY() {
        runner.injectFirst((tmpRunner, argument) -> cpuRunner.setIY(argument.intValue() & 0xFF00));
        return this;
    }

    public Test<Integer> runWithFirst8bitOperand(int... instruction) {
        return create(
                (tmpRunner, argument) ->
                        new InstructionOperand<Byte, CpuRunnerImpl>(instruction).inject(cpuRunner, (byte)(argument.intValue() & 0xFF)),
                true
        );
    }

    public IntegerTestBuilder first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte() {
        runner.injectBoth((tmpRunner, first, second) -> {
                    new MemoryByte(get8MSBplus8LSB(first.intValue())).inject(tmpRunner, (byte) (second.intValue() & 0xFF));
                }
        );
        return this;
    }

    public IntegerTestBuilder secondIsRegisterPairPSW(int registerPairPSW) {
        runner.injectSecond(new MemoryExpand(), new RegisterPairPSW(registerPairPSW));
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

    public IntegerTestBuilder verifyPairAndPSW(int registerPair, Function<RunnerContext<Integer>, Integer> operation) {
        lastOperation = operation;
        addVerifier(new RegisterPair_PSW_Verifier(cpuVerifier, operation, registerPair));
        return this;
    }

    public IntegerTestBuilder verifyRegister(int register, Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = operator;
        return verifyRegister(register);
    }

    public IntegerTestBuilder verifyRegister(int register) {
        if (lastOperation == null) {
            throw new IllegalStateException("Last operation is not set!");
        }
        addVerifier(new RegisterVerifier<>(cpuVerifier, lastOperation, register));
        return this;
    }

    public IntegerTestBuilder verifyPair(int registerPair, Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = operator;
        addVerifier(new RegisterPair_SP_Verifier<>(cpuVerifier, operator, registerPair));
        return this;
    }

    public IntegerTestBuilder verifyIX(Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = operator;
        addVerifier(new IX_Verifier<Integer>(cpuVerifier, operator));
        return this;
    }

    public IntegerTestBuilder verifyIY(Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = operator;
        addVerifier(new IY_Verifier<Integer>(cpuVerifier, operator));
        return this;
    }

    public IntegerTestBuilder verifyPC(Function<RunnerContext<Integer>, Integer> operator) {
        lastOperation = operator;
        addVerifier(new PC_Verifier(cpuVerifier, operator));
        return this;
    }

    public IntegerTestBuilder verifyIFF1isEnabled() {
        addVerifier(context -> cpuVerifier.checkInterruptsAreEnabled(0));
        return this;
    }

}
