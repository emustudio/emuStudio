/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package net.sf.emustudio.intel8080.impl;

import emulib.plugins.cpu.CPU;
import emulib.plugins.memory.MemoryContext;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.LockSupport;
import net.sf.emustudio.intel8080.api.CpuEngine;
import net.sf.emustudio.intel8080.api.DispatchListener;
import net.sf.emustudio.intel8080.api.FrequencyChangedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmulatorEngine implements CpuEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmulatorEngine.class);

    public static final int REG_A = 7, REG_B = 0, REG_C = 1, REG_D = 2, REG_E = 3, REG_H = 4, REG_L = 5;
    public static final int FLAG_S = 0x80, FLAG_Z = 0x40, FLAG_AC = 0x10, FLAG_P = 0x4, FLAG_C = 0x1;
    private final static int[] CONDITION = new int[]{
            FLAG_Z, FLAG_Z, FLAG_C, FLAG_C, FLAG_P, FLAG_P, FLAG_S, FLAG_S
    };
    private final static int[] CONDITION_VALUES = new int[]{
            0, FLAG_Z, 0, FLAG_C, 0, FLAG_P, 0, FLAG_S
    };

    public boolean INTE = false; // enabling / disabling of interrupts
    private boolean isINT = false;
    private short b1 = 0; // the raw interrupt instruction
    private short b2 = 0;
    private short b3 = 0;

    public int PC = 0; // program counter
    public int SP = 0; // stack pointer
    public int [] regs = new int[8];
    public short flags = 2; // registers
    public volatile CPU.RunState currentRunState = CPU.RunState.STATE_STOPPED_NORMAL;

    private final MemoryContext<Short> memory;
    private final ContextImpl context;
    private final List<FrequencyChangedListener> frequencyChangedListeners = new CopyOnWriteArrayList<>();

    private final int checkTimeSlice = 100;
    private long executedCycles = 0;

    private volatile DispatchListener dispatchListener;

    public EmulatorEngine(MemoryContext<Short> memory, ContextImpl context) {
        this.memory = memory;
        this.context = context;
    }

    @Override
    public void setDispatchListener(DispatchListener dispatchListener) {
        this.dispatchListener = dispatchListener;
    }

    @Override
    public long getAndResetExecutedCycles() {
        long tmpExecutedCycles = executedCycles;
        executedCycles = 0;
        return tmpExecutedCycles;
    }

    public void addFrequencyChangedListener(FrequencyChangedListener listener) {
        frequencyChangedListeners.add(listener);
    }

    public void removeFrequencyChangedListener(FrequencyChangedListener listener) {
        frequencyChangedListeners.remove(listener);
    }

    @Override
    public void fireFrequencyChanged(float newFrequency) {
        for (FrequencyChangedListener listener : frequencyChangedListeners) {
            listener.frequencyChanged(newFrequency);
        }
    }

    public void reset(int startPos) {
        Arrays.fill(regs, 0);
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
                    if (e.getCause() != null && e.getCause() instanceof IndexOutOfBoundsException) {
                        return CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
                    }
                    return CPU.RunState.STATE_STOPPED_BAD_INSTR;
                } catch (IndexOutOfBoundsException e) {
                    return CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
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
        return currentRunState;
    }


    public void interrupt(short b1, short b2, short b3) {
        if (!INTE) {
            return;
        }
        isINT = true;
        this.b1 = b1;
        this.b2 = b2;
        this.b3 = b3;
    }

    /* Get an 8080 register and return it */
    private int getreg(int reg) {
        if (reg == 6) {
            return memory.read((regs[REG_H] << 8) | regs[REG_L]);
        }
        return regs[reg];
    }

    /* Put a value into an 8080 register from memory */
    private void putreg(int reg, int val) {
        if (reg == 6) {
            memory.write((regs[REG_H] << 8) | regs[REG_L], (short)(val & 0xFF));
        } else {
            regs[reg] = val & 0xFF;
        }
    }

    private void putpair(int reg, int val) {
        if (reg == 3) {
            SP = val;
        } else {
            int index = reg * 2;
            regs[index] = (val >>> 8) & 0xFF;
            regs[index + 1] = val & 0xFF;
        }
    }

    private int getpair(int reg) {
        if (reg == 3) {
            return SP;
        }
        int index = reg * 2;
        return regs[index] << 8 | regs[index + 1];
    }

    /* Return the value of a selected register pair, in PUSH
     format where 3 means regs[REG_A]& flags, not SP
     */
    private int getpush(int reg) {
        switch (reg) {
            case 0:
                return (regs[REG_B] << 8) | regs[REG_C];
            case 1:
                return (regs[REG_D] << 8) | regs[REG_E];
            case 2:
                return (regs[REG_H] << 8) | regs[REG_L];
            case 3:
                return (regs[REG_A] << 8) | (flags & 0xD7) | 2;
        }
        return 0;
    }

    /* Place data into the indicated register pair, in PUSH
     format where 3 means regs[REG_A]& flags, not SP
     */
    private void putpush(int reg, int val) {
        int high, low;
        high = (val >>> 8) & 0xFF;
        low = val & 0xFF;
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
                regs[REG_A] = high;
                flags = (short) (low & 0xD7 | 2);
                break;
        }
    }

    private void auxCarry(int before, int sumWith) {
        int mask = sumWith & before;
        int xormask = sumWith ^ before;

        int C0 = mask & 1;
        int C1 = ((mask >>> 1) ^ (C0 & (xormask >>> 1))) & 1;
        int C2 = ((mask >>> 2) ^ (C1 & (xormask >>> 2))) & 1;
        int C3 = ((mask >>> 3) ^ (C2 & (xormask >>> 3))) & 1;

        if (C3 != 0) {
            flags |= FLAG_AC;
        } else {
            flags &= (~FLAG_AC);
        }
    }

    private int readWord(int address) {
        Short[] read = memory.readWord(address);
        return (read[1] << 8) | read[0];
    }

    private void writeWord(int address, int value) {
        memory.writeWord(address, new Short[] { (short)(value & 0xFF), (short)((value >>> 8) & 0xFF) } );
    }

    private static final Method[] DISPATCH_TABLE = new Method[256];

    static {
        try {
            DISPATCH_TABLE[0] = EmulatorEngine.class.getDeclaredMethod("O0_NOP", short.class);
            DISPATCH_TABLE[6] = EmulatorEngine.class.getDeclaredMethod("MC7_O6_MVI", short.class);
            DISPATCH_TABLE[7] = EmulatorEngine.class.getDeclaredMethod("O7_RLC", short.class);
            DISPATCH_TABLE[14] = EmulatorEngine.class.getDeclaredMethod("MC7_O6_MVI", short.class);
            DISPATCH_TABLE[15] = EmulatorEngine.class.getDeclaredMethod("O15_RRC", short.class);
            DISPATCH_TABLE[22] = EmulatorEngine.class.getDeclaredMethod("MC7_O6_MVI", short.class);
            DISPATCH_TABLE[23] = EmulatorEngine.class.getDeclaredMethod("O23_RAL", short.class);
            DISPATCH_TABLE[30] = EmulatorEngine.class.getDeclaredMethod("MC7_O6_MVI", short.class);
            DISPATCH_TABLE[31] = EmulatorEngine.class.getDeclaredMethod("O31_RAR", short.class);
            DISPATCH_TABLE[34] = EmulatorEngine.class.getDeclaredMethod("O34_SHLD", short.class);
            DISPATCH_TABLE[38] = EmulatorEngine.class.getDeclaredMethod("MC7_O6_MVI", short.class);
            DISPATCH_TABLE[39] = EmulatorEngine.class.getDeclaredMethod("O39_DAA", short.class);
            DISPATCH_TABLE[42] = EmulatorEngine.class.getDeclaredMethod("O42_LHLD", short.class);
            DISPATCH_TABLE[46] = EmulatorEngine.class.getDeclaredMethod("MC7_O6_MVI", short.class);
            DISPATCH_TABLE[47] = EmulatorEngine.class.getDeclaredMethod("O47_CMA", short.class);
            DISPATCH_TABLE[50] = EmulatorEngine.class.getDeclaredMethod("O50_STA", short.class);
            DISPATCH_TABLE[54] = EmulatorEngine.class.getDeclaredMethod("MC7_O6_MVI", short.class);
            DISPATCH_TABLE[55] = EmulatorEngine.class.getDeclaredMethod("O55_STC", short.class);
            DISPATCH_TABLE[58] = EmulatorEngine.class.getDeclaredMethod("O58_LDA", short.class);
            DISPATCH_TABLE[62] = EmulatorEngine.class.getDeclaredMethod("MC7_O6_MVI", short.class);
            DISPATCH_TABLE[63] = EmulatorEngine.class.getDeclaredMethod("O63_CMC", short.class);
            DISPATCH_TABLE[64] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[65] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[66] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[67] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[68] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[69] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[70] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[71] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[72] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[73] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[74] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[75] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[76] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[77] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[78] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[79] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[80] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[81] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[82] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[83] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[84] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[85] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[86] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[87] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[88] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[89] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[90] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[91] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[92] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[93] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[94] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[95] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[96] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[97] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[98] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[99] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[100] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[101] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[102] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[103] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[104] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[105] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[106] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[107] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[108] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[109] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[110] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[111] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[112] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[113] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[114] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[115] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[116] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[117] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[118] = EmulatorEngine.class.getDeclaredMethod("O118_HLT", short.class);
            DISPATCH_TABLE[119] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[120] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[121] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[122] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[123] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[124] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[125] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[126] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[127] = EmulatorEngine.class.getDeclaredMethod("MC0_O40_MOV", short.class);
            DISPATCH_TABLE[195] = EmulatorEngine.class.getDeclaredMethod("O195_JMP", short.class);
            DISPATCH_TABLE[198] = EmulatorEngine.class.getDeclaredMethod("O198_ADI", short.class);
            DISPATCH_TABLE[201] = EmulatorEngine.class.getDeclaredMethod("O201_RET", short.class);
            DISPATCH_TABLE[205] = EmulatorEngine.class.getDeclaredMethod("O205_CALL", short.class);
            DISPATCH_TABLE[206] = EmulatorEngine.class.getDeclaredMethod("O206_ACI", short.class);
            DISPATCH_TABLE[211] = EmulatorEngine.class.getDeclaredMethod("O211_OUT", short.class);
            DISPATCH_TABLE[214] = EmulatorEngine.class.getDeclaredMethod("O214_SUI", short.class);
            DISPATCH_TABLE[219] = EmulatorEngine.class.getDeclaredMethod("O219_IN", short.class);
            DISPATCH_TABLE[222] = EmulatorEngine.class.getDeclaredMethod("O222_SBI", short.class);
            DISPATCH_TABLE[227] = EmulatorEngine.class.getDeclaredMethod("O227_XTHL", short.class);
            DISPATCH_TABLE[230] = EmulatorEngine.class.getDeclaredMethod("O230_ANI", short.class);
            DISPATCH_TABLE[233] = EmulatorEngine.class.getDeclaredMethod("O233_PCHL", short.class);
            DISPATCH_TABLE[235] = EmulatorEngine.class.getDeclaredMethod("O235_XCHG", short.class);
            DISPATCH_TABLE[238] = EmulatorEngine.class.getDeclaredMethod("O238_XRI", short.class);
            DISPATCH_TABLE[243] = EmulatorEngine.class.getDeclaredMethod("O243_DI", short.class);
            DISPATCH_TABLE[246] = EmulatorEngine.class.getDeclaredMethod("O246_ORI", short.class);
            DISPATCH_TABLE[249] = EmulatorEngine.class.getDeclaredMethod("O249_SPHL", short.class);
            DISPATCH_TABLE[251] = EmulatorEngine.class.getDeclaredMethod("O251_EI", short.class);
            DISPATCH_TABLE[254] = EmulatorEngine.class.getDeclaredMethod("O254_CPI", short.class);
        } catch (NoSuchMethodException e) {
            LOGGER.error("Could not set up dispatch table. The emulator won't work correctly", e);
        }
    }

    private int O0_NOP(short OP) {
        return 4;
    }

    private int O7_RLC(short OP) {
        int temp = (regs[REG_A] & 0x80) >>> 7;

        flags &= (~FLAG_C);
        flags |= temp;

        regs[REG_A] = (regs[REG_A] << 1 | temp) & 0xFF;
        return 4;
    }

    private int O15_RRC(short OP) {
        int temp = regs[REG_A] & 0x01;

        flags &= (~FLAG_C);
        flags |= temp;

        regs[REG_A] = (regs[REG_A] >>> 1) | (temp << 7);
        return 4;
    }

    private int O23_RAL(short OP) {
        int temp = regs[REG_A] << 1;
        regs[REG_A] = temp & 0xFF;
        regs[REG_A] |= (flags & FLAG_C);

        flags &= (~FLAG_C);
        flags |= EmulatorTables.CARRY_TABLE[temp];
        return 4;
    }

    private int O31_RAR(short OP) {
        int newCarry = regs[REG_A] & 1;
        regs[REG_A] = regs[REG_A] >>> 1;
        if ((flags & FLAG_C) == FLAG_C) {
            regs[REG_A] |= 0x80;
        }
        flags &= (~FLAG_C);
        flags |= newCarry;

        return 4;
    }

    private int O34_SHLD(short OP) {
        int DAR = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        memory.writeWord(DAR, new Short[] { (short)regs[REG_L] , (short) regs[REG_H] });
        return 16;
    }

    private int O39_DAA(short OP) {
        int temp = regs[REG_A];

        boolean acFlag = (flags & FLAG_AC) == FLAG_AC;
        boolean cFlag = (flags & FLAG_C) == FLAG_C;

        if (acFlag || (temp & 0x0F) > 9) {
            temp += 6;
            auxCarry(regs[REG_A], 6);
            if ((temp & 0x100) == 0x100) {
                flags |= FLAG_C;
            } else {
                flags &= ~FLAG_C;
            }
        }
        if (cFlag || ((temp >>> 4) & 0x0F) > 9) {
            temp += 0x60;
            if ((temp & 0x100) == 0x100) {
                flags |= FLAG_C;
            } else {
                flags &= ~FLAG_C;
            }
        }
        regs[REG_A] = temp & 0xFF;
        flags = (short)(EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]] | (flags & FLAG_C) | (flags & FLAG_AC));
        return 4;
    }

    private int O42_LHLD(short OP) {
        int DAR = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        regs[REG_L] = memory.read(DAR);
        regs[REG_H] = memory.read(DAR + 1);
        return 16;
    }

    private int O47_CMA(short OP) {
        regs[REG_A] = ~regs[REG_A];
        regs[REG_A] &= 0xFF;
        return 4;
    }

    private int O50_STA(short OP) {
        int DAR = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        memory.write(DAR, (short)regs[REG_A]);
        return 13;
    }

    private int O55_STC(short OP) {
        flags |= FLAG_C;
        return 4;
    }

    private int O58_LDA(short OP) {
        int DAR = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
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
        PC = readWord(PC);
        return 10;
    }

    private int O198_ADI(short OP) {
        int DAR = regs[REG_A];
        int diff = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_A] += diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(DAR, diff);

        regs[REG_A] = regs[REG_A] & 0xFF;
        return 7;
    }

    private int O201_RET(short OP) {
        PC = readWord(SP);
        SP = (SP + 2) & 0xFFFF;
        return 10;
    }

    private int O205_CALL(short OP) {
        SP = (SP - 2) & 0xFFFF;
        writeWord(SP, (PC + 2) & 0xFFFF);
        PC = readWord(PC);
        return 17;
    }

    private int O206_ACI(short OP) {
        int X = regs[REG_A];
        int diff = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        if ((flags & FLAG_C) == FLAG_C) {
            diff++;
        }
        regs[REG_A] += diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(X, diff);

        regs[REG_A] = regs[REG_A] & 0xFF;
        return 7;
    }

    private int O211_OUT(short OP) throws IOException {
        int DAR = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        context.fireIO(DAR, false, (short)regs[REG_A]);
        return 10;
    }

    private int O214_SUI(short OP) {
        int DAR = regs[REG_A];
        int diff = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_A] -= diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(DAR, (-diff) & 0xFF);

        regs[REG_A] = regs[REG_A] & 0xFF;
        return 7;
    }

    private int O219_IN(short OP) throws IOException {
        int DAR = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_A] = context.fireIO(DAR, true, (short) 0);
        return 10;
    }

    private int O222_SBI(short OP) {
        int DAR = regs[REG_A];
        int diff = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        if ((flags & FLAG_C) != 0) {
            diff++;
        }
        regs[REG_A] -= diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(DAR, (-diff) & 0xFF);

        regs[REG_A] = regs[REG_A] & 0xFF;
        return 7;
    }

    private int O227_XTHL(short OP) {
        int DAR = readWord(SP);
        writeWord(SP, (regs[REG_H] << 8) | regs[REG_L]);
        regs[REG_H] = (DAR >>> 8) & 0xFF;
        regs[REG_L] = DAR & 0xFF;
        return 18;
    }

    private int O230_ANI(short OP) {
        regs[REG_A] &= (memory.read(PC) & 0xFF);
        PC = (PC + 1) & 0xFFFF;
        flags = EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]];
        return 7;
    }

    private int O233_PCHL(short OP) {
        PC = (regs[REG_H] << 8) | regs[REG_L];
        return 5;
    }

    private int O235_XCHG(short OP) {
        int x = regs[REG_H];
        int y = regs[REG_L];
        regs[REG_H] = regs[REG_D];
        regs[REG_L] = regs[REG_E];
        regs[REG_D] = x;
        regs[REG_E] = y;
        return 4;
    }

    private int O238_XRI(short OP) {
        regs[REG_A] ^= (memory.read(PC) & 0xFF);
        PC = (PC + 1) & 0xFFFF;
        flags = EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]];
        return 7;
    }

    private int O243_DI(short OP) {
        INTE = false;
        return 4;
    }

    private int O246_ORI(short OP) {
        regs[REG_A] |= (memory.read(PC) & 0xFF);
        PC = (PC + 1) & 0xFFFF;
        flags = EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]];
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
        int diff = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        DAR -= diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[DAR & 0x1FF];
        auxCarry(X, (-diff) & 0xFF);

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
        putreg((OP >>> 3) & 0x07, memory.read(PC));
        PC = (PC + 1) & 0xFFFF;
        if (((OP >>> 3) & 0x07) == 6) {
            return 10;
        } else {
            return 7;
        }
    }

    private int MCF_01_LXI(short OP) {
        putpair((OP >>> 4) & 0x03, readWord(PC));
        PC = (PC + 2) & 0xFFFF;
        return 10;
    }

    private int MEF_0A_LDAX(short OP) {
        putreg(7, memory.read(getpair((OP >>> 4) & 0x03)));
        return 7;
    }

    private int MEF_02_STAX(short OP) {
        memory.write(getpair((OP >>> 4) & 0x03), (short)getreg(7));
        return 7;
    }

    private int MF8_B8_CMP(short OP) {
        int X = regs[REG_A];
        int DAR = X & 0xFF;
        int diff = getreg(OP & 0x07);
        DAR -= diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[DAR & 0x1FF];
        auxCarry(X, (-diff) & 0xFF);

        return ((OP & 0x07) == 6) ? 7 : 4;
    }

    private int MC7_C2_JMP(short OP) {
        int index = (OP >>> 3) & 0x07;
        if ((flags & CONDITION[index]) == CONDITION_VALUES[index]) {
            PC = readWord(PC);
        } else {
            PC = (PC + 2) & 0xFFFF;
        }
        return 10;
    }

    private int MC7_C4_CALL(short OP) {
        int index = (OP >>> 3) & 0x07;
        if ((flags & CONDITION[index]) == CONDITION_VALUES[index]) {
            int DAR = readWord(PC);
            SP = (SP - 2) & 0xFFFF;
            writeWord(SP, (PC + 2) & 0xFFFF);
            PC = DAR;
            return 17;
        } else {
            PC = (PC + 2) & 0xFFFF;
            return 11;
        }
    }

    private int MC7_C0_RET(short OP) {
        int index = (OP >>> 3) & 0x07;
        if ((flags & CONDITION[index]) == CONDITION_VALUES[index]) {
            PC = readWord(SP);
            SP = (SP + 2) & 0xFFFF;
        }
        return 10;
    }

    private int MC7_C7_RST(short OP) {
        SP = (SP - 2) & 0xFFFF;
        writeWord(SP, PC);
        PC = OP & 0x38;
        return 11;
    }

    private int MCF_C5_PUSH(short OP) {
        int DAR = getpush((OP >>> 4) & 0x03);
        SP = (SP - 2) & 0xFFFF;
        writeWord(SP, DAR);
        return 11;
    }

    private int MCF_C1_POP(short OP) {
        int DAR = readWord(SP);
        SP = (SP + 2) & 0xFFFF;
        putpush((OP >>> 4) & 0x03, DAR);
        return 10;
    }

    private int MF8_80_ADD(short OP) {
        int X = regs[REG_A];
        int diff = getreg(OP & 0x07);
        regs[REG_A] += diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(X, diff);

        regs[REG_A] = regs[REG_A] & 0xFF;
        return ((OP & 0x07) == 6) ? 7 : 4;
    }

    private int MF8_88_ADC(short OP) {
        int X = regs[REG_A];
        int diff = getreg(OP & 0x07);
        if ((flags & FLAG_C) == FLAG_C) {
            diff++;
        }
        regs[REG_A] += diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(X, diff);

        regs[REG_A] = regs[REG_A] & 0xFF;
        return ((OP & 0x07) == 6) ? 7 : 4;
    }

    private int MF8_90_SUB(short OP) {
        int X = regs[REG_A];
        int diff = getreg(OP & 0x07);
        regs[REG_A] -= diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(X, (-diff) & 0xFF);

        regs[REG_A] = regs[REG_A] & 0xFF;
        return ((OP & 0x07) == 6) ? 7 : 4;
    }

    private int MF8_98_SBB(short OP) {
        int X = regs[REG_A];
        int diff = getreg(OP & 0x07);
        if ((flags & FLAG_C) != 0) {
            diff++;
        }
        regs[REG_A] -= diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(X, (-diff) & 0xFF);

        regs[REG_A] = regs[REG_A] & 0xFF;
        return ((OP & 0x07) == 6) ? 7 : 4;
    }

    private int MC7_04_INR(short OP) {
        int DAR = (getreg((OP >>> 3) & 0x07) + 1) & 0xFF;
        flags = (short) (EmulatorTables.INC_TABLE[DAR] | (flags & FLAG_C));
        putreg((OP >>> 3) & 0x07, DAR);
        return 5;
    }

    private int MC7_05_DCR(short OP) {
        int DAR = (getreg((OP >>> 3) & 0x07) - 1) & 0xFF;
        flags = (short) (EmulatorTables.DEC_TABLE[DAR] | (flags & FLAG_C));
        putreg((OP >>> 3) & 0x07, DAR);
        return 5;
    }

    private int MCF_03_INX(short OP) {
        int DAR = (getpair((OP >>> 4) & 0x03) + 1) & 0xFFFF;
        putpair((OP >>> 4) & 0x03, DAR);
        return 5;
    }

    private int MCF_0B_DCX(short OP) {
        int DAR = (getpair((OP >>> 4) & 0x03) - 1) & 0xFFFF;
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
        flags = EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]];
        return 4;
    }

    private int MF8_A8_XRA(short OP) {
        regs[REG_A] ^= getreg(OP & 0x07);
        flags = EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]];
        return 4;
    }

    private int MF8_B0_ORA(short OP) {
        regs[REG_A] |= getreg(OP & 0x07);
        flags = EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]];
        return 4;
    }

    private int dispatch() throws InvocationTargetException, IllegalAccessException {
        short OP;

        DispatchListener tmpListener = dispatchListener;
        if (tmpListener != null) {
            tmpListener.beforeDispatch();
        }

        /* if interrupt is waiting, instruction won't be read from memory
         * but from one or all of 3 bytes (b1,b2,b3) which represents either
         * rst or call instruction incomed from external peripheral device
         */
        if (isINT) {
            if (INTE) {
                if ((b1 & 0xC7) == 0xC7) {                      /* RST */
                    SP = (SP - 2) & 0xFFFF;
                    writeWord(SP, PC);
                    PC = b1 & 0x38;
                    return 11;
                } else if (b1 == 0315) {                        /* CALL */
                    SP = (SP - 2) & 0xFFFF;
                    writeWord(SP, (PC + 2) & 0xFFFF);
                    PC = ((b3 & 0xFF) << 8) | (b2 & 0xFF);
                    return 17;
                }
            }
            isINT = false;
        }

        OP = 0;
        try {
            OP = memory.read(PC);
        } catch (NullPointerException e) {
            System.out.println("NPE; PC = " + Integer.toHexString(PC));
            currentRunState = CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
            return 0;
        }
        PC = (PC + 1) & 0xFFFF;

        try {
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
            Method instr = DISPATCH_TABLE[OP];
            if (instr == null) {
                currentRunState = CPU.RunState.STATE_STOPPED_BAD_INSTR;
                return 0;
            }
            return (Integer) instr.invoke(this, OP);
        } finally {
            if (tmpListener != null) {
                tmpListener.afterDispatch();
            }
        }
    }

}
