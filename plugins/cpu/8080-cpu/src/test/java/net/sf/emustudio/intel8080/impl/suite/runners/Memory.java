package net.sf.emustudio.intel8080.impl.suite.runners;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;

import java.util.function.Function;

import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_H;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_L;

public class Memory implements Function<Byte, RunnerContext<Byte>> {
    private final CpuRunner runner;
    private final int instruction;
    private final int address;
    private int flagsBefore;

    public Memory(CpuRunner runner, int instruction, int address) {
        this.runner = runner;
        this.instruction = instruction;
        this.address = address;

        if (address <= 0) {
            throw new IllegalArgumentException("Address can be only > 0!");
        }
    }

    @Override
    public RunnerContext<Byte> apply(Byte first) {
        int[] program = new int[address + 2];
        program[0] = instruction;
        program[address] = first & 0xFF;

        runner.resetProgram(program);
        runner.setRegister(REG_H, (address >>> 8) & 0xFF);
        runner.setRegister(REG_L, address & 0xFF);
        runner.setFlags(flagsBefore);

        runner.step();

        try {
            return new RunnerContext<>(first, (byte)0, runner.readByte(1), flagsBefore);
        } finally {
            flagsBefore = runner.getFlags();
        }
    }

}