package net.sf.emustudio.intel8080.impl.suite;

import net.sf.emustudio.intel8080.impl.suite.runners.AccumulatorWithByte;
import net.sf.emustudio.intel8080.impl.suite.runners.AccumulatorWithMemory;
import net.sf.emustudio.intel8080.impl.suite.runners.AccumulatorWithRegister;
import net.sf.emustudio.intel8080.impl.suite.runners.HLWithOperand;
import net.sf.emustudio.intel8080.impl.suite.runners.HLWithMemoryByte;
import net.sf.emustudio.intel8080.impl.suite.runners.HLWithRegister;
import net.sf.emustudio.intel8080.impl.suite.runners.HLWithRegisterPair;
import net.sf.emustudio.intel8080.impl.suite.runners.ImmediateByte;
import net.sf.emustudio.intel8080.impl.suite.runners.ImmediateWordWithFlags;
import net.sf.emustudio.intel8080.impl.suite.runners.ImmediateWordWithMemoryByte;
import net.sf.emustudio.intel8080.impl.suite.runners.ImmediateWordWithMemoryWord;
import net.sf.emustudio.intel8080.impl.suite.runners.ImmediateWordWithRegister;
import net.sf.emustudio.intel8080.impl.suite.runners.ImmediateWordWithRegisterPair;
import net.sf.emustudio.intel8080.impl.suite.runners.ImmediateWordWithSPAndFlags;
import net.sf.emustudio.intel8080.impl.suite.runners.Register;
import net.sf.emustudio.intel8080.impl.suite.runners.RegisterPair;
import net.sf.emustudio.intel8080.impl.suite.runners.RegisterPairWithMemory;
import net.sf.emustudio.intel8080.impl.suite.runners.RegisterPairWithRegister;
import net.sf.emustudio.intel8080.impl.suite.runners.SPWithMemoryAndFlags;
import net.sf.emustudio.intel8080.impl.suite.runners.SPWithMemoryWordAndRegisterPair;
import net.sf.emustudio.intel8080.impl.suite.runners.SPWithRegisterPairAndPSW;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;
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
import java.util.function.BiFunction;
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
        return verifyFlags(flagsBuilder);
    }

    public SpecificTestBuilder verifyFlags(FlagsBuilder flagsBuilder) {
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
        protected Function<Byte, RunnerContext<Byte>> runner;

        public UnaryByte(CpuRunner cpuRunner, CpuVerifier cpuVerifier) {
            super(cpuRunner, cpuVerifier);
        }

        public Test<Byte>.Unary run(int instruction, int register) {
            runner = new Register(cpuRunner, instruction, register);
            return create();
        }

        public Test<Byte>.Unary runM(int instruction, int address) {
            runner = new HLWithMemoryByte(cpuRunner, instruction, address);
            return create();
        }

        public Test<Byte>.Unary runHL(int instruction, int address) {
            runner = new HLWithOperand(cpuRunner, instruction, address);
            return create();
        }

        public Test<Byte>.Unary runHL(int instruction, int address, int register) {
            runner = new HLWithRegister(cpuRunner, instruction, address, register);
            return create();
        }

        public Test<Byte>.Unary runB(int instruction) {
            runner = new ImmediateByte(cpuRunner, instruction);
            return create();
        }

        private Test<Byte>.Unary create() {
            if (runner == null || verifiers.isEmpty()) {
                throw new IllegalStateException("Runner and at least one verifier must be set");
            }
            return Test.<Byte>create(runner, verifiers);
        }
    }

    public static class UnaryInteger extends TestBuilder<Integer, UnaryInteger> {
        protected Function<Integer, RunnerContext<Integer>> runner;

        public UnaryInteger(CpuRunner cpuRunner, CpuVerifier cpuVerifier) {
            super(cpuRunner, cpuVerifier);
        }

        public Test<Integer>.Unary runPair(int instruction, int registerPair) {
            runner = new RegisterPair(cpuRunner, instruction, registerPair);
            return create();
        }

        public Test<Integer>.Unary runPair(int instruction, int registerPair, int value) {
            runner = new RegisterPairWithMemory(cpuRunner, instruction, registerPair, value);
            return create();
        }

        public Test<Integer>.Unary runPair(int instruction, int registerPair, int register, int value) {
            runner = new RegisterPairWithRegister(cpuRunner, instruction, registerPair, register, value);
            return create();
        }

        public Test<Integer>.Unary runB(int instruction) {
            runner = new ImmediateWordWithFlags(cpuRunner, instruction, 0);
            return create();
        }

        public Test<Integer>.Unary runB(int instruction, int flags) {
            runner = new ImmediateWordWithFlags(cpuRunner, instruction, flags);
            return create();
        }

        public Test<Integer>.Unary runB(int instruction, int flags, int SP) {
            runner = new ImmediateWordWithSPAndFlags(cpuRunner, instruction, SP, flags);
            return create();
        }

        public Test<Integer>.Unary runB(int instruction, byte value) {
            runner = new ImmediateWordWithMemoryByte(cpuRunner, instruction, value);
            return create();
        }

        public Test<Integer>.Unary runBword(int instruction, int value) {
            runner = new ImmediateWordWithMemoryWord(cpuRunner, instruction, value);
            return create();
        }

        public Test<Integer>.Unary runBPair(int instruction, int registerPair, int value) {
            runner = new ImmediateWordWithRegisterPair(cpuRunner, instruction, registerPair, value);
            return create();
        }

        public Test<Integer>.Unary runB(int instruction, int register, byte value) {
            runner = new ImmediateWordWithRegister(cpuRunner, instruction, register, value);
            return create();
        }


        public UnaryInteger verifyPC(Function<RunnerContext<Integer>, Integer> operation) {
            lastOperation = operation;
            verifiers.add(new PCVerifier(cpuVerifier, operation));
            return this;
        }

        private Test<Integer>.Unary create() {
            if (runner == null || verifiers.isEmpty()) {
                throw new IllegalStateException("Runner and at least one verifier must be set");
            }
            return Test.<Integer>create(runner, verifiers);
        }

    }

    public static class BinaryByte extends TestBuilder<Byte, BinaryByte> {
        protected BiFunction<Byte, Byte, RunnerContext<Byte>> runner;

        public BinaryByte(CpuRunner cpuRunner, CpuVerifier cpuVerifier) {
            super(cpuRunner, cpuVerifier);
        }

        public Test<Byte>.Binary run(int instruction, int register) {
            runner = new AccumulatorWithRegister(cpuRunner, register, instruction);
            return create();
        }

        public Test<Byte>.Binary runM(int instruction, int address) {
            runner = new AccumulatorWithMemory(cpuRunner, instruction, address);
            return create();
        }

        public Test<Byte>.Binary runB(int instruction) {
            runner = new AccumulatorWithByte(cpuRunner, instruction);
            return create();
        }

        private Test<Byte>.Binary create() {
            if (runner == null || verifiers.isEmpty()) {
                throw new IllegalStateException("Runner and at least one verifier must be set");
            }
            return Test.<Byte>create(runner, verifiers);
        }
    }

    public static class BinaryInteger extends TestBuilder<Integer, BinaryInteger> {
        protected BiFunction<Integer, Integer, RunnerContext<Integer>> runner;

        public BinaryInteger(CpuRunner cpuRunner, CpuVerifier cpuVerifier) {
            super(cpuRunner, cpuVerifier);
        }

        public Test<Integer>.Binary runHLWithPair(int instruction, int registerPair) {
            runner = new HLWithRegisterPair(cpuRunner, instruction, registerPair);
            return create();
        }

        public Test<Integer>.Binary runSPWithPairAndPSW(int instruction, int registerPair) {
            runner = new SPWithRegisterPairAndPSW(cpuRunner, instruction, registerPair);
            return create();
        }

        public Test<Integer>.Binary runSPWithMemoryWordAndPair(int instruction, int registerPair, int address) {
            runner = new SPWithMemoryWordAndRegisterPair(cpuRunner, instruction, registerPair, address);
            return create();
        }

        public Test<Integer>.Binary runM(int instruction) {
            runner = new SPWithMemoryAndFlags(cpuRunner, instruction, 0);
            return create();
        }

        public Test<Integer>.Binary runM(int instruction, int flags) {
            runner = new SPWithMemoryAndFlags(cpuRunner, instruction, flags);
            return create();
        }

        public BinaryInteger verifyPandPSW(Function<RunnerContext<Integer>, Integer> operation, int registerPair) {
            lastOperation = operation;
            verifiers.add(new RegisterPair_PSW_Verifier(cpuVerifier, operation, registerPair));
            return this;
        }

        public BinaryInteger verifyPC(Function<RunnerContext<Integer>, Integer> operation) {
            lastOperation = operation;
            verifiers.add(new PCVerifier(cpuVerifier, operation));
            return this;
        }

        private Test<Integer>.Binary create() {
            if (runner == null || verifiers.isEmpty()) {
                throw new IllegalStateException("Runner and at least one verifier must be set");
            }
            return Test.<Integer>create(runner, verifiers);
        }

    }
}
