package net.sf.emustudio.intel8080.impl.suite.runners;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;

import java.util.function.Function;

public class Register implements Function<Byte, RunnerContext<Byte>> {
    private final CpuRunner runner;
    private final int instruction;
    private final int register;
    private int flagsBefore;

    public Register(CpuRunner runner, int instruction, int register) {
        this.runner = runner;
        this.instruction = instruction;
        this.register = register;
    }

    @Override
    public RunnerContext<Byte> apply(Byte operand) {
        runner.resetProgram(instruction);
        runner.setRegister(register, operand);
        runner.setFlags(flagsBefore);

        runner.step();

        try {
            return new RunnerContext<>(operand, (byte)0, runner.getRegister(register), flagsBefore);
        } finally {
            flagsBefore = runner.getFlags();
        }
    }

}
