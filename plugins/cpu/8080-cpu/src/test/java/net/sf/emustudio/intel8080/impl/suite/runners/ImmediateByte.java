package net.sf.emustudio.intel8080.impl.suite.runners;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;

import java.util.Objects;
import java.util.function.Function;

public class ImmediateByte implements Function<Byte, RunnerContext<Byte>> {
    private final CpuRunner runner;
    private final int instruction;

    public ImmediateByte(CpuRunner runner, int instruction) {
        this.runner = Objects.requireNonNull(runner);
        this.instruction = instruction;
    }

    @Override
    public RunnerContext<Byte> apply(Byte operand) {
        runner.resetProgram(instruction, operand);

        int PC = runner.getPC();
        int SP = runner.getSP();

        runner.step();
        return new RunnerContext<>(operand, (byte)0, 0, 0, PC, SP);
    }

}