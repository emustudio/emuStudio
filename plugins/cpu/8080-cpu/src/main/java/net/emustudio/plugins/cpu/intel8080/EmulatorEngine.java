/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.cpu.intel8080;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.helpers.SleepUtils;
import net.emustudio.plugins.cpu.intel8080.api.CpuEngine;
import net.emustudio.plugins.cpu.intel8080.api.DispatchListener;
import net.emustudio.plugins.cpu.intel8080.api.FrequencyChangedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
    public int[] regs = new int[8];
    public short flags = 2; // registers
    public volatile CPU.RunState currentRunState = CPU.RunState.STATE_STOPPED_NORMAL;
    private byte lastOpcode;

    private final MemoryContext<Byte> memory;
    private final ContextImpl context;
    private final List<FrequencyChangedListener> frequencyChangedListeners = new CopyOnWriteArrayList<>();

    private long executedCycles = 0;

    private volatile DispatchListener dispatchListener;

    public EmulatorEngine(MemoryContext<Byte> memory, ContextImpl context) {
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
        int checkTimeSlice = 100;
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
                    LOGGER.debug("Unexpected error", e);
                    if (e.getCause() != null && e.getCause() instanceof IndexOutOfBoundsException) {
                        return CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
                    }
                    return CPU.RunState.STATE_STOPPED_BAD_INSTR;
                } catch (IndexOutOfBoundsException e) {
                    LOGGER.debug("Unexpected error", e);
                    return CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
                } catch (Breakpoint e) {
                    return CPU.RunState.STATE_STOPPED_BREAK;
                }
            }
            endTime = System.nanoTime() - startTime;
            if (endTime < slice) {
                // time correction
                SleepUtils.preciseSleepNanos(slice - endTime);
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
            return readByte((regs[REG_H] << 8) | regs[REG_L]);
        }
        return regs[reg];
    }

    /* Put a value into an 8080 register from memory */
    private void putreg(int reg, int val) {
        if (reg == 6) {
            memory.write((regs[REG_H] << 8) | regs[REG_L], (byte) (val & 0xFF));
        } else {
            regs[reg] = val & 0xFF;
        }
    }

    private void putpair(int reg, int val) {
        if (reg == 3) {
            SP = val & 0xFFFF;
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

        System.out.println("REG: " + index);
        System.out.println("REG: " + regs[index]);
        System.out.println("REG: " + regs[index + 1]);

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

    private int readByte(int address) {
        return memory.read(address) & 0xFF;
    }

    private int readWord(int address) {
        Byte[] read = memory.read(address, 2);
        return ((read[1] << 8) | (read[0] & 0xFF)) & 0xFFFF;
    }

    private void writeWord(int address, int value) {
        memory.write(address, new Byte[]{(byte) (value & 0xFF), (byte) ((value >>> 8) & 0xFF)}, 2);
    }

    private static final Method[] DISPATCH_TABLE = new Method[256];

    static {
        try {
            DISPATCH_TABLE[0x00] = EmulatorEngine.class.getDeclaredMethod("I_NOP");
            DISPATCH_TABLE[0x01] = EmulatorEngine.class.getDeclaredMethod("I_LXI");
            DISPATCH_TABLE[0x02] = EmulatorEngine.class.getDeclaredMethod("I_STAX");
            DISPATCH_TABLE[0x03] = EmulatorEngine.class.getDeclaredMethod("I_INX");
            DISPATCH_TABLE[0x04] = EmulatorEngine.class.getDeclaredMethod("I_INR");
            DISPATCH_TABLE[0x05] = EmulatorEngine.class.getDeclaredMethod("I_DCR");
            DISPATCH_TABLE[0x06] = EmulatorEngine.class.getDeclaredMethod("I_MVI");
            DISPATCH_TABLE[0x07] = EmulatorEngine.class.getDeclaredMethod("I_RLC");
            DISPATCH_TABLE[0x09] = EmulatorEngine.class.getDeclaredMethod("I_DAD");
            DISPATCH_TABLE[0x0A] = EmulatorEngine.class.getDeclaredMethod("I_LDAX");
            DISPATCH_TABLE[0x0B] = EmulatorEngine.class.getDeclaredMethod("I_DCX");
            DISPATCH_TABLE[0x0C] = EmulatorEngine.class.getDeclaredMethod("I_INR");
            DISPATCH_TABLE[0x0D] = EmulatorEngine.class.getDeclaredMethod("I_DCR");
            DISPATCH_TABLE[0x0E] = EmulatorEngine.class.getDeclaredMethod("I_MVI");
            DISPATCH_TABLE[0x0F] = EmulatorEngine.class.getDeclaredMethod("I_RRC");
            DISPATCH_TABLE[0x11] = EmulatorEngine.class.getDeclaredMethod("I_LXI");
            DISPATCH_TABLE[0x12] = EmulatorEngine.class.getDeclaredMethod("I_STAX");
            DISPATCH_TABLE[0x13] = EmulatorEngine.class.getDeclaredMethod("I_INX");
            DISPATCH_TABLE[0x14] = EmulatorEngine.class.getDeclaredMethod("I_INR");
            DISPATCH_TABLE[0x15] = EmulatorEngine.class.getDeclaredMethod("I_DCR");
            DISPATCH_TABLE[0x16] = EmulatorEngine.class.getDeclaredMethod("I_MVI");
            DISPATCH_TABLE[0x17] = EmulatorEngine.class.getDeclaredMethod("I_RAL");
            DISPATCH_TABLE[0x19] = EmulatorEngine.class.getDeclaredMethod("I_DAD");
            DISPATCH_TABLE[0x1A] = EmulatorEngine.class.getDeclaredMethod("I_LDAX");
            DISPATCH_TABLE[0x1B] = EmulatorEngine.class.getDeclaredMethod("I_DCX");
            DISPATCH_TABLE[0x1C] = EmulatorEngine.class.getDeclaredMethod("I_INR");
            DISPATCH_TABLE[0x1D] = EmulatorEngine.class.getDeclaredMethod("I_DCR");
            DISPATCH_TABLE[0x1E] = EmulatorEngine.class.getDeclaredMethod("I_MVI");
            DISPATCH_TABLE[0x1F] = EmulatorEngine.class.getDeclaredMethod("I_RAR");
            DISPATCH_TABLE[0x21] = EmulatorEngine.class.getDeclaredMethod("I_LXI");
            DISPATCH_TABLE[0x22] = EmulatorEngine.class.getDeclaredMethod("I_SHLD");
            DISPATCH_TABLE[0x23] = EmulatorEngine.class.getDeclaredMethod("I_INX");
            DISPATCH_TABLE[0x24] = EmulatorEngine.class.getDeclaredMethod("I_INR");
            DISPATCH_TABLE[0x25] = EmulatorEngine.class.getDeclaredMethod("I_DCR");
            DISPATCH_TABLE[0x26] = EmulatorEngine.class.getDeclaredMethod("I_MVI");
            DISPATCH_TABLE[0x27] = EmulatorEngine.class.getDeclaredMethod("I_DAA");
            DISPATCH_TABLE[0x29] = EmulatorEngine.class.getDeclaredMethod("I_DAD");
            DISPATCH_TABLE[0x2A] = EmulatorEngine.class.getDeclaredMethod("I_LHLD");
            DISPATCH_TABLE[0x2B] = EmulatorEngine.class.getDeclaredMethod("I_DCX");
            DISPATCH_TABLE[0x2C] = EmulatorEngine.class.getDeclaredMethod("I_INR");
            DISPATCH_TABLE[0x2D] = EmulatorEngine.class.getDeclaredMethod("I_DCR");
            DISPATCH_TABLE[0x2E] = EmulatorEngine.class.getDeclaredMethod("I_MVI");
            DISPATCH_TABLE[0x2F] = EmulatorEngine.class.getDeclaredMethod("I_CMA");
            DISPATCH_TABLE[0x31] = EmulatorEngine.class.getDeclaredMethod("I_LXI");
            DISPATCH_TABLE[0x32] = EmulatorEngine.class.getDeclaredMethod("I_STA");
            DISPATCH_TABLE[0x33] = EmulatorEngine.class.getDeclaredMethod("I_INX");
            DISPATCH_TABLE[0x34] = EmulatorEngine.class.getDeclaredMethod("I_INR");
            DISPATCH_TABLE[0x35] = EmulatorEngine.class.getDeclaredMethod("I_DCR");
            DISPATCH_TABLE[0x36] = EmulatorEngine.class.getDeclaredMethod("I_MVI");
            DISPATCH_TABLE[0x37] = EmulatorEngine.class.getDeclaredMethod("I_STC");
            DISPATCH_TABLE[0x39] = EmulatorEngine.class.getDeclaredMethod("I_DAD");
            DISPATCH_TABLE[0x3A] = EmulatorEngine.class.getDeclaredMethod("I_LDA");
            DISPATCH_TABLE[0x3B] = EmulatorEngine.class.getDeclaredMethod("I_DCX");
            DISPATCH_TABLE[0x3C] = EmulatorEngine.class.getDeclaredMethod("I_INR");
            DISPATCH_TABLE[0x3D] = EmulatorEngine.class.getDeclaredMethod("I_DCR");
            DISPATCH_TABLE[0x3E] = EmulatorEngine.class.getDeclaredMethod("I_MVI");
            DISPATCH_TABLE[0x3F] = EmulatorEngine.class.getDeclaredMethod("I_CMC");
            DISPATCH_TABLE[0x40] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x41] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x42] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x43] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x44] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x45] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x46] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x47] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x48] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x49] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x4A] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x4B] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x4C] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x4D] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x4E] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x4F] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x50] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x51] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x52] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x53] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x54] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x55] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x56] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x57] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x58] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x59] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x5A] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x5B] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x5C] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x5D] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x5E] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x5F] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x60] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x61] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x62] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x63] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x64] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x65] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x66] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x67] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x68] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x69] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x6A] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x6B] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x6C] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x6D] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x6E] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x6F] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x70] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x71] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x72] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x73] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x74] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x75] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x76] = EmulatorEngine.class.getDeclaredMethod("I_HLT");
            DISPATCH_TABLE[0x77] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x78] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x79] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x7A] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x7B] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x7C] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x7D] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x7E] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x7F] = EmulatorEngine.class.getDeclaredMethod("I_MOV");
            DISPATCH_TABLE[0x80] = EmulatorEngine.class.getDeclaredMethod("I_ADD");
            DISPATCH_TABLE[0x81] = EmulatorEngine.class.getDeclaredMethod("I_ADD");
            DISPATCH_TABLE[0x82] = EmulatorEngine.class.getDeclaredMethod("I_ADD");
            DISPATCH_TABLE[0x83] = EmulatorEngine.class.getDeclaredMethod("I_ADD");
            DISPATCH_TABLE[0x84] = EmulatorEngine.class.getDeclaredMethod("I_ADD");
            DISPATCH_TABLE[0x85] = EmulatorEngine.class.getDeclaredMethod("I_ADD");
            DISPATCH_TABLE[0x86] = EmulatorEngine.class.getDeclaredMethod("I_ADD");
            DISPATCH_TABLE[0x87] = EmulatorEngine.class.getDeclaredMethod("I_ADD");
            DISPATCH_TABLE[0x88] = EmulatorEngine.class.getDeclaredMethod("I_ADC");
            DISPATCH_TABLE[0x89] = EmulatorEngine.class.getDeclaredMethod("I_ADC");
            DISPATCH_TABLE[0x8A] = EmulatorEngine.class.getDeclaredMethod("I_ADC");
            DISPATCH_TABLE[0x8B] = EmulatorEngine.class.getDeclaredMethod("I_ADC");
            DISPATCH_TABLE[0x8C] = EmulatorEngine.class.getDeclaredMethod("I_ADC");
            DISPATCH_TABLE[0x8D] = EmulatorEngine.class.getDeclaredMethod("I_ADC");
            DISPATCH_TABLE[0x8E] = EmulatorEngine.class.getDeclaredMethod("I_ADC");
            DISPATCH_TABLE[0x8F] = EmulatorEngine.class.getDeclaredMethod("I_ADC");
            DISPATCH_TABLE[0x90] = EmulatorEngine.class.getDeclaredMethod("I_SUB");
            DISPATCH_TABLE[0x91] = EmulatorEngine.class.getDeclaredMethod("I_SUB");
            DISPATCH_TABLE[0x92] = EmulatorEngine.class.getDeclaredMethod("I_SUB");
            DISPATCH_TABLE[0x93] = EmulatorEngine.class.getDeclaredMethod("I_SUB");
            DISPATCH_TABLE[0x94] = EmulatorEngine.class.getDeclaredMethod("I_SUB");
            DISPATCH_TABLE[0x95] = EmulatorEngine.class.getDeclaredMethod("I_SUB");
            DISPATCH_TABLE[0x96] = EmulatorEngine.class.getDeclaredMethod("I_SUB");
            DISPATCH_TABLE[0x97] = EmulatorEngine.class.getDeclaredMethod("I_SUB");
            DISPATCH_TABLE[0x98] = EmulatorEngine.class.getDeclaredMethod("I_SBB");
            DISPATCH_TABLE[0x99] = EmulatorEngine.class.getDeclaredMethod("I_SBB");
            DISPATCH_TABLE[0x9A] = EmulatorEngine.class.getDeclaredMethod("I_SBB");
            DISPATCH_TABLE[0x9B] = EmulatorEngine.class.getDeclaredMethod("I_SBB");
            DISPATCH_TABLE[0x9C] = EmulatorEngine.class.getDeclaredMethod("I_SBB");
            DISPATCH_TABLE[0x9D] = EmulatorEngine.class.getDeclaredMethod("I_SBB");
            DISPATCH_TABLE[0x9E] = EmulatorEngine.class.getDeclaredMethod("I_SBB");
            DISPATCH_TABLE[0x9F] = EmulatorEngine.class.getDeclaredMethod("I_SBB");
            DISPATCH_TABLE[0xA0] = EmulatorEngine.class.getDeclaredMethod("I_ANA");
            DISPATCH_TABLE[0xA1] = EmulatorEngine.class.getDeclaredMethod("I_ANA");
            DISPATCH_TABLE[0xA2] = EmulatorEngine.class.getDeclaredMethod("I_ANA");
            DISPATCH_TABLE[0xA3] = EmulatorEngine.class.getDeclaredMethod("I_ANA");
            DISPATCH_TABLE[0xA4] = EmulatorEngine.class.getDeclaredMethod("I_ANA");
            DISPATCH_TABLE[0xA5] = EmulatorEngine.class.getDeclaredMethod("I_ANA");
            DISPATCH_TABLE[0xA6] = EmulatorEngine.class.getDeclaredMethod("I_ANA");
            DISPATCH_TABLE[0xA7] = EmulatorEngine.class.getDeclaredMethod("I_ANA");
            DISPATCH_TABLE[0xA8] = EmulatorEngine.class.getDeclaredMethod("I_XRA");
            DISPATCH_TABLE[0xA9] = EmulatorEngine.class.getDeclaredMethod("I_XRA");
            DISPATCH_TABLE[0xAA] = EmulatorEngine.class.getDeclaredMethod("I_XRA");
            DISPATCH_TABLE[0xAB] = EmulatorEngine.class.getDeclaredMethod("I_XRA");
            DISPATCH_TABLE[0xAC] = EmulatorEngine.class.getDeclaredMethod("I_XRA");
            DISPATCH_TABLE[0xAD] = EmulatorEngine.class.getDeclaredMethod("I_XRA");
            DISPATCH_TABLE[0xAE] = EmulatorEngine.class.getDeclaredMethod("I_XRA");
            DISPATCH_TABLE[0xAF] = EmulatorEngine.class.getDeclaredMethod("I_XRA");
            DISPATCH_TABLE[0xB0] = EmulatorEngine.class.getDeclaredMethod("I_ORA");
            DISPATCH_TABLE[0xB1] = EmulatorEngine.class.getDeclaredMethod("I_ORA");
            DISPATCH_TABLE[0xB2] = EmulatorEngine.class.getDeclaredMethod("I_ORA");
            DISPATCH_TABLE[0xB3] = EmulatorEngine.class.getDeclaredMethod("I_ORA");
            DISPATCH_TABLE[0xB4] = EmulatorEngine.class.getDeclaredMethod("I_ORA");
            DISPATCH_TABLE[0xB5] = EmulatorEngine.class.getDeclaredMethod("I_ORA");
            DISPATCH_TABLE[0xB6] = EmulatorEngine.class.getDeclaredMethod("I_ORA");
            DISPATCH_TABLE[0xB7] = EmulatorEngine.class.getDeclaredMethod("I_ORA");
            DISPATCH_TABLE[0xB8] = EmulatorEngine.class.getDeclaredMethod("I_CMP");
            DISPATCH_TABLE[0xB9] = EmulatorEngine.class.getDeclaredMethod("I_CMP");
            DISPATCH_TABLE[0xBA] = EmulatorEngine.class.getDeclaredMethod("I_CMP");
            DISPATCH_TABLE[0xBB] = EmulatorEngine.class.getDeclaredMethod("I_CMP");
            DISPATCH_TABLE[0xBC] = EmulatorEngine.class.getDeclaredMethod("I_CMP");
            DISPATCH_TABLE[0xBD] = EmulatorEngine.class.getDeclaredMethod("I_CMP");
            DISPATCH_TABLE[0xBE] = EmulatorEngine.class.getDeclaredMethod("I_CMP");
            DISPATCH_TABLE[0xBF] = EmulatorEngine.class.getDeclaredMethod("I_CMP");
            DISPATCH_TABLE[0xC0] = EmulatorEngine.class.getDeclaredMethod("I_RET_COND"); // RNZ
            DISPATCH_TABLE[0xC1] = EmulatorEngine.class.getDeclaredMethod("I_POP");
            DISPATCH_TABLE[0xC2] = EmulatorEngine.class.getDeclaredMethod("I_JMP_COND"); // JNZ
            DISPATCH_TABLE[0xC3] = EmulatorEngine.class.getDeclaredMethod("I_JMP");
            DISPATCH_TABLE[0xC4] = EmulatorEngine.class.getDeclaredMethod("I_CALL_COND"); // CNZ
            DISPATCH_TABLE[0xC5] = EmulatorEngine.class.getDeclaredMethod("I_PUSH");
            DISPATCH_TABLE[0xC6] = EmulatorEngine.class.getDeclaredMethod("I_ADI");
            DISPATCH_TABLE[0xC7] = EmulatorEngine.class.getDeclaredMethod("I_RST");
            DISPATCH_TABLE[0xC8] = EmulatorEngine.class.getDeclaredMethod("I_RET_COND"); // RZ
            DISPATCH_TABLE[0xC9] = EmulatorEngine.class.getDeclaredMethod("I_RET");
            DISPATCH_TABLE[0xCA] = EmulatorEngine.class.getDeclaredMethod("I_JMP_COND"); // JZ
            DISPATCH_TABLE[0xCC] = EmulatorEngine.class.getDeclaredMethod("I_CALL_COND"); // CZ
            DISPATCH_TABLE[0xCD] = EmulatorEngine.class.getDeclaredMethod("I_CALL");
            DISPATCH_TABLE[0xCE] = EmulatorEngine.class.getDeclaredMethod("I_ACI");
            DISPATCH_TABLE[0xCF] = EmulatorEngine.class.getDeclaredMethod("I_RST");
            DISPATCH_TABLE[0xD0] = EmulatorEngine.class.getDeclaredMethod("I_RET_COND"); // RNC
            DISPATCH_TABLE[0xD1] = EmulatorEngine.class.getDeclaredMethod("I_POP");
            DISPATCH_TABLE[0xD2] = EmulatorEngine.class.getDeclaredMethod("I_JMP_COND"); // JNC
            DISPATCH_TABLE[0xD3] = EmulatorEngine.class.getDeclaredMethod("I_OUT");
            DISPATCH_TABLE[0xD4] = EmulatorEngine.class.getDeclaredMethod("I_CALL_COND"); // CNC
            DISPATCH_TABLE[0xD5] = EmulatorEngine.class.getDeclaredMethod("I_PUSH");
            DISPATCH_TABLE[0xD6] = EmulatorEngine.class.getDeclaredMethod("I_SUI");
            DISPATCH_TABLE[0xD7] = EmulatorEngine.class.getDeclaredMethod("I_RST");
            DISPATCH_TABLE[0xD8] = EmulatorEngine.class.getDeclaredMethod("I_RET_COND"); // RC
            DISPATCH_TABLE[0xDA] = EmulatorEngine.class.getDeclaredMethod("I_JMP_COND"); // JC
            DISPATCH_TABLE[0xDB] = EmulatorEngine.class.getDeclaredMethod("I_IN");
            DISPATCH_TABLE[0xDC] = EmulatorEngine.class.getDeclaredMethod("I_CALL_COND"); // CC
            DISPATCH_TABLE[0xDE] = EmulatorEngine.class.getDeclaredMethod("I_SBI");
            DISPATCH_TABLE[0xDF] = EmulatorEngine.class.getDeclaredMethod("I_RST");
            DISPATCH_TABLE[0xE0] = EmulatorEngine.class.getDeclaredMethod("I_RET_COND"); // RPO
            DISPATCH_TABLE[0xE1] = EmulatorEngine.class.getDeclaredMethod("I_POP");
            DISPATCH_TABLE[0xE2] = EmulatorEngine.class.getDeclaredMethod("I_JMP_COND"); // JPO
            DISPATCH_TABLE[0xE3] = EmulatorEngine.class.getDeclaredMethod("I_XTHL");
            DISPATCH_TABLE[0xE4] = EmulatorEngine.class.getDeclaredMethod("I_CALL_COND"); // CPO
            DISPATCH_TABLE[0xE5] = EmulatorEngine.class.getDeclaredMethod("I_PUSH");
            DISPATCH_TABLE[0xE6] = EmulatorEngine.class.getDeclaredMethod("I_ANI");
            DISPATCH_TABLE[0xE7] = EmulatorEngine.class.getDeclaredMethod("I_RST");
            DISPATCH_TABLE[0xE8] = EmulatorEngine.class.getDeclaredMethod("I_RET_COND"); // RPE
            DISPATCH_TABLE[0xE9] = EmulatorEngine.class.getDeclaredMethod("I_PCHL");
            DISPATCH_TABLE[0xEA] = EmulatorEngine.class.getDeclaredMethod("I_JMP_COND"); // JPE
            DISPATCH_TABLE[0xEB] = EmulatorEngine.class.getDeclaredMethod("I_XCHG");
            DISPATCH_TABLE[0xEC] = EmulatorEngine.class.getDeclaredMethod("I_CALL_COND"); // CPE
            DISPATCH_TABLE[0xEE] = EmulatorEngine.class.getDeclaredMethod("I_XRI");
            DISPATCH_TABLE[0xEF] = EmulatorEngine.class.getDeclaredMethod("I_RST");
            DISPATCH_TABLE[0xF0] = EmulatorEngine.class.getDeclaredMethod("I_RET_COND"); // RP
            DISPATCH_TABLE[0xF1] = EmulatorEngine.class.getDeclaredMethod("I_POP");
            DISPATCH_TABLE[0xF2] = EmulatorEngine.class.getDeclaredMethod("I_JMP_COND"); // JP
            DISPATCH_TABLE[0xF3] = EmulatorEngine.class.getDeclaredMethod("I_DI");
            DISPATCH_TABLE[0xF4] = EmulatorEngine.class.getDeclaredMethod("I_CALL_COND"); // CP
            DISPATCH_TABLE[0xF5] = EmulatorEngine.class.getDeclaredMethod("I_PUSH");
            DISPATCH_TABLE[0xF6] = EmulatorEngine.class.getDeclaredMethod("I_ORI");
            DISPATCH_TABLE[0xF7] = EmulatorEngine.class.getDeclaredMethod("I_RST");
            DISPATCH_TABLE[0xF8] = EmulatorEngine.class.getDeclaredMethod("I_RET_COND"); // RM
            DISPATCH_TABLE[0xF9] = EmulatorEngine.class.getDeclaredMethod("I_SPHL");
            DISPATCH_TABLE[0xFA] = EmulatorEngine.class.getDeclaredMethod("I_JMP_COND"); // JM
            DISPATCH_TABLE[0xFB] = EmulatorEngine.class.getDeclaredMethod("I_EI");
            DISPATCH_TABLE[0xFC] = EmulatorEngine.class.getDeclaredMethod("I_CALL_COND"); // CM
            DISPATCH_TABLE[0xFE] = EmulatorEngine.class.getDeclaredMethod("I_CPI");
            DISPATCH_TABLE[0xFF] = EmulatorEngine.class.getDeclaredMethod("I_RST");
        } catch (NoSuchMethodException e) {
            LOGGER.error("Could not set up dispatch table. The emulator won't work correctly", e);
        }
    }

    private int I_NOP() {
        return 4;
    }

    private int I_RLC() {
        int temp = (regs[REG_A] & 0x80) >>> 7;

        flags &= (~FLAG_C);
        flags |= temp;

        regs[REG_A] = (regs[REG_A] << 1 | temp) & 0xFF;
        return 4;
    }

    private int I_RRC() {
        int temp = regs[REG_A] & 0x01;

        flags &= (~FLAG_C);
        flags |= temp;

        regs[REG_A] = ((regs[REG_A] >>> 1) | (temp << 7)) & 0xFF;
        return 4;
    }

    private int I_RAL() {
        int temp = regs[REG_A] << 1;
        regs[REG_A] = temp & 0xFF;
        regs[REG_A] |= (flags & FLAG_C);

        flags &= (~FLAG_C);
        flags |= EmulatorTables.CARRY_TABLE[temp];
        return 4;
    }

    private int I_RAR() {
        int newCarry = regs[REG_A] & 1;
        regs[REG_A] = regs[REG_A] >>> 1;
        if ((flags & FLAG_C) == FLAG_C) {
            regs[REG_A] |= 0x80;
        }
        flags &= (~FLAG_C);
        flags |= newCarry;

        return 4;
    }

    private int I_SHLD() {
        int DAR = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        memory.write(DAR, new Byte[]{(byte) regs[REG_L], (byte) regs[REG_H]}, 2);
        return 16;
    }

    private int I_DAA() {
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
        flags = (short) (EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]] | (flags & FLAG_C) | (flags & FLAG_AC));
        return 4;
    }

    private int I_LHLD() {
        int DAR = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        regs[REG_L] = readByte(DAR);
        regs[REG_H] = readByte(DAR + 1);
        return 16;
    }

    private int I_CMA() {
        regs[REG_A] = ~regs[REG_A];
        regs[REG_A] &= 0xFF;
        return 4;
    }

    private int I_STA() {
        int DAR = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        memory.write(DAR, (byte) regs[REG_A]);
        return 13;
    }

    private int I_STC() {
        flags |= FLAG_C;
        return 4;
    }

    private int I_LDA() {
        int DAR = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        regs[REG_A] = readByte(DAR);
        return 13;
    }

    private int I_CMC() {
        if ((flags & FLAG_C) != 0) {
            flags &= (~FLAG_C);
        } else {
            flags |= FLAG_C;
        }
        return 4;
    }

    private int I_HLT() {
        currentRunState = CPU.RunState.STATE_STOPPED_NORMAL;
        return 7;
    }

    private int I_JMP() {
        PC = readWord(PC);
        return 10;
    }

    private int I_ADI() {
        int DAR = regs[REG_A];
        int diff = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_A] += diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(DAR, diff);

        regs[REG_A] &= 0xFF;
        return 7;
    }

    private int I_RET() {
        PC = readWord(SP);
        SP = (SP + 2) & 0xFFFF;
        return 10;
    }

    private int I_CALL() {
        SP = (SP - 2) & 0xFFFF;
        writeWord(SP, (PC + 2) & 0xFFFF);
        PC = readWord(PC);
        return 17;
    }

    private int I_ACI() {
        int X = regs[REG_A];
        int diff = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        if ((flags & FLAG_C) == FLAG_C) {
            diff++;
        }
        regs[REG_A] += diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(X, diff);

        regs[REG_A] &= 0xFF;
        return 7;
    }

    private int I_OUT() throws IOException {
        int DAR = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        context.fireIO(DAR, false, (byte) regs[REG_A]);
        return 10;
    }

    private int I_SUI() {
        int DAR = regs[REG_A];
        int diff = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_A] -= diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(DAR, (-diff) & 0xFF);

        regs[REG_A] &= 0xFF;
        return 7;
    }

    private int I_IN() throws IOException {
        int DAR = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_A] = context.fireIO(DAR, true, (byte) 0) & 0xFF;
        return 10;
    }

    private int I_SBI() {
        int DAR = regs[REG_A];
        int diff = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        if ((flags & FLAG_C) != 0) {
            diff++;
        }
        regs[REG_A] -= diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(DAR, (-diff) & 0xFF);

        regs[REG_A] &= 0xFF;
        return 7;
    }

    private int I_XTHL() {
        int DAR = readWord(SP);
        writeWord(SP, (regs[REG_H] << 8) | regs[REG_L]);
        regs[REG_H] = (DAR >>> 8) & 0xFF;
        regs[REG_L] = DAR & 0xFF;
        return 18;
    }

    private int I_ANI() {
        regs[REG_A] &= readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        flags = EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]];
        return 7;
    }

    private int I_PCHL() {
        PC = (regs[REG_H] << 8) | regs[REG_L];
        return 5;
    }

    private int I_XCHG() {
        int x = regs[REG_H];
        int y = regs[REG_L];
        regs[REG_H] = regs[REG_D];
        regs[REG_L] = regs[REG_E];
        regs[REG_D] = x;
        regs[REG_E] = y;
        return 4;
    }

    private int I_XRI() {
        regs[REG_A] ^= readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        flags = EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]];
        return 7;
    }

    private int I_DI() {
        INTE = false;
        return 4;
    }

    private int I_ORI() {
        regs[REG_A] |= readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        flags = EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]];
        return 7;
    }

    private int I_SPHL() {
        SP = ((regs[REG_H] << 8) | regs[REG_L]) & 0xFFFF;
        return 5;
    }

    private int I_EI() {
        INTE = true;
        return 4;
    }

    private int I_CPI() {
        int X = regs[REG_A];
        int DAR = regs[REG_A] & 0xFF;
        int diff = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        DAR -= diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[DAR & 0x1FF];
        auxCarry(X, (-diff) & 0xFF);

        return 7;
    }

    private int I_MOV() {
        putreg((lastOpcode >>> 3) & 0x07, getreg(lastOpcode & 0x07));
        if (((lastOpcode & 0x07) == 6) || (((lastOpcode >>> 3) & 0x07) == 6)) {
            return 7;
        } else {
            return 5;
        }
    }

    private int I_MVI() {
        putreg((lastOpcode >>> 3) & 0x07, readByte(PC));
        PC = (PC + 1) & 0xFFFF;
        if (((lastOpcode >>> 3) & 0x07) == 6) {
            return 10;
        } else {
            return 7;
        }
    }

    private int I_LXI() {
        putpair((lastOpcode >>> 4) & 0x03, readWord(PC));
        PC = (PC + 2) & 0xFFFF;
        return 10;
    }

    private int I_LDAX() {
        int address = getpair((lastOpcode >>> 4) & 0x03);
        System.out.println(address);
        putreg(7, readByte(address));
        return 7;
    }

    private int I_STAX() {
        memory.write(getpair((lastOpcode >>> 4) & 0x03), (byte) getreg(7));
        return 7;
    }

    private int I_CMP() {
        int X = regs[REG_A];
        int DAR = X & 0xFF;
        int diff = getreg(lastOpcode & 0x07);
        DAR -= diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[DAR & 0x1FF];
        auxCarry(X, (-diff) & 0xFF);

        return ((lastOpcode & 0x07) == 6) ? 7 : 4;
    }

    private int I_JMP_COND() {
        int index = (lastOpcode >>> 3) & 0x07;
        if ((flags & CONDITION[index]) == CONDITION_VALUES[index]) {
            PC = readWord(PC);
        } else {
            PC = (PC + 2) & 0xFFFF;
        }
        return 10;
    }

    private int I_CALL_COND() {
        int index = (lastOpcode >>> 3) & 0x07;
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

    private int I_RET_COND() {
        int index = (lastOpcode >>> 3) & 0x07;
        if ((flags & CONDITION[index]) == CONDITION_VALUES[index]) {
            PC = readWord(SP);
            SP = (SP + 2) & 0xFFFF;
        }
        return 10;
    }

    private int I_RST() {
        SP = (SP - 2) & 0xFFFF;
        writeWord(SP, PC);
        PC = lastOpcode & 0x38;
        return 11;
    }

    private int I_PUSH() {
        int DAR = getpush((lastOpcode >>> 4) & 0x03);
        SP = (SP - 2) & 0xFFFF;
        writeWord(SP, DAR);
        return 11;
    }

    private int I_POP() {
        int DAR = readWord(SP);
        SP = (SP + 2) & 0xFFFF;
        putpush((lastOpcode >>> 4) & 0x03, DAR);
        return 10;
    }

    private int I_ADD() {
        int X = regs[REG_A];
        int diff = getreg(lastOpcode & 0x07);
        regs[REG_A] += diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(X, diff);

        regs[REG_A] &= 0xFF;
        return ((lastOpcode & 0x07) == 6) ? 7 : 4;
    }

    private int I_ADC() {
        int X = regs[REG_A];
        int diff = getreg(lastOpcode & 0x07);
        if ((flags & FLAG_C) == FLAG_C) {
            diff++;
        }
        regs[REG_A] += diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(X, diff);

        regs[REG_A] &= 0xFF;
        return ((lastOpcode & 0x07) == 6) ? 7 : 4;
    }

    private int I_SUB() {
        int X = regs[REG_A];
        int diff = getreg(lastOpcode & 0x07);
        regs[REG_A] -= diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(X, (-diff) & 0xFF);

        regs[REG_A] &= 0xFF;
        return ((lastOpcode & 0x07) == 6) ? 7 : 4;
    }

    private int I_SBB() {
        int X = regs[REG_A];
        int diff = getreg(lastOpcode & 0x07);
        if ((flags & FLAG_C) != 0) {
            diff++;
        }
        regs[REG_A] -= diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(X, (-diff) & 0xFF);

        regs[REG_A] &= 0xFF;
        return ((lastOpcode & 0x07) == 6) ? 7 : 4;
    }

    private int I_INR() {
        int DAR = (getreg((lastOpcode >>> 3) & 0x07) + 1) & 0xFF;
        flags = (short) (EmulatorTables.INC_TABLE[DAR] | (flags & FLAG_C));
        putreg((lastOpcode >>> 3) & 0x07, DAR);
        return 5;
    }

    private int I_DCR() {
        int DAR = (getreg((lastOpcode >>> 3) & 0x07) - 1) & 0xFF;
        flags = (short) (EmulatorTables.DEC_TABLE[DAR] | (flags & FLAG_C));
        putreg((lastOpcode >>> 3) & 0x07, DAR);
        return 5;
    }

    private int I_INX() {
        int DAR = (getpair((lastOpcode >>> 4) & 0x03) + 1) & 0xFFFF;
        putpair((lastOpcode >>> 4) & 0x03, DAR);
        return 5;
    }

    private int I_DCX() {
        int DAR = (getpair((lastOpcode >>> 4) & 0x03) - 1) & 0xFFFF;
        putpair((lastOpcode >>> 4) & 0x03, DAR);
        return 5;
    }

    private int I_DAD() {
        int DAR = getpair((lastOpcode >>> 4) & 0x03);
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

    private int I_ANA() {
        regs[REG_A] &= getreg(lastOpcode & 0x07);
        flags = EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]];
        return 4;
    }

    private int I_XRA() {
        regs[REG_A] ^= getreg(lastOpcode & 0x07);
        flags = EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]];
        return 4;
    }

    private int I_ORA() {
        regs[REG_A] |= getreg(lastOpcode & 0x07);
        flags = EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]];
        return 4;
    }

    private int dispatch() throws InvocationTargetException, IllegalAccessException {
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
                } else if (b1 == 0xCD) {                        /* CALL */
                    SP = (SP - 2) & 0xFFFF;
                    writeWord(SP, (PC + 2) & 0xFFFF);
                    PC = ((b3 & 0xFF) << 8) | (b2 & 0xFF);
                    return 17;
                }
            }
            isINT = false;
        }

        lastOpcode = 0;
        try {
            lastOpcode = (byte)readByte(PC);
        } catch (NullPointerException e) {
            LOGGER.error("NPE; PC=" + Integer.toHexString(PC), e);
            currentRunState = CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
            return 0;
        }
        PC = (PC + 1) & 0xFFFF;

        try {
            Method instr = DISPATCH_TABLE[lastOpcode & 0xFF];
            if (instr == null) {
                currentRunState = CPU.RunState.STATE_STOPPED_BAD_INSTR;
                return 0;
            }
            return (Integer) instr.invoke(this);
        } finally {
            if (tmpListener != null) {
                tmpListener.afterDispatch();
            }
        }
    }
}
