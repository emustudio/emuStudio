package net.sf.emustudio.intel8080.impl.suite;

import net.sf.emustudio.intel8080.impl.suite.injectors.AddressAndMemoryWord;
import net.sf.emustudio.intel8080.impl.suite.injectors.MemoryAddress;
import net.sf.emustudio.intel8080.impl.suite.injectors.MemoryByte;
import net.sf.emustudio.intel8080.impl.suite.injectors.MemoryExpand;
import net.sf.emustudio.intel8080.impl.suite.injectors.MemoryWord;
import net.sf.emustudio.intel8080.impl.suite.injectors.Register;
import net.sf.emustudio.intel8080.impl.suite.injectors.RegisterPair;
import net.sf.emustudio.intel8080.impl.suite.injectors.RegisterPairPSW;
import net.sf.emustudio.intel8080.impl.suite.runners.BinaryRunner;
import net.sf.emustudio.intel8080.impl.suite.injectors.InstructionNoOperands;
import net.sf.emustudio.intel8080.impl.suite.injectors.InstructionWordOperand;
import net.sf.emustudio.intel8080.impl.suite.injectors.InstructionByteOperand;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerInjector;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;
import net.sf.emustudio.intel8080.impl.suite.runners.UnaryRunner;
import net.sf.emustudio.intel8080.impl.suite.verifiers.FlagsVerifier;
import net.sf.emustudio.intel8080.impl.suite.verifiers.MemoryByteVerifier;
import net.sf.emustudio.intel8080.impl.suite.verifiers.MemoryWordVerifier;
import net.sf.emustudio.intel8080.impl.suite.verifiers.PCVerifier;
import net.sf.emustudio.intel8080.impl.suite.verifiers.RegisterPair_PSW_Verifier;
import net.sf.emustudio.intel8080.impl.suite.verifiers.RegisterPair_SP_Verifier;
import net.sf.emustudio.intel8080.impl.suite.verifiers.RegisterVerifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class TestBuilder<K extends Number, SpecificTestBuilder extends TestBuilder> {
    protected final CpuRunner cpuRunner;
    protected final CpuVerifier cpuVerifier;
    protected final List<Consumer<RunnerContext<K>>> verifiers = new ArrayList<>();
    protected Function<RunnerContext<K>, Integer> lastOperation;

    private TestBuilder(CpuRunner cpuRunner, CpuVerifier cpuVerifier) {
        this.cpuRunner = Objects.requireNonNull(cpuRunner);
        this.cpuVerifier = Objects.requireNonNull(cpuVerifier);
    }

    public SpecificTestBuilder clearVerifiers() {
        verifiers.clear();
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder verifyAll(Collection<Consumer<RunnerContext<K>>> verifiers) {
        this.verifiers.addAll(verifiers);
        return (SpecificTestBuilder)this;
    }

    public List<Consumer<RunnerContext<K>>> getVerifiers() {
        return Collections.unmodifiableList(new ArrayList<>(verifiers));
    }

    public SpecificTestBuilder verifyFlags(FlagsBuilder flagsBuilder, Function<RunnerContext<K>, Integer> operator) {
        lastOperation = operator;
        return verifyFlagsOfLastOp(flagsBuilder);
    }

    public SpecificTestBuilder verifyFlagsOfLastOp(FlagsBuilder flagsBuilder) {
        if (lastOperation == null) {
            throw new IllegalStateException("Last operation is not set!");
        }
        verifiers.add(new FlagsVerifier<K>(cpuVerifier, lastOperation, flagsBuilder));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder verifyByte(int address, Function<RunnerContext<K>, Integer> operator) {
        lastOperation = operator;
        return verifyByte(address);
    }

    public SpecificTestBuilder verifyWord(Function<RunnerContext<K>, Integer> operator,
                                    Function<RunnerContext<K>, Integer> addressOperator) {
        lastOperation = operator;
        verifiers.add(new MemoryWordVerifier(cpuVerifier, operator, addressOperator));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder verifyByte(int address) {
        return verifyByte(context -> address);
    }

    public SpecificTestBuilder verifyByte(Function<RunnerContext<K>, Integer> addressOperator,
                                          Function<RunnerContext<K>, Integer> operator) {
        lastOperation = operator;
        return verifyByte(addressOperator);
    }

    public SpecificTestBuilder verifyByte(Function<RunnerContext<K>, Integer> addressOperator) {
        if (lastOperation == null) {
            throw new IllegalStateException("Last operation is not set!");
        }
        verifiers.add(new MemoryByteVerifier<>(cpuVerifier, lastOperation, addressOperator));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder verifyRegister(int register, Function<RunnerContext<K>, Integer> operator) {
        lastOperation = operator;
        return verifyRegister(register);
    }

    public SpecificTestBuilder verifyRegister(int register) {
        if (lastOperation == null) {
            throw new IllegalStateException("Last operation is not set!");
        }
        verifiers.add(new RegisterVerifier<K>(cpuVerifier, lastOperation, register));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder verifyPair(int registerPair, Function<RunnerContext<K>, Integer> operator) {
        lastOperation = operator;
        verifiers.add(new RegisterPair_SP_Verifier<K>(cpuVerifier, operator, registerPair));
        return (SpecificTestBuilder)this;
    }

    public static class UnaryByte extends TestBuilder<Byte, UnaryByte> {
        private final UnaryRunner<Byte> runner = new UnaryRunner<>(cpuRunner);

        public UnaryByte(CpuRunner cpuRunner, CpuVerifier cpuVerifier) {
            super(cpuRunner, cpuVerifier);
        }

        public UnaryByte keepCurrentInjectorsAfterRun() {
            runner.keepCurrentInjectorsAfterClear();
            return this;
        }

        public UnaryByte operandIsRegister(int register) {
            runner.inject(new Register(register));
            return this;
        }

        public UnaryByte operandIsMemoryByteAt(int address) {
            runner.inject(new MemoryByte(address));
            return this;
        }

        public UnaryByte setPair(int registerPair, int value) {
            runner.inject((tmpRunner, argument) -> {
                tmpRunner.ensureProgramSize(value + 1);
                tmpRunner.setRegisterPair(registerPair, value);
            });
            return this;
        }

        public Test<Byte>.Unary run(int instruction) {
            return create(new InstructionNoOperands<>(instruction));
        }

        public Test<Byte>.Unary runWithOperand(int instruction) {
            return create(new InstructionByteOperand(instruction));
        }

        private Test<Byte>.Unary create(RunnerInjector<Byte> instruction) {
            if (verifiers.isEmpty()) {
                throw new IllegalStateException("At least one verifier must be set");
            }
            UnaryRunner<Byte> tmpRunner = runner.clone();
            tmpRunner.inject(instruction);
            runner.clearInjectors();
            return Test.<Byte>create(tmpRunner, verifiers);
        }
    }

    public static class UnaryInteger extends TestBuilder<Integer, UnaryInteger> {
        private final UnaryRunner<Integer> runner = new UnaryRunner<>(cpuRunner);

        public UnaryInteger(CpuRunner cpuRunner, CpuVerifier cpuVerifier) {
            super(cpuRunner, cpuVerifier);
        }

        public UnaryInteger operandIsPair(int registerPair) {
            runner.inject(
                    new MemoryExpand(),
                    new RegisterPair(registerPair)
            );
            return this;
        }

        public UnaryInteger keepCurrentInjectorsAfterRun() {
            runner.keepCurrentInjectorsAfterClear();
            return this;
        }

        public UnaryInteger operandIsMemoryAddressByte(int value) {
            runner.inject(new MemoryAddress(Byte.valueOf((byte)value)));
            return this;
        }

        public UnaryInteger operandIsMemoryAddressWord(int value) {
            runner.inject(new MemoryAddress(value));
            return this;
        }

        public UnaryInteger setRegister(int register, int value) {
            runner.inject((tmpRunner, argument) -> tmpRunner.setRegister(register, value));
            return this;
        }

        public UnaryInteger setPair(int registerPair, int value) {
            runner.inject(
                    (tmpRunner, argument) -> tmpRunner.ensureProgramSize(value + 2),
                    (tmpRunner, argument) -> tmpRunner.setRegisterPair(registerPair, value));
            return this;
        }

        public UnaryInteger setFlags(int flags) {
            runner.inject((tmpRunner, argument) -> tmpRunner.setFlags(flags));
            return this;
        }

        public UnaryInteger setSP(int SP) {
            runner.inject((tmpRunner, argument) -> tmpRunner.setSP(SP));
            return this;
        }

        public Test<Integer>.Unary run(int instruction) {
            return create(new InstructionNoOperands<Integer>(instruction));
        }

        public Test<Integer>.Unary runWithOperand(int instruction) {
            return create(new InstructionWordOperand(instruction));
        }

        public UnaryInteger verifyPC(Function<RunnerContext<Integer>, Integer> operation) {
            lastOperation = operation;
            verifiers.add(new PCVerifier(cpuVerifier, operation));
            return this;
        }

        private Test<Integer>.Unary create(RunnerInjector<Integer> instruction) {
            if (verifiers.isEmpty()) {
                throw new IllegalStateException("At least one verifier must be set");
            }
            UnaryRunner<Integer> tmpRunner = runner.clone();
            tmpRunner.inject(instruction);
            runner.clearInjectors();
            return Test.<Integer>create(tmpRunner, verifiers);
        }

    }

    public static class BinaryByte extends TestBuilder<Byte, BinaryByte> {
        private final BinaryRunner<Byte> runner = new BinaryRunner<Byte>(cpuRunner);

        public BinaryByte(CpuRunner cpuRunner, CpuVerifier cpuVerifier) {
            super(cpuRunner, cpuVerifier);
        }

        public BinaryByte keepCurrentInjectorsAfterRun() {
            runner.keepCurrentInjectorsAfterClear();
            return this;
        }

        public BinaryByte firstIsRegister(int register) {
            runner.injectFirst(new Register(register));
            return this;
        }

        public BinaryByte secondIsRegister(int register) {
            runner.injectSecond(new Register(register));
            return this;
        }

        public BinaryByte secondIsMemoryByteAt(int address) {
            runner.injectSecond(new MemoryByte(address));
            return this;
        }

        public BinaryByte setPair(int registerPair, int value) {
            runner.injectFirst(
                    (tmpRunner, argument) -> tmpRunner.ensureProgramSize(value + 1),
                    (tmpRunner, argument) -> tmpRunner.setRegisterPair(registerPair, value)
            );
            return this;
        }

        public Test<Byte>.Binary run(int instruction) {
            return create(new InstructionNoOperands<>(instruction), true);
        }

        public Test<Byte>.Binary runWithFirstOperand(int instruction) {
            return create(new InstructionByteOperand(instruction), true);
        }

        public Test<Byte>.Binary runWithSecondOperand(int instruction) {
            return create(new InstructionByteOperand(instruction), false);
        }

        private Test<Byte>.Binary create(RunnerInjector<Byte> instruction, boolean first) {
            if (runner == null || verifiers.isEmpty()) {
                throw new IllegalStateException("Runner and at least one verifier must be set");
            }
            BinaryRunner<Byte> tmpRunner = runner.clone();
            if (first) {
                tmpRunner.injectFirst(instruction);
            } else {
                tmpRunner.injectSecond(instruction);
            }
            runner.clearInjectors();
            return Test.<Byte>create(tmpRunner, verifiers);
        }
    }

    public static class BinaryInteger extends TestBuilder<Integer, BinaryInteger> {
        private final BinaryRunner<Integer> runner = new BinaryRunner<Integer>(cpuRunner);

        public BinaryInteger(CpuRunner cpuRunner, CpuVerifier cpuVerifier) {
            super(cpuRunner, cpuVerifier);
        }

        public BinaryInteger keepCurrentInjectorsAfterRun() {
            runner.keepCurrentInjectorsAfterClear();
            return this;
        }

        public BinaryInteger firstIsPair(int registerPair) {
            runner.injectFirst(new MemoryExpand(), new RegisterPair(registerPair));
            return this;
        }

        public BinaryInteger secondIsPair(int registerPair) {
            runner.injectSecond(new MemoryExpand(), new RegisterPair(registerPair));
            return this;
        }

        public BinaryInteger firstIsRegisterPairPSW(int registerPairPSW) {
            runner.injectFirst(new MemoryExpand(), new RegisterPairPSW(registerPairPSW));
            return this;
        }

        public BinaryInteger secondIsRegisterPairPSW(int registerPairPSW) {
            runner.injectSecond(new MemoryExpand(), new RegisterPairPSW(registerPairPSW));
            return this;
        }

        public BinaryInteger firstIsMemoryWordAt(int address) {
            runner.injectFirst(new MemoryWord(address));
            return this;
        }

        public BinaryInteger secondIsMemoryWordAt(int address) {
            runner.injectSecond(new MemoryWord(address));
            return this;
        }

        public BinaryInteger firstIsAddressAndSecondIsMemoryWord() {
            runner.injectBoth(new AddressAndMemoryWord());
            return this;
        }

        public BinaryInteger setPair(int registerPair, int value) {
            runner.injectFirst((tmpRunner, argument) -> tmpRunner.setRegisterPair(registerPair, value));
            return this;
        }

        public BinaryInteger setFlags(int flags) {
            runner.injectFirst((tmpRunner, argument) -> tmpRunner.setFlags(flags));
            return this;
        }

        public Test<Integer>.Binary run(int instruction) {
            return create(new InstructionNoOperands<>(instruction), true);
        }

        public Test<Integer>.Binary runWithFirstOperand(int instruction) {
            return create(new InstructionWordOperand(instruction), true);
        }

        public Test<Integer>.Binary runWithSecondOperand(int instruction) {
            return create(new InstructionWordOperand(instruction), false);
        }

        public BinaryInteger verifyPairAndPSW(int registerPair, Function<RunnerContext<Integer>, Integer> operation) {
            lastOperation = operation;
            verifiers.add(new RegisterPair_PSW_Verifier(cpuVerifier, operation, registerPair));
            return this;
        }

        public BinaryInteger verifyPC(Function<RunnerContext<Integer>, Integer> operation) {
            lastOperation = operation;
            verifiers.add(new PCVerifier(cpuVerifier, operation));
            return this;
        }

        private Test<Integer>.Binary create(RunnerInjector<Integer> instruction, boolean first) {
            if (runner == null || verifiers.isEmpty()) {
                throw new IllegalStateException("Runner and at least one verifier must be set");
            }
            BinaryRunner<Integer> tmpRunner = runner.clone();
            if (first) {
                tmpRunner.injectFirst(instruction);
            } else {
                tmpRunner.injectSecond(instruction);
            }
            runner.clearInjectors();
            return Test.<Integer>create(tmpRunner, verifiers);
        }

    }
}
