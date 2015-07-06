package net.sf.emustudio.intel8080.impl.suite.runners;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;

import java.util.Objects;
import java.util.function.Function;

public class RegisterPairWithMemory implements Function<Integer, RunnerContext<Integer>> {
    private final CpuRunner runner;
    private final int instruction;
    private final int registerPair;
    private final int value;

    public RegisterPairWithMemory(CpuRunner runner, int instruction, int registerPair, int value) {
        this.runner = Objects.requireNonNull(runner);
        this.instruction = instruction;
        this.registerPair = registerPair;
        this.value = value;
    }

    @Override
    public RunnerContext<Integer> apply(Integer address) {
        int[] program = new int[1 + address];
        program[0] = instruction;
        program[address] = value;
        runner.resetProgram(program);
        runner.setRegisterPair(registerPair, address);

        int SPbefore = runner.getSP();
        int PCbefore = runner.getPC();
        runner.step();

        return new RunnerContext<>(address, 0, runner.getAccumulator(), 0, PCbefore, SPbefore);
    }

}
