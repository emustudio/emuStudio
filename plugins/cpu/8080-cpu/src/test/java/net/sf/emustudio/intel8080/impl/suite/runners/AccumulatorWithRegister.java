package net.sf.emustudio.intel8080.impl.suite.runners;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;

import java.util.Objects;
import java.util.function.BiFunction;

public class AccumulatorWithRegister implements BiFunction<Byte, Byte, RunnerContext<Byte>> {
    private final CpuRunner runner;
    private final int register;
    private final int instruction;
    private int flagsBefore;

    public AccumulatorWithRegister(CpuRunner runner, int register, int instruction) {
        this.runner = Objects.requireNonNull(runner);
        this.register = register;
        this.instruction = instruction;
    }

    @Override
    public RunnerContext<Byte> apply(Byte first, Byte second) {
        runner.resetProgram(instruction);
        runner.setAccumulator(first);
        runner.setRegister(register, second);
        runner.setFlags(flagsBefore);

        runner.step();

        try {
            return new RunnerContext<>(first, second, runner.getAccumulator(), flagsBefore);
        } finally {
            flagsBefore = runner.getFlags();
        }
    }

}
