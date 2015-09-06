/*
 * Copyright (C) 2008-2015 Peter Jakubčo
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

import emulib.plugins.cpu.CPU;
import emulib.plugins.cpu.CPU.RunState;
import emulib.plugins.device.DeviceContext;
import emulib.plugins.memory.MemoryContext;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.AND_OR_XOR_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.DAA_H_C_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.DAA_H_NOT_C_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.DAA_NOT_H_C_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.DAA_NOT_H_NOT_C_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.DAA_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.DEC_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.INC_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.NEG_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.PARITY_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.RRCA_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.SIGN_ZERO_CARRY_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.SIGN_ZERO_TABLE;

/**
 * Main implementation class for CPU emulation CPU works in a separate thread
 * (parallel with other hardware)
 */
public class EmulatorEngine {
    public static final int REG_A = 7, REG_B = 0, REG_C = 1, REG_D = 2, REG_E = 3, REG_H = 4, REG_L = 5;
    public static final int FLAG_S = 0x80, FLAG_Z = 0x40, FLAG_H = 0x10, FLAG_PV = 0x4, FLAG_N = 0x02, FLAG_C = 0x1;

    private final static int[] CONDITION = new int[] {
            FLAG_Z, FLAG_Z, FLAG_C, FLAG_C, FLAG_PV, FLAG_PV, FLAG_S, FLAG_S
    };
    private final static int[] CONDITION_VALUES = new int[] {
            0, FLAG_Z, 0, FLAG_C, 0, FLAG_PV, 0, FLAG_S
    };
    
    private final ContextImpl context;
    private final MemoryContext<Short, Integer> memory;

    public final int[] regs = new int[8];
    public final int[] regs2 = new int[8];
    public volatile int flags = 2;
    public volatile int  flags2 = 2;    
    
    // special registers
    public int PC = 0, SP = 0, IX = 0, IY = 0;
    public int I = 0, R = 0; // interrupt r., refresh r.
    
    public byte intMode = 0; // interrupt mode (0,1,2)
    // Interrupt flip-flops
    public final boolean[] IFF = new boolean[2]; // interrupt enable flip-flops
    // No-Extra wait for CPC Interrupt?
    private boolean noWait = false;
    // Flag to cause an interrupt to execute
    private boolean isINT = false;
    // Interrupt Vector
    private int interruptVector = 0xff;
    // Interrupt mask
    private int interruptPending = 0;
    // device that want to interrupt
    private DeviceContext interruptDevice;

    private RunState currentRunState = RunState.STATE_STOPPED_NORMAL;
    public int checkTimeSlice = 100;
    private long executedCycles = 0;

    private volatile DispatchListener dispatchListener;

    public interface DispatchListener {
        void beforeDispatch();

        void afterDispatch();
    }

    public EmulatorEngine(MemoryContext<Short, Integer> memory, ContextImpl context) {
        this.memory = Objects.requireNonNull(memory);
        this.context = Objects.requireNonNull(context);
    }

    public void setDispatchListener(DispatchListener dispatchListener) {
        this.dispatchListener = dispatchListener;
    }

    public long getAndResetExecutedCycles() {
        long tmpExecutedCycles = executedCycles;
        executedCycles = 0;
        return tmpExecutedCycles;
    }
    
    public void reset(int startPos) {
        SP = IX = IY = 0;
        I = R = 0;
        Arrays.fill(regs, 0);
        Arrays.fill(regs2, 0);
        flags = 0;
        flags2 = 0;
        IFF[0] = false;
        IFF[1] = false;
        PC = startPos;
        interruptPending = 0;
        isINT = noWait = false;
        currentRunState = RunState.STATE_STOPPED_BREAK;
    }

    public CPU.RunState step() throws Exception {
        boolean oldIFF = IFF[0];
        noWait = false;
        currentRunState = CPU.RunState.STATE_STOPPED_BREAK;
        evalStep(memory.read(PC++));
        isINT = (interruptPending != 0) && oldIFF && IFF[0];
        if (PC > 0xffff) {
            PC = 0xffff;
            return RunState.STATE_STOPPED_ADDR_FALLOUT;
        }
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
                    cycles = evalStep(memory.read(PC++));
                    cycles_executed += cycles;
                    executedCycles += cycles;
                    if (cpu.isBreakpointSet(PC)) {
                        throw new Breakpoint();
                    }
                } catch (IllegalArgumentException e) {
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
        return currentRunState;
    }

    public void setInterrupt(DeviceContext device, int mask) {
        this.interruptDevice = device;
        this.interruptPending |= mask;
    }

    public void clearInterrupt(DeviceContext device, int mask) {
        if (interruptDevice == device) {
            this.interruptPending &= ~mask;
        }
    }

    public void setInterruptVector(byte[] vector) {
        if ((vector == null) || (vector.length == 0)) {
            return;
        }
        this.interruptVector = vector[0];
    }

    private int getreg(int reg) {
        if (reg == 6) {
            return memory.read((regs[REG_H] << 8) | regs[REG_L]);
        }
        return regs[reg];
    }
    
    private int getreg2(int reg) {
        if (reg == 6) {
            return 0;
        }
        return regs[reg];
    }

    private void putreg(int reg, int val) {
        if (reg == 6) {
            memory.write((regs[REG_H] << 8) | regs[REG_L], (short)val);
        } else {
            regs[reg] = val;
        }
    }

    private void putreg2(int reg, int val) {
        if (reg != 6) {
            regs[reg] = val;
        }
    }

    void putpair(int reg, int val) {
        if (reg == 3) {
            SP = val;
        } else {
            int high = (val >>> 8) & 0xFF;
            int low = val & 0xFF;
            int index = reg * 2;
            regs[index] = high;
            regs[index+1] = low;
        }
    }
    
    private void putpair2(int reg, int val) {
        int high = (val >>> 8) & 0xFF;
        int low = val & 0xFF;
        int index = reg * 2;
        if (reg == 3) {
            regs[REG_A] = high;
            flags = (short)low;
        } else {
            regs[index] = high;
            regs[index+1] = low;
        }
    }

    private int getpair(int reg) {
        if (reg == 3) {
            return SP;
        }
        int index = reg * 2;
        return regs[index] << 8 | regs[index + 1];
    }

    private int getpair2(int reg) {
        if (reg == 3) {
            return regs[REG_A] << 8 | (flags & 0xFF);
        }
        int index = reg * 2;
        return regs[index] << 8 | regs[index + 1];
    }

    private int getpair(short special, int reg) {
        if (reg == 3) {
            return SP;
        } else if (reg == 2) {
            return (special == 0xDD) ? IX : IY;
        }
        int index = reg * 2;
        return regs[index] << 8 | regs[index + 1];
    }

    private boolean getCC1(int cc) {
        switch (cc) {
            case 0:
                return ((flags & FLAG_Z) == 0); // NZ
            case 1:
                return ((flags & FLAG_Z) != 0); // Z
            case 2:
                return ((flags & FLAG_C) == 0); // NC
            case 3:
                return ((flags & FLAG_C) != 0); // C
        }
        return false;
    }

    /**
     * Put a value into IX/IY register
     */
    private void putspecial(short spec, int val) {
        val &= 0xFFFF;
        switch (spec) {
            case 0xDD:
                IX = val;
                break;
            case 0xFD:
                IY = val;
                break;
        }
    }

    private int getspecial(short spec) {
        switch (spec) {
            case 0xDD:
                return IX;
            case 0xFD:
                return IY;
        }
        return 0;
    }

    private int doInterrupt() {
        isINT = false;
        int cycles = 0;

        if (!noWait) {
            cycles += 14;
        }
//        if (interruptDevice != null) {
        //          interruptDevice.setInterrupt(1);
        //    }
        IFF[0] = IFF[1] = false;
        switch (intMode) {
            case 0:  // rst p (interruptVector)
                cycles += 11;
                RunState old_runstate = currentRunState;
                evalStep((short) interruptVector); // must ignore halt
                if (currentRunState == RunState.STATE_STOPPED_NORMAL) {
                    currentRunState = old_runstate;
                }
                break;
            case 1: // rst 0xFF
                cycles += 12;
                memory.writeWord(SP - 2, PC);
                SP = (SP - 2) & 0xffff;
                PC = 0xFF & 0x38;
                break;
            case 2:
                cycles += 13;
                memory.writeWord(SP - 2, PC);
                PC = memory.readWord((I << 8) | interruptVector);
                break;
        }
        return cycles;
    }
    
    private void overflow(int i, int j, int result) {
        int signFirst = i & 0x80;
        int signSecond = j & 0x80;
        if (signFirst != signSecond) {
            flags &= (~FLAG_PV);
        } else if ((result & 0x80) != signFirst){
            flags |= FLAG_PV;
        }
    }

    private void bigOverflow(int i, int j, int result) {
        int sign = i & 0x8000;
        if (sign != (j & 0x8000)) {
            flags &= (~FLAG_PV);
        } else if ((result & 0x8000) != sign){
            flags |= FLAG_PV;
        }
    }

    private void auxCarry(int before, int sumWith) {
        int mask = sumWith & before;
        int xormask = sumWith ^ before;

        int C0 = mask&1;
        int C1 = ((mask>>>1) ^ (C0&(xormask>>>1)))&1;
        int C2 = ((mask>>>2) ^ (C1&(xormask>>>2)))&1;
        int C3 = ((mask>>>3) ^ (C2&(xormask>>>3)))&1;

        if (C3 != 0) {
            flags |= FLAG_H;
        } else {
            flags &= (~FLAG_H);
        }
    }
    
    private void halfCarry11(int before, int sumWith) {
        int mask = sumWith & before;
        int xormask = sumWith ^ before;

        int C0 = mask&1;
        int C1 = ((mask>>>1) ^ (C0&(xormask>>>1)))&1;
        int C2 = ((mask>>>2) ^ (C1&(xormask>>>2)))&1;
        int C3 = ((mask>>>3) ^ (C2&(xormask>>>3)))&1;
        int C4 = ((mask>>>4) ^ (C3&(xormask>>>4)))&1;
        int C5 = ((mask>>>5) ^ (C4&(xormask>>>5)))&1;
        int C6 = ((mask>>>6) ^ (C5&(xormask>>>6)))&1;
        int C7 = ((mask>>>7) ^ (C6&(xormask>>>7)))&1;
        int C8 = ((mask>>>8) ^ (C7&(xormask>>>8)))&1;
        int C9 = ((mask>>>9) ^ (C8&(xormask>>>9)))&1;
        int C10 = ((mask>>>10) ^ (C9&(xormask>>>10)))&1;
        int C11 = ((mask>>>11) ^ (C10&(xormask>>>11)))&1;

        if (C11 != 0) {
            flags |= FLAG_H;
        } else {
            flags &= (~FLAG_H);
        }
    }

    private void carry15(int before, int sumWith) {
        int mask = sumWith & before;
        int xormask = sumWith ^ before;

        int C0 = mask&1;
        int C1 = ((mask>>>1) ^ (C0&(xormask>>>1)))&1;
        int C2 = ((mask>>>2) ^ (C1&(xormask>>>2)))&1;
        int C3 = ((mask>>>3) ^ (C2&(xormask>>>3)))&1;
        int C4 = ((mask>>>4) ^ (C3&(xormask>>>4)))&1;
        int C5 = ((mask>>>5) ^ (C4&(xormask>>>5)))&1;
        int C6 = ((mask>>>6) ^ (C5&(xormask>>>6)))&1;
        int C7 = ((mask>>>7) ^ (C6&(xormask>>>7)))&1;
        int C8 = ((mask>>>8) ^ (C7&(xormask>>>8)))&1;
        int C9 = ((mask>>>9) ^ (C8&(xormask>>>9)))&1;
        int C10 = ((mask>>>10) ^ (C9&(xormask>>>10)))&1;
        int C11 = ((mask>>>11) ^ (C10&(xormask>>>11)))&1;
        int C12 = ((mask>>>12) ^ (C11&(xormask>>>12)))&1;
        int C13 = ((mask>>>13) ^ (C12&(xormask>>>13)))&1;
        int C14 = ((mask>>>14) ^ (C13&(xormask>>>14)))&1;
        int C15 = ((mask>>>15) ^ (C14&(xormask>>>15)))&1;

        if (C15 != 0) {
            flags |= FLAG_C;
        } else {
            flags &= (~FLAG_C);
        }
    }
    
    private int evalStep(short OP) {
        int tmp, tmp1, tmp2, tmp3;
        short special = 0; // prefix if available = 0xDD or 0xFD

        DispatchListener tmpListener = dispatchListener;
        if (tmpListener != null) {
            tmpListener.beforeDispatch();
        }

        try {
        /* if interrupt is waiting, instruction won't be read from memory
         * but from one or all of 3 bytes (b1,b2,b3) which represents either
         * rst or call instruction incomed from external peripheral device
         */
            if (isINT) {
                return doInterrupt();
            }
            R = ((R + 1) & 0xFF);
            if (OP == 0x76) { /* HALT */
                currentRunState = RunState.STATE_STOPPED_NORMAL;
                return 4;
            }

        /* Handle below all operations which refer to registers or register pairs.
         After that, a large switch statement takes care of all other opcodes */
            switch (OP & 0xC0) {
                case 0x40: /* LD r,r' */
                    tmp = (OP >>> 3) & 0x07;
                    tmp1 = OP & 0x07;
                    putreg(tmp, (short) getreg(tmp1));
                    if ((tmp1 == 6) || (tmp == 6)) {
                        return 7;
                    } else {
                        return 4;
                    }
            }
            switch (OP) {
                case 0x00: /* NOP */
                    return 4;
                case 0x02: /* LD (BC),A */
                    memory.write(getpair(0), (short) regs[REG_A]);
                    return 7;
            /* INC ss */
                case 0x03:
                case 0x13:
                case 0x23:
                case 0x33:
                    tmp = (OP >>> 4) & 0x03;
                    putpair(tmp, (getpair(tmp) + 1) & 0xFFFF);
                    return 6;
            /* ADD HL, ss*/
                case 0x09:
                case 0x19:
                case 0x29:
                case 0x39:
                    tmp = getpair((OP >>> 4) & 0x03);
                    tmp1 = getpair(2);
                    carry15(tmp, tmp1);
                    halfCarry11(tmp, tmp1);
                    flags &= (~FLAG_N);
                    tmp += tmp1;
                    putpair(2, tmp & 0xFFFF);
                    return 11;
            /* DEC ss*/
                case 0x0B:
                case 0x1B:
                case 0x2B:
                case 0x3B:
                    tmp = (OP >>> 4) & 0x03;
                    putpair(tmp, (getpair(tmp) - 1) & 0xFFFF);
                    return 6;
            /* POP qq */
                case 0xC1:
                case 0xD1:
                case 0xE1:
                case 0xF1:
                    tmp = (OP >>> 4) & 0x03;
                    tmp1 = memory.readWord(SP);
                    SP = (SP + 2) & 0xffff;
                    putpair2(tmp, tmp1);
                    return 10;
            /* PUSH qq */
                case 0xC5:
                case 0xD5:
                case 0xE5:
                case 0xF5:
                    tmp = (OP >>> 4) & 0x03;
                    tmp1 = getpair2(tmp);
                    SP = (SP - 2) & 0xffff;
                    memory.writeWord(SP, tmp1);
                    return 11;
            /* LD r,n */
                case 0x06:
                case 0x0E:
                case 0x16:
                case 0x1E:
                case 0x26:
                case 0x2E:
                case 0x36:
                case 0x3E:
                    tmp = (OP >>> 3) & 0x07;
                    putreg(tmp, memory.read(PC++));
                    if (tmp == 6) {
                        return 10;
                    } else {
                        return 7;
                    }
            /* INC r */
                case 0x04:
                case 0x0C:
                case 0x14:
                case 0x1C:
                case 0x24:
                case 0x2C:
                case 0x34:
                case 0x3C:
                    tmp = (OP >>> 3) & 0x07;
                    tmp1 = (getreg(tmp) + 1) & 0xFF;
                    flags = INC_TABLE[tmp1] | (flags & FLAG_C);
                    putreg(tmp, tmp1);
                    return (tmp == 6) ? 11 : 4;
            /* DEC r */
                case 0x05:
                case 0x0D:
                case 0x15:
                case 0x1D:
                case 0x25:
                case 0x2D:
                case 0x35:
                case 0x3D:
                    tmp = (OP >>> 3) & 0x07;
                    tmp1 = (getreg(tmp) - 1) & 0xFF;
                    flags = DEC_TABLE[tmp1] | (flags & FLAG_C);
                    putreg(tmp, tmp1);
                    return (tmp == 6) ? 11 : 4;
            /* RET cc */
                case 0xC0:
                case 0xC8:
                case 0xD0:
                case 0xD8:
                case 0xE0:
                case 0xE8:
                case 0xF0:
                case 0xF8:
                    tmp = (OP >>> 3) & 7;
                    if ((flags & CONDITION[tmp]) == CONDITION_VALUES[tmp]) {
                        PC = memory.readWord(SP);
                        SP = (SP + 2) & 0xffff;
                        return 11;
                    }
                    return 5;
            /* RST p */
                case 0xC7:
                case 0xCF:
                case 0xD7:
                case 0xDF:
                case 0xE7:
                case 0xEF:
                case 0xF7:
                case 0xFF:
                    memory.writeWord(SP - 2, PC);
                    SP = (SP - 2) & 0xffff;
                    PC = OP & 0x38;
                    return 11;
            /* ADD A,r */
                case 0x80:
                case 0x81:
                case 0x82:
                case 0x83:
                case 0x84:
                case 0x85:
                case 0x86:
                case 0x87:
                    int X = regs[REG_A];
                    int diff = getreg(OP & 0x07);
                    regs[REG_A] += diff;

                    flags = SIGN_ZERO_CARRY_TABLE[regs[REG_A] & 0x1FF];
                    regs[REG_A] = regs[REG_A] & 0xFF;

                    auxCarry(X, diff);
                    overflow(X, diff, regs[REG_A]);

                    return ((OP & 0x07) == 6) ? 7 : 4;
            /* ADC A,r */
                case 0x88:
                case 0x89:
                case 0x8A:
                case 0x8B:
                case 0x8C:
                case 0x8D:
                case 0x8E:
                case 0x8F:
                    X = regs[REG_A];
                    diff = getreg(OP & 0x07);
                    if ((flags & FLAG_C) != 0) {
                        diff++;
                    }
                    regs[REG_A] += diff;

                    flags = SIGN_ZERO_CARRY_TABLE[regs[REG_A] & 0x1FF];
                    regs[REG_A] = regs[REG_A] & 0xFF;

                    auxCarry(X, diff);
                    overflow(X, diff, regs[REG_A]);

                    return ((OP & 0x07) == 6) ? 7 : 4;
            /* SUB r */
                case 0x90:
                case 0x91:
                case 0x92:
                case 0x93:
                case 0x94:
                case 0x95:
                case 0x96:
                case 0x97:
                    X = regs[REG_A];
                    diff = -getreg(OP & 0x07);
                    regs[REG_A] += diff;
                    diff &= 0xFF;

                    flags = SIGN_ZERO_CARRY_TABLE[regs[REG_A] & 0x1FF] | FLAG_N;
                    regs[REG_A] = regs[REG_A] & 0xFF;

                    auxCarry(X, diff);
                    overflow(X, diff, regs[REG_A]);

                    return ((OP & 0x07) == 6) ? 7 : 4;
            /* SBC A,r */
                case 0x98:
                case 0x99:
                case 0x9A:
                case 0x9B:
                case 0x9C:
                case 0x9D:
                case 0x9E:
                case 0x9F:
                    X = regs[REG_A];
                    diff = -getreg(OP & 0x07);
                    if ((flags & FLAG_C) != 0) {
                        diff--;
                    }
                    regs[REG_A] += diff;
                    diff &= 0xFF;

                    flags = SIGN_ZERO_CARRY_TABLE[regs[REG_A] & 0x1FF] | FLAG_N;
                    regs[REG_A] = regs[REG_A] & 0xFF;

                    auxCarry(X, diff);
                    overflow(X, diff, regs[REG_A]);

                    return ((OP & 0x07) == 6) ? 7 : 4;
            /* AND r */
                case 0xA0:
                case 0xA1:
                case 0xA2:
                case 0xA3:
                case 0xA4:
                case 0xA5:
                case 0xA6:
                case 0xA7:
                    regs[REG_A] = (regs[REG_A] & getreg(OP & 7)) & 0xFF;
                    flags = AND_OR_XOR_TABLE[regs[REG_A]];
                    return (OP == 0xA6) ? 7 : 4;
            /* XOR r */
                case 0xA8:
                case 0xA9:
                case 0xAA:
                case 0xAB:
                case 0xAC:
                case 0xAD:
                case 0xAE:
                case 0xAF:
                    regs[REG_A] = ((regs[REG_A] ^ getreg(OP & 7)) & 0xff);
                    flags = AND_OR_XOR_TABLE[regs[REG_A]];
                    return (OP == 0xAE) ? 7 : 4;
            /* OR r */
                case 0xB0:
                case 0xB1:
                case 0xB2:
                case 0xB3:
                case 0xB4:
                case 0xB5:
                case 0xB6:
                case 0xB7:
                    regs[REG_A] = (regs[REG_A] | getreg(OP & 7)) & 0xFF;
                    flags = AND_OR_XOR_TABLE[regs[REG_A]];
                    return (OP == 0xB6) ? 7 : 4;
            /* CP r */
                case 0xB8:
                case 0xB9:
                case 0xBA:
                case 0xBB:
                case 0xBC:
                case 0xBD:
                case 0xBE:
                case 0xBF:
                    diff = -getreg(OP & 7);
                    tmp2 = regs[REG_A] + diff;
                    diff &= 0xFF;

                    flags = SIGN_ZERO_CARRY_TABLE[tmp2 & 0x1FF] | FLAG_N;
                    auxCarry(regs[REG_A], diff);
                    overflow(regs[REG_A], diff, tmp2 & 0xFF);

                    return (OP == 0xBE) ? 7 : 4;
                case 0x07: /* RLCA */
                    tmp = regs[REG_A] >>> 7;
                    regs[REG_A] = ((((regs[REG_A] << 1) & 0xFF) | tmp) & 0xff);
                    flags = ((flags & 0xEC) | tmp);
                    return 4;
                case 0x08: /* EX AF,AF' */
                    tmp = regs[REG_A];
                    regs[REG_A] = regs2[REG_A];
                    regs2[REG_A] = tmp;
                    tmp = flags;
                    flags = flags2;
                    flags2 = tmp;
                    return 4;
                case 0x0A: /* LD A,(BC) */
                    tmp = memory.read(getpair(0));
                    regs[REG_A] = tmp;
                    return 7;
                case 0x0F: /* RRCA */
                    flags = ((flags & 0xEC) | (regs[REG_A] & 1));
                    regs[REG_A] = RRCA_TABLE[regs[REG_A]];
                    return 4;
                case 0x10: /* DJNZ e */
                    tmp = memory.read(PC++);
                    regs[REG_B]--;
                    regs[REG_B] &= 0xFF;
                    if (regs[REG_B] != 0) {
                        PC += (byte) tmp;
                        PC &= 0xFFFF;
                        return 13;
                    }
                    return 8;
                case 0x12: /* LD (DE), A */
                    memory.write(getpair(1), (short) regs[REG_A]);
                    return 7;
                case 0x17: /* RLA */
                    tmp = regs[REG_A] >>> 7;
                    regs[REG_A] = (((regs[REG_A] << 1) | (flags & 1)) & 0xff);
                    flags = ((flags & 0xEC) | tmp);
                    return 4;
                case 0x1A: /* LD A,(DE) */
                    tmp = memory.read(getpair(1));
                    regs[REG_A] = (tmp & 0xff);
                    return 7;
                case 0x1F: /* RRA */
                    tmp = (flags & 1) << 7;
                    flags = ((flags & 0xEC) | (regs[REG_A] & 1));
                    regs[REG_A] = ((regs[REG_A] >>> 1 | tmp) & 0xff);
                    return 4;
                case 0x27: /* DAA */
                    int temp = regs[REG_A];
                    boolean acFlag = (flags & FLAG_H) == FLAG_H;
                    boolean cFlag = (flags & FLAG_C) == FLAG_C;

                    if (!acFlag && !cFlag) {
                        regs[REG_A] = DAA_NOT_H_NOT_C_TABLE[temp] & 0xFF;
                        flags = (DAA_NOT_H_NOT_C_TABLE[temp] >> 8) & 0xFF | (flags & FLAG_N);
                    } else if (acFlag && !cFlag) {
                        regs[REG_A] = DAA_H_NOT_C_TABLE[temp] & 0xFF;
                        flags = (DAA_H_NOT_C_TABLE[temp] >> 8) & 0xFF | (flags & FLAG_N);
                    } else if (!acFlag && cFlag) {
                        regs[REG_A] = DAA_NOT_H_C_TABLE[temp] & 0xFF;
                        flags = (DAA_NOT_H_C_TABLE[temp] >> 8) & 0xFF | (flags & FLAG_N);
                    } else {
                        regs[REG_A] = DAA_H_C_TABLE[temp] & 0xFF;
                        flags = (DAA_H_C_TABLE[temp] >> 8) & 0xFF | (flags & FLAG_N);
                    }
                    return 4;
                case 0x2F: /* CPL */
                    regs[REG_A] = ((~regs[REG_A]) & 0xFF);
                    flags |= FLAG_N | FLAG_H;
                    return 4;
                case 0x37: /* SCF */
                    flags |= FLAG_N | FLAG_C;
                    flags &= ~FLAG_H;
                    return 4;
                case 0x3F: /* CCF */
                    tmp = flags & FLAG_C;
                    if (tmp == 0) {
                        flags |= FLAG_C;
                    } else {
                        flags &= ~FLAG_C;
                    }
                    flags &= ~FLAG_N;
                    return 4;
                case 0xC9: /* RET */
                    PC = memory.readWord(SP);
                    SP += 2;
                    return 10;
                case 0xD9: /* EXX */
                    tmp = regs[REG_B];
                    regs[REG_B] = regs2[REG_B];
                    regs2[REG_B] = tmp;
                    tmp = regs[REG_C];
                    regs[REG_C] = regs2[REG_C];
                    regs2[REG_C] = tmp;
                    tmp = regs[REG_D];
                    regs[REG_D] = regs2[REG_D];
                    regs2[REG_D] = tmp;
                    tmp = regs[REG_E];
                    regs[REG_E] = regs2[REG_E];
                    regs2[REG_E] = tmp;
                    tmp = regs[REG_H];
                    regs[REG_H] = regs2[REG_H];
                    regs2[REG_H] = tmp;
                    tmp = regs[REG_L];
                    regs[REG_L] = regs2[REG_L];
                    regs2[REG_L] = tmp;
                    return 4;
                case 0xE3: /* EX (SP),HL */
                    tmp = memory.read(SP);
                    tmp1 = memory.read(SP + 1);
                    memory.write(SP, (short) regs[REG_L]);
                    memory.write(SP + 1, (short) regs[REG_H]);
                    regs[REG_L] = tmp & 0xFF;
                    regs[REG_H] = tmp1 & 0xFF;
                    return 19;
                case 0xE9: /* JP (HL) */
                    PC = ((regs[REG_H] << 8) | regs[REG_L]);
                    return 4;
                case 0xEB: /* EX DE,HL */
                    tmp = regs[REG_D];
                    regs[REG_D] = regs[REG_H];
                    regs[REG_H] = tmp;
                    tmp = regs[REG_E];
                    regs[REG_E] = regs[REG_L];
                    regs[REG_L] = tmp;
                    return 4;
                case 0xF3: /* DI */
                    IFF[0] = IFF[1] = false;
                    return 4;
                case 0xF9: /* LD SP,HL */
                    SP = ((regs[REG_H] << 8) | regs[REG_L]);
                    return 6;
                case 0xFB:
                    IFF[0] = IFF[1] = true;
                    return 4;
                case 0xED:
                    OP = memory.read(PC++);
                    switch (OP) {
                    /* IN r,(C) */
                        case 0x40:
                        case 0x48:
                        case 0x50:
                        case 0x58:
                        case 0x60:
                        case 0x68:
                        case 0x78:
                            tmp = (OP >>> 3) & 0x7;
                            putreg(tmp, context.fireIO(regs[REG_C], true, 0));
                            flags = ((flags & 1) | DAA_TABLE[tmp]);
                            return 12;
                    /* OUT (C),r */
                        case 0x41:
                        case 0x49:
                        case 0x51:
                        case 0x59:
                        case 0x61:
                        case 0x69:
                        case 0x79:
                            tmp = (OP >>> 3) & 0x7;
                            context.fireIO(regs[REG_C], false, (short) getreg(tmp));
                            return 12;
                    /* SBC HL, ss */
                        case 0x42:
                        case 0x52:
                        case 0x62:
                        case 0x72:
                            tmp = -getpair((OP >>> 4) & 0x03);
                            tmp1 = getpair(2);
                            if ((flags & FLAG_C) == FLAG_C) {
                                tmp--;
                            }

                            int sum = (tmp1 + tmp) & 0xFFFF;
                            tmp &= 0xFFFF;

                            if ((sum & 0x8000) != 0) {
                                flags |= FLAG_S;
                            } else {
                                flags &= (~FLAG_S);
                            }
                            if (sum == 0) {
                                flags |= FLAG_Z;
                            } else {
                                flags &= (~FLAG_Z);
                            }
                            flags |= FLAG_N;

                            carry15(tmp1, tmp);
                            halfCarry11(tmp1, tmp);
                            bigOverflow(tmp1, tmp, sum);
                            putpair(2, sum);

                            return 15;
                    /* ADC HL,ss */
                        case 0x4A:
                        case 0x5A:
                        case 0x6A:
                        case 0x7A:
                            tmp = getpair((OP >>> 4) & 0x03);
                            tmp1 = getpair(2);
                            if ((flags & FLAG_C) == FLAG_C) {
                                tmp++;
                            }
                            sum = tmp + tmp1;
                            tmp1 &= 0xFFFF;
                            sum &= 0xFFFF;

                            if ((sum & 0x8000) != 0) {
                                flags |= FLAG_S;
                            } else {
                                flags &= (~FLAG_S);
                            }
                            if (sum == 0) {
                                flags |= FLAG_Z;
                            } else {
                                flags &= (~FLAG_Z);
                            }
                            flags &= (~FLAG_N);
                            carry15(tmp, tmp1);
                            halfCarry11(tmp, tmp1);
                            bigOverflow(tmp, tmp1, sum);
                            putpair(2, sum);

                            return 11;
                        case 0x44: /* NEG */
                            flags = NEG_TABLE[regs[REG_A]] & 0xFF;
                            regs[REG_A] = (NEG_TABLE[regs[REG_A]] >>> 8) & 0xFF;
                            return 8;
                        case 0x45: /* RETN */
                            IFF[0] = IFF[1];
                            PC = memory.readWord(SP);
                            SP = (SP + 2) & 0xffff;
                            return 14;
                        case 0x46: /* IM 0 */
                            intMode = 0;
                            return 8;
                        case 0x47: /* LD I,A */
                            I = regs[REG_A];
                            return 9;
                        case 0x4D: /* RETI - weird.. */
                            IFF[0] = IFF[1];
                            PC = memory.readWord(SP);
                            SP = (SP + 2) & 0xffff;
                            return 14;
                        case 0x4F: /* LD R,A */
                            R = regs[REG_A];
                            return 9;
                        case 0x56: /* IM 1 */
                            intMode = 1;
                            return 8;
                        case 0x57: /* LD A,I */
                            regs[REG_A] = I;
                            flags = EmulatorTables.SIGN_ZERO_TABLE[(short) (I & 0xFF)]
                                    | (IFF[1] ? FLAG_PV : 0) | (flags & FLAG_C);
                            return 9;
                        case 0x5E: /* IM 2 */
                            intMode = 2;
                            return 8;
                        case 0x5F: /* LD A,R */
                            regs[REG_A] = R;
                            flags = EmulatorTables.SIGN_ZERO_TABLE[(short) (R & 0xFF)]
                                    | (IFF[1] ? FLAG_PV : 0) | (flags & FLAG_C);
                            return 9;
                        case 0x67: /* RRD */
                            tmp = regs[REG_A] & 0x0F;
                            tmp1 = memory.read((regs[REG_H] << 8) | regs[REG_L]);
                            regs[REG_A] = ((regs[REG_A] & 0xF0) | (tmp1 & 0x0F));
                            tmp1 = ((tmp1 >>> 4) & 0x0F) | (tmp << 4);
                            memory.write(((regs[REG_H] << 8) | regs[REG_L]), (short) (tmp1 & 0xff));
                            flags = (DAA_TABLE[regs[REG_A]] | (flags & FLAG_C));
                            return 18;
                        case 0x6F: /* RLD */
                            tmp = memory.read((regs[REG_H] << 8) | regs[REG_L]);
                            tmp1 = (tmp >>> 4) & 0x0F;
                            tmp = ((tmp << 4) & 0xF0) | (regs[REG_A] & 0x0F);
                            regs[REG_A] = ((regs[REG_A] & 0xF0) | tmp1);
                            memory.write((regs[REG_H] << 8) | regs[REG_L], (short) (tmp & 0xff));
                            flags = (DAA_TABLE[regs[REG_A]] | (flags & FLAG_C));
                            return 18;
                        case 0x70: /* IN (C) - unsupported */
                            tmp = (context.fireIO(regs[REG_C], true, 0) & 0xFF);
                            flags = ((flags & 1) | DAA_TABLE[tmp]);
                            return 12;
                        case 0x71: /* OUT (C),0 - unsupported */
                            context.fireIO(regs[REG_C], false, 0);
                            return 12;
                        case 0xA0: /* LDI */
                            tmp1 = (regs[REG_H] << 8) | regs[REG_L];
                            tmp2 = (regs[REG_D] << 8) | regs[REG_E];
                            tmp = (regs[REG_B] << 8) | regs[REG_C];

                            memory.write(tmp2, memory.read(tmp1));

                            tmp1 = (tmp1 + 1) & 0xFFFF;
                            tmp2 = (tmp2 + 1) & 0xFFFF;
                            tmp = (tmp - 1) & 0xFFFF;

                            regs[REG_H] = (tmp1 >>> 8) & 0xFF;
                            regs[REG_L] = tmp1 & 0xFF;
                            regs[REG_D] = (tmp2 >>> 8) & 0xFF;
                            regs[REG_E] = tmp2 & 0xFF;
                            regs[REG_B] = (tmp >>> 8) & 0xFF;
                            regs[REG_C] = tmp & 0xFF;
                            flags = ((flags & FLAG_S) | (flags & FLAG_Z) | (flags & FLAG_C)) & (~FLAG_PV);
                            if (tmp != 0) {
                                flags |= FLAG_PV;
                            }
                            return 16;
                        case 0xA1: /* CPI */
                            tmp1 = (regs[REG_H] << 8) | regs[REG_L];
                            tmp2 = (regs[REG_B] << 8) | regs[REG_C];

                            tmp = memory.read(tmp1);
                            tmp1 = (tmp1 + 1) & 0xFFFF;
                            tmp2 = (tmp2 - 1) & 0xFFFF;

                            flags = EmulatorTables.SIGN_ZERO_TABLE[(regs[REG_A] - tmp) & 0xFF] | FLAG_N | (flags & FLAG_C);
                            auxCarry(regs[REG_A], (-tmp) & 0xFF);

                            if (tmp2 != 0) {
                                flags |= FLAG_PV;
                            }

                            regs[REG_H] = (tmp1 >>> 8) & 0xFF;
                            regs[REG_L] = tmp1 & 0xFF;
                            regs[REG_B] = (tmp2 >>> 8) & 0xFF;
                            regs[REG_C] = tmp2 & 0xFF;
                            return 16;
                        case 0xA2: /* INI */
                            tmp = (context.fireIO(regs[REG_C], true, 0) & 0xFF);
                            tmp1 = (regs[REG_H] << 8) | regs[REG_L];
                            memory.write(tmp1, (short) tmp);
                            tmp1 = (tmp1 + 1) & 0xFFFF;
                            regs[REG_H] = ((tmp1 >>> 8) & 0xff);
                            regs[REG_L] = (tmp1 & 0xFF);
                            regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);
                            flags = ((flags & 0xBF) | FLAG_N | ((regs[REG_B] == 0) ? FLAG_Z : 0));
                            return 16;
                        case 0xA3: /* OUTI */
                            tmp1 = (regs[REG_H] << 8) | regs[REG_L];
                            context.fireIO(regs[REG_C], false, memory.read(tmp1));
                            tmp1 = (tmp1 + 1) & 0xFFFF;
                            regs[REG_H] = ((tmp1 >>> 8) & 0xff);
                            regs[REG_L] = (tmp1 & 0xFF);
                            regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);
                            flags = ((flags & 0xBF) | FLAG_N | ((regs[REG_B] == 0) ? FLAG_Z : 0));
                            return 16;
                        case 0xA8: /* LDD */
                            tmp1 = (regs[REG_H] << 8) | regs[REG_L];
                            tmp2 = (regs[REG_D] << 8) | regs[REG_E];
                            tmp = (regs[REG_B] << 8) | regs[REG_C];

                            memory.write(tmp2, memory.read(tmp1));

                            tmp1 = (tmp1 - 1) & 0xFFFF;
                            tmp2 = (tmp2 - 1) & 0xFFFF;
                            tmp = (tmp - 1) & 0xFFFF;

                            regs[REG_H] = (tmp1 >>> 8) & 0xFF;
                            regs[REG_L] = tmp1 & 0xFF;
                            regs[REG_D] = (tmp2 >>> 8) & 0xFF;
                            regs[REG_E] = tmp2 & 0xFF;
                            regs[REG_B] = (tmp >>> 8) & 0xFF;
                            regs[REG_C] = tmp & 0xFF;
                            flags = ((flags & FLAG_S) | (flags & FLAG_Z) | (flags & FLAG_C)) & (~FLAG_PV);
                            if (tmp != 0) {
                                flags |= FLAG_PV;
                            }
                            return 16;
                        case 0xA9: /* CPD */
                            tmp1 = (regs[REG_H] << 8) | regs[REG_L];
                            tmp2 = (regs[REG_B] << 8) | regs[REG_C];

                            tmp = memory.read(tmp1);
                            tmp1 = (tmp1 - 1) & 0xFFFF;
                            tmp2 = (tmp2 - 1) & 0xFFFF;

                            flags = EmulatorTables.SIGN_ZERO_TABLE[(regs[REG_A] - tmp) & 0xFF] | FLAG_N | (flags & FLAG_C);
                            auxCarry(regs[REG_A], (-tmp) & 0xFF);

                            if (tmp2 != 0) {
                                flags |= FLAG_PV;
                            }

                            regs[REG_H] = (tmp1 >>> 8) & 0xFF;
                            regs[REG_L] = tmp1 & 0xFF;
                            regs[REG_B] = (tmp2 >>> 8) & 0xFF;
                            regs[REG_C] = tmp2 & 0xFF;
                            return 16;
                        case 0xAA: /* IND */
                            tmp = (context.fireIO(regs[REG_C], true, 0) & 0xFF);
                            tmp1 = (regs[REG_H] << 8) | regs[REG_L];
                            memory.write(tmp1, (short) tmp);
                            tmp1 = (tmp1 - 1) & 0xFFFF;
                            regs[REG_H] = ((tmp1 >>> 8) & 0xff);
                            regs[REG_L] = (tmp1 & 0xFF);
                            regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);
                            flags = (flags & 0xBF) | FLAG_N | ((regs[REG_B] == 0) ? FLAG_Z : 0);
                            return 16;
                        case 0xAB: /* OUTD */
                            tmp1 = (regs[REG_H] << 8) | regs[REG_L];
                            context.fireIO(regs[REG_C], false, memory.read(tmp1));
                            tmp1 = (tmp1 - 1) & 0xFFFF;
                            regs[REG_H] = ((tmp1 >>> 8) & 0xff);
                            regs[REG_L] = (tmp1 & 0xFF);
                            regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);
                            flags = (flags & 0xBF) | FLAG_N | ((regs[REG_B] == 0) ? FLAG_Z : 0);
                            return 16;
                        case 0xB0: /* LDIR */
                            tmp1 = (regs[REG_H] << 8) | regs[REG_L];
                            tmp2 = (regs[REG_D] << 8) | regs[REG_E];
                            tmp = (regs[REG_B] << 8) | regs[REG_C];

                            memory.write(tmp2, memory.read(tmp1));

                            tmp1 = (tmp1 + 1) & 0xFFFF;
                            tmp2 = (tmp2 + 1) & 0xFFFF;
                            tmp = (tmp - 1) & 0xFFFF;

                            regs[REG_H] = (tmp1 >>> 8) & 0xFF;
                            regs[REG_L] = tmp1 & 0xFF;
                            regs[REG_D] = (tmp2 >>> 8) & 0xFF;
                            regs[REG_E] = tmp2 & 0xFF;
                            regs[REG_B] = (tmp >>> 8) & 0xFF;
                            regs[REG_C] = tmp & 0xFF;
                            flags &= ((~FLAG_PV) & (~FLAG_N) & (~FLAG_H));

                            if (tmp == 0) {
                                return 16;
                            }
                            PC = (PC - 2) & 0xFFFF;
                            return 21;
                        case 0xB1: /* CPIR */
                            tmp1 = (regs[REG_H] << 8) | regs[REG_L];
                            tmp2 = (regs[REG_B] << 8) | regs[REG_C];

                            tmp = memory.read(tmp1);
                            tmp1 = (tmp1 + 1) & 0xFFFF;
                            tmp2 = (tmp2 - 1) & 0xFFFF;

                            flags = EmulatorTables.SIGN_ZERO_TABLE[(regs[REG_A] - tmp) & 0xFF] | FLAG_N | (flags & FLAG_C);
                            auxCarry(regs[REG_A], (-tmp) & 0xFF);

                            if (tmp2 != 0) {
                                flags |= FLAG_PV;
                            }

                            regs[REG_H] = (tmp1 >>> 8) & 0xFF;
                            regs[REG_L] = tmp1 & 0xFF;
                            regs[REG_B] = (tmp2 >>> 8) & 0xFF;
                            regs[REG_C] = tmp2 & 0xFF;

                            if ((tmp2 == 0) || (regs[REG_A] == tmp)) {
                                return 16;
                            }
                            PC = (PC - 2) & 0xFFFF;
                            return 21;
                        case 0xB2: /* INIR */
                            tmp = (context.fireIO(regs[REG_C], true, 0) & 0xFF);
                            tmp1 = (regs[REG_H] << 8) | regs[REG_L];
                            memory.write(tmp1, (short) tmp);
                            tmp1 = (tmp1 + 1) & 0xFFFF;
                            regs[REG_H] = ((tmp1 >>> 8) & 0xff);
                            regs[REG_L] = (tmp1 & 0xFF);
                            regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);
                            flags |= (FLAG_N | FLAG_Z);
                            if (regs[REG_B] == 0) {
                                return 16;
                            }
                            PC -= 2;
                            return 21;
                        case 0xB3: /* OTIR */
                            tmp1 = (regs[REG_H] << 8) | regs[REG_L];
                            context.fireIO(regs[REG_C], false, memory.read(tmp1));
                            tmp1 = (tmp1 + 1) & 0xFFFF;
                            regs[REG_H] = ((tmp1 >>> 8) & 0xff);
                            regs[REG_L] = (tmp1 & 0xFF);
                            regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);
                            flags |= (FLAG_N | FLAG_Z);
                            if (regs[REG_B] == 0) {
                                return 16;
                            }
                            PC -= 2;
                            return 21;
                        case 0xB8: /* LDDR */
                            tmp1 = (regs[REG_H] << 8) | regs[REG_L];
                            tmp2 = (regs[REG_D] << 8) | regs[REG_E];
                            memory.write(tmp2, memory.read(tmp1));
                            tmp1 = (tmp1 - 1) & 0xFFFF;
                            tmp2 = (tmp2 - 1) & 0xFFFF;
                            tmp = (((regs[REG_B] << 8) | regs[REG_C]) - 1) & 0xFFFF;
                            regs[REG_H] = ((tmp1 >>> 8) & 0xff);
                            regs[REG_L] = (tmp1 & 0xFF);
                            regs[REG_B] = ((tmp >>> 8) & 0xff);
                            regs[REG_C] = (tmp & 0xFF);
                            regs[REG_D] = ((tmp2 >>> 8) & 0xff);
                            regs[REG_E] = (tmp2 & 0xFF);
                            flags &= 0xE9;
                            if (tmp == 0) {
                                return 16;
                            }
                            PC -= 2;
                            return 21;
                        case 0xB9: /* CPDR */
                            tmp1 = (regs[REG_H] << 8) | regs[REG_L];
                            tmp2 = (regs[REG_B] << 8) | regs[REG_C];

                            tmp = memory.read(tmp1);
                            tmp1 = (tmp1 - 1) & 0xFFFF;
                            tmp2 = (tmp2 - 1) & 0xFFFF;

                            flags = EmulatorTables.SIGN_ZERO_TABLE[(regs[REG_A] - tmp) & 0xFF] | FLAG_N | (flags & FLAG_C);
                            auxCarry(regs[REG_A], (-tmp) & 0xFF);

                            if (tmp2 != 0) {
                                flags |= FLAG_PV;
                            }

                            regs[REG_H] = (tmp1 >>> 8) & 0xFF;
                            regs[REG_L] = tmp1 & 0xFF;
                            regs[REG_B] = (tmp2 >>> 8) & 0xFF;
                            regs[REG_C] = tmp2 & 0xFF;

                            if ((tmp2 == 0) || (regs[REG_A] == tmp)) {
                                return 16;
                            }
                            PC = (PC - 2) & 0xFFFF;
                            return 21;
                        case 0xBA: /* INDR */
                            tmp = (context.fireIO(regs[REG_C], true, 0) & 0xFF);
                            tmp1 = (regs[REG_H] << 8) | regs[REG_L];
                            memory.write(tmp1, (short) tmp);
                            tmp1 = (tmp1 - 1) & 0xFFFF;
                            regs[REG_H] = (tmp1 >>> 8);
                            regs[REG_L] = (tmp1 & 0xFF);
                            regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);
                            flags |= (FLAG_N | FLAG_Z);
                            if (regs[REG_B] == 0) {
                                return 16;
                            }
                            PC -= 2;
                            return 21;
                        case 0xBB: /* OTDR */
                            tmp1 = (regs[REG_H] << 8) | regs[REG_L];
                            context.fireIO(regs[REG_C], false, memory.read(tmp1));
                            tmp1 = (tmp1 - 1) & 0xFFFF;
                            regs[REG_H] = (tmp1 >>> 8);
                            regs[REG_L] = (tmp1 & 0xFF);
                            regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);
                            flags |= (FLAG_N | FLAG_Z);
                            if (regs[REG_B] == 0) {
                                return 16;
                            }
                            PC -= 2;
                            return 21;
                    /* LD (nn), ss */
                        case 0x43:
                        case 0x53:
                        case 0x63:
                        case 0x73:
                            tmp = memory.readWord(PC);
                            PC += 2;

                            tmp1 = getpair((OP >>> 4) & 3);
                            memory.writeWord(tmp, tmp1);
                            return 20;
                    /* LD ss,(nn) */
                        case 0x4B:
                        case 0x5B:
                        case 0x6B:
                        case 0x7B:
                            tmp = memory.readWord(PC);
                            PC += 2;

                            tmp1 = memory.readWord(tmp);
                            putpair((OP >>> 4) & 3, tmp1);
                            return 20;
                    }
                    currentRunState = RunState.STATE_STOPPED_BAD_INSTR;
                    return 0;
                case 0xDD:
                    special = 0xDD;
                case 0xFD:
                    if (OP == 0xFD) {
                        special = 0xFD;
                    }
                    OP = memory.read(PC++);
                    switch (OP) {
                    /* ADD ii,pp */
                        case 0x09:
                        case 0x19:
                        case 0x29:
                        case 0x39:
                            tmp = getspecial(special);
                            tmp1 = getpair(special, (OP >>> 4) & 0x03);

                            carry15(tmp, tmp1);
                            halfCarry11(tmp, tmp1);
                            flags &= (~FLAG_N);

                            tmp += tmp1;
                            putspecial(special, tmp);
                            return 15;
                        case 0x23: /* INC ii */
                            if (special == 0xDD) {
                                IX = (IX + 1) & 0xFFFF;
                            } else {
                                IY = (IY + 1) & 0xFFFF;
                            }
                            return 10;
                        case 0x2B: /* DEC ii */
                            if (special == 0xDD) {
                                IX = (IX - 1) & 0xFFFF;
                            } else {
                                IY = (IY - 1) & 0xFFFF;
                            }
                            return 10;
                        case 0xE1: /* POP ii */
                            if (special == 0xDD) {
                                IX = memory.readWord(SP);
                            } else {
                                IY = memory.readWord(SP);
                            }
                            SP += 2;
                            return 14;
                        case 0xE3: /* EX (SP),ii */
                            tmp = memory.readWord(SP);
                            if (special == 0xDD) {
                                tmp1 = IX;
                                IX = tmp;
                            } else {
                                tmp1 = IY;
                                IY = tmp;
                            }
                            memory.writeWord(SP, tmp1);
                            return 23;
                        case 0xE5: /* PUSH ii */
                            SP -= 2;
                            if (special == 0xDD) {
                                memory.writeWord(SP, IX);
                            } else {
                                memory.writeWord(SP, IY);
                            }
                            return 15;
                        case 0xE9: /* JP (ii) */
                            if (special == 0xDD) {
                                PC = IX;
                            } else {
                                PC = IY;
                            }
                            return 8;
                        case 0xF9: /* LD SP,ii */
                            SP = (special == 0xDD) ? IX : IY;
                            return 10;
                    }

                    tmp = memory.read(PC++);
                    switch (OP) {
                        case 0x76:
                            break;
                    /* LD r,(ii+d) */
                        case 0x46:
                        case 0x4E:
                        case 0x56:
                        case 0x5E:
                        case 0x66:
                        case 0x6E:
                        case 0x7E:
                            tmp1 = (OP >>> 3) & 7;
                            putreg2(tmp1, memory.read((getspecial(special) + (byte) tmp) & 0xFFFF));
                            return 19;
                    /* LD (ii+d),r */
                        case 0x70:
                        case 0x71:
                        case 0x72:
                        case 0x73:
                        case 0x74:
                        case 0x75:
                        case 0x77:
                            tmp1 = (OP & 7);
                            tmp2 = (getspecial(special) + (byte) tmp) & 0xFFFF;
                            memory.write(tmp2, (short) getreg2(tmp1));
                            return 19;
                        case 0x34: /* INC (ii+d) */
                            tmp1 = (getspecial(special) + (byte) tmp) & 0xFFFF;
                            tmp2 = (memory.read(tmp1) + 1) & 0xFF;

                            memory.write(tmp1, (short) tmp2);
                            flags = INC_TABLE[tmp2] | (flags & FLAG_C);
                            return 23;
                        case 0x35: /* DEC (ii+d) */
                            tmp1 = (getspecial(special) + (byte) tmp) & 0xFFFF;
                            tmp2 = (memory.read(tmp1) - 1) & 0xFF;
                            memory.write(tmp1, (short) tmp2);
                            flags = DEC_TABLE[tmp2] | (flags & FLAG_C);
                            return 23;
                        case 0x86: /* ADD A,(ii+d) */
                            tmp1 = regs[REG_A];
                            tmp2 = memory.read((getspecial(special) + (byte) tmp) & 0xFFFF) & 0xFF;

                            regs[REG_A] += tmp2;
                            flags = SIGN_ZERO_CARRY_TABLE[regs[REG_A] & 0x1FF];
                            regs[REG_A] = regs[REG_A] & 0xFF;

                            auxCarry(tmp1, tmp2);
                            overflow(tmp1, tmp2, regs[REG_A]);

                            return 19;
                        case 0x8E: /* ADC A,(ii+d) */
                            tmp1 = regs[REG_A];
                            tmp2 = memory.read((getspecial(special) + (byte) tmp) & 0xFFFF) & 0xFF;
                            if ((flags & FLAG_C) == FLAG_C) {
                                tmp2++;
                            }
                            regs[REG_A] += tmp2;
                            flags = SIGN_ZERO_CARRY_TABLE[regs[REG_A] & 0x1FF];
                            regs[REG_A] = regs[REG_A] & 0xFF;

                            auxCarry(tmp1, tmp2);
                            overflow(tmp1, tmp2, regs[REG_A]);

                            return 19;
                        case 0x96: /* SUB (ii+d) */
                            tmp1 = regs[REG_A];
                            tmp2 = -(memory.read((getspecial(special) + (byte) tmp) & 0xFFFF) & 0xFF);

                            regs[REG_A] += tmp2;
                            tmp2 &= 0xFF;

                            flags = SIGN_ZERO_CARRY_TABLE[regs[REG_A] & 0x1FF] | FLAG_N;
                            regs[REG_A] = (short) (regs[REG_A] & 0xFF);

                            auxCarry(tmp1, tmp2);
                            overflow(tmp1, tmp2, regs[REG_A]);

                            return 19;
                        case 0x9E: /* SBC A,(ii+d) */
                            tmp1 = regs[REG_A];
                            tmp2 = -(memory.read((getspecial(special) + (byte) tmp) & 0xFFFF) & 0xFF);
                            if ((flags & FLAG_C) == FLAG_C) {
                                tmp2--;
                            }
                            regs[REG_A] += tmp2;
                            tmp2 &= 0xFF;

                            flags = SIGN_ZERO_CARRY_TABLE[regs[REG_A] & 0x1FF] | FLAG_N;
                            regs[REG_A] = regs[REG_A] & 0xFF;

                            auxCarry(tmp1, tmp2);
                            overflow(tmp1, tmp2, regs[REG_A]);

                            return 19;
                        case 0xA6: /* AND (ii+d) */
                            tmp1 = memory.read((getspecial(special) + (byte) tmp) & 0xFFFF);
                            regs[REG_A] = (regs[REG_A] & tmp1) & 0xFF;
                            flags = AND_OR_XOR_TABLE[regs[REG_A]];
                            return 19;
                        case 0xAE: /* XOR (ii+d) */
                            tmp1 = memory.read((getspecial(special) + (byte) tmp) & 0xFFFF);
                            regs[REG_A] = ((regs[REG_A] ^ tmp1) & 0xff);
                            flags = AND_OR_XOR_TABLE[regs[REG_A]];
                            return 19;
                        case 0xB6: /* OR (ii+d) */
                            tmp1 = memory.read((getspecial(special) + (byte) tmp) & 0xFFFF);
                            regs[REG_A] = ((regs[REG_A] | tmp1) & 0xff);
                            flags = AND_OR_XOR_TABLE[regs[REG_A]];
                            return 19;
                        case 0xBE: /* CP (ii+d) */
                            diff = -memory.read((getspecial(special) + (byte) tmp) & 0xFFFF);
                            tmp2 = regs[REG_A] + diff;
                            diff &= 0xFF;

                            flags = SIGN_ZERO_CARRY_TABLE[tmp2 & 0x1FF] | FLAG_N;
                            auxCarry(regs[REG_A], diff);
                            overflow(regs[REG_A], diff, tmp2 & 0xFF);

                            return 19;
                    }
                    tmp |= ((memory.read(PC++)) << 8);
                    switch (OP) {
                        case 0x21: /* LD ii,nn */
                            putspecial(special, tmp);
                            return 14;
                        case 0x22: /* LD (nn),ii */
                            memory.writeWord(tmp, getspecial(special));
                            return 16;
                        case 0x2A: /* LD ii,(nn) */
                            tmp1 = memory.readWord(tmp);
                            putspecial(special, tmp1);
                            return 20;
                        case 0x36: /* LD (ii+d),n */
                            memory.write((getspecial(special) + (byte) (tmp & 0xFF)) & 0xFFFF, (short) ((tmp >>> 8)));
                            return 19;
                        case 0xCB:
                            OP = (short) ((tmp >>> 8) & 0xff);
                            tmp &= 0xff;
                            switch (OP) {
                            /* BIT b,(ii+d) */
                                case 0x46:
                                case 0x4E:
                                case 0x56:
                                case 0x5E:
                                case 0x66:
                                case 0x6E:
                                case 0x76:
                                case 0x7E:
                                    tmp2 = (OP >>> 3) & 7;
                                    tmp1 = memory.read((getspecial(special) + (byte) tmp) & 0xffff);
                                    flags = ((flags & 0x95) | FLAG_H | (((tmp1 & (1 << tmp2)) == 0) ? FLAG_Z : 0));
                                    return 20;
                            /* RES b,(ii+d) */
                                case 0x86:
                                case 0x8E:
                                case 0x96:
                                case 0x9E:
                                case 0xA6:
                                case 0xAE:
                                case 0xB6:
                                case 0xBE:
                                    tmp2 = (OP >>> 3) & 7;
                                    tmp3 = (getspecial(special) + (byte) tmp) & 0xffff;
                                    tmp1 = memory.read(tmp3);
                                    tmp1 = (tmp1 & (~(1 << tmp2)));
                                    memory.write(tmp3, (short) (tmp1 & 0xff));
                                    return 23;
                            /* SET b,(ii+d) */
                                case 0xC6:
                                case 0xCE:
                                case 0xD6:
                                case 0xDE:
                                case 0xE6:
                                case 0xEE:
                                case 0xF6:
                                case 0xFE:
                                    tmp2 = (OP >>> 3) & 7;
                                    tmp3 = (getspecial(special) + (byte) tmp) & 0xffff;
                                    tmp1 = memory.read(tmp3);
                                    tmp1 = (tmp1 | (1 << tmp2));
                                    memory.write(tmp3, (short) (tmp1 & 0xff));
                                    return 23;
                                case 0x06: /* RLC (ii+d) */
                                    tmp = (getspecial(special) + (byte) tmp) & 0xffff;
                                    tmp1 = memory.read(tmp);

                                    tmp2 = (tmp1 >>> 7) & 1;
                                    tmp1 = ((((tmp1 << 1) & 0xFF) | tmp2) & 0xFF);

                                    memory.write(tmp, (short)(tmp1 & 0xFF));
                                    flags = SIGN_ZERO_TABLE[tmp1] | PARITY_TABLE[tmp1] | tmp2;

                                    return 23;
                                case 0x0E: /* RRC (ii+d) */
                                    tmp = (getspecial(special) + (byte) tmp) & 0xffff;
                                    tmp1 = memory.read(tmp);

                                    tmp2 = tmp1 & 1;
                                    tmp1 = (((tmp1 >>> 1) & 0x7F) | (tmp2 << 7)) & 0xFF;

                                    memory.write(tmp, (short)(tmp1 & 0xFF));
                                    flags = SIGN_ZERO_TABLE[tmp1] | PARITY_TABLE[tmp1] | tmp2;

                                    return 23;
                                case 0x16: /* RL (ii+d) */
                                    tmp = (getspecial(special) + (byte) tmp) & 0xffff;
                                    tmp1 = memory.read(tmp);

                                    tmp2 = (tmp1 >>> 7) & 1;
                                    tmp1 = ((((tmp1 << 1) & 0xFF) | flags & FLAG_C) & 0xFF);
                                    memory.write(tmp, (short)(tmp1 & 0xFF));

                                    flags = SIGN_ZERO_TABLE[tmp1] | PARITY_TABLE[tmp1] | tmp2;

                                    return 23;
                                case 0x1E: /* RR (ii+d) */
                                    tmp = (getspecial(special) + (byte) tmp) & 0xffff;
                                    tmp1 = memory.read(tmp);

                                    tmp2 = tmp1 & 1;
                                    tmp1 = ((((tmp1 >> 1) & 0xFF) | (flags & FLAG_C) << 7) & 0xFF);
                                    memory.write(tmp, (short)(tmp1 & 0xFF));

                                    flags = SIGN_ZERO_TABLE[tmp1] | PARITY_TABLE[tmp1] | tmp2;

                                    return 23;
                                case 0x26: /* SLA (ii+d) */
                                    tmp2 = (getspecial(special) + (byte) tmp) & 0xffff;
                                    tmp1 = memory.read(tmp2);
                                    flags = ((tmp1 >>> 7) & 0xff);
                                    tmp1 <<= 1;
                                    memory.write(tmp2, (short) (tmp1 & 0xff));
                                    flags |= DAA_TABLE[tmp1 & 0xff];
                                    return 23;
                                case 0x2E: /* SRA (ii+d) */
                                    tmp2 = (getspecial(special) + (byte) tmp) & 0xffff;
                                    tmp1 = memory.read(tmp2);
                                    tmp3 = tmp1 & 0x80;
                                    flags = (tmp1 & 1);
                                    tmp1 >>>= 1;
                                    tmp1 |= tmp3;
                                    memory.write(tmp2, (short) (tmp1 & 0xff));
                                    flags |= DAA_TABLE[tmp1];
                                    return 23;
                                case 0x36: /* SLL (ii+d) unsupported */
                                    tmp2 = (getspecial(special) + (byte) tmp) & 0xffff;
                                    tmp1 = memory.read(tmp2);
                                    flags = ((tmp1 >>> 7) & 0xff);
                                    tmp3 = tmp1 & 1;
                                    tmp1 <<= 1;
                                    tmp1 |= tmp3;
                                    memory.write(tmp2, (short) (tmp1 & 0xff));
                                    flags |= DAA_TABLE[tmp1 & 0xff];
                                    return 23;
                                case 0x3E: /* SRL (ii+d) */
                                    tmp2 = (getspecial(special) + (byte) tmp) & 0xffff;
                                    tmp1 = memory.read(tmp2);
                                    flags = (tmp1 & 1);
                                    tmp1 >>>= 1;
                                    memory.write(tmp2, (short) (tmp1 & 0xff));
                                    flags |= ((tmp1 == 0) ? FLAG_Z : 0) | PARITY_TABLE[tmp1];
                                    return 23;
                            }
                            currentRunState = RunState.STATE_STOPPED_BAD_INSTR;
                            return 0;
                    }
                    currentRunState = RunState.STATE_STOPPED_BAD_INSTR;
                    return 0;
                case 0xCB:
                    OP = memory.read(PC++);
                    switch (OP) {
                    /* RLC r */
                        case 0x00:
                        case 0x01:
                        case 0x02:
                        case 0x03:
                        case 0x04:
                        case 0x05:
                        case 0x06:
                        case 0x07:
                            tmp = OP & 7;
                            tmp1 = getreg(tmp);

                            tmp2 = (tmp1 >>> 7) & 1;
                            tmp1 = (((tmp1 << 1) & 0xFF) | tmp2) & 0xFF;
                            putreg(tmp, tmp1);

                            flags = SIGN_ZERO_TABLE[tmp1] | PARITY_TABLE[tmp1] | tmp2;

                            if (tmp == 6) {
                                return 15;
                            } else {
                                return 8;
                            }
                    /* RRC r */
                        case 0x08:
                        case 0x09:
                        case 0x0A:
                        case 0x0B:
                        case 0x0C:
                        case 0x0D:
                        case 0x0E:
                        case 0x0F:
                            tmp = OP & 7;
                            tmp1 = getreg(tmp);

                            tmp2 = tmp1 & 1;
                            tmp1 = (((tmp1 >>> 1) & 0x7F) | (tmp2 << 7)) & 0xFF;
                            putreg(tmp, tmp1);

                            flags = SIGN_ZERO_TABLE[tmp1] | PARITY_TABLE[tmp1] | tmp2;

                            if (tmp == 6) {
                                return 15;
                            } else {
                                return 8;
                            }
                    /* RL r */
                        case 0x10:
                        case 0x11:
                        case 0x12:
                        case 0x13:
                        case 0x14:
                        case 0x15:
                        case 0x16:
                        case 0x17:
                            tmp = OP & 7;
                            tmp1 = getreg(tmp);

                            tmp2 = (tmp1 >>> 7) & 1;
                            tmp1 = ((((tmp1 << 1) & 0xFF) | flags & FLAG_C) & 0xFF);
                            putreg(tmp, tmp1);

                            flags = SIGN_ZERO_TABLE[tmp1] | PARITY_TABLE[tmp1] | tmp2;

                            if (tmp == 6) {
                                return 15;
                            } else {
                                return 8;
                            }
                    /* RR r */
                        case 0x18:
                        case 0x19:
                        case 0x1A:
                        case 0x1B:
                        case 0x1C:
                        case 0x1D:
                        case 0x1E:
                        case 0x1F:
                            tmp = OP & 7;
                            tmp1 = getreg(tmp);

                            tmp2 = tmp1 & 1;
                            tmp1 = ((((tmp1 >> 1) & 0x7F) | (flags & FLAG_C) << 7) & 0xFF);
                            putreg(tmp, tmp1);

                            flags = SIGN_ZERO_TABLE[tmp1] | PARITY_TABLE[tmp1] | tmp2;

                            if (tmp == 6) {
                                return 15;
                            } else {
                                return 8;
                            }
                    /* SLA r */
                        case 0x20:
                        case 0x21:
                        case 0x22:
                        case 0x23:
                        case 0x24:
                        case 0x25:
                        case 0x26:
                        case 0x27:
                            tmp = OP & 7;
                            tmp1 = getreg(tmp);
                            flags = (tmp1 >>> 7);
                            tmp1 <<= 1;
                            putreg(tmp, tmp1);
                            flags |= DAA_TABLE[tmp1 & 0xff];
                            if (tmp == 6) {
                                return 15;
                            } else {
                                return 8;
                            }
                    /* SRA r */
                        case 0x28:
                        case 0x29:
                        case 0x2A:
                        case 0x2B:
                        case 0x2C:
                        case 0x2D:
                        case 0x2E:
                        case 0x2F:
                            tmp = OP & 7;
                            tmp1 = getreg(tmp);
                            tmp2 = tmp1 & 0x80;
                            flags = (tmp1 & 1);
                            tmp1 >>>= 1;
                            tmp1 |= tmp2;
                            putreg(tmp, tmp1);
                            flags |= DAA_TABLE[tmp1];
                            if (tmp == 6) {
                                return 15;
                            } else {
                                return 8;
                            }
                    /* SLL r - unsupported */
                        case 0x30:
                        case 0x31:
                        case 0x32:
                        case 0x33:
                        case 0x34:
                        case 0x35:
                        case 0x36:
                        case 0x37:
                            tmp = OP & 7;
                            tmp1 = getreg(tmp);
                            flags = (tmp1 >>> 7);
                            tmp2 = tmp1 & 1;
                            tmp1 <<= 1;
                            tmp1 |= tmp2;
                            putreg(tmp, tmp1);
                            flags |= DAA_TABLE[tmp1 & 0xff];
                            if (tmp == 6) {
                                return 15;
                            } else {
                                return 8;
                            }
                    /* SRL r */
                        case 0x38:
                        case 0x39:
                        case 0x3A:
                        case 0x3B:
                        case 0x3C:
                        case 0x3D:
                        case 0x3E:
                        case 0x3F:
                            tmp = OP & 7;
                            tmp1 = getreg(tmp);
                            flags = (tmp1 & 1);
                            tmp1 >>>= 1;
                            putreg(tmp, tmp1);
                            flags |= ((tmp1 == 0) ? FLAG_Z : 0) | PARITY_TABLE[tmp1];
                            if (tmp == 6) {
                                return 15;
                            } else {
                                return 8;
                            }
                    }
                    switch (OP & 0xC0) {
                        case 0x40: /* BIT b,r */
                            tmp = (OP >>> 3) & 7;
                            tmp2 = OP & 7;
                            tmp1 = getreg(tmp2);
                            flags = ((flags & 0x95) | FLAG_H | (((tmp1 & (1 << tmp)) == 0) ? FLAG_Z : 0));
                            if (tmp2 == 6) {
                                return 12;
                            } else {
                                return 8;
                            }
                        case 0x80: /* RES b,r */
                            tmp = (OP >>> 3) & 7;
                            tmp2 = OP & 7;
                            tmp1 = getreg(tmp2);
                            tmp1 = (tmp1 & (~(1 << tmp)));
                            putreg(tmp2, tmp1);
                            if (tmp2 == 6) {
                                return 15;
                            } else {
                                return 8;
                            }
                        case 0xC0: /* SET b,r */
                            tmp = (OP >>> 3) & 7;
                            tmp2 = OP & 7;
                            tmp1 = getreg(tmp2);
                            tmp1 = (tmp1 | (1 << tmp));
                            putreg(tmp2, tmp1);
                            if (tmp2 == 6) {
                                return 15;
                            } else {
                                return 8;
                            }
                    }
                    currentRunState = RunState.STATE_STOPPED_BAD_INSTR;
                    return 0;
            }
            tmp = memory.read(PC++);
            switch (OP) {
            /* JR cc,d */
                case 0x20:
                case 0x28:
                case 0x30:
                case 0x38:
                    if (getCC1((OP >>> 3) & 3)) {
                        PC += (byte) tmp;
                        PC &= 0xFFFF;
                        return 12;
                    }
                    return 7;
                case 0x18: /* JR e */
                    PC += (byte) tmp;
                    PC &= 0xFFFF;
                    return 12;
                case 0xC6: /* ADD A,d */
                    int DAR = regs[REG_A];
                    int diff = tmp;
                    regs[REG_A] += diff;

                    flags = SIGN_ZERO_CARRY_TABLE[regs[REG_A] & 0x1FF];
                    regs[REG_A] = regs[REG_A] & 0xFF;

                    auxCarry(DAR, diff);
                    overflow(DAR, diff, regs[REG_A]);

                    return 7;
                case 0xCE: /* ADC A,d */
                    DAR = regs[REG_A];
                    diff = tmp;
                    if ((flags & FLAG_C) != 0) {
                        diff++;
                    }
                    regs[REG_A] += diff;

                    flags = SIGN_ZERO_CARRY_TABLE[regs[REG_A] & 0x1FF];
                    regs[REG_A] = regs[REG_A] & 0xFF;

                    auxCarry(DAR, diff);
                    overflow(DAR, diff, regs[REG_A]);

                    return 7;
                case 0xD3: /* OUT (d),A */
                    context.fireIO(tmp, false, (short) regs[REG_A]);
                    return 11;
                case 0xD6: /* SUB d */
                    tmp1 = regs[REG_A];
                    tmp = -tmp;
                    regs[REG_A] += tmp;
                    tmp &= 0xFF;

                    flags = SIGN_ZERO_CARRY_TABLE[regs[REG_A] & 0x1FF] | FLAG_N;
                    regs[REG_A] = regs[REG_A] & 0xFF;

                    auxCarry(tmp1, tmp);
                    overflow(tmp1, tmp, regs[REG_A]);

                    return 7;
                case 0xDB: /* IN A,(d) */
                    regs[REG_A] = (context.fireIO(tmp, true, 0) & 0xFF);
                    return 11;
                case 0xDE: /* SBC A,d */
                    tmp2 = regs[REG_A];
                    diff = -tmp;
                    if ((flags & FLAG_C) != 0) {
                        diff--;
                    }
                    regs[REG_A] += diff;
                    diff &= 0xFF;

                    flags = SIGN_ZERO_CARRY_TABLE[regs[REG_A] & 0x1FF] | FLAG_N;
                    regs[REG_A] = regs[REG_A] & 0xFF;

                    auxCarry(tmp2, diff);
                    overflow(tmp2, diff, regs[REG_A]);

                    return 7;
                case 0xE6: /* AND d */
                    regs[REG_A] = (regs[REG_A] & tmp) & 0xFF;
                    flags = AND_OR_XOR_TABLE[regs[REG_A]];
                    return 7;
                case 0xEE: /* XOR d */
                    regs[REG_A] = ((regs[REG_A] ^ tmp) & 0xFF);
                    flags = AND_OR_XOR_TABLE[regs[REG_A]];
                    return 7;
                case 0xF6: /* OR d */
                    regs[REG_A] = (regs[REG_A] | tmp) & 0xFF;
                    flags = AND_OR_XOR_TABLE[regs[REG_A]];
                    return 7;
                case 0xFE: /* CP d */
                    tmp = -tmp;
                    tmp2 = regs[REG_A] + tmp;
                    tmp &= 0xFF;

                    flags = SIGN_ZERO_CARRY_TABLE[tmp2 & 0x1FF] | FLAG_N;
                    auxCarry(regs[REG_A], tmp);
                    overflow(regs[REG_A], tmp, tmp2 & 0xFF);

                    return 7;
            }
            tmp += (memory.read(PC++) << 8);
            switch (OP) {
            /* LD ss, nn */
                case 0x01:
                case 0x11:
                case 0x21:
                case 0x31:
                    putpair((OP >>> 4) & 3, tmp);
                    return 10;
            /* JP cc,nn */
                case 0xC2:
                case 0xCA:
                case 0xD2:
                case 0xDA:
                case 0xE2:
                case 0xEA:
                case 0xF2:
                case 0xFA:
                    tmp1 = (OP >>> 3) & 7;
                    if ((flags & CONDITION[tmp1]) == CONDITION_VALUES[tmp1]) {
                        PC = tmp;
                    }
                    return 10;
            /* CALL cc,nn */
                case 0xC4:
                case 0xCC:
                case 0xD4:
                case 0xDC:
                case 0xE4:
                case 0xEC:
                case 0xF4:
                case 0xFC:
                    tmp1 = (OP >>> 3) & 7;
                    if ((flags & CONDITION[tmp1]) == CONDITION_VALUES[tmp1]) {
                        SP = (SP - 2) & 0xffff;
                        memory.writeWord(SP, PC);
                        PC = tmp;
                        return 17;
                    }
                    return 10;
                case 0x22: /* LD (nn),HL */
                    tmp1 = getpair(2);
                    memory.writeWord(tmp, tmp1);
                    return 16;
                case 0x2A: /* LD HL,(nn) */
                    tmp1 = memory.readWord(tmp);
                    putpair(2, tmp1);
                    return 16;
                case 0x32: /* LD (nn),A */
                    memory.write(tmp, (short) regs[REG_A]);
                    return 13;
                case 0x3A: /* LD A,(nn) */
                    regs[REG_A] = (memory.read(tmp) & 0xff);
                    return 13;
                case 0xC3: /* JP nn */
                    PC = tmp;
                    return 10;
                case 0xCD: /* CALL nn */
                    SP = (SP - 2) & 0xffff;
                    memory.writeWord(SP, PC);
                    PC = tmp;
                    return 17;
            }
            currentRunState = RunState.STATE_STOPPED_BAD_INSTR;
        } finally {
            if (tmpListener != null) {
                tmpListener.afterDispatch();
            }
        }
        return 0;
    }

}
