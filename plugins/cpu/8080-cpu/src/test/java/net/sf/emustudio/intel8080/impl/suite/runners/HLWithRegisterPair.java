package net.sf.emustudio.intel8080.impl.suite.runners;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;

import java.util.function.BiFunction;

public class HLWithRegisterPair implements BiFunction<Integer, Integer, RunnerContext<Integer>>  {
    public static final int REG_PAIR_HL = 2;

    private final CpuRunner runner;
    private final int instruction;
    private final int registerPair;
    private int flagsBefore;

    public HLWithRegisterPair(CpuRunner runner, int instruction, int registerPair) {
        this.runner = runner;
        this.instruction = instruction;
        this.registerPair = registerPair;
    }

    @Override
    public RunnerContext<Integer> apply(Integer first, Integer second) {
        runner.resetProgram(instruction);
        runner.setRegisterPair(REG_PAIR_HL, first);
        runner.setRegisterPair(registerPair, second);
        runner.setFlags(flagsBefore);

        runner.step();

        try {
            return new RunnerContext<>(first, second, runner.getRegister(REG_PAIR_HL), flagsBefore);
        } finally {
            flagsBefore = runner.getFlags();
        }
    }

}