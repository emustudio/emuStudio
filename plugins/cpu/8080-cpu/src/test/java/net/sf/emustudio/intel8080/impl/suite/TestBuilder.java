package net.sf.emustudio.intel8080.impl.suite;

import net.sf.emustudio.intel8080.impl.suite.runners.AccumulatorWithByte;
import net.sf.emustudio.intel8080.impl.suite.runners.AccumulatorWithMemory;
import net.sf.emustudio.intel8080.impl.suite.runners.AccumulatorWithRegister;
import net.sf.emustudio.intel8080.impl.suite.runners.HLWithRegisterPair;
import net.sf.emustudio.intel8080.impl.suite.runners.Memory;
import net.sf.emustudio.intel8080.impl.suite.runners.Register;
import net.sf.emustudio.intel8080.impl.suite.runners.RegisterPair;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;
import net.sf.emustudio.intel8080.impl.suite.verifiers.MemoryAndFlagsVerifier;
import net.sf.emustudio.intel8080.impl.suite.verifiers.RegisterAndFlagsVerifier;
import net.sf.emustudio.intel8080.impl.suite.verifiers.RegisterPairAndFlagsVerifier;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class TestBuilder<K, SpecificTestBuilder extends TestBuilder, TestType> {
    protected final CpuRunner cpuRunner;
    protected final CpuVerifier cpuVerifier;
    protected FlagsBuilder flagsBuilder = new FlagsBuilder();
    protected Consumer<RunnerContext<K>> verifier;

    private TestBuilder(CpuRunner cpuRunner, CpuVerifier cpuVerifier) {
        this.cpuRunner = Objects.requireNonNull(cpuRunner);
        this.cpuVerifier = Objects.requireNonNull(cpuVerifier);
    }

    public SpecificTestBuilder checkFlags(FlagsBuilder flagsBuilder) {
        this.flagsBuilder = flagsBuilder;
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder verifyM(int address, Function<RunnerContext<K>, Integer> operator) {
        verifier = new MemoryAndFlagsVerifier(cpuVerifier, operator, flagsBuilder, address);
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder verifyR(int register, Function<RunnerContext<K>, Integer> operator) {
        verifier = new RegisterAndFlagsVerifier(cpuVerifier, operator, flagsBuilder, register);
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder verifyR(int register, Function<RunnerContext<K>, Integer> operator,
                                       Function<RunnerContext<K>, Integer> flagsResult) {
        RegisterAndFlagsVerifier tmp = new RegisterAndFlagsVerifier(cpuVerifier, operator, flagsBuilder, register);
        tmp.setFlagResultOp(flagsResult);
        verifier = tmp;
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder verifyPair(int registerPair, Function<RunnerContext<K>, Integer> operator) {
        verifier = new RegisterPairAndFlagsVerifier(cpuVerifier, operator, registerPair, flagsBuilder);
        return (SpecificTestBuilder)this;
    }

    public static class UnaryByte extends TestBuilder<Byte, UnaryByte, Test<Byte>.Unary> {
        protected Function<Byte, RunnerContext<Byte>> runner;

        public UnaryByte(CpuRunner cpuRunner, CpuVerifier cpuVerifier) {
            super(cpuRunner, cpuVerifier);
        }

        public Test<Byte>.Unary run(int instruction, int register) {
            runner = new Register(cpuRunner, instruction, register);
            return create();
        }

        public Test<Byte>.Unary runM(int instruction, int address) {
            runner = new Memory(cpuRunner, instruction, address);
            return create();
        }

        private Test<Byte>.Unary create() {
            if (runner == null || verifier == null) {
                throw new IllegalStateException("Runner and at least one verifier must be set");
            }
            return Test.<Byte>create(runner, verifier);
        }
    }

    public static class UnaryInteger extends TestBuilder<Integer, UnaryInteger, Test<Integer>.Unary> {
        protected Function<Integer, RunnerContext<Integer>> runner;

        public UnaryInteger(CpuRunner cpuRunner, CpuVerifier cpuVerifier) {
            super(cpuRunner, cpuVerifier);
        }

        public Test<Integer>.Unary runPair(int instruction, int registerPair) {
            runner = new RegisterPair(cpuRunner, instruction, registerPair);
            return create();
        }

        private Test<Integer>.Unary create() {
            if (runner == null || verifier == null) {
                throw new IllegalStateException("Runner and at least one verifier must be set");
            }
            return Test.<Integer>create(runner, verifier);
        }

    }

    public static class BinaryByte extends TestBuilder<Byte, BinaryByte, Test<Byte>.Binary> {
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
            if (runner == null || verifier == null) {
                throw new IllegalStateException("Runner and at least one verifier must be set");
            }
            return Test.<Byte>create(runner, verifier);
        }
    }

    public static class BinaryInteger extends TestBuilder<Integer, BinaryInteger, Test<Integer>.Binary> {
        protected BiFunction<Integer, Integer, RunnerContext<Integer>> runner;

        public BinaryInteger(CpuRunner cpuRunner, CpuVerifier cpuVerifier) {
            super(cpuRunner, cpuVerifier);
        }

        public Test<Integer>.Binary runHLWithPair(int instruction, int registerPair) {
            runner = new HLWithRegisterPair(cpuRunner, instruction, registerPair);
            return create();
        }

        private Test<Integer>.Binary create() {
            if (runner == null || verifier == null) {
                throw new IllegalStateException("Runner and at least one verifier must be set");
            }
            return Test.<Integer>create(runner, verifier);
        }

    }
}
