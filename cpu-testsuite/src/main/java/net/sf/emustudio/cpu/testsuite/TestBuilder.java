package net.sf.emustudio.cpu.testsuite;

import net.sf.emustudio.cpu.testsuite.injectors.AddressAndMemoryWord;
import net.sf.emustudio.cpu.testsuite.injectors.InstructionNoOperands;
import net.sf.emustudio.cpu.testsuite.injectors.InstructionOperand;
import net.sf.emustudio.cpu.testsuite.injectors.MemoryAddress;
import net.sf.emustudio.cpu.testsuite.injectors.MemoryByte;
import net.sf.emustudio.cpu.testsuite.injectors.MemoryWord;
import net.sf.emustudio.cpu.testsuite.runners.Runner;
import net.sf.emustudio.cpu.testsuite.runners.RunnerContext;
import net.sf.emustudio.cpu.testsuite.runners.RunnerInjector;
import net.sf.emustudio.cpu.testsuite.verifiers.FlagsVerifier;
import net.sf.emustudio.cpu.testsuite.verifiers.MemoryByteVerifier;
import net.sf.emustudio.cpu.testsuite.verifiers.MemoryWordVerifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class TestBuilder<
        K extends Number, SpecificTestBuilder extends TestBuilder,
        CpuRunnerType extends CpuRunner, CpuVerifierType extends CpuVerifier> {
    protected final CpuRunnerType cpuRunner;
    protected final CpuVerifierType cpuVerifier;
    protected final List<Consumer<RunnerContext<K>>> verifiers = new ArrayList<Consumer<RunnerContext<K>>>();
    protected Function<RunnerContext<K>, Integer> lastOperation;
    protected final Runner runner;

    protected TestBuilder(CpuRunnerType cpuRunner, CpuVerifierType cpuVerifier) {
        this.cpuRunner = Objects.requireNonNull(cpuRunner);
        this.cpuVerifier = Objects.requireNonNull(cpuVerifier);
        this.runner = new Runner<K, CpuRunnerType>(cpuRunner);
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
        verifiers.add(new MemoryByteVerifier<K>(cpuVerifier, lastOperation, addressOperator));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder keepCurrentInjectorsAfterRun() {
        runner.keepCurrentInjectorsAfterClear();
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder firstIsMemoryByteAt(int address) {
        runner.injectFirst(new MemoryByte(address));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder secondIsMemoryByteAt(int address) {
        runner.injectSecond(new MemoryByte(address));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder firstIsMemoryWordAt(int address) {
        runner.injectFirst(new MemoryWord(address));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder secondIsMemoryWordAt(int address) {
        runner.injectSecond(new MemoryWord(address));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder firstIsMemoryAddressByte(int value) {
        runner.injectFirst(new MemoryAddress(Byte.valueOf((byte) value)));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder secondIsMemoryAddressByte(int value) {
        runner.injectSecond(new MemoryAddress(Byte.valueOf((byte) value)));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder firstIsMemoryAddressWord(int value) {
        runner.injectFirst(new MemoryAddress(value));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder secondIsMemoryAddressWord(int value) {
        runner.injectSecond(new MemoryAddress(value));
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder firstIsAddressAndSecondIsMemoryWord() {
        runner.injectBoth(new AddressAndMemoryWord());
        return (SpecificTestBuilder)this;
    }

    public SpecificTestBuilder setFlags(int flags) {
        runner.injectFirst((tmpRunner, argument) -> tmpRunner.setFlags(flags));
        return (SpecificTestBuilder)this;
    }

    public Test<K> run(int instruction) {
        return create(new InstructionNoOperands<>(instruction), true);
    }

    public Test<K> runWithFirstOperand(int instruction) {
        return create(new InstructionOperand<K, CpuRunnerType>(instruction), true);
    }

    public Test<K> runWithSecondOperand(int instruction) {
        return create(new InstructionOperand<K, CpuRunnerType>(instruction), false);
    }

    private Test<K> create(RunnerInjector<K, CpuRunnerType> instruction, boolean first) {
        if (verifiers.isEmpty()) {
            throw new IllegalStateException("At least one verifier must be set");
        }
        Runner<K, CpuRunnerType> tmpRunner = runner.clone();
        if (first) {
            tmpRunner.injectFirst(instruction);
        } else {
            tmpRunner.injectSecond(instruction);
        }
        runner.clearInjectors();
        return new Test<K>(tmpRunner, verifiers);
    }
}
