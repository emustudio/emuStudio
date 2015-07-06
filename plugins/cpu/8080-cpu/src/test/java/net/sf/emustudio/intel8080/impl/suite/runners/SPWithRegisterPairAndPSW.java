package net.sf.emustudio.intel8080.impl.suite.runners;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;

import java.util.Objects;
import java.util.function.BiFunction;

public class SPWithRegisterPairAndPSW implements BiFunction<Integer, Integer, RunnerContext<Integer>> {
    private final CpuRunner runner;
    private final int instruction;
    private final int registerPair;
    private int flagsBefore = -1;

    public SPWithRegisterPairAndPSW(CpuRunner runner, int instruction, int registerPair) {
        this.runner = Objects.requireNonNull(runner);
        this.instruction = instruction;
        this.registerPair = registerPair;
    }

    @Override
    public RunnerContext<Integer> apply(Integer first, Integer second) {
        int[] program = new int[first + 1];
        program[0] = instruction;
        runner.resetProgram(program);
        runner.setRegisterPairPSW(registerPair, second);
        runner.setRegisterPair(3, first);

        if (flagsBefore == -1) {
            flagsBefore = runner.getFlags();
        }
        runner.step();

        try {
            return new RunnerContext<>(first, second, 0, flagsBefore);
        } finally {
            flagsBefore = runner.getFlags();
        }
    }

}