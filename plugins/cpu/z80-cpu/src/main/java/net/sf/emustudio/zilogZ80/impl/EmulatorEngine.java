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
package net.sf.emustudio.zilogZ80.impl;

import emulib.plugins.cpu.CPU;
import emulib.plugins.cpu.CPU.RunState;
import emulib.plugins.device.DeviceContext;
import emulib.plugins.memory.MemoryContext;
import net.sf.emustudio.intel8080.api.CpuEngine;
import net.sf.emustudio.intel8080.api.DispatchListener;
import net.sf.emustudio.intel8080.api.FrequencyChangedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.LockSupport;

import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.AND_OR_XOR_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.DAA_C_H_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.DAA_C_NOT_H_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.DAA_NOT_C_H_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.DAA_NOT_C_NOT_H_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.DAA_NOT_N_H_FOR_H_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.DAA_NOT_N_NOT_H_FOR_H_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.DAA_N_H_FOR_H_TABLE;
import static net.sf.emustudio.zilogZ80.impl.EmulatorTables.DAA_N_NOT_H_FOR_H_TABLE;
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
@SuppressWarnings("unused")
public class EmulatorEngine implements CpuEngine {
    private final static Logger LOGGER = LoggerFactory.getLogger(EmulatorEngine.class);

    public static final int REG_A = 7, REG_B = 0, REG_C = 1, REG_D = 2, REG_E = 3, REG_H = 4, REG_L = 5;
    public static final int FLAG_S = 0x80, FLAG_Z = 0x40, FLAG_H = 0x10, FLAG_PV = 0x4, FLAG_N = 0x02, FLAG_C = 0x1;

    private final static int[] CONDITION = new int[] {
            FLAG_Z, FLAG_Z, FLAG_C, FLAG_C, FLAG_PV, FLAG_PV, FLAG_S, FLAG_S
    };
    private final static int[] CONDITION_VALUES = new int[] {
            0, FLAG_Z, 0, FLAG_C, 0, FLAG_PV, 0, FLAG_S
    };

    private final ContextImpl context;
    private final MemoryContext<Short> memory;
    private final List<FrequencyChangedListener> frequencyChangedListeners = new CopyOnWriteArrayList<>();

    public final int[] regs = new int[8];
    public final int[] regs2 = new int[8];
    public int flags = 2;
    public int flags2 = 2;    
    
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
    private long executedCycles = 0;

    private volatile DispatchListener dispatchListener;

    public EmulatorEngine(MemoryContext<Short> memory, ContextImpl context) {
        this.memory = Objects.requireNonNull(memory);
        this.context = Objects.requireNonNull(context);
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

    void reset(int startPos) {
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

    CPU.RunState step() throws Exception {
        boolean oldIFF = IFF[0];
        noWait = false;
        currentRunState = CPU.RunState.STATE_STOPPED_BREAK;
        short opcode = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        dispatch(opcode);
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
                    short opcode = memory.read(PC);
                    PC = (PC + 1) & 0xFFFF;
                    cycles = dispatch(opcode);
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
                } catch (IOException e) {
                    LOGGER.error("Unexpected error", e);
                    return RunState.STATE_STOPPED_BAD_INSTR;
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

    void setInterrupt(DeviceContext device, int mask) {
        this.interruptDevice = device;
        this.interruptPending |= mask;
    }

    void clearInterrupt(DeviceContext device, int mask) {
        if (interruptDevice == device) {
            this.interruptPending &= ~mask;
        }
    }

    void setInterruptVector(byte[] vector) {
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

    private void putpair(int reg, int val, boolean reg3IsSP) {
        int high = (val >>> 8) & 0xFF;
        int low = val & 0xFF;
        int index = reg * 2;

        if (reg == 3) {
            if (reg3IsSP) {
                SP = val;
            } else {
                regs[REG_A] = high;
                flags = (short)low;
            }
        } else {
            regs[index] = high;
            regs[index+1] = low;
        }
    }

    private int getpair(int reg, boolean reg3IsSP) {
        if (reg == 3) {
            return reg3IsSP ? SP : (regs[REG_A] << 8 | (flags & 0xFF));
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

    private int readWord(int address) {
        Short[] read = memory.readWord(address);
        return (read[1] << 8) | read[0];
    }

    private void writeWord(int address, int value) {
        memory.writeWord(address, new Short[] { (short)(value & 0xFF), (short)((value >>> 8) & 0xFF) } );
    }

    private int doInterrupt() throws IOException, InvocationTargetException, IllegalAccessException {
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
                dispatch((short) interruptVector); // must ignore halt
                if (currentRunState == RunState.STATE_STOPPED_NORMAL) {
                    currentRunState = old_runstate;
                }
                break;
            case 1: // rst 0xFF
                cycles += 12;
                writeWord(SP - 2, PC);
                SP = (SP - 2) & 0xffff;
                PC = 0xFF & 0x38;
                break;
            case 2:
                cycles += 13;
                writeWord(SP - 2, PC);
                PC = readWord((I << 8) | interruptVector);
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

    private void incrementR() {
        R = (R & 0x80) | (((R & 0x7F) + 1) & 0x7F);
    }


    private final static Method[] DISPATCH_TABLE = new Method[256];
    private final static Method[] DISPATCH_TABLE_ED = new Method[256];
    private final static Method[] DISPATCH_TABLE_DD_FD = new Method[256];

    static {
        try {
            DISPATCH_TABLE[0x00] = EmulatorEngine.class.getDeclaredMethod("O0_NOP", short.class);
            DISPATCH_TABLE[0x02] = EmulatorEngine.class.getDeclaredMethod("O2_LD_LPAR_BC_RPAR__A", short.class);
            DISPATCH_TABLE[0x03] = EmulatorEngine.class.getDeclaredMethod("INC_SS", short.class);
            DISPATCH_TABLE[0x13] = EmulatorEngine.class.getDeclaredMethod("INC_SS", short.class);
            DISPATCH_TABLE[0x23] = EmulatorEngine.class.getDeclaredMethod("INC_SS", short.class);
            DISPATCH_TABLE[0x33] = EmulatorEngine.class.getDeclaredMethod("INC_SS", short.class);

            DISPATCH_TABLE[0x09] = EmulatorEngine.class.getDeclaredMethod("ADD_HL_SS", short.class);
            DISPATCH_TABLE[0x19] = EmulatorEngine.class.getDeclaredMethod("ADD_HL_SS", short.class);
            DISPATCH_TABLE[0x29] = EmulatorEngine.class.getDeclaredMethod("ADD_HL_SS", short.class);
            DISPATCH_TABLE[0x39] = EmulatorEngine.class.getDeclaredMethod("ADD_HL_SS", short.class);

            DISPATCH_TABLE[0x01] = EmulatorEngine.class.getDeclaredMethod("LD_SS_NN", short.class);
            DISPATCH_TABLE[0x11] = EmulatorEngine.class.getDeclaredMethod("LD_SS_NN", short.class);
            DISPATCH_TABLE[0x21] = EmulatorEngine.class.getDeclaredMethod("LD_SS_NN", short.class);
            DISPATCH_TABLE[0x31] = EmulatorEngine.class.getDeclaredMethod("LD_SS_NN", short.class);

            DISPATCH_TABLE[0x0B] = EmulatorEngine.class.getDeclaredMethod("DEC_SS", short.class);
            DISPATCH_TABLE[0x1B] = EmulatorEngine.class.getDeclaredMethod("DEC_SS", short.class);
            DISPATCH_TABLE[0x2B] = EmulatorEngine.class.getDeclaredMethod("DEC_SS", short.class);
            DISPATCH_TABLE[0x3B] = EmulatorEngine.class.getDeclaredMethod("DEC_SS", short.class);

            DISPATCH_TABLE[0xC1] = EmulatorEngine.class.getDeclaredMethod("POP_QQ", short.class);
            DISPATCH_TABLE[0xD1] = EmulatorEngine.class.getDeclaredMethod("POP_QQ", short.class);
            DISPATCH_TABLE[0xE1] = EmulatorEngine.class.getDeclaredMethod("POP_QQ", short.class);
            DISPATCH_TABLE[0xF1] = EmulatorEngine.class.getDeclaredMethod("POP_QQ", short.class);

            DISPATCH_TABLE[0xC5] = EmulatorEngine.class.getDeclaredMethod("PUSH_QQ", short.class);
            DISPATCH_TABLE[0xD5] = EmulatorEngine.class.getDeclaredMethod("PUSH_QQ", short.class);
            DISPATCH_TABLE[0xE5] = EmulatorEngine.class.getDeclaredMethod("PUSH_QQ", short.class);
            DISPATCH_TABLE[0xF5] = EmulatorEngine.class.getDeclaredMethod("PUSH_QQ", short.class);

            DISPATCH_TABLE[0x06] = EmulatorEngine.class.getDeclaredMethod("LD_R_N", short.class);
            DISPATCH_TABLE[0x0E] = EmulatorEngine.class.getDeclaredMethod("LD_R_N", short.class);
            DISPATCH_TABLE[0x16] = EmulatorEngine.class.getDeclaredMethod("LD_R_N", short.class);
            DISPATCH_TABLE[0x1E] = EmulatorEngine.class.getDeclaredMethod("LD_R_N", short.class);
            DISPATCH_TABLE[0x26] = EmulatorEngine.class.getDeclaredMethod("LD_R_N", short.class);
            DISPATCH_TABLE[0x2E] = EmulatorEngine.class.getDeclaredMethod("LD_R_N", short.class);
            DISPATCH_TABLE[0x36] = EmulatorEngine.class.getDeclaredMethod("LD_R_N", short.class);
            DISPATCH_TABLE[0x3E] = EmulatorEngine.class.getDeclaredMethod("LD_R_N", short.class);

            DISPATCH_TABLE[0x04] = EmulatorEngine.class.getDeclaredMethod("INC_R", short.class);
            DISPATCH_TABLE[0x0C] = EmulatorEngine.class.getDeclaredMethod("INC_R", short.class);
            DISPATCH_TABLE[0x14] = EmulatorEngine.class.getDeclaredMethod("INC_R", short.class);
            DISPATCH_TABLE[0x1C] = EmulatorEngine.class.getDeclaredMethod("INC_R", short.class);
            DISPATCH_TABLE[0x24] = EmulatorEngine.class.getDeclaredMethod("INC_R", short.class);
            DISPATCH_TABLE[0x2C] = EmulatorEngine.class.getDeclaredMethod("INC_R", short.class);
            DISPATCH_TABLE[0x34] = EmulatorEngine.class.getDeclaredMethod("INC_R", short.class);
            DISPATCH_TABLE[0x3C] = EmulatorEngine.class.getDeclaredMethod("INC_R", short.class);

            DISPATCH_TABLE[0x05] = EmulatorEngine.class.getDeclaredMethod("DEC_R", short.class);
            DISPATCH_TABLE[0x0D] = EmulatorEngine.class.getDeclaredMethod("DEC_R", short.class);
            DISPATCH_TABLE[0x15] = EmulatorEngine.class.getDeclaredMethod("DEC_R", short.class);
            DISPATCH_TABLE[0x1D] = EmulatorEngine.class.getDeclaredMethod("DEC_R", short.class);
            DISPATCH_TABLE[0x25] = EmulatorEngine.class.getDeclaredMethod("DEC_R", short.class);
            DISPATCH_TABLE[0x2D] = EmulatorEngine.class.getDeclaredMethod("DEC_R", short.class);
            DISPATCH_TABLE[0x35] = EmulatorEngine.class.getDeclaredMethod("DEC_R", short.class);
            DISPATCH_TABLE[0x3D] = EmulatorEngine.class.getDeclaredMethod("DEC_R", short.class);

            DISPATCH_TABLE[0xC0] = EmulatorEngine.class.getDeclaredMethod("RET_CC", short.class);
            DISPATCH_TABLE[0xC8] = EmulatorEngine.class.getDeclaredMethod("RET_CC", short.class);
            DISPATCH_TABLE[0xD0] = EmulatorEngine.class.getDeclaredMethod("RET_CC", short.class);
            DISPATCH_TABLE[0xD8] = EmulatorEngine.class.getDeclaredMethod("RET_CC", short.class);
            DISPATCH_TABLE[0xE0] = EmulatorEngine.class.getDeclaredMethod("RET_CC", short.class);
            DISPATCH_TABLE[0xE8] = EmulatorEngine.class.getDeclaredMethod("RET_CC", short.class);
            DISPATCH_TABLE[0xF0] = EmulatorEngine.class.getDeclaredMethod("RET_CC", short.class);
            DISPATCH_TABLE[0xF8] = EmulatorEngine.class.getDeclaredMethod("RET_CC", short.class);

            DISPATCH_TABLE[0xC2] = EmulatorEngine.class.getDeclaredMethod("JP_CC_NN", short.class);
            DISPATCH_TABLE[0xCA] = EmulatorEngine.class.getDeclaredMethod("JP_CC_NN", short.class);
            DISPATCH_TABLE[0xD2] = EmulatorEngine.class.getDeclaredMethod("JP_CC_NN", short.class);
            DISPATCH_TABLE[0xDA] = EmulatorEngine.class.getDeclaredMethod("JP_CC_NN", short.class);
            DISPATCH_TABLE[0xE2] = EmulatorEngine.class.getDeclaredMethod("JP_CC_NN", short.class);
            DISPATCH_TABLE[0xEA] = EmulatorEngine.class.getDeclaredMethod("JP_CC_NN", short.class);
            DISPATCH_TABLE[0xF2] = EmulatorEngine.class.getDeclaredMethod("JP_CC_NN", short.class);
            DISPATCH_TABLE[0xFA] = EmulatorEngine.class.getDeclaredMethod("JP_CC_NN", short.class);

            DISPATCH_TABLE[0xC4] = EmulatorEngine.class.getDeclaredMethod("CALL_CC_NN", short.class);
            DISPATCH_TABLE[0xCC] = EmulatorEngine.class.getDeclaredMethod("CALL_CC_NN", short.class);
            DISPATCH_TABLE[0xD4] = EmulatorEngine.class.getDeclaredMethod("CALL_CC_NN", short.class);
            DISPATCH_TABLE[0xDC] = EmulatorEngine.class.getDeclaredMethod("CALL_CC_NN", short.class);
            DISPATCH_TABLE[0xE4] = EmulatorEngine.class.getDeclaredMethod("CALL_CC_NN", short.class);
            DISPATCH_TABLE[0xEC] = EmulatorEngine.class.getDeclaredMethod("CALL_CC_NN", short.class);
            DISPATCH_TABLE[0xF4] = EmulatorEngine.class.getDeclaredMethod("CALL_CC_NN", short.class);
            DISPATCH_TABLE[0xFC] = EmulatorEngine.class.getDeclaredMethod("CALL_CC_NN", short.class);

            DISPATCH_TABLE[0xC7] = EmulatorEngine.class.getDeclaredMethod("RST_P", short.class);
            DISPATCH_TABLE[0xCF] = EmulatorEngine.class.getDeclaredMethod("RST_P", short.class);
            DISPATCH_TABLE[0xD7] = EmulatorEngine.class.getDeclaredMethod("RST_P", short.class);
            DISPATCH_TABLE[0xDF] = EmulatorEngine.class.getDeclaredMethod("RST_P", short.class);
            DISPATCH_TABLE[0xE7] = EmulatorEngine.class.getDeclaredMethod("RST_P", short.class);
            DISPATCH_TABLE[0xEF] = EmulatorEngine.class.getDeclaredMethod("RST_P", short.class);
            DISPATCH_TABLE[0xF7] = EmulatorEngine.class.getDeclaredMethod("RST_P", short.class);
            DISPATCH_TABLE[0xFF] = EmulatorEngine.class.getDeclaredMethod("RST_P", short.class);

            DISPATCH_TABLE[0x80] = EmulatorEngine.class.getDeclaredMethod("ADD_A_R", short.class);
            DISPATCH_TABLE[0x81] = EmulatorEngine.class.getDeclaredMethod("ADD_A_R", short.class);
            DISPATCH_TABLE[0x82] = EmulatorEngine.class.getDeclaredMethod("ADD_A_R", short.class);
            DISPATCH_TABLE[0x83] = EmulatorEngine.class.getDeclaredMethod("ADD_A_R", short.class);
            DISPATCH_TABLE[0x84] = EmulatorEngine.class.getDeclaredMethod("ADD_A_R", short.class);
            DISPATCH_TABLE[0x85] = EmulatorEngine.class.getDeclaredMethod("ADD_A_R", short.class);
            DISPATCH_TABLE[0x86] = EmulatorEngine.class.getDeclaredMethod("ADD_A_R", short.class);
            DISPATCH_TABLE[0x87] = EmulatorEngine.class.getDeclaredMethod("ADD_A_R", short.class);

            DISPATCH_TABLE[0x88] = EmulatorEngine.class.getDeclaredMethod("ADC_A_R", short.class);
            DISPATCH_TABLE[0x89] = EmulatorEngine.class.getDeclaredMethod("ADC_A_R", short.class);
            DISPATCH_TABLE[0x8A] = EmulatorEngine.class.getDeclaredMethod("ADC_A_R", short.class);
            DISPATCH_TABLE[0x8B] = EmulatorEngine.class.getDeclaredMethod("ADC_A_R", short.class);
            DISPATCH_TABLE[0x8C] = EmulatorEngine.class.getDeclaredMethod("ADC_A_R", short.class);
            DISPATCH_TABLE[0x8D] = EmulatorEngine.class.getDeclaredMethod("ADC_A_R", short.class);
            DISPATCH_TABLE[0x8E] = EmulatorEngine.class.getDeclaredMethod("ADC_A_R", short.class);
            DISPATCH_TABLE[0x8F] = EmulatorEngine.class.getDeclaredMethod("ADC_A_R", short.class);

            DISPATCH_TABLE[0x90] = EmulatorEngine.class.getDeclaredMethod("SUB_R", short.class);
            DISPATCH_TABLE[0x91] = EmulatorEngine.class.getDeclaredMethod("SUB_R", short.class);
            DISPATCH_TABLE[0x92] = EmulatorEngine.class.getDeclaredMethod("SUB_R", short.class);
            DISPATCH_TABLE[0x93] = EmulatorEngine.class.getDeclaredMethod("SUB_R", short.class);
            DISPATCH_TABLE[0x94] = EmulatorEngine.class.getDeclaredMethod("SUB_R", short.class);
            DISPATCH_TABLE[0x95] = EmulatorEngine.class.getDeclaredMethod("SUB_R", short.class);
            DISPATCH_TABLE[0x96] = EmulatorEngine.class.getDeclaredMethod("SUB_R", short.class);
            DISPATCH_TABLE[0x97] = EmulatorEngine.class.getDeclaredMethod("SUB_R", short.class);

            DISPATCH_TABLE[0x98] = EmulatorEngine.class.getDeclaredMethod("SBC_A_R", short.class);
            DISPATCH_TABLE[0x99] = EmulatorEngine.class.getDeclaredMethod("SBC_A_R", short.class);
            DISPATCH_TABLE[0x9A] = EmulatorEngine.class.getDeclaredMethod("SBC_A_R", short.class);
            DISPATCH_TABLE[0x9B] = EmulatorEngine.class.getDeclaredMethod("SBC_A_R", short.class);
            DISPATCH_TABLE[0x9C] = EmulatorEngine.class.getDeclaredMethod("SBC_A_R", short.class);
            DISPATCH_TABLE[0x9D] = EmulatorEngine.class.getDeclaredMethod("SBC_A_R", short.class);
            DISPATCH_TABLE[0x9E] = EmulatorEngine.class.getDeclaredMethod("SBC_A_R", short.class);
            DISPATCH_TABLE[0x9F] = EmulatorEngine.class.getDeclaredMethod("SBC_A_R", short.class);

            DISPATCH_TABLE[0xA0] = EmulatorEngine.class.getDeclaredMethod("AND_R", short.class);
            DISPATCH_TABLE[0xA1] = EmulatorEngine.class.getDeclaredMethod("AND_R", short.class);
            DISPATCH_TABLE[0xA2] = EmulatorEngine.class.getDeclaredMethod("AND_R", short.class);
            DISPATCH_TABLE[0xA3] = EmulatorEngine.class.getDeclaredMethod("AND_R", short.class);
            DISPATCH_TABLE[0xA4] = EmulatorEngine.class.getDeclaredMethod("AND_R", short.class);
            DISPATCH_TABLE[0xA5] = EmulatorEngine.class.getDeclaredMethod("AND_R", short.class);
            DISPATCH_TABLE[0xA6] = EmulatorEngine.class.getDeclaredMethod("AND_R", short.class);
            DISPATCH_TABLE[0xA7] = EmulatorEngine.class.getDeclaredMethod("AND_R", short.class);

            DISPATCH_TABLE[0xA8] = EmulatorEngine.class.getDeclaredMethod("XOR_R", short.class);
            DISPATCH_TABLE[0xA9] = EmulatorEngine.class.getDeclaredMethod("XOR_R", short.class);
            DISPATCH_TABLE[0xAA] = EmulatorEngine.class.getDeclaredMethod("XOR_R", short.class);
            DISPATCH_TABLE[0xAB] = EmulatorEngine.class.getDeclaredMethod("XOR_R", short.class);
            DISPATCH_TABLE[0xAC] = EmulatorEngine.class.getDeclaredMethod("XOR_R", short.class);
            DISPATCH_TABLE[0xAD] = EmulatorEngine.class.getDeclaredMethod("XOR_R", short.class);
            DISPATCH_TABLE[0xAE] = EmulatorEngine.class.getDeclaredMethod("XOR_R", short.class);
            DISPATCH_TABLE[0xAF] = EmulatorEngine.class.getDeclaredMethod("XOR_R", short.class);

            DISPATCH_TABLE[0xB0] = EmulatorEngine.class.getDeclaredMethod("OR_R", short.class);
            DISPATCH_TABLE[0xB1] = EmulatorEngine.class.getDeclaredMethod("OR_R", short.class);
            DISPATCH_TABLE[0xB2] = EmulatorEngine.class.getDeclaredMethod("OR_R", short.class);
            DISPATCH_TABLE[0xB3] = EmulatorEngine.class.getDeclaredMethod("OR_R", short.class);
            DISPATCH_TABLE[0xB4] = EmulatorEngine.class.getDeclaredMethod("OR_R", short.class);
            DISPATCH_TABLE[0xB5] = EmulatorEngine.class.getDeclaredMethod("OR_R", short.class);
            DISPATCH_TABLE[0xB6] = EmulatorEngine.class.getDeclaredMethod("OR_R", short.class);
            DISPATCH_TABLE[0xB7] = EmulatorEngine.class.getDeclaredMethod("OR_R", short.class);

            DISPATCH_TABLE[0xB8] = EmulatorEngine.class.getDeclaredMethod("CP_R", short.class);
            DISPATCH_TABLE[0xB9] = EmulatorEngine.class.getDeclaredMethod("CP_R", short.class);
            DISPATCH_TABLE[0xBA] = EmulatorEngine.class.getDeclaredMethod("CP_R", short.class);
            DISPATCH_TABLE[0xBB] = EmulatorEngine.class.getDeclaredMethod("CP_R", short.class);
            DISPATCH_TABLE[0xBC] = EmulatorEngine.class.getDeclaredMethod("CP_R", short.class);
            DISPATCH_TABLE[0xBD] = EmulatorEngine.class.getDeclaredMethod("CP_R", short.class);
            DISPATCH_TABLE[0xBE] = EmulatorEngine.class.getDeclaredMethod("CP_R", short.class);
            DISPATCH_TABLE[0xBF] = EmulatorEngine.class.getDeclaredMethod("CP_R", short.class);

            DISPATCH_TABLE[0x07] = EmulatorEngine.class.getDeclaredMethod("O7_RLCA", short.class);
            DISPATCH_TABLE[0x08] = EmulatorEngine.class.getDeclaredMethod("O8_EX_AF_AFF", short.class);
            DISPATCH_TABLE[0x0A] = EmulatorEngine.class.getDeclaredMethod("OA_LD_A_LPAR_BC_RPAR", short.class);
            DISPATCH_TABLE[0x0F] = EmulatorEngine.class.getDeclaredMethod("OF_RRCA", short.class);
            DISPATCH_TABLE[0x10] = EmulatorEngine.class.getDeclaredMethod("O10_DJNZ", short.class);
            DISPATCH_TABLE[0x12] = EmulatorEngine.class.getDeclaredMethod("O12_LD_LPAR_DE_RPAR_A", short.class);
            DISPATCH_TABLE[0x17] = EmulatorEngine.class.getDeclaredMethod("O17_RLA", short.class);
            DISPATCH_TABLE[0x18] = EmulatorEngine.class.getDeclaredMethod("O18_JR_E", short.class);
            DISPATCH_TABLE[0x1A] = EmulatorEngine.class.getDeclaredMethod("O1A_LD_A_LPAR_DE_RPAR", short.class);
            DISPATCH_TABLE[0x1F] = EmulatorEngine.class.getDeclaredMethod("O1F_RRA", short.class);
            DISPATCH_TABLE[0x22] = EmulatorEngine.class.getDeclaredMethod("O22_LD_LPAR_NN_RPAR_HL", short.class);
            DISPATCH_TABLE[0x2A] = EmulatorEngine.class.getDeclaredMethod("O2A_LD_HL_LPAR_NN_RPAR", short.class);
            DISPATCH_TABLE[0x32] = EmulatorEngine.class.getDeclaredMethod("O32_LD_LPAR_NN_RPAR_A", short.class);
            DISPATCH_TABLE[0x3A] = EmulatorEngine.class.getDeclaredMethod("O3A_LD_A_LPAR_NN_RPAR", short.class);
            DISPATCH_TABLE[0xC3] = EmulatorEngine.class.getDeclaredMethod("C3_JP_NN", short.class);
            DISPATCH_TABLE[0xC6] = EmulatorEngine.class.getDeclaredMethod("C6_ADD_A_d", short.class);
            DISPATCH_TABLE[0xCD] = EmulatorEngine.class.getDeclaredMethod("CD_CALL_NN", short.class);
            DISPATCH_TABLE[0xD3] = EmulatorEngine.class.getDeclaredMethod("D3_OUT_LPAR_D_RPAR_A", short.class);
            DISPATCH_TABLE[0xD6] = EmulatorEngine.class.getDeclaredMethod("D6_SUB_d", short.class);
            DISPATCH_TABLE[0xDB] = EmulatorEngine.class.getDeclaredMethod("DB_IN_A_LPAR_d_RPAR", short.class);
            DISPATCH_TABLE[0xDE] = EmulatorEngine.class.getDeclaredMethod("DE_SBC_A_d", short.class);
            DISPATCH_TABLE[0xE6] = EmulatorEngine.class.getDeclaredMethod("E6_AND_d", short.class);
            DISPATCH_TABLE[0xEE] = EmulatorEngine.class.getDeclaredMethod("EE_XOR_d", short.class);
            DISPATCH_TABLE[0xF6] = EmulatorEngine.class.getDeclaredMethod("F6_OR_d", short.class);
            DISPATCH_TABLE[0xFE] = EmulatorEngine.class.getDeclaredMethod("FE_CP_d", short.class);

            DISPATCH_TABLE[0x20] = EmulatorEngine.class.getDeclaredMethod("JR_CC_D", short.class);
            DISPATCH_TABLE[0x28] = EmulatorEngine.class.getDeclaredMethod("JR_CC_D", short.class);
            DISPATCH_TABLE[0x30] = EmulatorEngine.class.getDeclaredMethod("JR_CC_D", short.class);
            DISPATCH_TABLE[0x38] = EmulatorEngine.class.getDeclaredMethod("JR_CC_D", short.class);

            DISPATCH_TABLE[0x27] = EmulatorEngine.class.getDeclaredMethod("O27_DAA", short.class);
            DISPATCH_TABLE[0x2F] = EmulatorEngine.class.getDeclaredMethod("O2F_CPL", short.class);
            DISPATCH_TABLE[0x37] = EmulatorEngine.class.getDeclaredMethod("O37_SCF", short.class);
            DISPATCH_TABLE[0x3F] = EmulatorEngine.class.getDeclaredMethod("O3F_CCF", short.class);
            DISPATCH_TABLE[0xC9] = EmulatorEngine.class.getDeclaredMethod("C9_RET", short.class);
            DISPATCH_TABLE[0xCE] = EmulatorEngine.class.getDeclaredMethod("CE_ADC_A_d", short.class);
            DISPATCH_TABLE[0xD9] = EmulatorEngine.class.getDeclaredMethod("D9_EXX", short.class);
            DISPATCH_TABLE[0xE3] = EmulatorEngine.class.getDeclaredMethod("E3_EX_LPAR_SP_RPAR_HL", short.class);
            DISPATCH_TABLE[0xE9] = EmulatorEngine.class.getDeclaredMethod("E9_JP_LPAR_HL_RPAR", short.class);
            DISPATCH_TABLE[0xEB] = EmulatorEngine.class.getDeclaredMethod("EB_EX_DE_HL", short.class);
            DISPATCH_TABLE[0xF3] = EmulatorEngine.class.getDeclaredMethod("F3_DI", short.class);
            DISPATCH_TABLE[0xF9] = EmulatorEngine.class.getDeclaredMethod("F9_LD_SP_HL", short.class);
            DISPATCH_TABLE[0xFB] = EmulatorEngine.class.getDeclaredMethod("FB_EI", short.class);
            DISPATCH_TABLE[0xED] = EmulatorEngine.class.getDeclaredMethod("ED_DISPATCH", short.class);

            DISPATCH_TABLE_ED[0x77] = EmulatorEngine.class.getDeclaredMethod("O0_NOP", short.class);
            DISPATCH_TABLE_ED[0x7F] = EmulatorEngine.class.getDeclaredMethod("O0_NOP", short.class);

            DISPATCH_TABLE_ED[0x40] = EmulatorEngine.class.getDeclaredMethod("IN_r_LPAR_C_RPAR", short.class);
            DISPATCH_TABLE_ED[0x48] = EmulatorEngine.class.getDeclaredMethod("IN_r_LPAR_C_RPAR", short.class);
            DISPATCH_TABLE_ED[0x50] = EmulatorEngine.class.getDeclaredMethod("IN_r_LPAR_C_RPAR", short.class);
            DISPATCH_TABLE_ED[0x58] = EmulatorEngine.class.getDeclaredMethod("IN_r_LPAR_C_RPAR", short.class);
            DISPATCH_TABLE_ED[0x60] = EmulatorEngine.class.getDeclaredMethod("IN_r_LPAR_C_RPAR", short.class);
            DISPATCH_TABLE_ED[0x68] = EmulatorEngine.class.getDeclaredMethod("IN_r_LPAR_C_RPAR", short.class);
            DISPATCH_TABLE_ED[0x78] = EmulatorEngine.class.getDeclaredMethod("IN_r_LPAR_C_RPAR", short.class);

            DISPATCH_TABLE_ED[0x41] = EmulatorEngine.class.getDeclaredMethod("OUT_LPAR_C_RPAR_r", short.class);
            DISPATCH_TABLE_ED[0x49] = EmulatorEngine.class.getDeclaredMethod("OUT_LPAR_C_RPAR_r", short.class);
            DISPATCH_TABLE_ED[0x51] = EmulatorEngine.class.getDeclaredMethod("OUT_LPAR_C_RPAR_r", short.class);
            DISPATCH_TABLE_ED[0x59] = EmulatorEngine.class.getDeclaredMethod("OUT_LPAR_C_RPAR_r", short.class);
            DISPATCH_TABLE_ED[0x61] = EmulatorEngine.class.getDeclaredMethod("OUT_LPAR_C_RPAR_r", short.class);
            DISPATCH_TABLE_ED[0x69] = EmulatorEngine.class.getDeclaredMethod("OUT_LPAR_C_RPAR_r", short.class);
            DISPATCH_TABLE_ED[0x79] = EmulatorEngine.class.getDeclaredMethod("OUT_LPAR_C_RPAR_r", short.class);

            DISPATCH_TABLE_ED[0x42] = EmulatorEngine.class.getDeclaredMethod("SBC_HL_SS", short.class);
            DISPATCH_TABLE_ED[0x52] = EmulatorEngine.class.getDeclaredMethod("SBC_HL_SS", short.class);
            DISPATCH_TABLE_ED[0x62] = EmulatorEngine.class.getDeclaredMethod("SBC_HL_SS", short.class);
            DISPATCH_TABLE_ED[0x72] = EmulatorEngine.class.getDeclaredMethod("SBC_HL_SS", short.class);

            DISPATCH_TABLE_ED[0x4A] = EmulatorEngine.class.getDeclaredMethod("ADC_HL_SS", short.class);
            DISPATCH_TABLE_ED[0x5A] = EmulatorEngine.class.getDeclaredMethod("ADC_HL_SS", short.class);
            DISPATCH_TABLE_ED[0x6A] = EmulatorEngine.class.getDeclaredMethod("ADC_HL_SS", short.class);
            DISPATCH_TABLE_ED[0x7A] = EmulatorEngine.class.getDeclaredMethod("ADC_HL_SS", short.class);

            DISPATCH_TABLE_ED[0x44] = EmulatorEngine.class.getDeclaredMethod("NEG", short.class);
            DISPATCH_TABLE_ED[0x4C] = EmulatorEngine.class.getDeclaredMethod("NEG", short.class);
            DISPATCH_TABLE_ED[0x54] = EmulatorEngine.class.getDeclaredMethod("NEG", short.class);
            DISPATCH_TABLE_ED[0x5C] = EmulatorEngine.class.getDeclaredMethod("NEG", short.class);
            DISPATCH_TABLE_ED[0x64] = EmulatorEngine.class.getDeclaredMethod("NEG", short.class);
            DISPATCH_TABLE_ED[0x6C] = EmulatorEngine.class.getDeclaredMethod("NEG", short.class);
            DISPATCH_TABLE_ED[0x74] = EmulatorEngine.class.getDeclaredMethod("NEG", short.class);
            DISPATCH_TABLE_ED[0x7C] = EmulatorEngine.class.getDeclaredMethod("NEG", short.class);

            DISPATCH_TABLE_ED[0x45] = EmulatorEngine.class.getDeclaredMethod("RETN", short.class);
            DISPATCH_TABLE_ED[0x55] = EmulatorEngine.class.getDeclaredMethod("RETN", short.class);
            DISPATCH_TABLE_ED[0x5D] = EmulatorEngine.class.getDeclaredMethod("RETN", short.class);
            DISPATCH_TABLE_ED[0x65] = EmulatorEngine.class.getDeclaredMethod("RETN", short.class);
            DISPATCH_TABLE_ED[0x6D] = EmulatorEngine.class.getDeclaredMethod("RETN", short.class);
            DISPATCH_TABLE_ED[0x75] = EmulatorEngine.class.getDeclaredMethod("RETN", short.class);
            DISPATCH_TABLE_ED[0x7D] = EmulatorEngine.class.getDeclaredMethod("RETN", short.class);

            DISPATCH_TABLE_ED[0x46] = EmulatorEngine.class.getDeclaredMethod("IM_0", short.class);
            DISPATCH_TABLE_ED[0x4E] = EmulatorEngine.class.getDeclaredMethod("IM_0", short.class);
            DISPATCH_TABLE_ED[0x66] = EmulatorEngine.class.getDeclaredMethod("IM_0", short.class);
            DISPATCH_TABLE_ED[0x6E] = EmulatorEngine.class.getDeclaredMethod("IM_0", short.class);

            DISPATCH_TABLE_ED[0x47] = EmulatorEngine.class.getDeclaredMethod("O47_LD_I_A", short.class);
            DISPATCH_TABLE_ED[0x4D] = EmulatorEngine.class.getDeclaredMethod("O4D_RETI", short.class);
            DISPATCH_TABLE_ED[0x4F] = EmulatorEngine.class.getDeclaredMethod("O4F_LD_R_A", short.class);

            DISPATCH_TABLE_ED[0x56] = EmulatorEngine.class.getDeclaredMethod("IM_1", short.class);
            DISPATCH_TABLE_ED[0x76] = EmulatorEngine.class.getDeclaredMethod("IM_1", short.class);

            DISPATCH_TABLE_ED[0x57] = EmulatorEngine.class.getDeclaredMethod("O57_LD_A_I", short.class);

            DISPATCH_TABLE_ED[0x5E] = EmulatorEngine.class.getDeclaredMethod("IM_2", short.class);
            DISPATCH_TABLE_ED[0x7E] = EmulatorEngine.class.getDeclaredMethod("IM_2", short.class);

            DISPATCH_TABLE_ED[0x5F] = EmulatorEngine.class.getDeclaredMethod("O5F_LD_A_R", short.class);
            DISPATCH_TABLE_ED[0x67] = EmulatorEngine.class.getDeclaredMethod("O67_RRD", short.class);
            DISPATCH_TABLE_ED[0x6F] = EmulatorEngine.class.getDeclaredMethod("O6F_RLD", short.class);
            DISPATCH_TABLE_ED[0x70] = EmulatorEngine.class.getDeclaredMethod("O70_IN_LPAR_C_RPAR", short.class);
            DISPATCH_TABLE_ED[0x71] = EmulatorEngine.class.getDeclaredMethod("O71_OUT_LPAR_C_RPAR_0", short.class);
            DISPATCH_TABLE_ED[0xA0] = EmulatorEngine.class.getDeclaredMethod("A0_LDI", short.class);
            DISPATCH_TABLE_ED[0xA1] = EmulatorEngine.class.getDeclaredMethod("A1_CPI", short.class);
            DISPATCH_TABLE_ED[0xA2] = EmulatorEngine.class.getDeclaredMethod("A2_INI", short.class);
            DISPATCH_TABLE_ED[0xA3] = EmulatorEngine.class.getDeclaredMethod("A3_OUTI", short.class);
            DISPATCH_TABLE_ED[0xA8] = EmulatorEngine.class.getDeclaredMethod("A8_LDD", short.class);
            DISPATCH_TABLE_ED[0xA9] = EmulatorEngine.class.getDeclaredMethod("A9_CPD", short.class);
            DISPATCH_TABLE_ED[0xAA] = EmulatorEngine.class.getDeclaredMethod("AA_IND", short.class);
            DISPATCH_TABLE_ED[0xAB] = EmulatorEngine.class.getDeclaredMethod("AB_OUTD", short.class);
            DISPATCH_TABLE_ED[0xB0] = EmulatorEngine.class.getDeclaredMethod("B0_LDIR", short.class);
            DISPATCH_TABLE_ED[0xB1] = EmulatorEngine.class.getDeclaredMethod("B1_CPIR", short.class);
            DISPATCH_TABLE_ED[0xB2] = EmulatorEngine.class.getDeclaredMethod("B2_INIR", short.class);
            DISPATCH_TABLE_ED[0xB3] = EmulatorEngine.class.getDeclaredMethod("B3_OTIR", short.class);
            DISPATCH_TABLE_ED[0xB8] = EmulatorEngine.class.getDeclaredMethod("B8_LDDR", short.class);
            DISPATCH_TABLE_ED[0xB9] = EmulatorEngine.class.getDeclaredMethod("B9_CPDR", short.class);
            DISPATCH_TABLE_ED[0xBA] = EmulatorEngine.class.getDeclaredMethod("BA_INDR", short.class);
            DISPATCH_TABLE_ED[0xBB] = EmulatorEngine.class.getDeclaredMethod("BB_OTDR", short.class);

            DISPATCH_TABLE_ED[0x43] = EmulatorEngine.class.getDeclaredMethod("LD_LPAR_N_RPAR_SS", short.class);
            DISPATCH_TABLE_ED[0x53] = EmulatorEngine.class.getDeclaredMethod("LD_LPAR_N_RPAR_SS", short.class);
            DISPATCH_TABLE_ED[0x63] = EmulatorEngine.class.getDeclaredMethod("LD_LPAR_N_RPAR_SS", short.class);
            DISPATCH_TABLE_ED[0x73] = EmulatorEngine.class.getDeclaredMethod("LD_LPAR_N_RPAR_SS", short.class);

            DISPATCH_TABLE_ED[0x4B] = EmulatorEngine.class.getDeclaredMethod("LD_SS_LPAR_N_RPAR", short.class);
            DISPATCH_TABLE_ED[0x5B] = EmulatorEngine.class.getDeclaredMethod("LD_SS_LPAR_N_RPAR", short.class);
            DISPATCH_TABLE_ED[0x6B] = EmulatorEngine.class.getDeclaredMethod("LD_SS_LPAR_N_RPAR", short.class);
            DISPATCH_TABLE_ED[0x7B] = EmulatorEngine.class.getDeclaredMethod("LD_SS_LPAR_N_RPAR", short.class);




        } catch (NoSuchMethodException | SecurityException e) {
            LOGGER.error("Could not set up dispatch table. The emulator won't work correctly", e);
        }
    }


    private int ED_DISPATCH(short OP) throws InvocationTargetException, IllegalAccessException {
        OP = memory.read(PC);
        incrementR();
        PC = (PC + 1) & 0xFFFF;

        Method instr = DISPATCH_TABLE_ED[OP];
        if (instr != null) {
            return (Integer) instr.invoke(this, OP);
        }
        currentRunState = RunState.STATE_STOPPED_BAD_INSTR;
        return 0;
    }

    private int DD_FD_DISPATCH(short OP) throws InvocationTargetException, IllegalAccessException {
        int special = OP;
        OP = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        incrementR();

        Method instr = DISPATCH_TABLE_DD_FD[OP];
        if (instr != null) {
            return (Integer) instr.invoke(this, OP, special);
        }
        currentRunState = RunState.STATE_STOPPED_BAD_INSTR;
        return 0;
    }


    private int O0_NOP(short OP) {
        return 4;
    }

    private int O2_LD_LPAR_BC_RPAR__A(short OP) {
        memory.write(getpair(0, false), (short) regs[REG_A]);
        return 7;
    }

    private int INC_SS(short OP) {
        int tmp = (OP >>> 4) & 0x03;
        putpair(tmp, (getpair(tmp, true) + 1) & 0xFFFF, true);
        return 6;
    }

    private int ADD_HL_SS(short OP) {
        int tmp = getpair((OP >>> 4) & 0x03, true);
        int tmp1 = getpair(2, true);
        carry15(tmp, tmp1);
        halfCarry11(tmp, tmp1);
        flags &= (~(FLAG_N | FLAG_S | FLAG_Z));
        tmp += tmp1;

        flags |= ((tmp & 0x8000) == 0x8000) ? FLAG_S : 0;
        flags |= (tmp == 0) ? FLAG_Z : 0;
        putpair(2, tmp & 0xFFFF, true);
        return 11;
    }

    private int DEC_SS(short OP) {
        int tmp = (OP >>> 4) & 0x03;
        putpair(tmp, (getpair(tmp, true) - 1) & 0xFFFF, true);
        return 6;
    }

    private int POP_QQ(short OP) {
        int tmp = (OP >>> 4) & 0x03;
        int tmp1 = readWord(SP);
        SP = (SP + 2) & 0xffff;
        putpair(tmp, tmp1, false);
        return 10;
    }

    private int PUSH_QQ(short OP) {
        int tmp = (OP >>> 4) & 0x03;
        int tmp1 = getpair(tmp, false);
        SP = (SP - 2) & 0xffff;
        writeWord(SP, tmp1);
        return 11;
    }

    private int LD_R_N(short OP) {
        int tmp = (OP >>> 3) & 0x07;
        putreg(tmp, memory.read(PC));
        PC = (PC + 1) & 0xFFFF;
        if (tmp == 6) {
            return 10;
        } else {
            return 7;
        }
    }

    private int INC_R(short OP) {
        int tmp = (OP >>> 3) & 0x07;
        int tmp1 = (getreg(tmp) + 1) & 0xFF;
        flags = INC_TABLE[tmp1] | (flags & FLAG_C);
        putreg(tmp, tmp1);
        return (tmp == 6) ? 11 : 4;
    }

    private int DEC_R(short OP) {
        int tmp = (OP >>> 3) & 0x07;
        int tmp1 = (getreg(tmp) - 1) & 0xFF;
        flags = DEC_TABLE[tmp1] | (flags & FLAG_C);
        putreg(tmp, tmp1);
        return (tmp == 6) ? 11 : 4;
    }

    private int RET_CC(short OP) {
        int tmp = (OP >>> 3) & 7;
        if ((flags & CONDITION[tmp]) == CONDITION_VALUES[tmp]) {
            PC = readWord(SP);
            SP = (SP + 2) & 0xffff;
            return 11;
        }
        return 5;
    }

    private int RST_P(short OP) {
        SP = (SP - 2) & 0xffff;
        writeWord(SP, PC);
        PC = OP & 0x38;
        return 11;
    }

    private int ADD_A_R(short OP) {
        int X = regs[REG_A];
        int diff = getreg(OP & 0x07);
        regs[REG_A] += diff;

        flags = SIGN_ZERO_CARRY_TABLE[regs[REG_A] & 0x1FF];
        regs[REG_A] = regs[REG_A] & 0xFF;

        auxCarry(X, diff);
        overflow(X, diff, regs[REG_A]);

        return ((OP & 0x07) == 6) ? 7 : 4;
    }

    private int ADC_A_R(short OP) {
        int X = regs[REG_A];
        int diff = getreg(OP & 0x07);
        if ((flags & FLAG_C) != 0) {
            diff++;
        }
        regs[REG_A] += diff;

        flags = SIGN_ZERO_CARRY_TABLE[regs[REG_A] & 0x1FF];
        regs[REG_A] = regs[REG_A] & 0xFF;

        auxCarry(X, diff);
        overflow(X, diff, regs[REG_A]);

        return ((OP & 0x07) == 6) ? 7 : 4;
    }

    private int SUB_R(short OP) {
        int X = regs[REG_A];
        int diff = -getreg(OP & 0x07);
        regs[REG_A] += diff;
        diff &= 0xFF;

        flags = SIGN_ZERO_CARRY_TABLE[regs[REG_A] & 0x1FF] | FLAG_N;
        regs[REG_A] = regs[REG_A] & 0xFF;

        auxCarry(X, diff);
        overflow(X, diff, regs[REG_A]);

        return ((OP & 0x07) == 6) ? 7 : 4;
    }

    private int SBC_A_R(short OP) {
        int X = regs[REG_A];
        int diff = -getreg(OP & 0x07);
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
    }

    private int AND_R(short OP) {
        regs[REG_A] = (regs[REG_A] & getreg(OP & 7)) & 0xFF;
        flags = AND_OR_XOR_TABLE[regs[REG_A]];
        return (OP == 0xA6) ? 7 : 4;
    }

    private int XOR_R(short OP) {
        regs[REG_A] = ((regs[REG_A] ^ getreg(OP & 7)) & 0xff);
        flags = AND_OR_XOR_TABLE[regs[REG_A]];
        return (OP == 0xAE) ? 7 : 4;
    }

    private int OR_R(short OP) {
        regs[REG_A] = (regs[REG_A] | getreg(OP & 7)) & 0xFF;
        flags = AND_OR_XOR_TABLE[regs[REG_A]];
        return (OP == 0xB6) ? 7 : 4;
    }

    private int CP_R(short OP) {
        int diff = -getreg(OP & 7);
        int tmp2 = regs[REG_A] + diff;
        diff &= 0xFF;

        flags = SIGN_ZERO_CARRY_TABLE[tmp2 & 0x1FF] | FLAG_N;
        auxCarry(regs[REG_A], diff);
        overflow(regs[REG_A], diff, tmp2 & 0xFF);

        return (OP == 0xBE) ? 7 : 4;
    }

    private int O7_RLCA(short OP) {
        int tmp = regs[REG_A] >>> 7;
        regs[REG_A] = ((((regs[REG_A] << 1) & 0xFF) | tmp) & 0xff);
        flags = ((flags & 0xEC) | tmp);
        return 4;
    }

    private int O8_EX_AF_AFF(short OP) {
        int tmp = regs[REG_A];
        regs[REG_A] = regs2[REG_A];
        regs2[REG_A] = tmp;
        tmp = flags;
        flags = flags2;
        flags2 = tmp;
        return 4;
    }

    private int OA_LD_A_LPAR_BC_RPAR(short OP) {
        regs[REG_A] = memory.read(getpair(0, false));
        return 7;
    }

    private int OF_RRCA(short OP) {
        flags = ((flags & 0xEC) | (regs[REG_A] & 1));
        regs[REG_A] = RRCA_TABLE[regs[REG_A]];
        return 4;
    }

    private int O10_DJNZ(short OP) {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_B]--;
        regs[REG_B] &= 0xFF;
        if (regs[REG_B] != 0) {
            PC += (byte) tmp;
            PC &= 0xFFFF;
            return 13;
        }
        return 8;
    }

    private int O12_LD_LPAR_DE_RPAR_A(short OP) {
        memory.write(getpair(1, false), (short) regs[REG_A]);
        return 7;
    }

    private int O17_RLA(short OP) {
        int tmp = regs[REG_A] >>> 7;
        regs[REG_A] = (((regs[REG_A] << 1) | (flags & FLAG_C)) & 0xff);
        flags = ((flags & 0xEC) | tmp);
        return 4;
    }

    private int O1A_LD_A_LPAR_DE_RPAR(short OP) {
        int tmp = memory.read(getpair(1, false));
        regs[REG_A] = (tmp & 0xff);
        return 7;
    }

    private int O1F_RRA(short OP) {
        int tmp = (flags & FLAG_C) << 7;
        flags = ((flags & 0xEC) | (regs[REG_A] & 1));
        regs[REG_A] = ((regs[REG_A] >>> 1 | tmp) & 0xff);
        return 4;
    }

    private int O27_DAA(short OP) {
        int temp = regs[REG_A];
        boolean acFlag = (flags & FLAG_H) == FLAG_H;
        boolean cFlag = (flags & FLAG_C) == FLAG_C;
        boolean nFlag = (flags & FLAG_N) == FLAG_N;

        if (!acFlag && !cFlag) {
            regs[REG_A] = DAA_NOT_C_NOT_H_TABLE[temp] & 0xFF;
            flags = (DAA_NOT_C_NOT_H_TABLE[temp] >> 8) & 0xFF | (flags & FLAG_N);
            if (nFlag) {
                flags |= DAA_N_NOT_H_FOR_H_TABLE[temp];
            } else {
                flags |= DAA_NOT_N_NOT_H_FOR_H_TABLE[temp];
            }
        } else if (acFlag && !cFlag) {
            regs[REG_A] = DAA_NOT_C_H_TABLE[temp] & 0xFF;
            flags = (DAA_NOT_C_H_TABLE[temp] >> 8) & 0xFF | (flags & FLAG_N);
            if (nFlag) {
                flags |= DAA_N_H_FOR_H_TABLE[temp];
            } else {
                flags |= DAA_NOT_N_H_FOR_H_TABLE[temp];
            }
        } else if (!acFlag && cFlag) {
            regs[REG_A] = DAA_C_NOT_H_TABLE[temp] & 0xFF;
            flags = (DAA_C_NOT_H_TABLE[temp] >> 8) & 0xFF | (flags & FLAG_N);
            if (nFlag) {
                flags |= DAA_N_NOT_H_FOR_H_TABLE[temp];
            } else {
                flags |= DAA_NOT_N_NOT_H_FOR_H_TABLE[temp];
            }
        } else {
            regs[REG_A] = DAA_C_H_TABLE[temp] & 0xFF;
            flags = (DAA_C_H_TABLE[temp] >> 8) & 0xFF | (flags & FLAG_N);
            if (nFlag) {
                flags |= DAA_N_H_FOR_H_TABLE[temp];
            } else {
                flags |= DAA_NOT_N_H_FOR_H_TABLE[temp];
            }
        }
        flags |= PARITY_TABLE[regs[REG_A]] | SIGN_ZERO_TABLE[regs[REG_A]];
        return 4;
    }

    private int O2F_CPL(short OP) {
        regs[REG_A] = ((~regs[REG_A]) & 0xFF);
        flags |= FLAG_N | FLAG_H;
        return 4;
    }

    private int O37_SCF(short OP) {
        flags |= FLAG_N | FLAG_C;
        flags &= ~FLAG_H;
        return 4;
    }

    private int O3F_CCF(short OP) {
        int tmp = flags & FLAG_C;
        if (tmp == 0) {
            flags |= FLAG_C;
        } else {
            flags &= ~FLAG_C;
        }
        flags &= ~FLAG_N;
        return 4;
    }

    private int C9_RET(short OP) {
        PC = readWord(SP);
        SP = (SP + 2) & 0xFFFF;
        return 10;
    }

    private int D9_EXX(short OP) {
        int tmp = regs[REG_B];
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
    }

    private int E3_EX_LPAR_SP_RPAR_HL(short OP) {
        int tmp = memory.read(SP);
        int x = (SP + 1) & 0xFFFF;
        int tmp1 = memory.read(x);
        memory.write(SP, (short) regs[REG_L]);
        memory.write(x, (short) regs[REG_H]);
        regs[REG_L] = tmp & 0xFF;
        regs[REG_H] = tmp1 & 0xFF;
        return 19;
    }

    private int E9_JP_LPAR_HL_RPAR(short OP) {
        PC = ((regs[REG_H] << 8) | regs[REG_L]);
        return 4;
    }

    private int EB_EX_DE_HL(short OP) {
        int tmp = regs[REG_D];
        regs[REG_D] = regs[REG_H];
        regs[REG_H] = tmp;
        tmp = regs[REG_E];
        regs[REG_E] = regs[REG_L];
        regs[REG_L] = tmp;
        return 4;
    }

    private int F3_DI(short OP) {
        IFF[0] = IFF[1] = false;
        return 4;
    }

    private int F9_LD_SP_HL(short OP) {
        SP = ((regs[REG_H] << 8) | regs[REG_L]);
        return 6;
    }

    private int FB_EI(short OP) {
        IFF[0] = IFF[1] = true;
        return 4;
    }

    private int IN_r_LPAR_C_RPAR(short OP) throws IOException {
        int tmp = (OP >>> 3) & 0x7;
        putreg(tmp, context.readIO(regs[REG_C]));
        flags = (flags & FLAG_C) | SIGN_ZERO_TABLE[regs[tmp]] | PARITY_TABLE[regs[tmp]];
        return 12;
    }

    private int OUT_LPAR_C_RPAR_r(short OP) throws IOException {
        int tmp = (OP >>> 3) & 0x7;
        context.writeIO(regs[REG_C], (short) getreg(tmp));
        return 12;
    }

    private int SBC_HL_SS(short OP) {
        int tmp = -getpair((OP >>> 4) & 0x03, true);
        int tmp1 = getpair(2, true);
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
        putpair(2, sum, true);

        return 15;
    }

    private int ADC_HL_SS(short OP) {
        int tmp = getpair((OP >>> 4) & 0x03, true);
        int tmp1 = getpair(2, true);
        if ((flags & FLAG_C) == FLAG_C) {
            tmp++;
        }
        int sum = tmp + tmp1;
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
        putpair(2, sum, false);

        return 11;
    }

    private int NEG(short OP) {
        flags = NEG_TABLE[regs[REG_A]] & 0xFF;
        regs[REG_A] = (NEG_TABLE[regs[REG_A]] >>> 8) & 0xFF;
        return 8;
    }

    private int RETN(short OP) {
        IFF[0] = IFF[1];
        PC = readWord(SP);
        SP = (SP + 2) & 0xffff;
        return 14;
    }

    private int IM_0(short OP) {
        intMode = 0;
        return 8;
    }

    private int O47_LD_I_A(short OP) {
        I = regs[REG_A];
        return 9;
    }

    private int O4D_RETI(short OP) {
        IFF[0] = IFF[1];
        PC = readWord(SP);
        SP = (SP + 2) & 0xffff;
        return 14;
    }

    private int O4F_LD_R_A(short OP) {
        R = regs[REG_A];
        return 9;
    }

    private int IM_1(short OP) {
        intMode = 1;
        return 8;
    }

    private int O57_LD_A_I(short OP) {
        regs[REG_A] = I & 0xFF;
        flags = SIGN_ZERO_TABLE[regs[REG_A]] | (IFF[1] ? FLAG_PV : 0) | (flags & FLAG_C);
        return 9;
    }

    private int IM_2(short OP) {
        intMode = 2;
        return 8;
    }

    private int O5F_LD_A_R(short OP) {
        regs[REG_A] = R & 0xFF;
        flags = SIGN_ZERO_TABLE[regs[REG_A]] | (IFF[1] ? FLAG_PV : 0) | (flags & FLAG_C);
        return 9;
    }

    private int O67_RRD(short OP) {
        int tmp = regs[REG_A] & 0x0F;
        int tmp1 = memory.read((regs[REG_H] << 8) | regs[REG_L]);
        regs[REG_A] = ((regs[REG_A] & 0xF0) | (tmp1 & 0x0F));
        tmp1 = ((tmp1 >>> 4) & 0x0F) | (tmp << 4);
        memory.write(((regs[REG_H] << 8) | regs[REG_L]), (short) (tmp1 & 0xff));
        flags = SIGN_ZERO_TABLE[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | (flags & FLAG_C);
        return 18;
    }

    private int O6F_RLD(short OP) {
        int tmp = memory.read((regs[REG_H] << 8) | regs[REG_L]);
        int tmp1 = (tmp >>> 4) & 0x0F;
        tmp = ((tmp << 4) & 0xF0) | (regs[REG_A] & 0x0F);
        regs[REG_A] = ((regs[REG_A] & 0xF0) | tmp1);
        memory.write((regs[REG_H] << 8) | regs[REG_L], (short) (tmp & 0xff));
        flags = SIGN_ZERO_TABLE[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | (flags & FLAG_C);
        return 18;
    }

    private int O70_IN_LPAR_C_RPAR(short OP) throws IOException {
        int tmp = (context.readIO(regs[REG_C]) & 0xFF);
        flags = SIGN_ZERO_TABLE[tmp] | PARITY_TABLE[tmp] | (flags & FLAG_C);
        return 12;
    }

    private int O71_OUT_LPAR_C_RPAR_0(short OP) throws IOException {
        context.writeIO(regs[REG_C], 0);
        return 12;
    }

    private int A0_LDI(short OP) {
        int tmp1 = getpair(2, false);
        int tmp2 = getpair(1, false);
        int tmp = getpair(0, false);

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
    }

    private int A1_CPI(short OP) {
        int tmp1 = (regs[REG_H] << 8) | regs[REG_L];
        int tmp2 = (regs[REG_B] << 8) | regs[REG_C];

        int tmp = memory.read(tmp1);
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
    }

    private int A2_INI(short OP) throws IOException {
        int tmp = context.readIO(regs[REG_C]) & 0xFF;
        int tmp1 = (regs[REG_H] << 8) | regs[REG_L];
        int tmp2 = tmp + (regs[REG_C] + 1) & 0xFF;

        memory.write(tmp1, (short) tmp);
        tmp1 = (tmp1 + 1) & 0xFFFF;
        regs[REG_H] = ((tmp1 >>> 8) & 0xff);
        regs[REG_L] = (tmp1 & 0xFF);
        regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);

        flags = SIGN_ZERO_TABLE[regs[REG_B]] | PARITY_TABLE[(tmp2 & 7) ^ regs[REG_B]];
        if ((tmp & 0x80) == 0x80) {
            flags |= FLAG_N;
        }
        if (tmp2 > 0xFF) {
            flags |= (FLAG_H | FLAG_C);
        }
        return 16;
    }

    private int A3_OUTI(short OP) throws IOException {
        int tmp1 = (regs[REG_H] << 8) | regs[REG_L];
        int tmp2 = memory.read(tmp1) & 0xFF;

        context.writeIO(regs[REG_C], tmp2);
        tmp1 = (tmp1 + 1) & 0xFFFF;
        regs[REG_H] = ((tmp1 >>> 8) & 0xff);
        regs[REG_L] = (tmp1 & 0xFF);
        regs[REG_B] = (regs[REG_B]- 1) & 0xFF;
        flags = SIGN_ZERO_TABLE[regs[REG_B]] | PARITY_TABLE[((tmp2 + regs[REG_L]) & 7) ^ regs[REG_B]];
        if ((tmp2 & 0x80) == 0x80) {
            flags |= FLAG_N;
        }
        if ((tmp2 + regs[REG_L]) > 0xFF) {
            flags |= FLAG_C | FLAG_H;
        }
        return 16;
    }

    private int A8_LDD(short OP) {
        int tmp1 = (regs[REG_H] << 8) | regs[REG_L];
        int tmp2 = (regs[REG_D] << 8) | regs[REG_E];
        int tmp = (regs[REG_B] << 8) | regs[REG_C];

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
    }

    private int A9_CPD(short OP) {
        int tmp1 = (regs[REG_H] << 8) | regs[REG_L];
        int tmp2 = (regs[REG_B] << 8) | regs[REG_C];

        int tmp = memory.read(tmp1);
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
    }

    private int AA_IND(short OP) throws IOException {
        int tmp = context.readIO(regs[REG_C]) & 0xFF;
        int tmp1 = (regs[REG_H] << 8) | regs[REG_L];
        int tmp2 = tmp + ((regs[REG_C] - 1) & 0xFF);

        memory.write(tmp1, (short) tmp);
        tmp1 = (tmp1 - 1) & 0xFFFF;
        regs[REG_H] = ((tmp1 >>> 8) & 0xff);
        regs[REG_L] = (tmp1 & 0xFF);
        regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);

        flags = SIGN_ZERO_TABLE[regs[REG_B]] | PARITY_TABLE[(tmp2 & 7) ^ regs[REG_B]];
        if ((tmp & 0x80) == 0x80) {
            flags |= FLAG_N;
        }
        if (tmp2 > 0xFF) {
            flags |= (FLAG_H | FLAG_C);
        }

        return 16;
    }

    private int AB_OUTD(short OP) throws IOException {
        int tmp1 = (regs[REG_H] << 8) | regs[REG_L];
        int tmp2 = memory.read(tmp1) & 0xFF;

        context.writeIO(regs[REG_C], tmp2);
        tmp1 = (tmp1 - 1) & 0xFFFF;
        regs[REG_H] = ((tmp1 >>> 8) & 0xff);
        regs[REG_L] = (tmp1 & 0xFF);
        regs[REG_B] = (regs[REG_B] - 1) & 0xFF;

        flags = SIGN_ZERO_TABLE[regs[REG_B]] | PARITY_TABLE[((tmp2 + regs[REG_L]) & 7) ^ regs[REG_B]];
        if ((tmp2 & 0x80) == 0x80) {
            flags |= FLAG_N;
        }
        if ((tmp2 + regs[REG_L]) > 0xFF) {
            flags |= FLAG_C | FLAG_H;
        }
        return 16;
    }

    private int B0_LDIR(short OP) {
        int tmp1 = (regs[REG_H] << 8) | regs[REG_L];
        int tmp2 = (regs[REG_D] << 8) | regs[REG_E];
        int tmp = (regs[REG_B] << 8) | regs[REG_C];

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
        if (tmp != 0) {
            flags |= FLAG_PV;
        } else {
            return 16;
        }
        PC = (PC - 2) & 0xFFFF;
        return 21;
    }

    private int B1_CPIR(short OP) {
        int tmp1 = (regs[REG_H] << 8) | regs[REG_L];
        int tmp2 = (regs[REG_B] << 8) | regs[REG_C];

        int tmp = memory.read(tmp1);
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
    }

    private int B2_INIR(short OP) throws IOException {
        int tmp = context.readIO(regs[REG_C]) & 0xFF;
        int tmp1 = (regs[REG_H] << 8) | regs[REG_L];
        int tmp2 = tmp + (regs[REG_C] + 1) & 0xFF;

        memory.write(tmp1, (short) tmp);
        tmp1 = (tmp1 + 1) & 0xFFFF;
        regs[REG_H] = ((tmp1 >>> 8) & 0xff);
        regs[REG_L] = (tmp1 & 0xFF);
        regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);

        flags = SIGN_ZERO_TABLE[regs[REG_B]] | PARITY_TABLE[(tmp2 & 7) ^ regs[REG_B]];
        if ((tmp & 0x80) == 0x80) {
            flags |= FLAG_N;
        }
        if (tmp2 > 0xFF) {
            flags |= (FLAG_H | FLAG_C);
        }

        if (regs[REG_B] == 0) {
            return 16;
        }
        PC = (PC - 2) & 0xFFFF;
        return 21;
    }

    private int B3_OTIR(short OP) throws IOException {
        int tmp1 = (regs[REG_H] << 8) | regs[REG_L];
        int tmp2 = memory.read(tmp1) & 0xFF;

        context.writeIO(regs[REG_C], tmp2);
        tmp1 = (tmp1 + 1) & 0xFFFF;
        regs[REG_H] = ((tmp1 >>> 8) & 0xff);
        regs[REG_L] = (tmp1 & 0xFF);
        regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);

        flags = SIGN_ZERO_TABLE[regs[REG_B]] | PARITY_TABLE[((tmp2 + regs[REG_L]) & 7) ^ regs[REG_B]];
        if ((tmp2 & 0x80) == 0x80) {
            flags |= FLAG_N;
        }
        if ((tmp2 + regs[REG_L]) > 0xFF) {
            flags |= FLAG_C | FLAG_H;
        }

        if (regs[REG_B] == 0) {
            return 16;
        }
        PC = (PC - 2) & 0xFFFF;
        return 21;
    }

    private int B8_LDDR(short OP) {
        int tmp1 = (regs[REG_H] << 8) | regs[REG_L];
        int tmp2 = (regs[REG_D] << 8) | regs[REG_E];
        memory.write(tmp2, memory.read(tmp1));
        tmp1 = (tmp1 - 1) & 0xFFFF;
        tmp2 = (tmp2 - 1) & 0xFFFF;
        int tmp = (((regs[REG_B] << 8) | regs[REG_C]) - 1) & 0xFFFF;
        regs[REG_H] = ((tmp1 >>> 8) & 0xff);
        regs[REG_L] = (tmp1 & 0xFF);
        regs[REG_B] = ((tmp >>> 8) & 0xff);
        regs[REG_C] = (tmp & 0xFF);
        regs[REG_D] = ((tmp2 >>> 8) & 0xff);
        regs[REG_E] = (tmp2 & 0xFF);
        flags &= 0xE9;
        if (tmp != 0) {
            flags |= FLAG_PV;
        } else {
            return 16;
        }
        PC = (PC - 2) & 0xFFFF;
        return 21;
    }

    private int B9_CPDR(short OP) {
        int tmp1 = (regs[REG_H] << 8) | regs[REG_L];
        int tmp2 = (regs[REG_B] << 8) | regs[REG_C];

        int tmp = memory.read(tmp1);
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
    }

    private int BA_INDR(short OP) throws IOException {
        int tmp = context.readIO(regs[REG_C]) & 0xFF;
        int tmp1 = (regs[REG_H] << 8) | regs[REG_L];
        int tmp2 = tmp + ((regs[REG_C] - 1) & 0xFF);

        memory.write(tmp1, (short) tmp);
        tmp1 = (tmp1 - 1) & 0xFFFF;
        regs[REG_H] = ((tmp1 >>> 8) & 0xff);
        regs[REG_L] = (tmp1 & 0xFF);
        regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);

        flags = SIGN_ZERO_TABLE[regs[REG_B]] | PARITY_TABLE[(tmp2 & 7) ^ regs[REG_B]];
        if ((tmp & 0x80) == 0x80) {
            flags |= FLAG_N;
        }
        if (tmp2 > 0xFF) {
            flags |= (FLAG_H | FLAG_C);
        }

        if (regs[REG_B] == 0) {
            return 16;
        }
        PC = (PC - 2) & 0xFFFF;
        return 21;
    }

    private int BB_OTDR(short OP) throws IOException {
        int tmp1 = (regs[REG_H] << 8) | regs[REG_L];
        int tmp2 = memory.read(tmp1) & 0xFF;

        context.writeIO(regs[REG_C], tmp2);
        tmp1 = (tmp1 - 1) & 0xFFFF;
        regs[REG_H] = (tmp1 >>> 8);
        regs[REG_L] = (tmp1 & 0xFF);
        regs[REG_B] = (regs[REG_B] - 1) & 0xFF;

        flags = SIGN_ZERO_TABLE[regs[REG_B]] | PARITY_TABLE[((tmp2 + regs[REG_L]) & 7) ^ regs[REG_B]];
        if ((tmp2 & 0x80) == 0x80) {
            flags |= FLAG_N;
        }
        if ((tmp2 + regs[REG_L]) > 0xFF) {
            flags |= FLAG_C | FLAG_H;
        }

        if (regs[REG_B] == 0) {
            return 16;
        }
        PC = (PC - 2) & 0xFFFF;
        return 21;
    }

    private int LD_LPAR_N_RPAR_SS(short OP) {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = getpair((OP >>> 4) & 3, true);
        writeWord(tmp, tmp1);
        return 20;
    }

    private int LD_SS_LPAR_N_RPAR(short OP) {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = readWord(tmp);
        putpair((OP >>> 4) & 3, tmp1, true);
        return 20;
    }

    private int JR_CC_D(short OP) {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        if (getCC1((OP >>> 3) & 3)) {
            PC += (byte) tmp;
            PC &= 0xFFFF;
            return 12;
        }
        return 7;
    }

    private int O18_JR_E(short OP) {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        PC += (byte) tmp;
        PC &= 0xFFFF;
        return 12;
    }

    private int C6_ADD_A_d(short OP) {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        int DAR = regs[REG_A];
        regs[REG_A] += tmp;

        flags = SIGN_ZERO_CARRY_TABLE[regs[REG_A] & 0x1FF];
        regs[REG_A] = regs[REG_A] & 0xFF;

        auxCarry(DAR, tmp);
        overflow(DAR, tmp, regs[REG_A]);

        return 7;
    }

    private int CE_ADC_A_d(short OP) {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        int DAR = regs[REG_A];
        int diff = tmp;
        if ((flags & FLAG_C) != 0) {
            diff++;
        }
        regs[REG_A] += diff;

        flags = SIGN_ZERO_CARRY_TABLE[regs[REG_A] & 0x1FF];
        regs[REG_A] = regs[REG_A] & 0xFF;

        auxCarry(DAR, diff);
        overflow(DAR, diff, regs[REG_A]);

        return 7;
    }

    private int D3_OUT_LPAR_D_RPAR_A(short OP) throws IOException {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        context.writeIO(tmp, (short) regs[REG_A]);
        return 11;
    }

    private int D6_SUB_d(short OP) {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        int tmp1 = regs[REG_A];
        tmp = -tmp;
        regs[REG_A] += tmp;
        tmp &= 0xFF;

        flags = SIGN_ZERO_CARRY_TABLE[regs[REG_A] & 0x1FF] | FLAG_N;
        regs[REG_A] = regs[REG_A] & 0xFF;

        auxCarry(tmp1, tmp);
        overflow(tmp1, tmp, regs[REG_A]);

        return 7;
    }

    private int DB_IN_A_LPAR_d_RPAR(short OP) throws IOException {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        regs[REG_A] = (context.readIO(tmp) & 0xFF);
        return 11;
    }

    private int DE_SBC_A_d(short OP) {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        int tmp2 = regs[REG_A];
        int diff = -tmp;
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
    }

    private int E6_AND_d(short OP) {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        regs[REG_A] = (regs[REG_A] & tmp) & 0xFF;
        flags = AND_OR_XOR_TABLE[regs[REG_A]];
        return 7;
    }

    private int EE_XOR_d(short OP) {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        regs[REG_A] = ((regs[REG_A] ^ tmp) & 0xFF);
        flags = AND_OR_XOR_TABLE[regs[REG_A]];
        return 7;
    }

    private int F6_OR_d(short OP) {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        regs[REG_A] = (regs[REG_A] | tmp) & 0xFF;
        flags = AND_OR_XOR_TABLE[regs[REG_A]];
        return 7;
    }

    private int FE_CP_d(short OP) {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        tmp = -tmp;
        int tmp2 = regs[REG_A] + tmp;
        tmp &= 0xFF;

        flags = SIGN_ZERO_CARRY_TABLE[tmp2 & 0x1FF] | FLAG_N;
        auxCarry(regs[REG_A], tmp);
        overflow(regs[REG_A], tmp, tmp2 & 0xFF);

        return 7;
    }

    private int LD_SS_NN(short OP) {
        Short[] word = memory.readWord(PC);
        int tmp = word[0] | (word[1] << 8);
        PC = (PC + 2) & 0xFFFF;

        putpair((OP >>> 4) & 3, tmp, true);
        return 10;
    }

    private int JP_CC_NN(short OP) {
        Short[] word = memory.readWord(PC);
        int tmp = word[0] | (word[1] << 8);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = (OP >>> 3) & 7;
        if ((flags & CONDITION[tmp1]) == CONDITION_VALUES[tmp1]) {
            PC = tmp;
        }
        return 10;
    }

    private int CALL_CC_NN(short OP) {
        Short[] word = memory.readWord(PC);
        int tmp = word[0] | (word[1] << 8);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = (OP >>> 3) & 7;
        if ((flags & CONDITION[tmp1]) == CONDITION_VALUES[tmp1]) {
            SP = (SP - 2) & 0xffff;
            writeWord(SP, PC);
            PC = tmp;
            return 17;
        }
        return 10;
    }

    private int O22_LD_LPAR_NN_RPAR_HL(short OP) {
        Short[] word = memory.readWord(PC);
        int tmp = word[0] | (word[1] << 8);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = getpair(2, false);
        writeWord(tmp, tmp1);
        return 16;
    }

    private int O2A_LD_HL_LPAR_NN_RPAR(short OP) {
        Short[] word = memory.readWord(PC);
        int tmp = word[0] | (word[1] << 8);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = readWord(tmp);
        putpair(2, tmp1, false);
        return 16;
    }

    private int O32_LD_LPAR_NN_RPAR_A(short OP) {
        Short[] word = memory.readWord(PC);
        int tmp = word[0] | (word[1] << 8);
        PC = (PC + 2) & 0xFFFF;

        memory.write(tmp, (short) regs[REG_A]);
        return 13;
    }

    private int O3A_LD_A_LPAR_NN_RPAR(short OP) {
        Short[] word = memory.readWord(PC);
        int tmp = word[0] | (word[1] << 8);
        PC = (PC + 2) & 0xFFFF;

        regs[REG_A] = (memory.read(tmp) & 0xff);
        return 13;
    }

    private int C3_JP_NN(short OP) {
        Short[] word = memory.readWord(PC);
        int tmp = word[0] | (word[1] << 8);
        PC = (PC + 2) & 0xFFFF;

        PC = tmp;
        return 10;
    }

    private int CD_CALL_NN(short OP) {
        Short[] word = memory.readWord(PC);
        int tmp = word[0] | (word[1] << 8);
        PC = (PC + 2) & 0xFFFF;

        SP = (SP - 2) & 0xffff;
        writeWord(SP, PC);
        PC = tmp;
        return 17;
    }


    private int dispatch(short OP) throws IOException, InvocationTargetException, IllegalAccessException {
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
            incrementR();
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

            /* Dispatch Instruction */
            Method instr = DISPATCH_TABLE[OP];
            if (instr != null) {
                return (Integer) instr.invoke(this, OP);
            }

            switch (OP) {
                case 0xDD:
                    special = 0xDD;
                case 0xFD:
                    if (OP == 0xFD) {
                        special = 0xFD;
                    }
                    OP = memory.read(PC);
                    PC = (PC + 1) & 0xFFFF;
                    incrementR();
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
                            flags &= (~(FLAG_N | FLAG_S | FLAG_Z));

                            tmp += tmp1;
                            flags |= ((tmp & 0x8000) == 0x8000) ? FLAG_S : 0;
                            flags |= (tmp == 0) ? FLAG_Z : 0;
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
                                IX = readWord(SP);
                            } else {
                                IY = readWord(SP);
                            }
                            SP = (SP + 2) & 0xFFFF;
                            return 14;
                        case 0xE3: /* EX (SP),ii */
                            tmp = readWord(SP);
                            if (special == 0xDD) {
                                tmp1 = IX;
                                IX = tmp;
                            } else {
                                tmp1 = IY;
                                IY = tmp;
                            }
                            writeWord(SP, tmp1);
                            return 23;
                        case 0xE5: /* PUSH ii */
                            SP = (SP - 2) & 0xFFFF;
                            if (special == 0xDD) {
                                writeWord(SP, IX);
                            } else {
                                writeWord(SP, IY);
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

                    tmp = memory.read(PC);
                    PC = (PC + 1) & 0xFFFF;
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
                            int diff = -memory.read((getspecial(special) + (byte) tmp) & 0xFFFF);
                            tmp2 = regs[REG_A] + diff;
                            diff &= 0xFF;

                            flags = SIGN_ZERO_CARRY_TABLE[tmp2 & 0x1FF] | FLAG_N;
                            auxCarry(regs[REG_A], diff);
                            overflow(regs[REG_A], diff, tmp2 & 0xFF);

                            return 19;
                    }
                    tmp |= ((memory.read(PC)) << 8);
                    PC = (PC + 1) & 0xFFFF;
                    switch (OP) {
                        case 0x21: /* LD ii,nn */
                            putspecial(special, tmp);
                            return 14;
                        case 0x22: /* LD (nn),ii */
                            writeWord(tmp, getspecial(special));
                            return 16;
                        case 0x2A: /* LD ii,(nn) */
                            tmp1 = readWord(tmp);
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
                                    flags = ((flags & FLAG_C) | FLAG_H | (((tmp1 & (1 << tmp2)) == 0) ? (FLAG_Z | FLAG_PV) : 0));
                                    if (tmp2 == 7) {
                                        flags |= (((tmp1 & (1 << 7)) == 0x80) ? FLAG_S : 0);
                                    }
                                    return 20;
                                case 0x78: // undocumented BIT 7,(ii+d)
                                case 0x79:
                                case 0x7A:
                                case 0x7B:
                                case 0x7C:
                                case 0x7D:
                                case 0x7F:
                                    tmp1 = memory.read((getspecial(special) + (byte) tmp) & 0xffff);
                                    flags = ((flags & FLAG_C) | FLAG_H | (((tmp1 & (1 << 7)) == 0) ? (FLAG_Z | FLAG_PV) : 0))
                                        | (((tmp1 & (1 << 7)) == 0x80) ? FLAG_S : 0);
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
                            /* SET 0,(ii+d),reg (undocumented) */
                                case 0xC0:
                                case 0xC1:
                                case 0xC2:
                                case 0xC3:
                                case 0xC4:
                                case 0xC5:
                                case 0xC7:
                                    tmp2 = (OP >>> 3) & 7;
                                    tmp3 = (getspecial(special) + (byte) tmp) & 0xffff;
                                    tmp1 = memory.read(tmp3);
                                    tmp1 = (tmp1 | (1 << tmp2));
                                    memory.write(tmp3, (short) (tmp1 & 0xff));
                                    regs[OP & 7] = tmp1 & 0xFF;
                                    return 23;
                                case 0x06: /* RLC (ii+d) */
                                case 0: // undocumented
                                    tmp = (getspecial(special) + (byte) tmp) & 0xffff;
                                    tmp1 = memory.read(tmp);

                                    tmp2 = (tmp1 >>> 7) & 1;
                                    tmp1 = ((((tmp1 << 1) & 0xFF) | tmp2) & 0xFF);

                                    memory.write(tmp, (short)(tmp1 & 0xFF));
                                    flags = SIGN_ZERO_TABLE[tmp1] | PARITY_TABLE[tmp1] | tmp2;

                                    if (OP == 0) {
                                        regs[REG_B] = (short)(tmp1 & 0xFF);
                                    }
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
                                    tmp = (getspecial(special) + (byte) tmp) & 0xffff;
                                    tmp1 = memory.read(tmp);

                                    tmp2 = (tmp1 >>> 7) & 1;
                                    tmp1 = (tmp1 << 1) & 0xFE;
                                    memory.write(tmp, (short)tmp1);

                                    flags = SIGN_ZERO_TABLE[tmp1] | PARITY_TABLE[tmp1] | tmp2;

                                    return 23;
                                case 0x2E: /* SRA (ii+d) */
                                    tmp = (getspecial(special) + (byte) tmp) & 0xffff;
                                    tmp1 = memory.read(tmp);

                                    tmp2 = tmp1 & 1;
                                    tmp1 = (tmp1 >> 1) & 0xFF | (tmp1 & 0x80);
                                    memory.write(tmp, (short)tmp1);

                                    flags = SIGN_ZERO_TABLE[tmp1] | PARITY_TABLE[tmp1] | tmp2;

                                    return 23;
                                case 0x36: /* SLL (ii+d) unsupported */
                                    tmp = (getspecial(special) + (byte) tmp) & 0xffff;
                                    tmp1 = memory.read(tmp);

                                    tmp2 = (tmp1 >>> 7) & 1;
                                    tmp1 = (tmp1 << 1) & 0xFF | tmp1 & 1;
                                    memory.write(tmp, (short)tmp1);

                                    flags = SIGN_ZERO_TABLE[tmp1] | PARITY_TABLE[tmp1] | tmp2;

                                    return 23;
                                case 0x3E: /* SRL (ii+d) */
                                    tmp = (getspecial(special) + (byte) tmp) & 0xffff;
                                    tmp1 = memory.read(tmp);

                                    tmp2 = tmp1 & 1;
                                    tmp1 = (tmp1 >>> 1) & 0x7F;
                                    memory.write(tmp, (short)tmp1);

                                    flags = SIGN_ZERO_TABLE[tmp1] | PARITY_TABLE[tmp1] | tmp2;

                                    return 23;
                            }
                            currentRunState = RunState.STATE_STOPPED_BAD_INSTR;
                            return 0;
                    }
                    currentRunState = RunState.STATE_STOPPED_BAD_INSTR;
                    return 0;
                case 0xCB:
                    OP = memory.read(PC);
                    PC = (PC + 1) & 0xFFFF;
                    incrementR();
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

                            tmp2 = (tmp1 >>> 7) & 1;
                            tmp1 = (tmp1 << 1) & 0xFE;
                            putreg(tmp, tmp1);

                            flags = SIGN_ZERO_TABLE[tmp1] | PARITY_TABLE[tmp1] | tmp2;

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

                            tmp2 = tmp1 & 1;
                            tmp1 = (tmp1 >> 1) & 0xFF | (tmp1 & 0x80);
                            putreg(tmp, tmp1);

                            flags = SIGN_ZERO_TABLE[tmp1] | PARITY_TABLE[tmp1] | tmp2;

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

                            tmp2 = (tmp1 >>> 7) & 1;
                            tmp1 = (tmp1 << 1) & 0xFF | tmp1 & 1;
                            putreg(tmp, tmp1);

                            flags = SIGN_ZERO_TABLE[tmp1] | PARITY_TABLE[tmp1] | tmp2;

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

                            tmp2 = tmp1 & 1;
                            tmp1 = (tmp1 >>> 1) & 0x7F;
                            putreg(tmp, tmp1);

                            flags = SIGN_ZERO_TABLE[tmp1] | PARITY_TABLE[tmp1] | tmp2;

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
                            flags = ((flags & FLAG_C) | FLAG_H | (((tmp1 & (1 << tmp)) == 0) ? (FLAG_Z | FLAG_PV) : 0));
                            if (tmp == 7) {
                                flags |= (((tmp1 & (1 << tmp)) == 0x80) ? FLAG_S : 0);
                            }
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
            }

            currentRunState = CPU.RunState.STATE_STOPPED_BAD_INSTR;
        } finally {
            if (tmpListener != null) {
                tmpListener.afterDispatch();
            }
        }
        return 0;
    }

}
