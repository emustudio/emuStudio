package net.sf.emustudio.intel8080.impl.suite.runners;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;

import java.util.function.Function;

public class RegisterPair implements Function<Integer, RunnerContext<Integer>> {
    private final CpuRunner runner;
    private final int instruction;
    private final int registerPair;
    private int flagsBefore;

    public RegisterPair(CpuRunner runner, int instruction, int registerPair) {
        this.runner = runner;
        this.instruction = instruction;
        this.registerPair = registerPair;
    }

    @Override
    public RunnerContext<Integer> apply(Integer operand) {
        runner.resetProgram(instruction);
        runner.setRegisterPair(registerPair, operand);
        runner.setFlags(flagsBefore);

        runner.step();

        try {
            return new RunnerContext<>(operand, 0, runner.getRegister(registerPair), flagsBefore);
        } finally {
            flagsBefore = runner.getFlags();
        }
    }

}
