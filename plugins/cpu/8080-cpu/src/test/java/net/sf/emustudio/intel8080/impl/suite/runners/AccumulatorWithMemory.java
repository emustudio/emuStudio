package net.sf.emustudio.intel8080.impl.suite.runners;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;

import java.util.function.BiFunction;

import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_H;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_L;

public class AccumulatorWithMemory implements BiFunction<Byte, Byte, RunnerContext<Byte>> {
    private final CpuRunner runner;
    private final int instruction;
    private final int address;
    private int flagsBefore;

    public AccumulatorWithMemory(CpuRunner runner, int instruction, int address) {
        this.runner = runner;
        this.instruction = instruction;
        this.address = address;
    }

    @Override
    public RunnerContext<Byte> apply(Byte first, Byte second) {
        int[] program = new int[address + 2];
        program[0] = instruction;
        program[address] = second & 0xFF;

        runner.resetProgram(program);
        runner.setRegister(REG_H, (address >>> 8) & 0xFF);
        runner.setRegister(REG_L, address & 0xFF);
        runner.setAccumulator(first);
        runner.setFlags(flagsBefore);

        runner.step();

        try {
            return new RunnerContext<>(first, second, runner.getAccumulator(), flagsBefore);
        } finally {
            flagsBefore = runner.getFlags();
        }
    }

}