package net.sf.emustudio.zilogZ80.impl.suite;

import net.sf.emustudio.cpu.testsuite.TestBuilder;
import net.sf.emustudio.cpu.testsuite.runners.RunnerContext;
import net.sf.emustudio.zilogZ80.impl.suite.injectors.Register;
import net.sf.emustudio.zilogZ80.impl.suite.verifiers.PC_Verifier;
import net.sf.emustudio.zilogZ80.impl.suite.verifiers.RegisterPair_SP_Verifier;
import net.sf.emustudio.zilogZ80.impl.suite.verifiers.RegisterVerifier;

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

    public ByteTestBuilder verifyRegister(int register, Function<RunnerContext<Byte>, Integer> operator) {
        lastOperation = operator;
        return verifyRegister(register);
    }

    public ByteTestBuilder verifyRegister(int register) {
        if (lastOperation == null) {
            throw new IllegalStateException("Last operation is not set!");
        }
        addVerifier(new RegisterVerifier<>(cpuVerifier, lastOperation, register));
        return this;
    }

    public ByteTestBuilder verifyPair(int registerPair, Function<RunnerContext<Byte>, Integer> operator) {
        lastOperation = operator;
        addVerifier(new RegisterPair_SP_Verifier<Byte>(cpuVerifier, operator, registerPair));
        return this;
    }

    public ByteTestBuilder verifyPC(Function<RunnerContext<Byte>, Integer> operator) {
        lastOperation = operator;
        addVerifier(new PC_Verifier(cpuVerifier, operator));
        return this;
    }
}
