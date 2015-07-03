package net.sf.emustudio.intel8080.impl.suite.runners;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;

import java.util.Objects;
import java.util.function.BiFunction;

public class SPWithMemoryAndFlags implements BiFunction<Integer, Integer, RunnerContext<Integer>> {
    private final CpuRunner runner;
    private final int instruction;
    private final int flags;

    public SPWithMemoryAndFlags(CpuRunner runner, int instruction, int flags) {
        this.flags = flags;
        this.runner = Objects.requireNonNull(runner);
        this.instruction = instruction;
    }

    @Override
    public RunnerContext<Integer> apply(Integer address, Integer second) {
        int[] program = new int[address + 2];
        program[0] = instruction;
        program[address] = second & 0xFF;
        program[address+1] = (second >>> 8) & 0xFF;

        runner.resetProgram(program);
        runner.setRegisterPair(3, address);
        runner.setFlags(flags);

        runner.step();

        return new RunnerContext<>(address, second, 0, flags);
    }

}