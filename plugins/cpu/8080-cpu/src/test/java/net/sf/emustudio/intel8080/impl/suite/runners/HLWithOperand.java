package net.sf.emustudio.intel8080.impl.suite.runners;

import net.sf.emustudio.intel8080.impl.suite.CpuRunner;

import java.util.Objects;
import java.util.function.Function;

public class HLWithOperand implements Function<Byte, RunnerContext<Byte>> {
    public static final int REG_PAIR_HL = 2;
    private final CpuRunner runner;
    private final int instruction;
    private final int address;
    private int flagsBefore;

    public HLWithOperand(CpuRunner runner, int instruction, int address) {
        this.runner = Objects.requireNonNull(runner);
        this.instruction = instruction;
        this.address = address;

        if (address <= 0) {
            throw new IllegalArgumentException("Address can be only > 0!");
        }
    }

    @Override
    public RunnerContext<Byte> apply(Byte first) {
        int[] program = new int[address + 1];
        program[0] = instruction;
        program[1] = first & 0xFF;
        runner.resetProgram(program);
        runner.setRegisterPair(REG_PAIR_HL, address);
        runner.setFlags(flagsBefore);

        runner.step();

        try {
            return new RunnerContext<>(first, (byte)0, runner.readByte(address), flagsBefore);
        } finally {
            flagsBefore = runner.getFlags();
        }
    }

}