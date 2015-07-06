package net.sf.emustudio.intel8080.impl.suite.runners;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;

import java.util.Objects;
import java.util.function.BiFunction;

public class AccumulatorWithByte implements BiFunction<Byte, Byte, RunnerContext<Byte>> {
    private final CpuRunner runner;
    private final int instruction;
    private int flagsBefore;

    public AccumulatorWithByte(CpuRunner runner, int instruction) {
        this.runner = Objects.requireNonNull(runner);
        this.instruction = instruction;
    }

    @Override
    public RunnerContext<Byte> apply(Byte first, Byte second) {
        runner.resetProgram(instruction, second & 0xFF);
        runner.setAccumulator(first);
        runner.setFlags(flagsBefore);

        runner.step();

        try {
            return new RunnerContext<>(first, second, runner.getAccumulator(), flagsBefore);
        } finally {
            flagsBefore = runner.getFlags();
        }
    }

}