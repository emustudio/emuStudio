/*
 * Copyright (C) 2015 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.emustudio.cpu.testsuite;

import emulib.plugins.cpu.CPU;
import emulib.plugins.memory.MemoryContext;
import net.sf.emustudio.cpu.testsuite.internal.RunStateListenerStub;
import net.sf.emustudio.cpu.testsuite.memory.MemoryStub;

import java.util.Arrays;
import java.util.List;
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
        length = Math.max(length, 65536);
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

    public void setProgram(List<? extends Number> program) {
        int[] array = new int[program.size()];

        int i = 0;
        for (Number n : program) {
            array[i++] = n.intValue();
        }
        setProgram(array);
    }

    public MemoryContext<Short> getMemory() {
        return memoryStub;
    }

    public void setProgram(short... program) {
        ensureProgramSize(program.length);
        System.arraycopy(program, 0, this.program, 0, program.length);
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

    public abstract List<Integer> getRegisters();

    public abstract void setRegister(int register, int value);

    public abstract void setFlags(int mask);

    public abstract int getFlags();

}
