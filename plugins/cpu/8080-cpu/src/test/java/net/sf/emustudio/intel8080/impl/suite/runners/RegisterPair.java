package net.sf.emustudio.intel8080.impl.suite.runners;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;

import java.util.Objects;
import java.util.function.Function;

public class RegisterPair implements Function<Integer, RunnerContext<Integer>> {
    private final CpuRunner runner;
    private final int instruction;
    private final int registerPair;
    private int flagsBefore;

    public RegisterPair(CpuRunner runner, int instruction, int registerPair) {
        this.runner = Objects.requireNonNull(runner);
        this.instruction = instruction;
        this.registerPair = registerPair;
    }

    @Override
    public RunnerContext<Integer> apply(Integer operand) {
        int[] program = new int[1 + operand];
        program[0] = instruction;
        runner.resetProgram(program);
        runner.setRegisterPair(registerPair, operand);
        runner.setFlags(flagsBefore);

        int SPbefore = runner.getSP();
        int PCbefore = runner.getPC();
        runner.step();

        try {
            return new RunnerContext<>(operand, 0, runner.getRegister(registerPair), flagsBefore, PCbefore, SPbefore);
        } finally {
            flagsBefore = runner.getFlags();
        }
    }

}
