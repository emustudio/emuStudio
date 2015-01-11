package net.sf.emustudio.intel8080.impl;

import emulib.emustudio.SettingsManager;
import emulib.plugins.cpu.CPU;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

public abstract class InstructionsTest {
    protected final static int FLAG_S_Z = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_Z;
    protected final static int FLAG_S_P = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_P;
    protected final static int FLAG_S_C = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_C;
    protected final static int FLAG_S_P_C = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_P;
    protected final static int FLAG_S_AC = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_AC;
    protected final static int FLAG_S_AC_P = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_P;
    protected final static int FLAG_S_AC_P_C = EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_P;
    protected final static int FLAG_S_AC_C = EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_AC;
    protected final static int FLAG_S_Z_P = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_P;
    protected final static int FLAG_S_Z_C = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_C;
    protected final static int FLAG_S_Z_P_C = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_P | EmulatorEngine.FLAG_C;
    protected final static int FLAG_S_Z_AC = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_AC;
    protected final static int FLAG_S_Z_AC_C = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_C;
    protected final static int FLAG_S_Z_AC_P = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_P;
    protected final static int FLAG_S_Z_AC_P_C = EmulatorEngine.FLAG_S | EmulatorEngine.FLAG_P | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_Z;
    protected final static int FLAG_Z_AC = EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_AC;
    protected final static int FLAG_Z_AC_P = EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_P;
    protected final static int FLAG_Z_AC_C = EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_AC;
    protected final static int FLAG_Z_AC_P_C = EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_P | EmulatorEngine.FLAG_AC;
    protected final static int FLAG_Z_P = EmulatorEngine.FLAG_P | EmulatorEngine.FLAG_Z;
    protected final static int FLAG_Z_P_C = EmulatorEngine.FLAG_P | EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_C;
    protected final static int FLAG_Z_C = EmulatorEngine.FLAG_Z | EmulatorEngine.FLAG_C;
    protected final static int FLAG_AC_C = EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_C;
    protected final static int FLAG_AC_P = EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_P;
    protected final static int FLAG_AC_P_C = EmulatorEngine.FLAG_AC | EmulatorEngine.FLAG_C | EmulatorEngine.FLAG_P;
    protected final static int FLAG_P_C = EmulatorEngine.FLAG_P | EmulatorEngine.FLAG_C;

    protected static final long PLUGIN_ID = 0L;

    private static int[] GENERAL_REGISTERS = new int[] {
            EmulatorEngine.REG_A, EmulatorEngine.REG_B, EmulatorEngine.REG_C,
            EmulatorEngine.REG_D, EmulatorEngine.REG_E, EmulatorEngine.REG_H,
            EmulatorEngine.REG_L
    };


    protected CpuImpl cpu;
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
        cpu = new CpuImpl(PLUGIN_ID, contextPool);
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

    protected void stepAndCheck(int value, int register) {
        cpu.step();
        assertEquals(value, cpu.getEngine().regs[register]);
    }

    protected void stepAndCheckAccAndFlags(int value, int flagsMask, int notFlagsMask) {
        stepAndCheck(value, EmulatorEngine.REG_A);
        if (flagsMask != -1) {
            checkFlags(flagsMask);
        }
        checkNotFlags(notFlagsMask);
    }

    protected void stepAndCheckPC(int PC) {
        cpu.step();
        assertEquals(PC, cpu.getEngine().PC);
    }

    protected void stepAndCheckPCandSPandMemory(int PC, int SP, int memValue) {
        stepAndCheckPC(PC);
        assertEquals(SP, cpu.getEngine().SP);
        assertEquals(memValue, memoryStub.readWord(cpu.getEngine().SP));
    }

    protected void stepAndCheckPCandSP(int PC, int SP) {
        stepAndCheckPC(PC);
        assertEquals(SP, cpu.getEngine().SP);
    }

    protected void stepAndCheckMemory(int value, int address) {
        cpu.step();
        assertEquals(value, (int)memoryStub.read(address));
    }

    protected void stepAndCheckMemoryAndFlags(int value, int address, int flagsMask, int notFlagsMask) {
        stepAndCheckMemory(value, address);
        checkFlags(flagsMask);
        checkNotFlags(notFlagsMask);
    }


    protected void stepAndCheckMemory(int address, int... values) {
        for (int value : values) {
            cpu.step();
            assertEquals(value, (int) memoryStub.read(address));
        }
    }

    protected void checkMemory(int value, int address) {
        assertEquals(value, (int) memoryStub.read(address));
    }

    protected short[] generateMVI(int... values) {
        int[] opcodes = new int[] {
                0x3E, 0x06, 0x0E, 0x16, 0x1E, 0x26, 0x2E
        };
        short[] result = new short[values.length * 2];
        for (int i = 0, j = 0; i < values.length; i++, j+=2) {
            result[j] = (short)opcodes[i];
            result[j+1] = (short)values[i];
        }
        return result;
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
        assertTrue((cpu.getEngine().flags & mask) == mask);
    }

    protected void checkNotFlags(int mask) {
        assertTrue((cpu.getEngine().flags & mask) == 0);
    }
}