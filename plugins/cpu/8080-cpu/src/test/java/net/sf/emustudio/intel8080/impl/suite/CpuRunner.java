package net.sf.emustudio.intel8080.impl.suite;

import emulib.plugins.cpu.CPU;
import emulib.plugins.memory.Memory;
import emulib.plugins.memory.MemoryContext;
import net.sf.emustudio.intel8080.impl.CpuImpl;
import net.sf.emustudio.intel8080.impl.EmulatorEngine;
import net.sf.emustudio.intel8080.impl.MemoryStub;
import net.sf.emustudio.intel8080.impl.RunStateListenerStub;

import java.util.Arrays;
import java.util.Objects;

import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_A;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_B;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_C;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_D;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_E;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_H;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_L;
import static org.junit.Assert.assertEquals;

public class CpuRunner {
    private final RunStateListenerStub runStateListener = new RunStateListenerStub();
    private final CpuImpl cpu;
    private final MemoryStub memoryStub;

    private short[] program = new short[1];
    private CPU.RunState expectedRunState = CPU.RunState.STATE_STOPPED_BREAK;

    public CpuRunner(CpuImpl cpu, MemoryStub memoryStub) {
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
        assertEquals("PC=" + cpu.getEngine().PC, expectedRunState, runStateListener.runState);
    }

    public void setAccumulator(int value) {
        setRegister(EmulatorEngine.REG_A, value);
    }

    public Byte getAccumulator() {
        return getRegister(EmulatorEngine.REG_A);
    }

    public void setRegister(int register, int value) {
        cpu.getEngine().regs[register] = value & 0xFF;
    }

    public void setRegisterPair(int registerPair, int value) {
        int highRegister;
        int lowRegister;
        switch (registerPair) {
            case 0:
                highRegister = REG_B;
                lowRegister = REG_C;
                break;
            case 1:
                highRegister = REG_D;
                lowRegister = REG_E;
                break;
            case 2:
                highRegister = REG_H;
                lowRegister = REG_L;
                break;
            case 3:
                cpu.getEngine().SP = value & 0xFFFF;
                return;
            default:
                throw new IllegalArgumentException("Expected value between <0,3> !");
        }
        cpu.getEngine().regs[highRegister] = (value >>> 8) & 0xFF;
        cpu.getEngine().regs[lowRegister] = value & 0xFF;
    }

    public void setRegisterPairPSW(int registerPair, int value) {
        if (registerPair < 3) {
            setRegisterPair(registerPair, value);
        } else if (registerPair == 3) {
            cpu.getEngine().regs[REG_A] = (value >>> 8) & 0xFF;
            cpu.getEngine().flags = (short)(value & 0xD7 | 2);
        } else {
            throw new IllegalArgumentException("Expected value between <0,3> !");
        }
    }

    public Byte getRegister(int register) {
        return new Byte((byte)cpu.getEngine().regs[register]);
    }

    public int getPC() {
        return cpu.getEngine().PC;
    }

    public void setSP(int SP) {
        cpu.getEngine().SP = SP;
    }

    public int getSP() {
        return cpu.getEngine().SP;
    }

    public void setFlags(int mask) {
        cpu.getEngine().flags |= mask;
    }

    public void resetFlags(int mask) {
        cpu.getEngine().flags &= ~mask;
    }

    public int getFlags() {
        return cpu.getEngine().flags;
    }

    public int readByte(int address) {
        return program[address];
    }
}
