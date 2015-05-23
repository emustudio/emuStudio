/*
 * Copyright (C) 2015 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.zilogZ80.impl;

import emulib.emustudio.SettingsManager;
import emulib.plugins.cpu.CPU;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;

import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_C;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_H;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_N;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_PV;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_S;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_Z;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_A;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_B;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_C;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_D;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_E;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_H;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_L;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class InstructionsTest {
    protected final static int FLAG_S_Z = FLAG_S | FLAG_Z;
    protected final static int FLAG_S_Z_PV = FLAG_S | FLAG_Z | FLAG_PV;
    protected final static int FLAG_S_Z_PV_N = FLAG_S | FLAG_Z | FLAG_PV | FLAG_N;
    protected final static int FLAG_S_Z_PV_N_C = FLAG_S | FLAG_Z | FLAG_PV | FLAG_N | FLAG_C;
    protected final static int FLAG_S_Z_PV_C = FLAG_S | FLAG_Z | FLAG_PV | FLAG_C;
    protected final static int FLAG_S_Z_H = FLAG_S | FLAG_Z | FLAG_H;
    protected final static int FLAG_S_Z_H_C = FLAG_S | FLAG_Z | FLAG_H | FLAG_C;
    protected final static int FLAG_S_Z_H_N = FLAG_S | FLAG_Z | FLAG_H | FLAG_N;
    protected final static int FLAG_S_Z_H_N_C = FLAG_S | FLAG_Z | FLAG_H | FLAG_N | FLAG_C;
    protected final static int FLAG_S_Z_H_PV = FLAG_S | FLAG_Z | FLAG_H | FLAG_PV;
    protected final static int FLAG_S_Z_H_PV_N = FLAG_S | FLAG_Z | FLAG_H | FLAG_PV | FLAG_N;
    protected final static int FLAG_S_Z_H_PV_N_C = FLAG_S | FLAG_PV | FLAG_C | FLAG_H | FLAG_Z | FLAG_N;
    protected final static int FLAG_S_Z_H_PV_C = FLAG_S | FLAG_PV | FLAG_C | FLAG_H | FLAG_Z;
    protected final static int FLAG_S_Z_N_C = FLAG_S | FLAG_Z | FLAG_N | FLAG_C;
    protected final static int FLAG_S_Z_C = FLAG_S | FLAG_Z | FLAG_C;
    protected final static int FLAG_S_PV_N = FLAG_S | FLAG_PV | FLAG_N;
    protected final static int FLAG_S_PV_N_C = FLAG_S | FLAG_PV | FLAG_N | FLAG_C;
    protected final static int FLAG_S_N = FLAG_S | FLAG_N;
    protected final static int FLAG_S_N_C = FLAG_S | FLAG_N | FLAG_C;
    protected final static int FLAG_S_C = FLAG_S | FLAG_C;
    protected final static int FLAG_S_PV = FLAG_S | FLAG_PV;
    protected final static int FLAG_S_PV_C = FLAG_S | FLAG_C | FLAG_PV;
    protected final static int FLAG_S_H = FLAG_S | FLAG_H;
    protected final static int FLAG_S_H_PV = FLAG_S | FLAG_H | FLAG_PV;
    protected final static int FLAG_S_H_PV_N_C = FLAG_C | FLAG_S | FLAG_H | FLAG_PV | FLAG_N;
    protected final static int FLAG_S_H_PV_C = FLAG_C | FLAG_S | FLAG_H | FLAG_PV;
    protected final static int FLAG_S_H_N = FLAG_S | FLAG_H | FLAG_N;
    protected final static int FLAG_S_H_C = FLAG_C | FLAG_S | FLAG_H;
    protected final static int FLAG_Z_H = FLAG_Z | FLAG_H;
    protected final static int FLAG_Z_H_PV = FLAG_Z | FLAG_H | FLAG_PV;
    protected final static int FLAG_Z_H_PV_N = FLAG_Z | FLAG_H | FLAG_PV | FLAG_N;
    protected final static int FLAG_Z_H_PV_C = FLAG_Z | FLAG_H | FLAG_PV | FLAG_C;
    protected final static int FLAG_Z_H_N = FLAG_Z | FLAG_H | FLAG_N;
    protected final static int FLAG_Z_H_N_C = FLAG_Z | FLAG_H | FLAG_N | FLAG_C;
    protected final static int FLAG_Z_H_C = FLAG_C | FLAG_Z | FLAG_H;
    protected final static int FLAG_Z_H_PV_N_C = FLAG_Z | FLAG_C | FLAG_PV | FLAG_H | FLAG_N;
    protected final static int FLAG_Z_PV_N = FLAG_PV | FLAG_Z | FLAG_N;
    protected final static int FLAG_Z_PV_N_C = FLAG_PV | FLAG_Z | FLAG_N | FLAG_C;
    protected final static int FLAG_Z_PV_C = FLAG_PV | FLAG_Z | FLAG_C;
    protected final static int FLAG_Z_N_C = FLAG_Z | FLAG_N | FLAG_C;
    protected final static int FLAG_Z_C = FLAG_Z | FLAG_C;
    protected final static int FLAG_H_N = FLAG_H | FLAG_N;
    protected final static int FLAG_H_N_C = FLAG_H | FLAG_N | FLAG_C;
    protected final static int FLAG_H_C = FLAG_H | FLAG_C;
    protected final static int FLAG_H_PV = FLAG_H | FLAG_PV;
    protected final static int FLAG_H_PV_N = FLAG_H | FLAG_PV | FLAG_N;
    protected final static int FLAG_H_PV_C = FLAG_H | FLAG_C | FLAG_PV;
    protected final static int FLAG_PV_N = FLAG_PV | FLAG_N;
    protected final static int FLAG_PV_N_C = FLAG_PV | FLAG_C | FLAG_N;
    protected final static int FLAG_PV_C = FLAG_PV | FLAG_C;
    protected final static int FLAG_N_C = FLAG_N | FLAG_C;

    protected static final long PLUGIN_ID = 0L;

    private final static int[] GENERAL_REGISTERS = new int[] {
            REG_A, REG_B, REG_C,
            REG_D, REG_E, REG_H,
            REG_L
    };


    protected EmulatorPlugin cpu;
    protected MemoryStub memoryStub;
    private RunStateListenerStub runStateListener;

    private short[] program;

    @Before
    public void setUp() throws Exception {
        memoryStub = new MemoryStub();

        ContextPool contextPool = EasyMock.createNiceMock(ContextPool.class);
        expect(contextPool.getMemoryContext(0, MemoryContext.class))
                .andReturn(memoryStub)
                .anyTimes();
        replay(contextPool);

        runStateListener = new RunStateListenerStub();
        cpu = new EmulatorPlugin(PLUGIN_ID, contextPool);
        cpu.addCPUListener(runStateListener);

        // simulate emuStudio boot
        cpu.initialize(EasyMock.createNiceMock(SettingsManager.class));
    }

    @After
    public void tearDown() {
        cpu.destroy();
        program = null;
    }

    protected void resetProgram(int... program) {
        this.program = new short[program.length];
        for (int i = 0; i < program.length; i++) {
            this.program[i] = (short)program[i];
        }
        resetProgram();
    }

    protected void resetProgram(short... program) {
        this.program = program;
        resetProgram();
    }

    protected void resetProgram() {
        memoryStub.setMemory(program);
        cpu.reset();
    }

    protected void checkRunState(CPU.RunState runState) {
        assertEquals(runState, runStateListener.runState);
    }
    
    private void stepWithAssert() {
        cpu.step();
        assertFalse(CPU.RunState.STATE_STOPPED_ADDR_FALLOUT == runStateListener.runState);
        assertFalse(CPU.RunState.STATE_STOPPED_BAD_INSTR == runStateListener.runState);
    }

    protected void stepAndCheck(int value, int register) {
        stepWithAssert();
        assertEquals(value, cpu.getEngine().regs[register]);
    }

    protected void stepAndCheckIX(int value) {
        stepWithAssert();
        assertEquals(value, cpu.getEngine().IX);
    }

    protected void stepAndCheckIY(int value) {
        stepWithAssert();
        assertEquals(value, cpu.getEngine().IY);
    }
    
    protected void stepAndCheckAccAndFlags(int value, int flagsMask, int notFlagsMask) {
        stepAndCheck(value, REG_A);
        if (flagsMask != -1) {
            checkFlags(flagsMask);
        }
        checkNotFlags(notFlagsMask);
    }

    protected void stepAndCheckPC(int PC) {
        stepWithAssert();
        assertEquals(PC, cpu.getEngine().PC);
    }

    protected void stepAndCheckPCandSPandMemory(int PC, int SP, int memValue) {
        stepAndCheckPC(PC);
        assertEquals(SP, cpu.getEngine().SP);
        assertEquals(memValue, memoryStub.readWord(cpu.getEngine().SP).intValue());
    }

    protected void stepAndCheckPCandSP(int PC, int SP) {
        stepAndCheckPC(PC);
        assertEquals(SP, cpu.getEngine().SP);
    }

    protected void stepAndCheckMemory(int value, int address) {
        stepWithAssert();
        assertEquals(value, (int)memoryStub.read(address));
    }

    protected void stepAndCheckMemoryAndFlags(int value, int address, int flagsMask, int notFlagsMask) {
        stepAndCheckMemory(value, address);
        if (flagsMask != -1) {
            checkFlags(flagsMask);
        }
        checkNotFlags(notFlagsMask);
    }


    protected void stepAndCheckMemory(int address, int... values) {
        for (int value : values) {
            stepWithAssert();
            assertEquals(value, (int) memoryStub.read(address));
        }
    }

    protected void checkMemory(int value, int address) {
        assertEquals(value, (int) memoryStub.read(address));
    }

    protected void setRegisters(int... values) {
        EmulatorEngine engine = cpu.getEngine();
        for (int i = 0; i < values.length; i++) {
            engine.regs[GENERAL_REGISTERS[i]] = (short)values[i];
        }
    }

    protected void setRegister(int register, int value) {
        cpu.getEngine().regs[register] = (short)value;
    }
    
    protected void setRegisterIX(int value) {
        cpu.getEngine().IX = value;
    }
    
    protected void setRegisterIY(int value) {
        cpu.getEngine().IY = value;
    }
    
    protected void checkRegister(int register, int value) {
        assertEquals(value, cpu.getEngine().regs[register]);
    }

    protected void stepAndCheckRegisters(int... values) {
        for (int i = 0; i < values.length; i++) {
            stepAndCheck(values[i], GENERAL_REGISTERS[i]);
        }
    }

    protected void stepAndCheckRegister(int register, int... values) {
        for (int value : values) {
            stepAndCheck(value, register);
        }
    }

    protected void setFlags(int mask) {
        cpu.getEngine().flags |= mask;
    }

    protected void resetFlags(int mask) {
        cpu.getEngine().flags &= ~mask;
    }
    
    protected void checkFlags(int mask) {
        assertTrue("Flags exp=" + Integer.toBinaryString(mask)
                + "; was=" + Integer.toBinaryString(cpu.getEngine().flags),
                (cpu.getEngine().flags & mask) == mask);
    }

    protected void checkNotFlags(int mask) {
        assertTrue("Flags NOT=" + Integer.toBinaryString(mask)
                + "; was=" + Integer.toBinaryString(cpu.getEngine().flags),
                (cpu.getEngine().flags & mask) == 0);
    }
}