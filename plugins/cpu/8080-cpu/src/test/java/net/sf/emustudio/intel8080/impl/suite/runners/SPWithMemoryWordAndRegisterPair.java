package net.sf.emustudio.intel8080.impl.suite.runners;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;

import java.util.Objects;
import java.util.function.BiFunction;

public class SPWithMemoryWordAndRegisterPair implements BiFunction<Integer, Integer, RunnerContext<Integer>> {
    private final CpuRunner runner;
    private final int instruction;
    private final int address;
    private final int registerPair;

    public SPWithMemoryWordAndRegisterPair(CpuRunner runner, int instruction, int registerPair, int address) {
        this.address = address;
        this.runner = Objects.requireNonNull(runner);
        this.instruction = instruction;
        this.registerPair = registerPair;
    }

    @Override
    public RunnerContext<Integer> apply(Integer first, Integer second) {
        int[] program = new int[address + 2];
        program[0] = instruction;
        program[address] = first & 0xFF;
        program[address + 1] = (first >>> 8) & 0xFF;

        runner.resetProgram(program);
        runner.setRegisterPair(3, address);
        runner.setRegisterPair(registerPair, second);

        runner.step();

        return new RunnerContext<>(first, second, 0, this.address);
    }

}