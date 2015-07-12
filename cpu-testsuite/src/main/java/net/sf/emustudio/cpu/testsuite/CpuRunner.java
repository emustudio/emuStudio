package net.sf.emustudio.cpu.testsuite;

import emulib.plugins.cpu.CPU;
import emulib.plugins.memory.MemoryContext;

import java.util.Arrays;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public abstract class CpuRunner<CpuType extends CPU> {
    private final RunStateListenerStub runStateListener = new RunStateListenerStub();
    protected final CpuType cpu;
    protected final MemoryStub memoryStub;

    private short[] program = new short[1];
    private CPU.RunState expectedRunState = CPU.RunState.STATE_STOPPED_BREAK;

    public CpuRunner(CpuType cpu, MemoryStub memoryStub) {
        this.cpu = Objects.requireNonNull(cpu);
        this.memoryStub = Objects.requireNonNull(memoryStub);
        cpu.addCPUListener(runStateListener);
    }

    public void ensureProgramSize(int length) {
        if (program.length < length) {
            this.program = Arrays.copyOf(this.program, length);
            resetProgram();
        }
    }

    public void setProgram(int... program) {
        ensureProgramSize(program.length);
        for (int i = 0; i < program.length; i++) {
            this.program[i] = (short)program[i];
        }
        resetProgram();
    }

    public int getProgramSize() {
        return program.length;
    }

    public MemoryContext<Short, Integer> getMemory() {
        return memoryStub;
    }

    public void setProgram(short... program) {
        ensureProgramSize(program.length);
        for (int i = 0; i < program.length; i++) {
            this.program[i] = program[i];
        }
        resetProgram();
    }

    public void resetProgram(short... program) {
        this.program = program;
        resetProgram();
    }

    public void setByte(int address, int value) {
        ensureProgramSize(address + 1);
        program[address] = (short)(value & 0xFF);
    }

    private void resetProgram() {
        memoryStub.setMemory(program);
    }

    public void reset() {
        cpu.reset();
    }

    public void expectRunState(CPU.RunState runState) {
        this.expectedRunState = Objects.requireNonNull(runState);
    }

    public void step() {
        cpu.step();
        System.out.flush();
        assertEquals("PC=" + getPC(), expectedRunState, runStateListener.runState);
    }

    public abstract int getPC();

    public abstract int getSP();

    public abstract void setFlags(int mask);

    public abstract void resetFlags(int mask);

    public abstract int getFlags();

    public int readByte(int address) {
        return program[address];
    }
}
