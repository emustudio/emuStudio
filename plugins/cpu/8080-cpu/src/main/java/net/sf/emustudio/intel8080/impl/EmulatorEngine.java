package net.sf.emustudio.intel8080.impl;

import emulib.plugins.cpu.CPU;
import emulib.plugins.memory.MemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.locks.LockSupport;

public class EmulatorEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmulatorEngine.class);

    public static final int REG_A = 7,
            REG_B = 0,
            REG_C = 1,
            REG_D = 2,
            REG_E = 3,
            REG_H = 4,
            REG_L = 5;

    public static final int FLAG_S = 0x80,
            FLAG_Z = 0x40,
            FLAG_AC = 0x10,
            FLAG_P = 0x4,
            FLAG_C = 0x1;

    private static final byte PARITY[] = {
            1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0,
            1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1,
            1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0,
            1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
            1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0,
            1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1,
            1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0,
            1, 0, 0, 1
    };

    private boolean INTE = false; // enabling / disabling of interrupts
    private boolean isINT = false;
    private short b1 = 0; // the raw interrupt instruction
    private short b2 = 0;
    private short b3 = 0;

    public int PC = 0; // program counter
    public int SP = 0; // stack pointer
    public short[] regs = new short[8];
    public short flags = 2; // registers
    public volatile CPU.RunState currentRunState = CPU.RunState.STATE_STOPPED_NORMAL;

    private final MemoryContext<Short> memory;
    private final ContextImpl context;

    public int checkTimeSlice = 100;
    private long executedCycles = 0;

    public EmulatorEngine(MemoryContext<Short> memory, ContextImpl context) {
        this.memory = memory;
        this.context = context;
    }

    public long getAndResetExecutedCycles() {
        long tmpExecutedCycles = executedCycles;
        executedCycles = 0;
        return tmpExecutedCycles;
    }

    public void reset(int startPos) {
        Arrays.fill(regs, (short) 0);
        SP = 0;
        flags = 2; //0000 0010b
        PC = startPos;
        INTE = false;
        currentRunState = CPU.RunState.STATE_STOPPED_BREAK;
    }

    public CPU.RunState step() throws Exception {
        currentRunState = CPU.RunState.STATE_STOPPED_BREAK;
        dispatch();
        return currentRunState;
    }

    public CPU.RunState run(CPU cpu) {
        long startTime, endTime;
        int cycles_executed;
        int cycles_to_execute = checkTimeSlice * context.getCPUFrequency();
        int cycles;
        long slice = checkTimeSlice * 1000000;

        currentRunState = CPU.RunState.STATE_RUNNING;
        while (!Thread.currentThread().isInterrupted() && (currentRunState == CPU.RunState.STATE_RUNNING)) {
            startTime = System.nanoTime();
            cycles_executed = 0;
            while ((cycles_executed < cycles_to_execute) && !Thread.currentThread().isInterrupted() && (currentRunState == CPU.RunState.STATE_RUNNING)) {
                try {
                    cycles = dispatch();
                    cycles_executed += cycles;
                    executedCycles += cycles;
                    if (cpu.isBreakpointSet(PC)) {
                        throw new Breakpoint();
                    }
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    return  CPU.RunState.STATE_STOPPED_BAD_INSTR;
                } catch (IndexOutOfBoundsException e) {
                    return  CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
                } catch (Breakpoint e) {
                    return CPU.RunState.STATE_STOPPED_BREAK;
                }
            }
            endTime = System.nanoTime() - startTime;
            if (endTime < slice) {
                // time correction
                LockSupport.parkNanos(slice - endTime);
            }
        }
        return CPU.RunState.STATE_STOPPED_NORMAL;
    }


    public void interrupt(short b1, short b2, short b3) {
        if (INTE == false) {
            return;
        }
        isINT = true;
        this.b1 = b1;
        this.b2 = b2;
        this.b3 = b3;
    }

    /* Get an 8080 register and return it */
    private short getreg(int reg) {
        if (reg == 6) {
            return memory.read((regs[REG_H] << 8) | regs[REG_L]);
        }
        return regs[reg];
    }

    /* Put a value into an 8080 register from memory */
    private void putreg(int reg, short val) {
        if (reg == 6) {
            memory.write((regs[REG_H] << 8) | regs[REG_L], val);
        } else {
            regs[reg] = val;
        }
    }

    /* Put a value into an 8080 register pair */
    void putpair(int reg, int val) {
        short high, low;
        high = (short) ((val >>> 8) & 0xFF);
        low = (short) (val & 0xFF);
        switch (reg) {
            case 0:
                regs[REG_B] = high;
                regs[REG_C] = low;
                break;
            case 1:
                regs[REG_D] = high;
                regs[REG_E] = low;
                break;
            case 2:
                regs[REG_H] = high;
                regs[REG_L] = low;
                break;
            case 3:
                SP = val;
                break;
        }
    }

    /* Return the value of a selected register pair */
    int getpair(int reg) {
        switch (reg) {
            case 0:
                return (regs[REG_B] << 8) | regs[REG_C];
            case 1:
                return (regs[REG_D] << 8) | regs[REG_E];
            case 2:
                return (regs[REG_H] << 8) | regs[REG_L];
            case 3:
                return SP;
        }
        return 0;
    }

    /* Return the value of a selected register pair, in PUSH
     format where 3 means regs[REG_A]& flags, not SP
     */
    private int getpush(int reg) {
        int stat;
        switch (reg) {
            case 0:
                return (regs[REG_B] << 8) | regs[REG_C];
            case 1:
                return (regs[REG_D] << 8) | regs[REG_E];
            case 2:
                return (regs[REG_H] << 8) | regs[REG_L];
            case 3:
                stat = (regs[REG_A] << 8) | flags;
                stat |= 0x02;
                return stat;
        }
        return 0;
    }

    /* Place data into the indicated register pair, in PUSH
     format where 3 means regs[REG_A]& flags, not SP
     */
    private void putpush(int reg, int val) {
        short high, low;
        high = (short) ((val >>> 8) & 0xFF);
        low = (short) (val & 0xFF);
        switch (reg) {
            case 0:
                regs[REG_B] = high;
                regs[REG_C] = low;
                break;
            case 1:
                regs[REG_D] = high;
                regs[REG_E] = low;
                break;
            case 2:
                regs[REG_H] = high;
                regs[REG_L] = low;
                break;
            case 3:
                regs[REG_A] = (short) ((val >>> 8) & 0xFF);
                flags = (short) (val & 0xFF);
                break;
        }
    }

    /* Set the <C>arry, <S>ign, <Z>ero and <P>arity flags following
     an arithmetic operation on 'reg'. 8080 changes AC flag
     */
    private void setarith(int reg, int before, int sumWith, boolean sub) {
        if (sub) {
            flags &= (~FLAG_C);
        } else {
            if ((reg & 0x100) != 0) {
                flags |= FLAG_C;
            } else {
                flags &= (~FLAG_C);
            }
        }
        if ((reg & 0x80) != 0) {
            flags |= FLAG_S;
        } else {
            flags &= (~FLAG_S);
        }
        if ((reg & 0xff) == 0) {
            flags |= FLAG_Z;
        } else {
            flags &= (~FLAG_Z);
        }
        // carry from 3.bit to 4.
        auxCarry(before, sumWith);
        parity(reg);
    }

    private void auxCarry(int before, int sumWith) {
        int mask = sumWith & before;
        int xormask = sumWith ^ before;

        int C0 = mask&1;
        int C1 = ((mask>>>1) ^ (C0&(xormask>>>1)))&1;
        int C2 = ((mask>>>2) ^ (C1&(xormask>>>2)))&1;
        int C3 = ((mask>>>3) ^ (C2&(xormask>>>3)))&1;

        if (C3 != 0) {
            flags |= FLAG_AC;
        } else {
            flags &= (~FLAG_AC);
        }
    }

    /* Set the <S>ign, <Z>ero amd <P>arity flags following
     an INR/DCR operation on 'reg'.
     */
    private void setinc(int reg, int before, int diff) {
        if ((reg & 0x80) != 0) {
            flags |= FLAG_S;
        } else {
            flags &= (~FLAG_S);
        }
        if ((reg & 0xff) == 0) {
            flags |= FLAG_Z;
        } else {
            flags &= (~FLAG_Z);
        }
        auxCarry(before, diff);
        parity(reg);
    }

    /* Set the <C>arry, <S>ign, <Z>ero amd <P>arity flags following
     a logical (bitwise) operation on 'reg'.
     */
    private void setlogical(int reg) {
        flags &= (~FLAG_C);
        if ((reg & 0x80) != 0) {
            flags |= FLAG_S;
        } else {
            flags &= (~FLAG_S);
        }
        if ((reg & 0xff) == 0) {
            flags |= FLAG_Z;
        } else {
            flags &= (~FLAG_Z);
        }
        parity(reg);
    }

    /* Set the Parity (P) flag based on parity of 'reg', i.e., number
     of bits on even: P=0200000, else P=0
     */
    private void parity(int reg) {
        if (PARITY[reg & 0xFF] == 1) {
            flags |= FLAG_P;
        } else {
            flags &= (~FLAG_P);
        }
    }

    /* Test an 8080 flag condition and return 1 if true, 0 if false */
    private boolean checkCondition(int con) {
        switch (con) {
            case 0:
                return ((flags & FLAG_Z) == 0);
            case 1:
                return ((flags & FLAG_Z) != 0);
            case 2:
                return ((flags & FLAG_C) == 0);
            case 3:
                return ((flags & FLAG_C) != 0);
            case 4:
                return ((flags & FLAG_P) == 0);
            case 5:
                return ((flags & FLAG_P) != 0);
            case 6:
                return ((flags & FLAG_S) == 0);
            case 7:
                return ((flags & FLAG_S) != 0);
        }
        return false;
    }

    private static final Method[] dispatchTable = new Method[256];

    static {
        try {
            dispatchTable[0] = EmulatorEngine.class.getDeclaredMethod("O0_NOP", short.class);
            dispatchTable[6] = EmulatorEngine.class.getDeclaredMethod("MC7_O6_MVI", short.class);
            dispatchTable[7] = EmulatorEngine.class.getDeclaredMethod("O7_RLC", short.class);
            dispatchTable[14] = EmulatorEngine.class.getDeclaredMethod("MC7_O6_MVI", short.class);
            dispatchTable[15] = EmulatorEngine.class.getDeclaredMethod("O15_RRC", short.class);
            dispatchTable[22] = EmulatorEngine.class.getDeclaredMethod("MC7_O6_MVI", short.class);
            dispatchTable[23] = EmulatorEngine.class.getDeclaredMethod("O23_RAL", short.class);
            dispatchTable[30] = EmulatorEngine.class.getDeclaredMethod("MC7_O6_MVI", short.class);
            dispatchTable[31] = EmulatorEngine.class.getDeclaredMethod("O31_RAR", short.class);
            dispatchTable[34] = EmulatorEngine.class.getDeclaredMethod("O34_SHLD", short.class);
            dispatchTable[38] = EmulatorEngine.class.getDeclaredMethod("MC7_O6_MVI", short.class);
            dispatchTable[39] = EmulatorEngine.class.getDeclaredMethod("O39_DAA", short.class);
            dispatchTable[42] = EmulatorEngine.class.getDeclaredMethod("O42_LHLD", short.class);
            dispatchTable[46] = EmulatorEngine.class.getDeclaredMethod("MC7_O6_MVI", short.class);
            dispatchTable[47] = EmulatorEngine.class.getDeclaredMethod("O47_CMA", short.class);
            dispatchTable[50] = EmulatorEngine.class.getDeclaredMethod("O50_STA", short.class);
            dispatchTable[54] = EmulatorEngine.class.getDeclaredMethod("MC7_O6_MVI", short.class);
            dispatchTable[55] = EmulatorEngine.class.getDeclaredMethod("O55_STC", short.class);
            dispatchTable[58] = EmulatorEngine.class.getDeclaredMethod("O58_LDA", short.class);
            dispatchTable[62] = EmulatorEngine.class.getDeclaredMethod("MC7_O6_MVI", short.class);
            dispatchTable[63] = EmulatorEngine.class.getDeclaredMethod("O63_CMC", short.class);
            dispatchTable[64] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[65] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[66] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[67] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[68] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[69] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[70] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[71] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[72] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[73] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[74] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[75] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[76] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[77] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[78] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[79] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[80] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[81] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[82] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[83] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[84] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[85] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[86] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[87] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[88] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[89] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[90] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[91] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[92] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[93] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[94] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[95] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[96] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[97] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[98] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[99] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[100] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[101] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[102] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[103] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[104] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[105] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[106] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[107] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[108] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[109] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[110] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[111] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[112] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[113] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[114] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[115] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[116] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[117] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[118] = EmulatorEngine.class.getDeclaredMethod("O118_HLT", short.class);
            dispatchTable[119] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[120] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[121] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[122] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[123] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[124] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[125] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[126] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[127] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            dispatchTable[195] = EmulatorEngine.class.getDeclaredMethod("O195_JMP", short.class);
            dispatchTable[198] = EmulatorEngine.class.getDeclaredMethod("O198_ADI", short.class);
            dispatchTable[201] = EmulatorEngine.class.getDeclaredMethod("O201_RET", short.class);
            dispatchTable[205] = EmulatorEngine.class.getDeclaredMethod("O205_CALL", short.class);
            dispatchTable[206] = EmulatorEngine.class.getDeclaredMethod("O206_ACI", short.class);
            dispatchTable[211] = EmulatorEngine.class.getDeclaredMethod("O211_OUT", short.class);
            dispatchTable[214] = EmulatorEngine.class.getDeclaredMethod("O214_SUI", short.class);
            dispatchTable[219] = EmulatorEngine.class.getDeclaredMethod("O219_IN", short.class);
            dispatchTable[222] = EmulatorEngine.class.getDeclaredMethod("O222_SBI", short.class);
            dispatchTable[227] = EmulatorEngine.class.getDeclaredMethod("O227_XTHL", short.class);
            dispatchTable[230] = EmulatorEngine.class.getDeclaredMethod("O230_ANI", short.class);
            dispatchTable[233] = EmulatorEngine.class.getDeclaredMethod("O233_PCHL", short.class);
            dispatchTable[235] = EmulatorEngine.class.getDeclaredMethod("O235_XCHG", short.class);
            dispatchTable[238] = EmulatorEngine.class.getDeclaredMethod("O238_XRI", short.class);
            dispatchTable[243] = EmulatorEngine.class.getDeclaredMethod("O243_DI", short.class);
            dispatchTable[246] = EmulatorEngine.class.getDeclaredMethod("O246_ORI", short.class);
            dispatchTable[249] = EmulatorEngine.class.getDeclaredMethod("O249_SPHL", short.class);
            dispatchTable[251] = EmulatorEngine.class.getDeclaredMethod("O251_EI", short.class);
            dispatchTable[254] = EmulatorEngine.class.getDeclaredMethod("O254_CPI", short.class);
        } catch (NoSuchMethodException e) {
            LOGGER.error("Could not set up dispatch table. The emulator won't work correctly", e);
        }
    }

    private int O0_NOP(short OP) {
        return 4;
    }

    private int O7_RLC(short OP) {
        int xx = (regs[REG_A] << 9) & 0200000;
        if (xx != 0) {
            flags |= FLAG_C;
        } else {
            flags &= (~FLAG_C);
        }
        regs[REG_A] = (short) ((regs[REG_A] << 1) & 0xFF);
        if (xx != 0) {
            regs[REG_A] |= 0x01;
        }
        return 4;
    }

    private int O15_RRC(short OP) {
        if ((regs[REG_A] & 0x01) == 1) {
            flags |= FLAG_C;
        } else {
            flags &= (~FLAG_C);
        }
        regs[REG_A] = (short) ((regs[REG_A] >>> 1) & 0xFF);
        if ((flags & FLAG_C) != 0) {
            regs[REG_A] |= 0x80;
        }
        return 4;
    }

    private int O23_RAL(short OP) {
        int xx = (regs[REG_A] << 9) & 0200000;
        regs[REG_A] = (short) ((regs[REG_A] << 1) & 0xFF);
        if ((flags & FLAG_C) != 0) {
            regs[REG_A] |= 1;
        } else {
            regs[REG_A] &= 0xFE;
        }
        if (xx != 0) {
            flags |= FLAG_C;
        } else {
            flags &= (~FLAG_C);
        }
        return 4;
    }

    private int O31_RAR(short OP) {
        int xx = 0;
        if ((regs[REG_A] & 0x01) == 1) {
            xx |= 0200000;
        }
        regs[REG_A] = (short) ((regs[REG_A] >>> 1) & 0xFF);
        if ((flags & FLAG_C) != 0) {
            regs[REG_A] |= 0x80;
        } else {
            regs[REG_A] &= 0x7F;
        }
        if (xx != 0) {
            flags |= FLAG_C;
        } else {
            flags &= (~FLAG_C);
        }
        return 4;
    }

    private int O34_SHLD(short OP) {
        int DAR = (Integer) memory.readWord(PC);
        PC += 2;
        memory.writeWord(DAR, (regs[REG_H] << 8) | regs[REG_L]);
        return 16;
    }

    private int O39_DAA(short OP) {
        int DAR = regs[REG_A];
        if (((DAR & 0x0F) > 9) || ((flags & FLAG_AC) != 0)) {
            DAR += 6;
            if ((DAR & 0x10) != (regs[REG_A] & 0x10)) {
                flags |= FLAG_AC;
            } else {
                flags &= (~FLAG_AC);
            }
            regs[REG_A] = (short) (DAR & 0xFF);
        }
        DAR = (regs[REG_A] >>> 4) & 0x0F;
        if ((DAR > 9) || ((flags & FLAG_C) != 0)) {
            DAR += 6;
            if ((DAR & 0x10) != 0) {
                flags |= FLAG_C;
            }
            regs[REG_A] &= 0x0F;
            regs[REG_A] |= ((DAR << 4) & 0xF0);
        }
        if ((regs[REG_A] & 0x80) != 0) {
            flags |= FLAG_S;
        } else {
            flags &= (~FLAG_S);
        }
        if ((regs[REG_A] & 0xff) == 0) {
            flags |= FLAG_Z;
        } else {
            flags &= (~FLAG_Z);
        }
        parity(regs[REG_A]);
        regs[REG_A] = (short) (regs[REG_A] & 0xFF);
        return 4;
    }

    private int O42_LHLD(short OP) {
        // TODO: test!
        int DAR = (Integer) memory.readWord(PC);
        PC += 2;
        regs[REG_L] = memory.read(DAR);
        regs[REG_H] = memory.read(DAR + 1);
        return 16;
    }

    private int O47_CMA(short OP) {
        regs[REG_A] = (short) (~regs[REG_A]);
        regs[REG_A] &= 0xFF;
        return 4;
    }

    private int O50_STA(short OP) {
        int DAR = (Integer) memory.readWord(PC);
        PC += 2;
        memory.write(DAR, regs[REG_A]);
        return 13;
    }

    private int O55_STC(short OP) {
        flags |= FLAG_C;
        return 4;
    }

    private int O58_LDA(short OP) {
        int DAR = (Integer) memory.readWord(PC);
        PC += 2;
        regs[REG_A] = memory.read(DAR);
        return 13;
    }

    private int O63_CMC(short OP) {
        if ((flags & FLAG_C) != 0) {
            flags &= (~FLAG_C);
        } else {
            flags |= FLAG_C;
        }
        return 4;
    }

    private int O118_HLT(short OP) {
        currentRunState = CPU.RunState.STATE_STOPPED_NORMAL;
        return 7;
    }

    private int O195_JMP(short OP) {
        PC = (Integer) memory.readWord(PC);
        return 10;
    }

    private int O198_ADI(short OP) {
        int DAR = regs[REG_A];
        int diff = memory.read(PC++);
        regs[REG_A] += diff;
        setarith(regs[REG_A], DAR, diff, false);
        regs[REG_A] = (short) (regs[REG_A] & 0xFF);
        return 7;
    }

    private int O201_RET(short OP) {
        PC = (Integer) memory.readWord(SP);
        SP += 2;
        return 10;
    }

    private int O205_CALL(short OP) {
        memory.writeWord(SP - 2, PC + 2);
        SP -= 2;
        PC = (Integer) memory.readWord(PC);
        return 17;
    }

    private int O206_ACI(short OP) {
        int DAR = regs[REG_A];
        int diff = memory.read(PC++);
        if ((flags & FLAG_C) != 0) {
            diff++;
        }
        regs[REG_A] += diff;
        setarith(regs[REG_A], DAR, diff, false);
        regs[REG_A] = (short) (regs[REG_A] & 0xFF);
        return 7;
    }

    private int O211_OUT(short OP) {
        int DAR = memory.read(PC++);
        context.fireIO(DAR, false, regs[REG_A]);
        return 10;
    }

    private int O214_SUI(short OP) {
        int DAR = regs[REG_A];
        int diff = memory.read(PC++);
        regs[REG_A] -= diff;
        setarith(regs[REG_A], DAR, (-diff) & 0xFF, true);
        regs[REG_A] = (short) (regs[REG_A] & 0xFF);
        return 7;
    }

    private int O219_IN(short OP) {
        int DAR = memory.read(PC++);
        regs[REG_A] = context.fireIO(DAR, true, (short) 0);
        return 10;
    }

    private int O222_SBI(short OP) {
        int DAR = regs[REG_A];
        int diff = memory.read(PC++);
        if ((flags & FLAG_C) != 0) {
            diff++;
        }
        regs[REG_A] -= diff;
        setarith(regs[REG_A], DAR, (-diff) & 0xFF, true);
        regs[REG_A] = (short) (regs[REG_A] & 0xFF);
        return 7;
    }

    private int O227_XTHL(short OP) {
        int DAR = (Integer) memory.readWord(SP);
        memory.writeWord(SP, (regs[REG_H] << 8) | regs[REG_L]);
        regs[REG_H] = (short) ((DAR >>> 8) & 0xFF);
        regs[REG_L] = (short) (DAR & 0xFF);
        return 18;
    }

    private int O230_ANI(short OP) {
        regs[REG_A] &= memory.read(PC++);
        flags &= (~FLAG_C);
        flags &= (~FLAG_AC);
        setlogical(regs[REG_A]);
        regs[REG_A] &= 0xFF;
        return 7;
    }

    private int O233_PCHL(short OP) {
        PC = (regs[REG_H] << 8) | regs[REG_L];
        return 5;
    }

    private int O235_XCHG(short OP) {
        short x = regs[REG_H];
        short y = regs[REG_L];
        regs[REG_H] = regs[REG_D];
        regs[REG_L] = regs[REG_E];
        regs[REG_D] = x;
        regs[REG_E] = y;
        return 4;
    }

    private int O238_XRI(short OP) {
        regs[REG_A] ^= memory.read(PC++);
        flags &= (~FLAG_C);
        flags &= (~FLAG_AC);
        setlogical(regs[REG_A]);
        regs[REG_A] &= 0xFF;
        return 7;
    }

    private int O243_DI(short OP) {
        INTE = false;
        return 4;
    }

    private int O246_ORI(short OP) {
        regs[REG_A] |= memory.read(PC++);
        flags &= (~FLAG_C);
        flags &= (~FLAG_AC);
        setlogical(regs[REG_A]);
        regs[REG_A] &= 0xFF;
        return 7;
    }

    private int O249_SPHL(short OP) {
        SP = (regs[REG_H] << 8) | regs[REG_L];
        return 5;
    }

    private int O251_EI(short OP) {
        INTE = true;
        return 4;
    }

    private int O254_CPI(short OP) {
        int X = regs[REG_A];
        int DAR = regs[REG_A] & 0xFF;
        int diff = memory.read(PC++);
        DAR -= diff;
        setarith(DAR, X, (-diff) & 0xFF, true);
        return 7;
    }

    private int MC0_O40_MOV(short OP) {
        putreg((OP >>> 3) & 0x07, getreg(OP & 0x07));
        if (((OP & 0x07) == 6) || (((OP >>> 3) & 0x07) == 6)) {
            return 7;
        } else {
            return 5;
        }
    }

    private int MC7_O6_MVI(short OP) {
        putreg((OP >>> 3) & 0x07, memory.read(PC++));
        if (((OP >>> 3) & 0x07) == 6) {
            return 10;
        } else {
            return 7;
        }
    }

    private int MCF_01_LXI(short OP) {
        putpair((OP >>> 4) & 0x03, (Integer) memory.readWord(PC));
        PC += 2;
        return 10;
    }

    private int MEF_0A_LDAX(short OP) {
        putreg(7, memory.read(getpair((OP >>> 4) & 0x03)));
        return 7;
    }

    private int MEF_02_STAX(short OP) {
        memory.write(getpair((OP >>> 4) & 0x03), getreg(7));
        return 7;
    }

    private int MF8_B8_CMP(short OP) {
        int X = regs[REG_A];
        int DAR = regs[REG_A] & 0xFF;
        int diff = getreg(OP & 0x07);
        DAR -= diff;
        setarith(DAR, X, (-diff) & 0xFF, true);
        if ((OP & 0x07) == 6) {
            return 7;
        } else {
            return 4;
        }
    }

    private int MC7_C2_JMP(short OP) {
        if (checkCondition((OP >>> 3) & 0x07)) {
            PC = (Integer) memory.readWord(PC);
        } else {
            PC += 2;
        }
        return 10;
    }

    private int MC7_C4_CALL(short OP) {
        if (checkCondition((OP >>> 3) & 0x07)) {
            int DAR = (Integer) memory.readWord(PC);
            PC += 2;
            memory.writeWord(SP - 2, PC);
            SP -= 2;
            PC = DAR;
            return 17;
        } else {
            PC += 2;
            return 11;
        }
    }

    private int MC7_C0_RET(short OP) {
        if (checkCondition((OP >>> 3) & 0x07)) {
            PC = (Integer) memory.readWord(SP);
            SP += 2;
        }
        return 10;
    }

    private int MC7_C7_RST(short OP) {
        memory.writeWord(SP - 2, PC);
        SP -= 2;
        PC = OP & 0x38;
        return 11;
    }

    private int MCF_C5_PUSH(short OP) {
        int DAR = getpush((OP >>> 4) & 0x03);
        memory.writeWord(SP - 2, DAR);
        SP -= 2;
        return 11;
    }

    private int MCF_C1_POP(short OP) {
        int DAR = (Integer) memory.readWord(SP);
        SP += 2;
        putpush((OP >>> 4) & 0x03, DAR);
        return 10;
    }

    private int MF8_80_ADD(short OP) {
        int X = regs[REG_A];
        int diff = getreg(OP & 0x07);
        regs[REG_A] += diff;
        setarith(regs[REG_A], X, diff, false);
        regs[REG_A] = (short) (regs[REG_A] & 0xFF);
        if ((OP & 0x07) == 6) {
            return 7;
        }
        return 4;
    }

    private int MF8_88_ADC(short OP) {
        int X = regs[REG_A];
        int diff = getreg(OP & 0x07);
        if ((flags & FLAG_C) != 0) {
            diff++;
        }
        regs[REG_A] += diff;
        setarith(regs[REG_A], X, diff, false);
        regs[REG_A] = (short) (regs[REG_A] & 0xFF);
        if ((OP & 0x07) == 6) {
            return 7;
        }
        return 4;
    }

    private int MF8_90_SUB(short OP) {
        int X = regs[REG_A];
        int diff = getreg(OP & 0x07);
        regs[REG_A] -= diff;
        setarith(regs[REG_A], X, (-diff) & 0xFF, true);
        regs[REG_A] = (short) (regs[REG_A] & 0xFF);
        if ((OP & 0x07) == 6) {
            return 7;
        }
        return 4;
    }

    private int MF8_98_SBB(short OP) {
        int X = regs[REG_A];
        int diff = getreg(OP & 0x07);
        if ((flags & FLAG_C) != 0) {
            diff++;
        }
        regs[REG_A] -= diff;
        setarith(regs[REG_A], X, (-diff) & 0xFF, true);
        regs[REG_A] = (short) (regs[REG_A] & 0xFF);
        if ((OP & 0x07) == 6) {
            return 7;
        }
        return 4;
    }

    private int MC7_04_INR(short OP) {
        int DAR = getreg((OP >>> 3) & 0x07) + 1;
        setinc(DAR, DAR - 1, 1);
        DAR = DAR & 0xFF;
        putreg((OP >>> 3) & 0x07, (short) DAR);
        return 5;
    }

    private int MC7_05_DCR(short OP) {
        int DAR = getreg((OP >>> 3) & 0x07) - 1;
        setinc(DAR, DAR + 1, (-1)&0xFF);
        DAR = DAR & 0xFF;
        putreg((OP >>> 3) & 0x07, (short) DAR);
        return 5;
    }

    private int MCF_03_INX(short OP) {
        int DAR = getpair((OP >>> 4) & 0x03) + 1;
        DAR = DAR & 0xFFFF;
        putpair((OP >>> 4) & 0x03, DAR);
        return 5;
    }

    private int MCF_0B_DCX(short OP) {
        int DAR = getpair((OP >>> 4) & 0x03) - 1;
        DAR = DAR & 0xFFFF;
        putpair((OP >>> 4) & 0x03, DAR);
        return 5;
    }

    private int MCF_09_DAD(short OP) {
        int DAR = getpair((OP >>> 4) & 0x03);
        DAR += getpair(2);
        if ((DAR & 0x10000) != 0) {
            flags |= FLAG_C;
        } else {
            flags &= (~FLAG_C);
        }
        DAR = DAR & 0xFFFF;
        putpair(2, DAR);
        return 10;
    }

    private int MF8_A0_ANA(short OP) {
        regs[REG_A] &= getreg(OP & 0x07);
        setlogical(regs[REG_A]);
        regs[REG_A] &= 0xFF;
        return 4;
    }

    private int MF8_A8_XRA(short OP) {
        short before = regs[REG_A];
        short diff = getreg(OP & 0x07);
        regs[REG_A] ^= diff;
        setlogical(regs[REG_A]);
        auxCarry(before, diff);
        regs[REG_A] &= 0xFF;
        return 4;
    }

    private int MF8_B0_ORA(short OP) {
        regs[REG_A] |= getreg(OP & 0x07);
        setlogical(regs[REG_A]);
        regs[REG_A] &= 0xFF;
        return 4;
    }

    private int dispatch() throws InvocationTargetException, IllegalAccessException {
        short OP;

        /* if interrupt is waiting, instruction won't be read from memory
         * but from one or all of 3 bytes (b1,b2,b3) which represents either
         * rst or call instruction incomed from external peripheral device
         */
        if (isINT == true) {
            if (INTE == true) {
                if ((b1 & 0xC7) == 0xC7) {                      /* RST */
                    memory.writeWord(SP - 2, PC);
                    SP -= 2;
                    PC = b1 & 0x38;
                    return 11;
                } else if (b1 == 0315) {                        /* CALL */
                    memory.writeWord(SP - 2, PC + 2);
                    SP -= 2;
                    PC = (int) (((b3 & 0xFF) << 8) | (b2 & 0xFF));
                    return 17;
                }
            }
            isINT = false;
        }
        OP = memory.read(PC++);

        /* Handle below all operations which refer to registers or register pairs.
         After that, a large switch statement takes care of all other opcodes */
        if ((OP & 0xCF) == 0x01) {                      /* LXI */
            return MCF_01_LXI(OP);
        } else if ((OP & 0xEF) == 0x0A) {                      /* LDAX */
            return MEF_0A_LDAX(OP);
        } else if ((OP & 0xEF) == 0x02) {                      /* STAX */
            return MEF_02_STAX(OP);
        } else if ((OP & 0xF8) == 0xB8) {                      /* CMP */
            return MF8_B8_CMP(OP);
        } else if ((OP & 0xC7) == 0xC2) {                      /* JMP <condition> */
            return MC7_C2_JMP(OP);
        } else if ((OP & 0xC7) == 0xC4) {                      /* CALL <condition> */
            return MC7_C4_CALL(OP);
        } else if ((OP & 0xC7) == 0xC0) {                      /* RET <condition> */
            return MC7_C0_RET(OP);
        } else if ((OP & 0xC7) == 0xC7) {                      /* RST */
            return MC7_C7_RST(OP);
        } else if ((OP & 0xCF) == 0xC5) {                      /* PUSH */
            return MCF_C5_PUSH(OP);
        } else if ((OP & 0xCF) == 0xC1) {                      /*POP */
            return MCF_C1_POP(OP);
        } else if ((OP & 0xF8) == 0x80) {                      /* ADD */
            return MF8_80_ADD(OP);
        } else if ((OP & 0xF8) == 0x88) {                      /* ADC */
            return MF8_88_ADC(OP);
        } else if ((OP & 0xF8) == 0x90) {                      /* SUB */
            return MF8_90_SUB(OP);
        } else if ((OP & 0xF8) == 0x98) {                      /* SBB */
            return MF8_98_SBB(OP);
        } else if ((OP & 0xC7) == 0x04) {                      /* INR */
            return MC7_04_INR(OP);
        } else if ((OP & 0xC7) == 0x05) {                      /* DCR */
            return MC7_05_DCR(OP);
        } else if ((OP & 0xCF) == 0x03) {                      /* INX */
            return MCF_03_INX(OP);
        } else if ((OP & 0xCF) == 0x0B) {                      /* DCX */
            return MCF_0B_DCX(OP);
        } else if ((OP & 0xCF) == 0x09) {                      /* DAD */
            return MCF_09_DAD(OP);
        } else if ((OP & 0xF8) == 0xA0) {                      /* ANA */
            return MF8_A0_ANA(OP);
        } else if ((OP & 0xF8) == 0xA8) {                      /* XRA */
            return MF8_A8_XRA(OP);
        } else if ((OP & 0xF8) == 0xB0) {                      /* ORA */
            return MF8_B0_ORA(OP);
        }
        /* Dispatch Instruction */
        Method instr = dispatchTable[OP];
        if (instr == null) {
            currentRunState = CPU.RunState.STATE_STOPPED_BAD_INSTR;
            return 0;
        }
        return (Integer)instr.invoke(this, OP);
    }

}
