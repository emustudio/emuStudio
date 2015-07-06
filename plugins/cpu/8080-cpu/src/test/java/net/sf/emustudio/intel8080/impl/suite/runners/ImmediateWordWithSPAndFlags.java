package net.sf.emustudio.intel8080.impl.suite.runners;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;

import java.util.Objects;
import java.util.function.Function;

public class ImmediateWordWithSPAndFlags implements Function<Integer, RunnerContext<Integer>> {
    private final CpuRunner runner;
    private final int instruction;
    private final int flags;
    private final int SP;

    public ImmediateWordWithSPAndFlags(CpuRunner runner, int instruction, int SP, int flags) {
        this.runner = Objects.requireNonNull(runner);
        this.instruction = instruction;
        this.SP = SP;
        this.flags = flags;
    }

    @Override
    public RunnerContext<Integer> apply(Integer first) {
        int[] program = new int[first + 2];
        program[0] = instruction;
        program[1] = first & 0xFF;
        program[2] = (first >>> 8) & 0xFF;
        runner.resetProgram(program);
        runner.setFlags(flags);
        runner.setSP(SP);

        int PC = runner.getPC();
        int SP = runner.getSP();

        runner.step();
        return new RunnerContext<>(first, 0, 0, flags, PC, SP);
    }

}