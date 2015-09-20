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
        lastOperation = Objects.requireNonNull(operator);
        return verifyRegister(register);
    }

    public ByteTestBuilder verifyRegisterI(Function<RunnerContext<Byte>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        addVerifier(context -> cpuVerifier.checkI(operator.apply(context)));
        return this;
    }

    public ByteTestBuilder verifyRegisterR(Function<RunnerContext<Byte>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        addVerifier(context -> cpuVerifier.checkR(operator.apply(context)));
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

    public ByteTestBuilder verifyPair(int registerPair, Function<RunnerContext<Byte>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        addVerifier(context -> cpuVerifier.checkRegisterPair(registerPair, operator.apply(context)));
        return this;
    }

    public ByteTestBuilder verifyPC(Function<RunnerContext<Byte>, Integer> operator) {
        lastOperation = Objects.requireNonNull(operator);
        addVerifier(context -> cpuVerifier.checkPC(operator.apply(context)));
        return this;
    }
}
