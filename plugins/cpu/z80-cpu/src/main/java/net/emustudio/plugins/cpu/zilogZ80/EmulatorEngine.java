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
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.CPU.RunState;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.plugins.cpu.intel8080.api.CpuEngine;
import net.emustudio.plugins.cpu.intel8080.api.DispatchListener;
import net.emustudio.plugins.cpu.intel8080.api.FrequencyChangedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.LockSupport;

import static net.emustudio.plugins.cpu.zilogZ80.DispatchTables.*;

/**
 * Main implementation class for CPU emulation CPU works in a separate thread
 * (parallel with other hardware)
 */
@SuppressWarnings("unused")
public class EmulatorEngine implements CpuEngine {
    private final static Logger LOGGER = LoggerFactory.getLogger(EmulatorEngine.class);

    public static final int REG_A = 7, REG_B = 0, REG_C = 1, REG_D = 2, REG_E = 3, REG_H = 4, REG_L = 5;
    public static final int FLAG_S = 0x80, FLAG_Z = 0x40, FLAG_H = 0x10, FLAG_PV = 0x4, FLAG_N = 0x02, FLAG_C = 0x1;

    private final static int[] CONDITION = new int[]{
        FLAG_Z, FLAG_Z, FLAG_C, FLAG_C, FLAG_PV, FLAG_PV, FLAG_S, FLAG_S
    };
    private final static int[] CONDITION_VALUES = new int[]{
        0, FLAG_Z, 0, FLAG_C, 0, FLAG_PV, 0, FLAG_S
    };

    private final ContextImpl context;
    private final MemoryContext<Byte> memory;
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
    private DeviceContext<?> interruptDevice;

    private RunState currentRunState = RunState.STATE_STOPPED_NORMAL;
    private long executedCycles = 0;

    private volatile DispatchListener dispatchListener;

    public EmulatorEngine(MemoryContext<Byte> memory, ContextImpl context) {
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
        short opcode = (short)(memory.read(PC) & 0xFF);
        PC = (PC + 1) & 0xFFFF;
        try {
            dispatch(opcode);
        } catch (Throwable e) {
            throw new Exception(e);
        }
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
                } catch (Breakpoint e) {
                    return CPU.RunState.STATE_STOPPED_BREAK;
                } catch (IndexOutOfBoundsException e) {
                    LOGGER.debug("Unexpected error", e);
                    return CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
                } catch (IOException e) {
                    LOGGER.error("Unexpected error", e);
                    return RunState.STATE_STOPPED_BAD_INSTR;
                } catch (Throwable e) {
                    LOGGER.debug("Unexpected error", e);
                    return CPU.RunState.STATE_STOPPED_BAD_INSTR;
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

    private int dispatch(short OP) throws Throwable {
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

            /* Dispatch Instruction */
            MethodHandle instr = DISPATCH_TABLE[OP];
            if (instr != null) {
                return (int) instr.invokeExact(this, OP);
            }
            currentRunState = CPU.RunState.STATE_STOPPED_BAD_INSTR;
        } finally {
            if (tmpListener != null) {
                tmpListener.afterDispatch();
            }
        }
        return 0;
    }

    int CB_DISPATCH(short OP) throws Throwable {
        return DISPATCH(DISPATCH_TABLE_CB);
    }

    int DD_DISPATCH(short OP) throws Throwable {
        return DISPATCH(DISPATCH_TABLE_DD);
    }

    int DD_CB_DISPATCH(short OP) throws Throwable {
        return SPECIAL_CB_DISPATCH(DISPATCH_TABLE_DD_CB);
    }

    int ED_DISPATCH(short OP) throws Throwable {
        return DISPATCH(DISPATCH_TABLE_ED);
    }

    int FD_DISPATCH(short OP) throws Throwable {
        return DISPATCH(DISPATCH_TABLE_FD);
    }

    int FD_CB_DISPATCH(short OP) throws Throwable {
        return SPECIAL_CB_DISPATCH(DISPATCH_TABLE_FD_CB);
    }

    private int DISPATCH(MethodHandle[] table) throws Throwable {
        short OP = (short)(memory.read(PC) & 0xFF);
        incrementR();
        PC = (PC + 1) & 0xFFFF;

        MethodHandle instr = table[OP];
        if (instr != null) {
            return (int) instr.invokeExact(this, OP);
        }
        currentRunState = RunState.STATE_STOPPED_BAD_INSTR;
        return 0;
    }

    int SPECIAL_CB_DISPATCH(MethodHandle[] table) throws Throwable {
        byte operand = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        short OP = (short)(memory.read(PC) & 0xFF);
        PC = (PC + 1) & 0xFFFF;
        incrementR();

        MethodHandle instr = table[OP];
        if (instr != null) {
            return (int) instr.invokeExact(this, OP, operand);
        }
        currentRunState = RunState.STATE_STOPPED_BAD_INSTR;
        return 0;
    }

    void setInterrupt(DeviceContext<?> device, int mask) {
        this.interruptDevice = device;
        this.interruptPending |= mask;
    }

    void clearInterrupt(DeviceContext<?> device, int mask) {
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

    private int doInterrupt() throws Throwable {
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
                writeWord((SP - 2) & 0xFFFF, PC);
                SP = (SP - 2) & 0xffff;
                PC = 0xFF & 0x38;
                break;
            case 2:
                cycles += 13;
                writeWord((SP - 2) & 0xFFFF, PC);
                PC = readWord((I << 8) | interruptVector);
                break;
        }
        return cycles;
    }


    private int getreg(int reg) {
        if (reg == 6) {
            return readByte((regs[REG_H] << 8) | regs[REG_L]);
        }
        return regs[reg];
    }

    private byte getreg2(int reg) {
        if (reg == 6) {
            return 0;
        }
        return (byte)regs[reg];
    }

    private void putreg(int reg, int val) {
        if (reg == 6) {
            memory.write((regs[REG_H] << 8) | regs[REG_L], (byte) (val & 0xFF));
        } else {
            regs[reg] = val & 0xFF;
        }
    }

    private void putreg2(int reg, int val) {
        if (reg != 6) {
            regs[reg] = val & 0xFF;
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
                flags = (short) low;
            }
        } else {
            regs[index] = high;
            regs[index + 1] = low;
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

    private int flagSZHPC(int a, int b, int sum) {
        int carryOut = (a > 0xFF - b) ? FLAG_C : 0;
        int carryIns = sum ^ a ^ b;
        int halfCarryOut = (carryIns >> 4) & 1;
        int overflowOut = (((carryIns >> 7) ^ carryOut) != 0) ? FLAG_PV : 0;

        return carryOut | (halfCarryOut << 4) | (overflowOut) | (sum == 0 ? FLAG_Z : 0) | (sum & 0x80);
    }

    private void bigOverflow(int i, int j, int result) {
        int sign = i & 0x8000;
        if (sign != (j & 0x8000)) {
            flags &= (~FLAG_PV);
        } else if ((result & 0x8000) != sign) {
            flags |= FLAG_PV;
        }
    }

    private void auxCarry(int a, int b) {
        int carryIns = ((a + b) & 0xFF) ^ a ^ b;
        int halfCarryOut = (carryIns >> 4) & 1;
        if (halfCarryOut != 0) {
            flags |= FLAG_H;
        } else {
            flags &= (~FLAG_H);
        }
    }

    private void halfCarry11(int before, int sumWith) {
        int mask = sumWith & before;
        int xormask = sumWith ^ before;

        int C0 = mask & 1;
        int C1 = ((mask >>> 1) ^ (C0 & (xormask >>> 1))) & 1;
        int C2 = ((mask >>> 2) ^ (C1 & (xormask >>> 2))) & 1;
        int C3 = ((mask >>> 3) ^ (C2 & (xormask >>> 3))) & 1;
        int C4 = ((mask >>> 4) ^ (C3 & (xormask >>> 4))) & 1;
        int C5 = ((mask >>> 5) ^ (C4 & (xormask >>> 5))) & 1;
        int C6 = ((mask >>> 6) ^ (C5 & (xormask >>> 6))) & 1;
        int C7 = ((mask >>> 7) ^ (C6 & (xormask >>> 7))) & 1;
        int C8 = ((mask >>> 8) ^ (C7 & (xormask >>> 8))) & 1;
        int C9 = ((mask >>> 9) ^ (C8 & (xormask >>> 9))) & 1;
        int C10 = ((mask >>> 10) ^ (C9 & (xormask >>> 10))) & 1;
        int C11 = ((mask >>> 11) ^ (C10 & (xormask >>> 11))) & 1;

        if (C11 != 0) {
            flags |= FLAG_H;
        } else {
            flags &= (~FLAG_H);
        }
    }

    private void carry15(int before, int sumWith) {
        int mask = sumWith & before;
        int xormask = sumWith ^ before;

        int C0 = mask & 1;
        int C1 = ((mask >>> 1) ^ (C0 & (xormask >>> 1))) & 1;
        int C2 = ((mask >>> 2) ^ (C1 & (xormask >>> 2))) & 1;
        int C3 = ((mask >>> 3) ^ (C2 & (xormask >>> 3))) & 1;
        int C4 = ((mask >>> 4) ^ (C3 & (xormask >>> 4))) & 1;
        int C5 = ((mask >>> 5) ^ (C4 & (xormask >>> 5))) & 1;
        int C6 = ((mask >>> 6) ^ (C5 & (xormask >>> 6))) & 1;
        int C7 = ((mask >>> 7) ^ (C6 & (xormask >>> 7))) & 1;
        int C8 = ((mask >>> 8) ^ (C7 & (xormask >>> 8))) & 1;
        int C9 = ((mask >>> 9) ^ (C8 & (xormask >>> 9))) & 1;
        int C10 = ((mask >>> 10) ^ (C9 & (xormask >>> 10))) & 1;
        int C11 = ((mask >>> 11) ^ (C10 & (xormask >>> 11))) & 1;
        int C12 = ((mask >>> 12) ^ (C11 & (xormask >>> 12))) & 1;
        int C13 = ((mask >>> 13) ^ (C12 & (xormask >>> 13))) & 1;
        int C14 = ((mask >>> 14) ^ (C13 & (xormask >>> 14))) & 1;
        int C15 = ((mask >>> 15) ^ (C14 & (xormask >>> 15))) & 1;

        if (C15 != 0) {
            flags |= FLAG_C;
        } else {
            flags &= (~FLAG_C);
        }
    }

    private void incrementR() {
        R = (R & 0x80) | (((R & 0x7F) + 1) & 0x7F);
    }


    int I_NOP(short OP) {
        return 4;
    }

    int I_LD_REF_BC_A(short OP) {
        memory.write(getpair(0, false), (byte) regs[REG_A]);
        return 7;
    }

    int I_INC_RP(short OP) {
        int tmp = (OP >>> 4) & 0x03;
        putpair(tmp, (getpair(tmp, true) + 1) & 0xFFFF, true);
        return 6;
    }

    int I_ADD_HL_RP(short OP) {
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

    int I_DEC_RP(short OP) {
        int regPair = (OP >>> 4) & 0x03;
        putpair(regPair, (getpair(regPair, true) - 1) & 0xFFFF, true);
        return 6;
    }

    int I_POP_RP(short OP) {
        int regPair = (OP >>> 4) & 0x03;
        int value = readWord(SP);
        SP = (SP + 2) & 0xffff;
        putpair(regPair, value, false);
        return 10;
    }

    int I_PUSH_RP(short OP) {
        int regPair = (OP >>> 4) & 0x03;
        int value = getpair(regPair, false);
        SP = (SP - 2) & 0xffff;
        writeWord(SP, value);
        return 11;
    }

    int I_LD_R_N(short OP) {
        int reg = (OP >>> 3) & 0x07;
        putreg(reg, memory.read(PC));
        PC = (PC + 1) & 0xFFFF;
        if (reg == 6) {
            return 10;
        } else {
            return 7;
        }
    }

    int I_INC_R(short OP) {
        int reg = (OP >>> 3) & 0x07;
        int value = (getreg(reg) + 1) & 0xFF;
        flags = EmulatorTables.INC_TABLE[value] | (flags & FLAG_C);
        putreg(reg, value);
        return (reg == 6) ? 11 : 4;
    }

    int I_DEC_R(short OP) {
        int reg = (OP >>> 3) & 0x07;
        int value = (getreg(reg) - 1) & 0xFF;
        flags = EmulatorTables.DEC_TABLE[value] | (flags & FLAG_C);
        putreg(reg, value);
        return (reg == 6) ? 11 : 4;
    }

    int I_RET_CC(short OP) {
        int cc = (OP >>> 3) & 7;
        if ((flags & CONDITION[cc]) == CONDITION_VALUES[cc]) {
            PC = readWord(SP);
            SP = (SP + 2) & 0xffff;
            return 11;
        }
        return 5;
    }

    int I_RST(short OP) {
        SP = (SP - 2) & 0xffff;
        writeWord(SP, PC);
        PC = OP & 0x38;
        return 11;
    }

    int I_ADD_A_R(short OP) {
        int value = getreg(OP & 0x07);
        int oldA = regs[REG_A];
        regs[REG_A] = (oldA + value) & 0xFF;
        flags = flagSZHPC(oldA, value, regs[REG_A]);
        return (OP == 0x86) ? 7 : 4;
    }

    int I_ADC_A_R(short OP) {
        int value = getreg(OP & 0x07);
        int oldA = regs[REG_A];
        regs[REG_A] = (oldA + value + (flags & FLAG_C)) & 0xFF;
        flags = flagSZHPC(oldA, (value + (flags & FLAG_C)) & 0xFF, regs[REG_A]);
        return (OP == 0x8E) ? 7 : 4;
    }

    int I_SUB_R(short OP) {
        int value = ((~getreg(OP & 0x07)) + 1) & 0xFF;
        int oldA = regs[REG_A];
        regs[REG_A] = (oldA + value) & 0xFF;
        flags = flagSZHPC(oldA, value, regs[REG_A]) | FLAG_N;
        return (OP == 0x96) ? 7 : 4;
    }

    int I_SBC_A_R(short OP) {
        int value = ((~getreg(OP & 0x07)) + 1) & 0xFF;
        int oldA = regs[REG_A];
        regs[REG_A] = (oldA + value - (flags & FLAG_C)) & 0xFF;
        flags = flagSZHPC(oldA, (value - (flags & FLAG_C)) & 0xFF, regs[REG_A]) | FLAG_N;
        return (OP == 0x9E) ? 7 : 4;
    }

    int I_AND_R(short OP) {
        regs[REG_A] = (regs[REG_A] & getreg(OP & 7)) & 0xFF;
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return (OP == 0xA6) ? 7 : 4;
    }

    int I_XOR_R(short OP) {
        regs[REG_A] = ((regs[REG_A] ^ getreg(OP & 7)) & 0xff);
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return (OP == 0xAE) ? 7 : 4;
    }

    int I_OR_R(short OP) {
        regs[REG_A] = (regs[REG_A] | getreg(OP & 7)) & 0xFF;
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return (OP == 0xB6) ? 7 : 4;
    }

    int I_CP_R(short OP) {
        int value = ((~getreg(OP & 7)) + 1) & 0xFF;
        int result = (regs[REG_A] + value) & 0xFF;
        flags = flagSZHPC(regs[REG_A], value, result) | FLAG_N;
        return (OP == 0xBE) ? 7 : 4;
    }

    int I_RLCA(short OP) {
        int tmp = regs[REG_A] >>> 7;
        regs[REG_A] = ((((regs[REG_A] << 1) & 0xFF) | tmp) & 0xff);
        flags = ((flags & 0xEC) | tmp);
        return 4;
    }

    int I_EX_AF_AFF(short OP) {
        int tmp = regs[REG_A];
        regs[REG_A] = regs2[REG_A];
        regs2[REG_A] = tmp;
        tmp = flags;
        flags = flags2;
        flags2 = tmp;
        return 4;
    }

    int I_LD_A_REF_BC(short OP) {
        regs[REG_A] = readByte(getpair(0, false));
        return 7;
    }

    int I_RRCA(short OP) {
        flags = ((flags & 0xEC) | (regs[REG_A] & 1));
        regs[REG_A] = EmulatorTables.RRCA_TABLE[regs[REG_A]];
        return 4;
    }

    int I_DJNZ(short OP) {
        byte tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_B]--;
        regs[REG_B] &= 0xFF;
        if (regs[REG_B] != 0) {
            PC = (PC + tmp) & 0xFFFF;
            return 13;
        }
        return 8;
    }

    int I_LD_REF_DE_A(short OP) {
        memory.write(getpair(1, false), (byte) regs[REG_A]);
        return 7;
    }

    int I_RLA(short OP) {
        int tmp = regs[REG_A] >>> 7;
        regs[REG_A] = (((regs[REG_A] << 1) | (flags & FLAG_C)) & 0xff);
        flags = ((flags & 0xEC) | tmp);
        return 4;
    }

    int I_LD_A_REF_DE(short OP) {
        regs[REG_A] = readByte(getpair(1, false));
        return 7;
    }

    int I_RRA(short OP) {
        int tmp = (flags & FLAG_C) << 7;
        flags = ((flags & 0xEC) | (regs[REG_A] & 1));
        regs[REG_A] = ((regs[REG_A] >>> 1 | tmp) & 0xff);
        return 4;
    }

    int I_DAA(short OP) {
        int temp = regs[REG_A];
        boolean acFlag = (flags & FLAG_H) == FLAG_H;
        boolean cFlag = (flags & FLAG_C) == FLAG_C;
        boolean nFlag = (flags & FLAG_N) == FLAG_N;

        if (!acFlag && !cFlag) {
            regs[REG_A] = EmulatorTables.DAA_NOT_C_NOT_H_TABLE[temp] & 0xFF;
            flags = (EmulatorTables.DAA_NOT_C_NOT_H_TABLE[temp] >> 8) & 0xFF | (flags & FLAG_N);
            if (nFlag) {
                flags |= EmulatorTables.DAA_N_NOT_H_FOR_H_TABLE[temp];
            } else {
                flags |= EmulatorTables.DAA_NOT_N_NOT_H_FOR_H_TABLE[temp];
            }
        } else if (acFlag && !cFlag) {
            regs[REG_A] = EmulatorTables.DAA_NOT_C_H_TABLE[temp] & 0xFF;
            flags = (EmulatorTables.DAA_NOT_C_H_TABLE[temp] >> 8) & 0xFF | (flags & FLAG_N);
            if (nFlag) {
                flags |= EmulatorTables.DAA_N_H_FOR_H_TABLE[temp];
            } else {
                flags |= EmulatorTables.DAA_NOT_N_H_FOR_H_TABLE[temp];
            }
        } else if (!acFlag) { // cFlag = true
            regs[REG_A] = EmulatorTables.DAA_C_NOT_H_TABLE[temp] & 0xFF;
            flags = (EmulatorTables.DAA_C_NOT_H_TABLE[temp] >> 8) & 0xFF | (flags & FLAG_N);
            if (nFlag) {
                flags |= EmulatorTables.DAA_N_NOT_H_FOR_H_TABLE[temp];
            } else {
                flags |= EmulatorTables.DAA_NOT_N_NOT_H_FOR_H_TABLE[temp];
            }
        } else { // acFlag = cFlag = true
            regs[REG_A] = EmulatorTables.DAA_C_H_TABLE[temp] & 0xFF;
            flags = (EmulatorTables.DAA_C_H_TABLE[temp] >> 8) & 0xFF | (flags & FLAG_N);
            if (nFlag) {
                flags |= EmulatorTables.DAA_N_H_FOR_H_TABLE[temp];
            } else {
                flags |= EmulatorTables.DAA_NOT_N_H_FOR_H_TABLE[temp];
            }
        }
        flags |= EmulatorTables.PARITY_TABLE[regs[REG_A]] | EmulatorTables.SIGN_ZERO_TABLE[regs[REG_A]];
        return 4;
    }

    int I_CPL(short OP) {
        regs[REG_A] = ((~regs[REG_A]) & 0xFF);
        flags |= FLAG_N | FLAG_H;
        return 4;
    }

    int I_SCF(short OP) {
        flags |= FLAG_N | FLAG_C;
        flags &= ~FLAG_H;
        return 4;
    }

    int I_CCF(short OP) {
        int tmp = flags & FLAG_C;
        if (tmp == 0) {
            flags |= FLAG_C;
        } else {
            flags &= ~FLAG_C;
        }
        flags &= ~FLAG_N;
        return 4;
    }

    int I_RET(short OP) {
        PC = readWord(SP);
        SP = (SP + 2) & 0xFFFF;
        return 10;
    }

    int I_EXX(short OP) {
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

    int I_EX_REF_SP_HL(short OP) {
        byte tmp = memory.read(SP);
        int x = (SP + 1) & 0xFFFF;
        byte tmp1 = memory.read(x);
        memory.write(SP, (byte) regs[REG_L]);
        memory.write(x, (byte) regs[REG_H]);
        regs[REG_L] = tmp & 0xFF;
        regs[REG_H] = tmp1 & 0xFF;
        return 19;
    }

    int I_JP_REF_HL(short OP) {
        PC = ((regs[REG_H] << 8) | regs[REG_L]);
        return 4;
    }

    int I_EX_DE_HL(short OP) {
        int tmp = regs[REG_D];
        regs[REG_D] = regs[REG_H];
        regs[REG_H] = tmp;
        tmp = regs[REG_E];
        regs[REG_E] = regs[REG_L];
        regs[REG_L] = tmp;
        return 4;
    }

    int I_DI(short OP) {
        IFF[0] = IFF[1] = false;
        return 4;
    }

    int I_LD_SP_HL(short OP) {
        SP = ((regs[REG_H] << 8) | regs[REG_L]);
        return 6;
    }

    int I_EI(short OP) {
        IFF[0] = IFF[1] = true;
        return 4;
    }

    int I_IN_R_REF_C(short OP) throws IOException {
        int tmp = (OP >>> 3) & 0x7;
        putreg(tmp, context.readIO(regs[REG_C]));
        flags = (flags & FLAG_C) | EmulatorTables.SIGN_ZERO_TABLE[regs[tmp]] | EmulatorTables.PARITY_TABLE[regs[tmp]];
        return 12;
    }

    int I_OUT_REF_C_R(short OP) throws IOException {
        int tmp = (OP >>> 3) & 0x7;
        context.writeIO(regs[REG_C], (byte)getreg(tmp));
        return 12;
    }

    int I_SBC_HL_RP(short OP) {
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

    int I_ADC_HL_RP(short OP) {
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

    int I_NEG(short OP) {
        flags = EmulatorTables.NEG_TABLE[regs[REG_A]] & 0xFF;
        regs[REG_A] = (EmulatorTables.NEG_TABLE[regs[REG_A]] >>> 8) & 0xFF;
        return 8;
    }

    int I_RETN(short OP) {
        IFF[0] = IFF[1];
        PC = readWord(SP);
        SP = (SP + 2) & 0xffff;
        return 14;
    }

    int I_IM_0(short OP) {
        intMode = 0;
        return 8;
    }

    int I_LD_I_A(short OP) {
        I = regs[REG_A];
        return 9;
    }

    int I_RETI(short OP) {
        IFF[0] = IFF[1];
        PC = readWord(SP);
        SP = (SP + 2) & 0xffff;
        return 14;
    }

    int I_LD_R_A(short OP) {
        R = regs[REG_A];
        return 9;
    }

    int I_IM_1(short OP) {
        intMode = 1;
        return 8;
    }

    int I_LD_A_I(short OP) {
        regs[REG_A] = I & 0xFF;
        flags = EmulatorTables.SIGN_ZERO_TABLE[regs[REG_A]] | (IFF[1] ? FLAG_PV : 0) | (flags & FLAG_C);
        return 9;
    }

    int I_IM_2(short OP) {
        intMode = 2;
        return 8;
    }

    int I_LD_A_R(short OP) {
        regs[REG_A] = R & 0xFF;
        flags = EmulatorTables.SIGN_ZERO_TABLE[regs[REG_A]] | (IFF[1] ? FLAG_PV : 0) | (flags & FLAG_C);
        return 9;
    }

    int I_RRD(short OP) {
        int tmp = regs[REG_A] & 0x0F;
        int tmp1 = memory.read((regs[REG_H] << 8) | regs[REG_L]);
        regs[REG_A] = ((regs[REG_A] & 0xF0) | (tmp1 & 0x0F));
        tmp1 = ((tmp1 >>> 4) & 0x0F) | (tmp << 4);
        memory.write(((regs[REG_H] << 8) | regs[REG_L]), (byte) (tmp1 & 0xff));
        flags = EmulatorTables.SIGN_ZERO_TABLE[regs[REG_A]] | EmulatorTables.PARITY_TABLE[regs[REG_A]] | (flags & FLAG_C);
        return 18;
    }

    int I_RLD(short OP) {
        int tmp = memory.read((regs[REG_H] << 8) | regs[REG_L]);
        int tmp1 = (tmp >>> 4) & 0x0F;
        tmp = ((tmp << 4) & 0xF0) | (regs[REG_A] & 0x0F);
        regs[REG_A] = ((regs[REG_A] & 0xF0) | tmp1);
        memory.write((regs[REG_H] << 8) | regs[REG_L], (byte) (tmp & 0xff));
        flags = EmulatorTables.SIGN_ZERO_TABLE[regs[REG_A]] | EmulatorTables.PARITY_TABLE[regs[REG_A]] | (flags & FLAG_C);
        return 18;
    }

    int I_IN_REF_C(short OP) throws IOException {
        int tmp = (context.readIO(regs[REG_C]) & 0xFF);
        flags = EmulatorTables.SIGN_ZERO_TABLE[tmp] | EmulatorTables.PARITY_TABLE[tmp] | (flags & FLAG_C);
        return 12;
    }

    int I_OUT_REF_C_0(short OP) throws IOException {
        context.writeIO(regs[REG_C], (byte) 0);
        return 12;
    }

    int I_LDI(short OP) {
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

    int I_CPI(short OP) {
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

    int I_LDD(short OP) {
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

    int I_CPD(short OP) {
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

    int I_LDIR(short OP) {
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

    int I_CPIR(short OP) {
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

    int I_INI(short OP) throws IOException {
        byte value = context.readIO(regs[REG_C]);
        int address = (regs[REG_H] << 8) | regs[REG_L];
        memory.write(address, value);

        regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);
        address++;

        regs[REG_H] = (address >>> 8) & 0xFF;
        regs[REG_L] = address & 0xFF;

        flags |= FLAG_N | (regs[REG_B] == 0 ? FLAG_Z : 0);
        return 16;
    }

    int I_INIR(short OP) throws IOException {
        byte value = context.readIO(regs[REG_C]);
        int address = (regs[REG_H] << 8) | regs[REG_L];
        memory.write(address, value);

        regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);
        address++;

        regs[REG_H] = (address >>> 8) & 0xFF;
        regs[REG_L] = address & 0xFF;

        flags |= FLAG_Z | FLAG_N; // FLAG_Z is set b/c it is expected that INIR will be repeated until B=0

        if (regs[REG_B] == 0) {
            return 16;
        }
        PC = (PC - 2) & 0xFFFF;
        return 21;
    }

    int I_IND(short OP) throws IOException {
        byte value = context.readIO(regs[REG_C]);
        int address = (regs[REG_H] << 8) | regs[REG_L];
        memory.write(address, value);

        regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);
        address--;

        regs[REG_H] = (address >>> 8) & 0xFF;
        regs[REG_L] = address & 0xFF;

        flags |= FLAG_N | (regs[REG_B] == 0 ? FLAG_Z : 0);
        return 16;
    }

    int I_INDR(short OP) throws IOException {
        byte value = context.readIO(regs[REG_C]);
        int address = (regs[REG_H] << 8) | regs[REG_L];
        memory.write(address, value);

        regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);
        address--;

        regs[REG_H] = (address >>> 8) & 0xFF;
        regs[REG_L] = address & 0xFF;

        flags |= FLAG_Z | FLAG_N; // FLAG_Z is set b/c it is expected that INIR will be repeated until B=0

        if (regs[REG_B] == 0) {
            return 16;
        }
        PC = (PC - 2) & 0xFFFF;
        return 21;
    }

    int I_OUTI(short OP) throws IOException {
        int address = (regs[REG_H] << 8) | regs[REG_L];
        byte value = memory.read(address);
        context.writeIO(regs[REG_C], value);

        address++;
        regs[REG_H] = (address >>> 8) & 0xFF;
        regs[REG_L] = address & 0xFF;
        regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);

        flags |= FLAG_N | (regs[REG_B] == 0 ? FLAG_Z : 0);
        return 16;
    }

    int I_OTIR(short OP) throws IOException {
        int address = (regs[REG_H] << 8) | regs[REG_L];
        byte value = memory.read(address);
        context.writeIO(regs[REG_C], value);

        address++;
        regs[REG_H] = (address >>> 8) & 0xFF;
        regs[REG_L] = address & 0xFF;
        regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);

        flags |= FLAG_Z | FLAG_N; // FLAG_Z is set b/c it is expected that OTIR will be repeated until B=0

        if (regs[REG_B] == 0) {
            return 16;
        }
        PC = (PC - 2) & 0xFFFF;
        return 21;
    }

    int I_OUTD(short OP) throws IOException {
        int address = (regs[REG_H] << 8) | regs[REG_L];
        byte value = memory.read(address);
        context.writeIO(regs[REG_C], value);

        address--;
        regs[REG_H] = (address >>> 8) & 0xFF;
        regs[REG_L] = address & 0xFF;
        regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);

        flags |= FLAG_N | (regs[REG_B] == 0 ? FLAG_Z : 0);
        return 16;
    }

    int I_OTDR(short OP) throws IOException {
        int address = (regs[REG_H] << 8) | regs[REG_L];
        byte value = memory.read(address);
        context.writeIO(regs[REG_C], value);

        address--;
        regs[REG_H] = (address >>> 8) & 0xFF;
        regs[REG_L] = address & 0xFF;
        regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);

        flags |= FLAG_Z | FLAG_N;

        if (regs[REG_B] == 0) {
            return 16;
        }
        PC = (PC - 2) & 0xFFFF;
        return 21;
    }

    int I_LDDR(short OP) {
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

    int I_CPDR(short OP) {
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int bc = (regs[REG_B] << 8) | regs[REG_C];

        int tmp = readByte(hl);
        hl = (hl - 1) & 0xFFFF;
        bc = (bc - 1) & 0xFFFF;

        flags = EmulatorTables.SIGN_ZERO_TABLE[(regs[REG_A] - tmp) & 0xFF] | FLAG_N | (flags & FLAG_C);
        auxCarry(regs[REG_A], (-tmp) & 0xFF);

        if (bc != 0) {
            flags |= FLAG_PV;
        }

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        if ((bc == 0) || (regs[REG_A] == tmp)) {
            return 16;
        }
        PC = (PC - 2) & 0xFFFF;
        return 21;
    }

    int I_LD_REF_NN_RP(short OP) {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = getpair((OP >>> 4) & 3, true);
        writeWord(tmp, tmp1);
        return 20;
    }

    int I_LD_RP_REF_NN(short OP) {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = readWord(tmp);
        putpair((OP >>> 4) & 3, tmp1, true);
        return 20;
    }

    int I_JR_CC_N(short OP) {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        if (getCC1((OP >>> 3) & 3)) {
            PC += (byte) tmp;
            PC &= 0xFFFF;
            return 12;
        }
        return 7;
    }

    int I_JR_N(short OP) {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        PC += (byte) tmp;
        PC &= 0xFFFF;
        return 12;
    }

    int I_ADD_A_N(short OP) {
        int value = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        int oldA = regs[REG_A];
        regs[REG_A] = (oldA + value) & 0xFF;
        flags = flagSZHPC(oldA, value, regs[REG_A]);
        return 7;
    }

    int I_ADC_A_N(short OP) {
        int value = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        int oldA = regs[REG_A];
        regs[REG_A] = (oldA + value + (flags & FLAG_C)) & 0xFF;
        flags = flagSZHPC(oldA, (value + (flags & FLAG_C)) & 0xFF, regs[REG_A]);
        return 7;
    }

    int I_OUT_REF_N_A(short OP) throws IOException {
        int tmp = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        context.writeIO(tmp, (byte) regs[REG_A]);
        return 11;
    }

    int I_SUB_N(short OP) {
        int value = ((~memory.read(PC)) + 1) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        int oldA = regs[REG_A];
        regs[REG_A] = (oldA + value) & 0xFF;
        flags = flagSZHPC(oldA, value, regs[REG_A]) | FLAG_N;
        return 7;
    }

    int I_IN_A_REF_N(short OP) throws IOException {
        int tmp = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_A] = (context.readIO(tmp) & 0xFF);
        return 11;
    }

    int I_SBC_A_N(short OP) {
        int value = ((~readByte(PC)) + 1) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        int oldA = regs[REG_A];
        regs[REG_A] = (oldA + value - (flags & FLAG_C)) & 0xFF;
        flags = flagSZHPC(oldA, (value - (flags & FLAG_C)) & 0xFF, regs[REG_A]) | FLAG_N;
        return 7;
    }

    int I_AND_N(short OP) {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        regs[REG_A] = (regs[REG_A] & tmp) & 0xFF;
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return 7;
    }

    int I_XOR_N(short OP) {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        regs[REG_A] = ((regs[REG_A] ^ tmp) & 0xFF);
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return 7;
    }

    int I_OR_N(short OP) {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        regs[REG_A] = (regs[REG_A] | tmp) & 0xFF;
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return 7;
    }

    int I_CP_N(short OP) {
        int value = ((~readByte(PC)) + 1) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        int diff = (regs[REG_A] + value) & 0xFF;
        flags = flagSZHPC(regs[REG_A], value, diff) | FLAG_N;
        return 7;
    }

    int I_LD_RP_NN(short OP) {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        putpair((OP >>> 4) & 3, tmp, true);
        return 10;
    }

    int I_JP_CC_NN(short OP) {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = (OP >>> 3) & 7;
        if ((flags & CONDITION[tmp1]) == CONDITION_VALUES[tmp1]) {
            PC = tmp;
        }
        return 10;
    }

    int I_CALL_CC_NN(short OP) {
        int tmp = readWord(PC);
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

    int I_LD_REF_NN_HL(short OP) {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = getpair(2, false);
        writeWord(tmp, tmp1);
        return 16;
    }

    int I_LD_HL_REF_NN(short OP) {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = readWord(tmp);
        putpair(2, tmp1, false);
        return 16;
    }

    int I_LD_REF_NN_A(short OP) {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        memory.write(tmp, (byte) regs[REG_A]);
        return 13;
    }

    int I_LD_A_REF_NN(short OP) {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        regs[REG_A] = (memory.read(tmp) & 0xff);
        return 13;
    }

    int I_JP_NN(short OP) {
        PC = readWord(PC);
        return 10;
    }

    int I_CALL_NN(short OP) {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        SP = (SP - 2) & 0xffff;
        writeWord(SP, PC);
        PC = tmp;
        return 17;
    }

    int I_LD_R_R(short OP) {
        int tmp = (OP >>> 3) & 0x07;
        int tmp1 = OP & 0x07;
        putreg(tmp, getreg(tmp1));
        if ((tmp1 == 6) || (tmp == 6)) {
            return 7;
        } else {
            return 4;
        }
    }

    int I_HALT(short OP) {
        currentRunState = RunState.STATE_STOPPED_NORMAL;
        return 4;
    }

    int I_RLC_R(short OP) {
        int tmp = OP & 7;
        int tmp1 = getreg(tmp) & 0xFF;

        int tmp2 = (tmp1 >>> 7) & 1;
        tmp1 = (((tmp1 << 1) & 0xFF) | tmp2) & 0xFF;
        putreg(tmp, tmp1);

        flags = EmulatorTables.SIGN_ZERO_TABLE[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;

        if (tmp == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_RRC_R(short OP) {
        int tmp = OP & 7;
        int tmp1 = getreg(tmp) & 0xFF;

        int tmp2 = tmp1 & 1;
        tmp1 = (((tmp1 >>> 1) & 0x7F) | (tmp2 << 7)) & 0xFF;
        putreg(tmp, tmp1);

        flags = EmulatorTables.SIGN_ZERO_TABLE[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;

        if (tmp == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_RL_R(short OP) {
        int tmp = OP & 7;
        int tmp1 = getreg(tmp)& 0xFF;

        int tmp2 = (tmp1 >>> 7) & 1;
        tmp1 = ((((tmp1 << 1) & 0xFF) | flags & FLAG_C) & 0xFF);
        putreg(tmp, tmp1);

        flags = EmulatorTables.SIGN_ZERO_TABLE[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;

        if (tmp == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_RR_R(short OP) {
        int tmp = OP & 7;
        int tmp1 = getreg(tmp) & 0xFF;

        int tmp2 = tmp1 & 1;
        tmp1 = ((((tmp1 >> 1) & 0x7F) | (flags & FLAG_C) << 7) & 0xFF);
        putreg(tmp, tmp1);

        flags = EmulatorTables.SIGN_ZERO_TABLE[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;

        if (tmp == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_SLA_R(short OP) {
        int tmp = OP & 7;
        int tmp1 = getreg(tmp) & 0xFF;

        int tmp2 = (tmp1 >>> 7) & 1;
        tmp1 = (tmp1 << 1) & 0xFE;
        putreg(tmp, tmp1);

        flags = EmulatorTables.SIGN_ZERO_TABLE[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;

        if (tmp == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_SRA_R(short OP) {
        int tmp = OP & 7;
        int tmp1 = getreg(tmp) & 0xFF;

        int tmp2 = tmp1 & 1;
        tmp1 = (tmp1 >> 1) & 0xFF | (tmp1 & 0x80);
        putreg(tmp, tmp1);

        flags = EmulatorTables.SIGN_ZERO_TABLE[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;

        if (tmp == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_SLL_R(short OP) {
        int tmp = OP & 7;
        int tmp1 = getreg(tmp) & 0xFF;

        int tmp2 = (tmp1 >>> 7) & 1;
        tmp1 = (tmp1 << 1) & 0xFF | tmp1 & 1;
        putreg(tmp, tmp1);

        flags = EmulatorTables.SIGN_ZERO_TABLE[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;

        if (tmp == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_SRL_R(short OP) {
        int tmp = OP & 7;
        int tmp1 = getreg(tmp) & 0xFF;

        int tmp2 = tmp1 & 1;
        tmp1 = (tmp1 >>> 1) & 0x7F;
        putreg(tmp, tmp1);

        flags = EmulatorTables.SIGN_ZERO_TABLE[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;

        if (tmp == 6) {
            return 15;
        } else {
            return 8;
        }
    }


    int I_BIT_N_R(short OP) {
        int tmp = (OP >>> 3) & 7;
        int tmp2 = OP & 7;
        int tmp1 = getreg(tmp2) & 0xFF;
        flags = ((flags & FLAG_C) | FLAG_H | (((tmp1 & (1 << tmp)) == 0) ? (FLAG_Z | FLAG_PV) : 0));
        if (tmp == 7) {
            flags |= (((tmp1 & (1 << tmp)) == 0x80) ? FLAG_S : 0);
        }
        if (tmp2 == 6) {
            return 12;
        } else {
            return 8;
        }
    }

    int I_RES_N_R(short OP) {
        int tmp = (OP >>> 3) & 7;
        int tmp2 = OP & 7;
        int tmp1 = getreg(tmp2) & 0xFF;
        tmp1 = (tmp1 & (~(1 << tmp)));
        putreg(tmp2, tmp1);
        if (tmp2 == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_SET_N_R(short OP) {
        int tmp = (OP >>> 3) & 7;
        int tmp2 = OP & 7;
        int tmp1 = getreg(tmp2) & 0xFF;
        tmp1 = (tmp1 | (1 << tmp));
        putreg(tmp2, tmp1);
        if (tmp2 == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_ADD_IX_RP(short OP) {
        return I_ADD_II_RP(OP, (short) 0xDD);
    }

    int I_ADD_IY_RP(short OP) {
        return I_ADD_II_RP(OP, (short) 0xFD);
    }

    int I_ADD_II_RP(short OP, short special) {
        int tmp = getspecial(special);
        int tmp1 = getpair(special, (OP >>> 4) & 0x03);

        carry15(tmp, tmp1);
        halfCarry11(tmp, tmp1);
        flags &= (~(FLAG_N | FLAG_S | FLAG_Z));

        tmp += tmp1;
        flags |= ((tmp & 0x8000) == 0x8000) ? FLAG_S : 0;
        flags |= (tmp == 0) ? FLAG_Z : 0;
        putspecial(special, tmp);
        return 15;
    }

    int I_LD_IX_NN(short OP) {
        return I_LD_II_NN(OP, (short) 0xDD);
    }

    int I_LD_IY_NN(short OP) {
        return I_LD_II_NN(OP, (short) 0xFD);
    }

    int I_LD_II_NN(short OP, short special) {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        putspecial(special, tmp);
        return 14;
    }

    int I_LD_REF_NN_IX(short OP) {
        return I_LD_REF_NN_II(OP, (short) 0xDD);
    }

    int I_LD_REF_NN_IY(short OP) {
        return I_LD_REF_NN_II(OP, (short) 0xFD);
    }

    int I_LD_REF_NN_II(short OP, short special) {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        writeWord(tmp, getspecial(special));
        return 16;
    }

    int I_INC_IX(short OP) {
        return I_INC_II(OP, (short) 0xDD);
    }

    int I_INC_IY(short OP) {
        return I_INC_II(OP, (short) 0xFD);
    }

    int I_INC_II(short OP, short special) {
        if (special == 0xDD) {
            IX = (IX + 1) & 0xFFFF;
        } else {
            IY = (IY + 1) & 0xFFFF;
        }
        return 10;
    }

    int I_LD_IX_REF_NN(short OP) {
        return I_LD_II_REF_NN(OP, (short) 0xDD);
    }

    int I_LD_IY_REF_NN(short OP) {
        return I_LD_II_REF_NN(OP, (short) 0xFD);
    }

    int I_LD_II_REF_NN(short OP, short special) {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        int tmp1 = readWord(tmp);
        putspecial(special, tmp1);
        return 20;
    }

    int I_DEC_IX(short OP) {
        return I_DEC_II(OP, (short) 0xDD);
    }

    int I_DEC_IY(short OP) {
        return I_DEC_II(OP, (short) 0xFD);
    }

    int I_DEC_II(short OP, short special) {
        putspecial(special, getspecial(special) - 1);
        return 10;
    }

    int I_INC_REF_IX_N(short OP) {
        return I_INC_REF_II_N(OP, (short) 0xDD);
    }

    int I_INC_REF_IY_N(short OP) {
        return I_INC_REF_II_N(OP, (short) 0xFD);
    }

    int I_INC_REF_II_N(short OP, short special) {
        int tmp = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        int tmp1 = (getspecial(special) + (byte) tmp) & 0xFFFF;
        int tmp2 = (memory.read(tmp1) + 1) & 0xFF;

        memory.write(tmp1, (byte) tmp2);
        flags = EmulatorTables.INC_TABLE[tmp2] | (flags & FLAG_C);
        return 23;
    }

    int I_DEC_REF_IX_N(short OP) {
        return I_DEC_REF_II_N(OP, (short) 0xDD);
    }

    int I_DEC_REF_IY_N(short OP) {
        return I_DEC_REF_II_N(OP, (short) 0xFD);
    }

    int I_DEC_REF_II_N(short OP, short special) {
        int tmp = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        int tmp1 = (getspecial(special) + (byte) tmp) & 0xFFFF;
        int tmp2 = (memory.read(tmp1) - 1) & 0xFF;
        memory.write(tmp1, (byte) tmp2);
        flags = EmulatorTables.DEC_TABLE[tmp2] | (flags & FLAG_C);
        return 23;
    }

    int I_LD_REF_IX_N_N(short OP) {
        return I_LD_REF_II_N_N(OP, (short) 0xDD);
    }

    int I_LD_REF_IY_N_N(short OP) {
        return I_LD_REF_II_N_N(OP, (short) 0xFD);
    }

    int I_LD_REF_II_N_N(short OP, short special) {
        byte offset = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        byte number = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        memory.write((getspecial(special) + offset) & 0xFFFF, number);
        return 19;
    }

    int I_LD_R_REF_IX_N(short OP) {
        return I_LD_R_REF_II_N(OP, (short) 0xDD);
    }

    int I_LD_R_REF_IY_N(short OP) {
        return I_LD_R_REF_II_N(OP, (short) 0xFD);
    }

    int I_LD_R_REF_II_N(short OP, short special) {
        byte offset = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int tmp1 = (OP >>> 3) & 7;
        putreg2(tmp1, memory.read((getspecial(special) + offset) & 0xFFFF));
        return 19;
    }

    int I_LD_REF_IX_N_R(short OP) {
        return I_LD_REF_II_N_R(OP, (short)0xDD);
    }

    int I_LD_REF_IY_N_R(short OP) {
        return I_LD_REF_II_N_R(OP, (short)0xFD);
    }

    int I_LD_REF_II_N_R(short OP, short special) {
        byte tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int tmp1 = (OP & 7);
        memory.write((getspecial(special) + tmp) & 0xFFFF, getreg2(tmp1));
        return 19;
    }

    int I_ADD_A_REF_IX_N(short OP) {
        return I_ADD_A_REF_II_N(OP, (short)0xDD);
    }

    int I_ADD_A_REF_IY_N(short OP) {
        return I_ADD_A_REF_II_N(OP, (short)0xFD);
    }

    int I_ADD_A_REF_II_N(short OP, short special) {
        byte offset = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int value = readByte((getspecial(special) + offset) & 0xFFFF);
        int oldA = regs[REG_A];
        regs[REG_A] = (oldA + value) & 0xFF;
        flags = flagSZHPC(oldA, value, regs[REG_A]);
        return 19;
    }

    int I_ADC_A_REF_IX_N(short OP) {
        return I_ADC_A_REF_II_N(OP, (short) 0xDD);
    }

    int I_ADC_A_REF_IY_N(short OP) {
        return I_ADC_A_REF_II_N(OP, (short) 0xFD);
    }

    int I_ADC_A_REF_II_N(short OP, short special) {
        byte offset = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int value = readByte((getspecial(special) + offset) & 0xFFFF);
        int oldA = regs[REG_A];
        regs[REG_A] = (oldA + value + (flags & FLAG_C)) & 0xFF;
        flags = flagSZHPC(oldA, (value + (flags & FLAG_C)) & 0xFF, regs[REG_A]);
        return 19;
    }

    int I_SUB_REF_IX_N(short OP) {
        return I_SUB_REF_II_N(OP, (short) 0xDD);
    }

    int I_SUB_REF_IY_N(short OP) {
        return I_SUB_REF_II_N(OP, (short) 0xFD);
    }

    int I_SUB_REF_II_N(short OP, short special) {
        byte offset = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int value = ((~readByte((getspecial(special) + offset) & 0xFFFF)) + 1) & 0xFF;
        int oldA = regs[REG_A];
        regs[REG_A] = (oldA + value) & 0xFF;
        flags = flagSZHPC(oldA, value, regs[REG_A]) | FLAG_N;
        return 19;
    }

    int I_SBC_A_REF_IX_N(short OP) {
        return I_SBC_A_REF_II_N(OP, (short) 0xDD);
    }

    int I_SBC_A_REF_IY_N(short OP) {
        return I_SBC_A_REF_II_N(OP, (short) 0xFD);
    }

    int I_SBC_A_REF_II_N(short OP, short special) {
        byte offset = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int value = ((~readByte((getspecial(special) + offset) & 0xFFFF)) + 1) & 0xFF;
        int oldA = regs[REG_A];
        regs[REG_A] = (oldA + value - (flags & FLAG_C)) & 0xFF;
        flags = flagSZHPC(oldA, (value - (flags & FLAG_C)) & 0xFF, regs[REG_A]) | FLAG_N;
        return 19;
    }

    int I_AND_REF_IX_N(short OP) {
        return I_AND_REF_II_N(OP, (short) 0xDD);
    }

    int I_AND_REF_IY_N(short OP) {
        return I_AND_REF_II_N(OP, (short) 0xFD);
    }

    int I_AND_REF_II_N(short OP, short special) {
        byte tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        int tmp1 = memory.read((getspecial(special) + tmp) & 0xFFFF);
        regs[REG_A] = (regs[REG_A] & tmp1) & 0xFF;
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return 19;
    }

    int I_XOR_REF_IX_N(short OP) {
        return I_XOR_REF_II_N(OP, (short) 0xDD);
    }

    int I_XOR_REF_IY_N(short OP) {
        return I_XOR_REF_II_N(OP, (short) 0xFD);
    }

    int I_XOR_REF_II_N(short OP, short special) {
        byte tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        int tmp1 = memory.read((getspecial(special) + tmp) & 0xFFFF);
        regs[REG_A] = ((regs[REG_A] ^ tmp1) & 0xff);
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return 19;
    }

    int I_OR_REF_IX_N(short OP) {
        return I_OR_REF_II_N(OP, (short) 0xDD);
    }

    int I_OR_REF_IY_N(short OP) {
        return I_OR_REF_II_N(OP, (short) 0xFD);
    }

    int I_OR_REF_II_N(short OP, short special) {
        byte tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int tmp1 = memory.read((getspecial(special) + tmp) & 0xFFFF);
        regs[REG_A] = ((regs[REG_A] | tmp1) & 0xff);
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return 19;
    }

    int I_CP_REF_IX_N(short OP) {
        return I_CP_REF_II_N(OP, (short) 0xDD);
    }

    int I_CP_REF_IY_N(short OP) {
        return I_CP_REF_II_N(OP, (short) 0xFD);
    }

    int I_CP_REF_II_N(short OP, short special) {
        byte offset = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int value = ((~readByte((getspecial(special) + offset) & 0xFFFF)) + 1) & 0xFF;
        int diff = (regs[REG_A] + value) & 0xFF;
        flags = flagSZHPC(regs[REG_A], value, diff) | FLAG_N;
        return 19;
    }

    int I_POP_IX(short OP) {
        IX = readWord(SP);
        SP = (SP + 2) & 0xFFFF;
        return 14;
    }

    int I_POP_IY(short OP) {
        IY = readWord(SP);
        SP = (SP + 2) & 0xFFFF;
        return 14;
    }

    int I_EX_REF_SP_IX(short OP) {
        int tmp = readWord(SP);
        int tmp1 = IX;
        IX = tmp;
        writeWord(SP, tmp1);
        return 23;
    }

    int I_EX_REF_SP_IY(short OP) {
        int tmp = readWord(SP);
        int tmp1 = IY;
        IY = tmp;
        writeWord(SP, tmp1);
        return 23;
    }

    int I_PUSH_IX(short OP) {
        SP = (SP - 2) & 0xFFFF;
        writeWord(SP, IX);
        return 15;
    }

    int I_PUSH_IY(short OP) {
        SP = (SP - 2) & 0xFFFF;
        writeWord(SP, IY);
        return 15;
    }

    int I_JP_REF_IX(short OP) {
        PC = IX;
        return 8;
    }

    int I_JP_REF_IY(short OP) {
        PC = IY;
        return 8;
    }

    int I_LD_SP_IX(short OP) {
        SP = IX;
        return 10;
    }

    int I_LD_SP_IY(short OP) {
        SP = IY;
        return 10;
    }

    int I_RLC_REF_IX_N_R(short OP, byte operand) {
        return I_RLC_REF_II_N_R(OP, operand, (short) 0xDD);
    }

    int I_RLC_REF_IY_N_R(short OP, byte operand) {
        return I_RLC_REF_II_N_R(OP, operand, (short) 0xFD);
    }

    int I_RLC_REF_II_N_R(short OP, byte operand, short special) {
        int address = (getspecial(special) + operand) & 0xFFFF;
        int value = readByte(address);
        int bit7 = (value >>> 7) & 1;

        value = (((value << 1) | bit7) & 0xFF);
        memory.write(address, (byte) value);
        flags = EmulatorTables.SIGN_ZERO_TABLE[value] | EmulatorTables.PARITY_TABLE[value] | bit7;

        // regs[6] is unused, so it's ok
        regs[OP & 7] = value & 0xFF;
        return 23;
    }

    int I_RRC_REF_IX_N_R(short OP, byte operand) {
        return I_RRC_REF_II_N_R(OP, operand, (short) 0xDD);
    }

    int I_RRC_REF_IY_N_R(short OP, byte operand) {
        return I_RRC_REF_II_N_R(OP, operand, (short) 0xFD);
    }

    int I_RRC_REF_II_N_R(short OP, byte operand, short special) {
        int tmp = (getspecial(special) + operand) & 0xffff;
        int tmp1 = memory.read(tmp);

        int tmp2 = tmp1 & 1;
        tmp1 = (((tmp1 >>> 1) & 0x7F) | (tmp2 << 7)) & 0xFF;

        memory.write(tmp, (byte) (tmp1 & 0xFF));
        flags = EmulatorTables.SIGN_ZERO_TABLE[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;

        // regs[6] is unused, so it's ok
        regs[OP & 7] = tmp1 & 0xFF;
        return 23;
    }

    int I_RL_REF_IX_N_R(short OP, byte operand) {
        return I_RL_REF_II_N_R(OP, operand, (short) 0xDD);
    }

    int I_RL_REF_IY_N_R(short OP, byte operand) {
        return I_RL_REF_II_N_R(OP, operand, (short) 0xFD);
    }

    int I_RL_REF_II_N_R(short OP, byte operand, short special) {
        int tmp = (getspecial(special) + operand) & 0xffff;
        int tmp1 = memory.read(tmp);

        int tmp2 = (tmp1 >>> 7) & 1;
        tmp1 = ((((tmp1 << 1) & 0xFF) | flags & FLAG_C) & 0xFF);
        memory.write(tmp, (byte) (tmp1 & 0xFF));

        flags = EmulatorTables.SIGN_ZERO_TABLE[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;
        // regs[6] is unused, so it's ok
        regs[OP & 7] = tmp1 & 0xFF;
        return 23;
    }

    int I_RR_REF_IX_N_R(short OP, byte operand) {
        return I_RR_REF_II_N_R(OP, operand, (short) 0xDD);
    }

    int I_RR_REF_IY_N_R(short OP, byte operand) {
        return I_RR_REF_II_N_R(OP, operand, (short) 0xFD);
    }

    int I_RR_REF_II_N_R(short OP, byte operand, short special) {
        int address = (getspecial(special) + operand) & 0xffff;
        int value = readByte(address);

        int bit0 = value & 1;
        value = ((((value >> 1) & 0xFF) | (flags & FLAG_C) << 7) & 0xFF);
        memory.write(address, (byte) (value & 0xFF));

        flags = EmulatorTables.SIGN_ZERO_TABLE[value] | EmulatorTables.PARITY_TABLE[value] | bit0;
        // regs[6] is unused, so it's ok
        regs[OP & 7] = value & 0xFF;
        return 23;
    }

    int I_SLA_REF_IX_N_R(short OP, byte operand) {
        return I_SLA_REF_II_N_R(OP, operand, (short) 0xDD);
    }

    int I_SLA_REF_IY_N_R(short OP, byte operand) {
        return I_SLA_REF_II_N_R(OP, operand, (short) 0xFD);
    }

    int I_SLA_REF_II_N_R(short OP, byte operand, short special) {
        int tmp = (getspecial(special) + operand) & 0xffff;
        int tmp1 = memory.read(tmp);

        int tmp2 = (tmp1 >>> 7) & 1;
        tmp1 = (tmp1 << 1) & 0xFE;
        memory.write(tmp, (byte) tmp1);

        flags = EmulatorTables.SIGN_ZERO_TABLE[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;
        // regs[6] is unused, so it's ok
        regs[OP & 7] = tmp1 & 0xFF;
        return 23;
    }

    int I_SRA_REF_IX_N_R(short OP, byte operand) {
        return I_SRA_REF_II_N_R(OP, operand, (short) 0xDD);
    }

    int I_SRA_REF_IY_N_R(short OP, byte operand) {
        return I_SRA_REF_II_N_R(OP, operand, (short) 0xFD);
    }

    int I_SRA_REF_II_N_R(short OP, byte operand, short special) {
        int tmp = (getspecial(special) + operand) & 0xffff;
        int tmp1 = memory.read(tmp);

        int tmp2 = tmp1 & 1;
        tmp1 = (tmp1 >> 1) & 0xFF | (tmp1 & 0x80);
        memory.write(tmp, (byte) tmp1);

        flags = EmulatorTables.SIGN_ZERO_TABLE[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;
        // regs[6] is unused, so it's ok
        regs[OP & 7] = tmp1 & 0xFF;
        return 23;
    }

    int I_SLL_REF_IX_N_R(short OP, byte operand) {
        return I_SLL_REF_II_N_R(OP, operand, (short) 0xDD);
    }

    int I_SLL_REF_IY_N_R(short OP, byte operand) {
        return I_SLL_REF_II_N_R(OP, operand, (short) 0xFD);
    }

    int I_SLL_REF_II_N_R(short OP, byte operand, short special) {
        int tmp = (getspecial(special) + operand) & 0xffff;
        int tmp1 = memory.read(tmp);

        int tmp2 = (tmp1 >>> 7) & 1;
        tmp1 = (tmp1 << 1) & 0xFF | tmp1 & 1;
        memory.write(tmp, (byte) tmp1);

        flags = EmulatorTables.SIGN_ZERO_TABLE[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;
        // regs[6] is unused, so it's ok
        regs[OP & 7] = tmp1 & 0xFF;
        return 23;
    }

    int I_SRL_REF_IX_N_R(short OP, byte operand) {
        return I_SRL_REF_II_N_R(OP, operand, (short)0xDD);
    }

    int I_SRL_REF_IY_N_R(short OP, byte operand) {
        return I_SRL_REF_II_N_R(OP, operand, (short)0xFD);
    }

    int I_SRL_REF_II_N_R(short OP, byte operand, short special) {
        int tmp = (getspecial(special) + operand) & 0xffff;
        int tmp1 = memory.read(tmp);

        int tmp2 = tmp1 & 1;
        tmp1 = (tmp1 >>> 1) & 0x7F;
        memory.write(tmp, (byte) tmp1);

        flags = EmulatorTables.SIGN_ZERO_TABLE[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;
        // regs[6] is unused, so it's ok
        regs[OP & 7] = tmp1 & 0xFF;
        return 23;
    }

    int I_BIT_N_REF_IX_N(short OP, byte operand) {
        return I_BIT_N_REF_II_N(OP, operand, (short) 0xDD);
    }

    int I_BIT_N_REF_IY_N(short OP, byte operand) {
        return I_BIT_N_REF_II_N(OP, operand, (short) 0xFD);
    }

    int I_BIT_N_REF_II_N(short OP, byte operand, short special) {
        int bitNumber = (OP >>> 3) & 7;
        int tmp1 = memory.read((getspecial(special) + operand) & 0xFFFF);
        flags = ((flags & FLAG_C) | FLAG_H | (((tmp1 & (1 << bitNumber)) == 0) ? (FLAG_Z | FLAG_PV) : 0));
        if (bitNumber == 7) {
            flags |= (((tmp1 & (1 << 7)) == 0x80) ? FLAG_S : 0);
        }
        return 20;
    }

    int I_RES_N_REF_IX_N_R(short OP, byte operand) {
        return I_RES_N_REF_II_N_R(OP, operand, (short) 0xDD);
    }

    int I_RES_N_REF_IY_N_R(short OP, byte operand) {
        return I_RES_N_REF_II_N_R(OP, operand, (short) 0xFD);
    }

    int I_RES_N_REF_II_N_R(short OP, byte operand, short special) {
        int tmp2 = (OP >>> 3) & 7;
        int tmp3 = (getspecial(special) + operand) & 0xffff;
        int tmp1 = memory.read(tmp3);
        tmp1 = (tmp1 & (~(1 << tmp2)));
        memory.write(tmp3, (byte) (tmp1 & 0xff));
        // regs[6] is unused, so it's ok
        regs[OP & 7] = tmp1 & 0xFF;
        return 23;
    }

    int I_SET_N_REF_IX_N_R(short OP, byte operand) {
        return I_SET_N_REF_II_N_R(OP, operand, (short) 0xDD);
    }

    int I_SET_N_REF_IY_N_R(short OP, byte operand) {
        return I_SET_N_REF_II_N_R(OP, operand, (short) 0xFD);
    }

    int I_SET_N_REF_II_N_R(short OP, byte operand, short special) {
        int tmp2 = (OP >>> 3) & 7;
        int tmp3 = (getspecial(special) + operand) & 0xffff;
        int tmp1 = memory.read(tmp3);
        tmp1 = (tmp1 | (1 << tmp2));
        memory.write(tmp3, (byte) (tmp1 & 0xff));

        // regs[6] is unused, so it's ok
        regs[OP & 7] = tmp1 & 0xFF;
        return 23;
    }
}
