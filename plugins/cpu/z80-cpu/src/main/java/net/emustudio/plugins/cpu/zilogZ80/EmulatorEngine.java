/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
import net.emustudio.emulib.runtime.helpers.SleepUtils;
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

import static net.emustudio.plugins.cpu.zilogZ80.DispatchTables.*;
import static net.emustudio.plugins.cpu.zilogZ80.EmulatorTables.*;

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

    private int lastOpcode;

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
        lastOpcode = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        try {
            dispatch();
        } catch (Exception e) {
            throw e;
        } catch (Throwable e) {
            throw new Exception(e);
        }
        isINT = (interruptPending != 0) && oldIFF && IFF[0];
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
                    lastOpcode = readByte(PC);
                    PC = (PC + 1) & 0xFFFF;
                    cycles = dispatch();
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
                SleepUtils.preciseSleepNanos(slice - endTime);
            }
        }
        return currentRunState;
    }

    private int dispatch() throws Throwable {
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
            MethodHandle instr = DISPATCH_TABLE[lastOpcode];
            if (instr != null) {
                return (int) instr.invokeExact(this);
            }
            return 4;
        } finally {
            if (tmpListener != null) {
                tmpListener.afterDispatch();
            }
        }
    }

    int CB_DISPATCH() throws Throwable {
        return DISPATCH(DISPATCH_TABLE_CB);
    }

    int DD_DISPATCH() throws Throwable {
        return DISPATCH(DISPATCH_TABLE_DD);
    }

    int DD_CB_DISPATCH() throws Throwable {
        return SPECIAL_CB_DISPATCH(DISPATCH_TABLE_DD_CB);
    }

    int ED_DISPATCH() throws Throwable {
        return DISPATCH(DISPATCH_TABLE_ED);
    }

    int FD_DISPATCH() throws Throwable {
        return DISPATCH(DISPATCH_TABLE_FD);
    }

    int FD_CB_DISPATCH() throws Throwable {
        return SPECIAL_CB_DISPATCH(DISPATCH_TABLE_FD_CB);
    }

    private int DISPATCH(MethodHandle[] table) throws Throwable {
        lastOpcode = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        incrementR();

        MethodHandle instr = table[lastOpcode];
        if (instr != null) {
            return (int) instr.invokeExact(this);
        }
        return 4;
    }

    int SPECIAL_CB_DISPATCH(MethodHandle[] table) throws Throwable {
        byte operand = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        lastOpcode = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        incrementR();

        MethodHandle instr = table[lastOpcode];
        if (instr != null) {
            return (int) instr.invokeExact(this, operand);
        }
        return 4;
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
                lastOpcode = interruptVector & 0xFF;
                dispatch(); // must ignore halt
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

    private void putreg(int reg, int val) {
        if (reg == 6) {
            memory.write((regs[REG_H] << 8) | regs[REG_L], (byte) (val & 0xFF));
        } else {
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

    private void bigOverflow(int i, int j, int result) {
        int sign = i & 0x8000;
        if (sign != (j & 0x8000)) {
            flags &= (~FLAG_PV);
        } else if ((result & 0x8000) != sign) {
            flags |= FLAG_PV;
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


    int I_NOP() {
        return 4;
    }

    int I_LD_REF_BC_A() {
        memory.write(getpair(0, false), (byte) regs[REG_A]);
        return 7;
    }

    int I_INC_RP() {
        int tmp = (lastOpcode >>> 4) & 0x03;
        putpair(tmp, (getpair(tmp, true) + 1) & 0xFFFF, true);
        return 6;
    }

    int I_ADD_HL_RP() {
        int tmp = getpair((lastOpcode >>> 4) & 0x03, true);
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

    int I_DEC_RP() {
        int regPair = (lastOpcode >>> 4) & 0x03;
        putpair(regPair, (getpair(regPair, true) - 1) & 0xFFFF, true);
        return 6;
    }

    int I_POP_RP() {
        int regPair = (lastOpcode >>> 4) & 0x03;
        int value = readWord(SP);
        SP = (SP + 2) & 0xffff;
        putpair(regPair, value, false);
        return 10;
    }

    int I_PUSH_RP() {
        int regPair = (lastOpcode >>> 4) & 0x03;
        int value = getpair(regPair, false);
        SP = (SP - 2) & 0xffff;
        writeWord(SP, value);
        return 11;
    }

    int I_LD_R_N() {
        int reg = (lastOpcode >>> 3) & 0x07;
        putreg(reg, memory.read(PC));
        PC = (PC + 1) & 0xFFFF;
        if (reg == 6) {
            return 10;
        } else {
            return 7;
        }
    }

    int I_INC_R() {
        int reg = (lastOpcode >>> 3) & 0x07;
        int regValue = getreg(reg);
        int sum = (regValue + 1) & 0x1FF;
        int sumByte = sum & 0xFF;
        flags = TABLE_SZ[sumByte] | (TABLE_HP[sum ^ 1 ^ regValue]) | (flags & FLAG_C);
        putreg(reg, sumByte);
        return (reg == 6) ? 11 : 4;
    }

    int I_DEC_R() {
        int reg = (lastOpcode >>> 3) & 0x07;
        int regValue = getreg(reg);
        int sum = (regValue - 1) & 0x1FF;
        int sumByte = sum & 0xFF;
        flags = TABLE_SUB[sumByte] | (TABLE_HP[sum ^ 1 ^ regValue]) | (flags & FLAG_C);
        putreg(reg, sumByte);
        return (reg == 6) ? 11 : 4;
    }

    int I_RET_CC() {
        int cc = (lastOpcode >>> 3) & 7;
        if ((flags & CONDITION[cc]) == CONDITION_VALUES[cc]) {
            PC = readWord(SP);
            SP = (SP + 2) & 0xffff;
            return 11;
        }
        return 5;
    }

    int I_RST() {
        SP = (SP - 2) & 0xffff;
        writeWord(SP, PC);
        PC = lastOpcode & 0x38;
        return 11;
    }

    int I_ADD_A_R() {
        int value = getreg(lastOpcode & 0x07);
        int oldA = regs[REG_A];
        int sum = (oldA + value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]);
        return (lastOpcode == 0x86) ? 7 : 4;
    }

    int I_ADC_A_R() {
        int value = getreg(lastOpcode & 0x07);
        int oldA = regs[REG_A];
        int sum = (oldA + value + (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]);
        return (lastOpcode == 0x8E) ? 7 : 4;
    }

    int I_SUB_R() {
        int value = getreg(lastOpcode & 0x07);

        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;

        flags = TABLE_SUB[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]);
        return (lastOpcode == 0x96) ? 7 : 4;
    }

    int I_SBC_A_R() {
        int value = getreg(lastOpcode & 0x07);

        int oldA = regs[REG_A];
        int sum = (oldA - value - (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;

        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA];
        return (lastOpcode == 0x9E) ? 7 : 4;
    }

    int I_AND_R() {
        regs[REG_A] = (regs[REG_A] & getreg(lastOpcode & 7)) & 0xFF;
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return (lastOpcode == 0xA6) ? 7 : 4;
    }

    int I_XOR_R() {
        regs[REG_A] = ((regs[REG_A] ^ getreg(lastOpcode & 7)) & 0xff);
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return (lastOpcode == 0xAE) ? 7 : 4;
    }

    int I_OR_R() {
        regs[REG_A] = (regs[REG_A] | getreg(lastOpcode & 7)) & 0xFF;
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return (lastOpcode == 0xB6) ? 7 : 4;
    }

    int I_CP_R() {
        int value = getreg(lastOpcode & 7);
        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        int result = sum & 0xFF;
        flags = TABLE_SUB[result] | (TABLE_CHP[sum ^ value ^ oldA]);
        return (lastOpcode == 0xBE) ? 7 : 4;
    }

    int I_RLCA() {
        int tmp = regs[REG_A] >>> 7;
        regs[REG_A] = ((((regs[REG_A] << 1) & 0xFF) | tmp) & 0xff);
        flags = ((flags & 0xEC) | tmp);
        return 4;
    }

    int I_EX_AF_AFF() {
        int tmp = regs[REG_A];
        regs[REG_A] = regs2[REG_A];
        regs2[REG_A] = tmp;
        tmp = flags;
        flags = flags2;
        flags2 = tmp;
        return 4;
    }

    int I_LD_A_REF_BC() {
        regs[REG_A] = readByte(getpair(0, false));
        return 7;
    }

    int I_RRCA() {
        flags = ((flags & 0xEC) | (regs[REG_A] & 1));
        regs[REG_A] = EmulatorTables.RRCA_TABLE[regs[REG_A]];
        return 4;
    }

    int I_DJNZ() {
        byte tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_B] = (regs[REG_B] - 1) & 0xFF;
        if (regs[REG_B] != 0) {
            PC = (PC - 2) + tmp;
            PC &= 0xFFFF;
            return 13;
        }
        return 8;
    }

    int I_LD_REF_DE_A() {
        memory.write(getpair(1, false), (byte) regs[REG_A]);
        return 7;
    }

    int I_RLA() {
        int tmp = regs[REG_A] >>> 7;
        regs[REG_A] = (((regs[REG_A] << 1) | (flags & FLAG_C)) & 0xff);
        flags = ((flags & 0xEC) | tmp);
        return 4;
    }

    int I_LD_A_REF_DE() {
        regs[REG_A] = readByte(getpair(1, false));
        return 7;
    }

    int I_RRA() {
        int tmp = (flags & FLAG_C) << 7;
        flags = ((flags & 0xEC) | (regs[REG_A] & 1));
        regs[REG_A] = ((regs[REG_A] >>> 1 | tmp) & 0xff);
        return 4;
    }

    int I_DAA() {
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
        flags |= EmulatorTables.PARITY_TABLE[regs[REG_A]] | TABLE_SZ[regs[REG_A]];
        return 4;
    }

    int I_CPL() {
        regs[REG_A] = ((~regs[REG_A]) & 0xFF);
        flags |= FLAG_N | FLAG_H;
        return 4;
    }

    int I_SCF() {
        flags |= FLAG_N | FLAG_C;
        flags &= ~FLAG_H;
        return 4;
    }

    int I_CCF() {
        int tmp = flags & FLAG_C;
        if (tmp == 0) {
            flags |= FLAG_C;
        } else {
            flags &= ~FLAG_C;
        }
        flags &= ~FLAG_N;
        return 4;
    }

    int I_RET() {
        PC = readWord(SP);
        SP = (SP + 2) & 0xFFFF;
        return 10;
    }

    int I_EXX() {
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

    int I_EX_REF_SP_HL() {
        byte tmp = memory.read(SP);
        int x = (SP + 1) & 0xFFFF;
        byte tmp1 = memory.read(x);
        memory.write(SP, (byte) regs[REG_L]);
        memory.write(x, (byte) regs[REG_H]);
        regs[REG_L] = tmp & 0xFF;
        regs[REG_H] = tmp1 & 0xFF;
        return 19;
    }

    int I_JP_REF_HL() {
        PC = ((regs[REG_H] << 8) | regs[REG_L]);
        return 4;
    }

    int I_EX_DE_HL() {
        int tmp = regs[REG_D];
        regs[REG_D] = regs[REG_H];
        regs[REG_H] = tmp;
        tmp = regs[REG_E];
        regs[REG_E] = regs[REG_L];
        regs[REG_L] = tmp;
        return 4;
    }

    int I_DI() {
        IFF[0] = IFF[1] = false;
        return 4;
    }

    int I_LD_SP_HL() {
        SP = ((regs[REG_H] << 8) | regs[REG_L]);
        return 6;
    }

    int I_EI() {
        IFF[0] = IFF[1] = true;
        return 4;
    }

    int I_IN_R_REF_C() throws IOException {
        int tmp = (lastOpcode >>> 3) & 0x7;
        putreg(tmp, context.readIO(regs[REG_C]));
        flags = (flags & FLAG_C) | TABLE_SZ[regs[tmp]] | EmulatorTables.PARITY_TABLE[regs[tmp]];
        return 12;
    }

    int I_OUT_REF_C_R() throws IOException {
        int tmp = (lastOpcode >>> 3) & 0x7;
        context.writeIO(regs[REG_C], (byte) getreg(tmp));
        return 12;
    }

    int I_SBC_HL_RP() {
        int tmp = -getpair((lastOpcode >>> 4) & 0x03, true);
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

    int I_ADC_HL_RP() {
        int tmp = getpair((lastOpcode >>> 4) & 0x03, true);
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

    int I_NEG() {
        int prevValue = regs[REG_A];
        regs[REG_A] = (((~prevValue) & 0xFF) + 1) & 0xFF;
        int zero = (regs[REG_A] == 0) ? FLAG_Z : 0;
        int carry = (prevValue == 0) ? 0 : FLAG_C;
        int carryIns = regs[REG_A] ^ prevValue;
        int overflow = (prevValue == 0x80) ? FLAG_PV : 0;
        flags = overflow | (regs[REG_A] & 0x80) | zero | (carryIns & 0x10) | FLAG_N | carry;
        return 8;
    }

    int I_RETN() {
        IFF[0] = IFF[1];
        PC = readWord(SP);
        SP = (SP + 2) & 0xffff;
        return 14;
    }

    int I_IM_0() {
        intMode = 0;
        return 8;
    }

    int I_LD_I_A() {
        I = regs[REG_A];
        return 9;
    }

    int I_RETI() {
        IFF[0] = IFF[1];
        PC = readWord(SP);
        SP = (SP + 2) & 0xffff;
        return 14;
    }

    int I_LD_R_A() {
        R = regs[REG_A];
        return 9;
    }

    int I_IM_1() {
        intMode = 1;
        return 8;
    }

    int I_LD_A_I() {
        regs[REG_A] = I & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (IFF[1] ? FLAG_PV : 0) | (flags & FLAG_C);
        return 9;
    }

    int I_IM_2() {
        intMode = 2;
        return 8;
    }

    int I_LD_A_R() {
        regs[REG_A] = R & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (IFF[1] ? FLAG_PV : 0) | (flags & FLAG_C);
        return 9;
    }

    int I_RRD() {
        int tmp = regs[REG_A] & 0x0F;
        int tmp1 = memory.read((regs[REG_H] << 8) | regs[REG_L]);
        regs[REG_A] = ((regs[REG_A] & 0xF0) | (tmp1 & 0x0F));
        tmp1 = ((tmp1 >>> 4) & 0x0F) | (tmp << 4);
        memory.write(((regs[REG_H] << 8) | regs[REG_L]), (byte) (tmp1 & 0xff));
        flags = TABLE_SZ[regs[REG_A]] | EmulatorTables.PARITY_TABLE[regs[REG_A]] | (flags & FLAG_C);
        return 18;
    }

    int I_RLD() {
        int tmp = memory.read((regs[REG_H] << 8) | regs[REG_L]);
        int tmp1 = (tmp >>> 4) & 0x0F;
        tmp = ((tmp << 4) & 0xF0) | (regs[REG_A] & 0x0F);
        regs[REG_A] = ((regs[REG_A] & 0xF0) | tmp1);
        memory.write((regs[REG_H] << 8) | regs[REG_L], (byte) (tmp & 0xff));
        flags = TABLE_SZ[regs[REG_A]] | EmulatorTables.PARITY_TABLE[regs[REG_A]] | (flags & FLAG_C);
        return 18;
    }

    int I_IN_REF_C() throws IOException {
        int tmp = (context.readIO(regs[REG_C]) & 0xFF);
        flags = TABLE_SZ[tmp] | EmulatorTables.PARITY_TABLE[tmp] | (flags & FLAG_C);
        return 12;
    }

    int I_OUT_REF_C_0() throws IOException {
        context.writeIO(regs[REG_C], (byte) 0);
        return 12;
    }

    int I_CPI() {
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int bc = (regs[REG_B] << 8) | regs[REG_C];

        int value = readByte(hl);
        hl = (hl + 1) & 0xFFFF;
        bc = (bc - 1) & 0xFFFF;

        int sum = (regs[REG_A] - value) & 0xFF;
        int flagH = (sum ^ regs[REG_A] ^ value) & FLAG_H;
        int flagP = (bc != 0) ? FLAG_PV : 0;
        flags = TABLE_SUB[sum] | flagH | flagP | (flags & FLAG_C);

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;
        return 16;
    }

    int I_CPIR() {
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int bc = (regs[REG_B] << 8) | regs[REG_C];

        int value = readByte(hl);
        hl = (hl + 1) & 0xFFFF;
        bc = (bc - 1) & 0xFFFF;

        int sum = (regs[REG_A] - value) & 0xFF;
        int flagH = (sum ^ regs[REG_A] ^ value) & FLAG_H;
        int flagP = (bc != 0) ? FLAG_PV : 0;
        flags = TABLE_SUB[sum] | flagH | flagP | (flags & FLAG_C);

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        if ((bc == 0) || (regs[REG_A] == value)) {
            return 16;
        }
        PC = (PC - 2) & 0xFFFF;
        return 21;
    }

    int I_CPD() {
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int bc = (regs[REG_B] << 8) | regs[REG_C];

        int value = readByte(hl);
        hl = (hl - 1) & 0xFFFF;
        bc = (bc - 1) & 0xFFFF;

        int sum = (regs[REG_A] - value) & 0xFF;
        int flagH = (sum ^ regs[REG_A] ^ value) & FLAG_H;
        int flagP = (bc != 0) ? FLAG_PV : 0;
        flags = TABLE_SUB[sum] | flagH | flagP | (flags & FLAG_C);

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;
        return 16;
    }

    int I_CPDR() {
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int bc = (regs[REG_B] << 8) | regs[REG_C];

        int value = readByte(hl);
        hl = (hl - 1) & 0xFFFF;
        bc = (bc - 1) & 0xFFFF;

        int sum = (regs[REG_A] - value) & 0xFF;
        int flagH = (sum ^ regs[REG_A] ^ value) & FLAG_H;
        int flagP = (bc != 0) ? FLAG_PV : 0;
        flags = TABLE_SUB[sum] | flagH | flagP | (flags & FLAG_C);

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        if ((bc == 0) || (regs[REG_A] == value)) {
            return 16;
        }
        PC = (PC - 2) & 0xFFFF;
        return 21;
    }

    int I_LDD() {
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int de = (regs[REG_D] << 8) | regs[REG_E];
        int bc = (regs[REG_B] << 8) | regs[REG_C];

        memory.write(de, memory.read(hl));

        hl = (hl - 1) & 0xFFFF;
        de = (de - 1) & 0xFFFF;
        bc = (bc - 1) & 0xFFFF;

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_D] = (de >>> 8) & 0xFF;
        regs[REG_E] = de & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;
        flags = ((flags & FLAG_S) | (flags & FLAG_Z) | (flags & FLAG_C)) & (~FLAG_PV);
        if (bc != 0) {
            flags |= FLAG_PV;
        }
        return 16;
    }

    int I_LDDR() {
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

    int I_LDI() {
        int hl = getpair(2, false);
        int de = getpair(1, false);
        int bc = getpair(0, false);

        memory.write(de, memory.read(hl));

        hl = (hl + 1) & 0xFFFF;
        de = (de + 1) & 0xFFFF;
        bc = (bc - 1) & 0xFFFF;

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_D] = (de >>> 8) & 0xFF;
        regs[REG_E] = de & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        int flagP = (bc != 0) ? FLAG_PV : 0;
        flags = (flags & FLAG_S) | (flags & FLAG_Z) | (flags & FLAG_C) | flagP;
        return 16;
    }

    int I_LDIR() {
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int de = (regs[REG_D] << 8) | regs[REG_E];
        int bc = (regs[REG_B] << 8) | regs[REG_C];

        memory.write(de, memory.read(hl));

        hl = (hl + 1) & 0xFFFF;
        de = (de + 1) & 0xFFFF;
        bc = (bc - 1) & 0xFFFF;

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_D] = (de >>> 8) & 0xFF;
        regs[REG_E] = de & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        int flagP = (bc != 0) ? FLAG_PV : 0;
        flags = (flags & FLAG_S) | (flags & FLAG_Z) | (flags & FLAG_C) | flagP;
        if (bc == 0) {
            return 16;
        }
        PC = (PC - 2) & 0xFFFF;
        return 21;
    }

    int I_INI() throws IOException {
        byte value = context.readIO(regs[REG_C]);
        int address = (regs[REG_H] << 8) | regs[REG_L];
        memory.write(address, value);

        regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);
        address++;

        regs[REG_H] = (address >>> 8) & 0xFF;
        regs[REG_L] = address & 0xFF;

        int flagZ = (regs[REG_B] == 0) ? FLAG_Z : 0;
        flags = flagZ | (flags & FLAG_C) | FLAG_N | (flags & FLAG_S) | (flags & FLAG_H) | (flags & FLAG_PV);
        return 16;
    }

    int I_INIR() throws IOException {
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

    int I_IND() throws IOException {
        byte value = context.readIO(regs[REG_C]);
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        memory.write(hl, value);

        regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);
        hl = (hl - 1) & 0xFFFF;

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;

        flags = FLAG_N | (regs[REG_B] == 0 ? FLAG_Z : 0) | (flags & FLAG_C) | (flags & FLAG_S) | (flags & FLAG_PV) | (flags & FLAG_H);
        return 16;
    }

    int I_INDR() throws IOException {
        byte value = context.readIO(regs[REG_C]);
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        memory.write(hl, value);

        regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);
        hl = (hl - 1) & 0xFFFF;

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;

        flags |= FLAG_Z | FLAG_N; // FLAG_Z is set b/c it is expected that INIR will be repeated until B=0

        if (regs[REG_B] == 0) {
            return 16;
        }
        PC = (PC - 2) & 0xFFFF;
        return 21;
    }

    int I_OUTI() throws IOException {
        int address = (regs[REG_H] << 8) | regs[REG_L];
        byte value = memory.read(address);
        context.writeIO(regs[REG_C], value);

        address++;
        regs[REG_H] = (address >>> 8) & 0xFF;
        regs[REG_L] = address & 0xFF;
        regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);

        flags = FLAG_N | (regs[REG_B] == 0 ? FLAG_Z : 0) | (flags & FLAG_C) | (flags & FLAG_S) | (flags & FLAG_PV) | (flags & FLAG_H);
        return 16;
    }

    int I_OTIR() throws IOException {
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

    int I_OUTD() throws IOException {
        int address = (regs[REG_H] << 8) | regs[REG_L];
        byte value = memory.read(address);
        context.writeIO(regs[REG_C], value);

        address--;
        regs[REG_H] = (address >>> 8) & 0xFF;
        regs[REG_L] = address & 0xFF;
        regs[REG_B] = ((regs[REG_B] - 1) & 0xFF);

        flags = FLAG_N | (regs[REG_B] == 0 ? FLAG_Z : 0) | (flags & FLAG_C) | (flags & FLAG_S) | (flags & FLAG_PV) | (flags & FLAG_H);
        return 16;
    }

    int I_OTDR() throws IOException {
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

    int I_LD_REF_NN_RP() {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = getpair((lastOpcode >>> 4) & 3, true);
        writeWord(tmp, tmp1);
        return 20;
    }

    int I_LD_RP_REF_NN() {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = readWord(tmp);
        putpair((lastOpcode >>> 4) & 3, tmp1, true);
        return 20;
    }

    int I_JR_CC_N() {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        if (getCC1((lastOpcode >>> 3) & 3)) {
            PC = (PC - 2) + (byte) tmp;
            PC &= 0xFFFF;
            return 12;
        }
        return 7;
    }

    int I_JR_N() {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        PC = (PC - 2) + (byte) tmp;
        PC &= 0xFFFF;
        return 12;
    }

    int I_ADD_A_N() {
        int value = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        int oldA = regs[REG_A];
        int sum = (oldA + value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]);
        return 7;
    }

    int I_ADC_A_N() {
        int value = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        int oldA = regs[REG_A];
        int sum = (oldA + value + (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]);
        return 7;
    }

    int I_OUT_REF_N_A() throws IOException {
        int tmp = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        context.writeIO(tmp, (byte) regs[REG_A]);
        return 11;
    }

    int I_SUB_N() {
        int value = readByte(PC);
        PC = (PC + 1) & 0xFFFF;

        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;

        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA];
        return 7;
    }

    int I_IN_A_REF_N() throws IOException {
        int tmp = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_A] = (context.readIO(tmp) & 0xFF);
        return 11;
    }

    int I_SBC_A_N() {
        int value = readByte(PC);
        PC = (PC + 1) & 0xFFFF;

        int oldA = regs[REG_A];
        int sum = (oldA - value - (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;

        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA];
        return 7;
    }

    int I_AND_N() {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        regs[REG_A] = (regs[REG_A] & tmp) & 0xFF;
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return 7;
    }

    int I_XOR_N() {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        regs[REG_A] = ((regs[REG_A] ^ tmp) & 0xFF);
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return 7;
    }

    int I_OR_N() {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        regs[REG_A] = (regs[REG_A] | tmp) & 0xFF;
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return 7;
    }

    int I_CP_N() {
        int value = readByte(PC);
        PC = (PC + 1) & 0xFFFF;

        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        int result = sum & 0xFF;
        flags = TABLE_SUB[result] | (TABLE_CHP[sum ^ value ^ oldA]);
        return 7;
    }

    int I_LD_RP_NN() {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        putpair((lastOpcode >>> 4) & 3, tmp, true);
        return 10;
    }

    int I_JP_CC_NN() {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = (lastOpcode >>> 3) & 7;
        if ((flags & CONDITION[tmp1]) == CONDITION_VALUES[tmp1]) {
            PC = tmp;
        }
        return 10;
    }

    int I_CALL_CC_NN() {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = (lastOpcode >>> 3) & 7;
        if ((flags & CONDITION[tmp1]) == CONDITION_VALUES[tmp1]) {
            SP = (SP - 2) & 0xffff;
            writeWord(SP, PC);
            PC = tmp;
            return 17;
        }
        return 10;
    }

    int I_LD_REF_NN_HL() {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = getpair(2, false);
        writeWord(tmp, tmp1);
        return 16;
    }

    int I_LD_HL_REF_NN() {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = readWord(tmp);
        putpair(2, tmp1, false);
        return 16;
    }

    int I_LD_REF_NN_A() {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        memory.write(tmp, (byte) regs[REG_A]);
        return 13;
    }

    int I_LD_A_REF_NN() {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        regs[REG_A] = (memory.read(tmp) & 0xff);
        return 13;
    }

    int I_JP_NN() {
        PC = readWord(PC);
        return 10;
    }

    int I_CALL_NN() {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        SP = (SP - 2) & 0xffff;
        writeWord(SP, PC);
        PC = tmp;
        return 17;
    }

    int I_LD_R_R() {
        int tmp = (lastOpcode >>> 3) & 0x07;
        int tmp1 = lastOpcode & 0x07;
        putreg(tmp, getreg(tmp1));
        if ((tmp1 == 6) || (tmp == 6)) {
            return 7;
        } else {
            return 4;
        }
    }

    int I_HALT() {
        currentRunState = RunState.STATE_STOPPED_NORMAL;
        return 4;
    }

    int I_RLC_R() {
        int tmp = lastOpcode & 7;
        int tmp1 = getreg(tmp) & 0xFF;

        int tmp2 = (tmp1 >>> 7) & 1;
        tmp1 = (((tmp1 << 1) & 0xFF) | tmp2) & 0xFF;
        putreg(tmp, tmp1);

        flags = TABLE_SZ[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;

        if (tmp == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_RRC_R() {
        int tmp = lastOpcode & 7;
        int tmp1 = getreg(tmp) & 0xFF;

        int tmp2 = tmp1 & 1;
        tmp1 = (((tmp1 >>> 1) & 0x7F) | (tmp2 << 7)) & 0xFF;
        putreg(tmp, tmp1);

        flags = TABLE_SZ[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;

        if (tmp == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_RL_R() {
        int tmp = lastOpcode & 7;
        int tmp1 = getreg(tmp) & 0xFF;

        int tmp2 = (tmp1 >>> 7) & 1;
        tmp1 = ((((tmp1 << 1) & 0xFF) | flags & FLAG_C) & 0xFF);
        putreg(tmp, tmp1);

        flags = TABLE_SZ[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;

        if (tmp == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_RR_R() {
        int tmp = lastOpcode & 7;
        int tmp1 = getreg(tmp) & 0xFF;

        int tmp2 = tmp1 & 1;
        tmp1 = ((((tmp1 >> 1) & 0x7F) | (flags & FLAG_C) << 7) & 0xFF);
        putreg(tmp, tmp1);

        flags = TABLE_SZ[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;

        if (tmp == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_SLA_R() {
        int tmp = lastOpcode & 7;
        int tmp1 = getreg(tmp) & 0xFF;

        int tmp2 = (tmp1 >>> 7) & 1;
        tmp1 = (tmp1 << 1) & 0xFE;
        putreg(tmp, tmp1);

        flags = TABLE_SZ[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;

        if (tmp == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_SRA_R() {
        int tmp = lastOpcode & 7;
        int tmp1 = getreg(tmp) & 0xFF;

        int tmp2 = tmp1 & 1;
        tmp1 = (tmp1 >> 1) & 0xFF | (tmp1 & 0x80);
        putreg(tmp, tmp1);

        flags = TABLE_SZ[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;

        if (tmp == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_SLL_R() {
        int tmp = lastOpcode & 7;
        int tmp1 = getreg(tmp) & 0xFF;

        int tmp2 = (tmp1 >>> 7) & 1;
        tmp1 = (tmp1 << 1) & 0xFF | tmp1 & 1;
        putreg(tmp, tmp1);

        flags = TABLE_SZ[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;

        if (tmp == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_SRL_R() {
        int tmp = lastOpcode & 7;
        int tmp1 = getreg(tmp) & 0xFF;

        int tmp2 = tmp1 & 1;
        tmp1 = (tmp1 >>> 1) & 0x7F;
        putreg(tmp, tmp1);

        flags = TABLE_SZ[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;

        if (tmp == 6) {
            return 15;
        } else {
            return 8;
        }
    }


    int I_BIT_N_R() {
        int tmp = (lastOpcode >>> 3) & 7;
        int tmp2 = lastOpcode & 7;
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

    int I_RES_N_R() {
        int tmp = (lastOpcode >>> 3) & 7;
        int tmp2 = lastOpcode & 7;
        int tmp1 = getreg(tmp2) & 0xFF;
        tmp1 = (tmp1 & (~(1 << tmp)));
        putreg(tmp2, tmp1);
        if (tmp2 == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_SET_N_R() {
        int tmp = (lastOpcode >>> 3) & 7;
        int tmp2 = lastOpcode & 7;
        int tmp1 = getreg(tmp2) & 0xFF;
        tmp1 = (tmp1 | (1 << tmp));
        putreg(tmp2, tmp1);
        if (tmp2 == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_ADD_IX_RP() {
        IX = I_ADD_II_RP(IX);
        return 15;
    }

    int I_ADD_IY_RP() {
        IY = I_ADD_II_RP(IY);
        return 15;
    }

    int I_ADD_II_RP(int special) {
        int tmp = special;
        int pair;
        int reg = (lastOpcode >>> 4) & 0x03;
        switch (reg) {
            case 3:
                pair = SP;
                break;
            case 2:
                pair = special;
                break;
            default:
                int index = reg * 2;
                pair = regs[index] << 8 | regs[index + 1];
        }

        carry15(tmp, pair);
        halfCarry11(tmp, pair);
        flags &= (~(FLAG_N | FLAG_S | FLAG_Z));

        tmp += pair;
        flags |= ((tmp & 0x8000) == 0x8000) ? FLAG_S : 0;
        flags |= (tmp == 0) ? FLAG_Z : 0;
        return tmp & 0xFFFF;
    }

    int I_LD_IX_NN() {
        IX = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        return 14;
    }

    int I_LD_IY_NN() {
        IY = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        return 14;
    }

    int I_LD_REF_NN_IX() {
        return I_LD_REF_NN_II(IX);
    }

    int I_LD_REF_NN_IY() {
        return I_LD_REF_NN_II(IY);
    }

    int I_LD_REF_NN_II(int special) {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        writeWord(tmp, special);
        return 16;
    }

    int I_INC_IX() {
        IX = (IX + 1) & 0xFFFF;
        return 10;
    }

    int I_INC_IY() {
        IY = (IY + 1) & 0xFFFF;
        return 10;
    }

    int I_LD_IX_REF_NN() {
        IX = I_LD_II_REF_NN();
        return 20;
    }

    int I_LD_IY_REF_NN() {
        IY = I_LD_II_REF_NN();
        return 20;
    }

    int I_LD_II_REF_NN() {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        return readWord(tmp);
    }

    int I_DEC_IX() {
        IX = (IX - 1) & 0xFFFF;
        return 10;
    }

    int I_DEC_IY() {
        IY = (IY - 1) & 0xFFFF;
        return 10;
    }

    int I_INC_REF_IX_N() {
        return I_INC_REF_II_N(IX);
    }

    int I_INC_REF_IY_N() {
        return I_INC_REF_II_N(IY);
    }

    int I_INC_REF_II_N(int special) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (special + disp) & 0xFFFF;
        int value = readByte(address);

        int sum = (value + 1) & 0x1FF;
        int sumByte = sum & 0xFF;
        flags = TABLE_SZ[sumByte] | (TABLE_HP[sum ^ 1 ^ value]) | (flags & FLAG_C);

        memory.write(address, (byte) sumByte);
        return 23;
    }

    int I_DEC_REF_IX_N() {
        return I_DEC_REF_II_N(IX);
    }

    int I_DEC_REF_IY_N() {
        return I_DEC_REF_II_N(IY);
    }

    int I_DEC_REF_II_N(int special) {
        int disp = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (special + (byte) disp) & 0xFFFF;
        int value = readByte(address);

        int sum = (value - 1) & 0x1FF;
        int sumByte = sum & 0xFF;

        flags = TABLE_SUB[sumByte] | (TABLE_HP[sum ^ 1 ^ value]) | (flags & FLAG_C);
        memory.write(address, (byte) sumByte);
        return 23;
    }

    int I_LD_REF_IX_N_N() {
        return I_LD_REF_II_N_N(IX);
    }

    int I_LD_REF_IY_N_N() {
        return I_LD_REF_II_N_N(IY);
    }

    int I_LD_REF_II_N_N(int special) {
        byte offset = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        byte number = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        memory.write((special + offset) & 0xFFFF, number);
        return 19;
    }

    int I_LD_R_REF_IX_N() {
        return I_LD_R_REF_II_N(IX);
    }

    int I_LD_R_REF_IY_N() {
        return I_LD_R_REF_II_N(IY);
    }

    int I_LD_R_REF_II_N(int special) {
        byte offset = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int tmp1 = (lastOpcode >>> 3) & 7;
        putreg(tmp1, memory.read((special + offset) & 0xFFFF));
        return 19;
    }

    int I_LD_REF_IX_N_R() {
        return I_LD_REF_II_N_R(IX);
    }

    int I_LD_REF_IY_N_R() {
        return I_LD_REF_II_N_R(IY);
    }

    int I_LD_REF_II_N_R(int special) {
        byte offset = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        memory.write((special + offset) & 0xFFFF, (byte) getreg(lastOpcode & 7));
        return 19;
    }

    int I_ADD_A_REF_IX_N() {
        return I_ADD_A_REF_II_N(IX);
    }

    int I_ADD_A_REF_IY_N() {
        return I_ADD_A_REF_II_N(IY);
    }

    int I_ADD_A_REF_II_N(int special) {
        byte offset = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int value = readByte((special + offset) & 0xFFFF);

        int oldA = regs[REG_A];
        int sum = (oldA + value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]);
        return 19;
    }

    int I_ADC_A_REF_IX_N() {
        return I_ADC_A_REF_II_N(IX);
    }

    int I_ADC_A_REF_IY_N() {
        return I_ADC_A_REF_II_N(IY);
    }

    int I_ADC_A_REF_II_N(int special) {
        byte offset = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int value = readByte((special + offset) & 0xFFFF);

        int oldA = regs[REG_A];
        int sum = (oldA + value + (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]);
        return 19;
    }

    int I_SUB_REF_IX_N() {
        return I_SUB_REF_II_N(IX);
    }

    int I_SUB_REF_IY_N() {
        return I_SUB_REF_II_N(IY);
    }

    int I_SUB_REF_II_N(int special) {
        byte offset = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int value = readByte((special + offset) & 0xFFFF);

        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;

        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA];
        return 19;
    }

    int I_SBC_A_REF_IX_N() {
        return I_SBC_A_REF_II_N(IX);
    }

    int I_SBC_A_REF_IY_N() {
        return I_SBC_A_REF_II_N(IY);
    }

    int I_SBC_A_REF_II_N(int special) {
        byte offset = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int value = readByte((special + offset) & 0xFFFF);

        int oldA = regs[REG_A];
        int sum = (oldA - value - (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;

        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA];
        return 19;
    }

    int I_AND_REF_IX_N() {
        return I_AND_REF_II_N(IX);
    }

    int I_AND_REF_IY_N() {
        return I_AND_REF_II_N(IY);
    }

    int I_AND_REF_II_N(int special) {
        byte tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        int tmp1 = memory.read((special + tmp) & 0xFFFF);
        regs[REG_A] = (regs[REG_A] & tmp1) & 0xFF;
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return 19;
    }

    int I_XOR_REF_IX_N() {
        return I_XOR_REF_II_N(IX);
    }

    int I_XOR_REF_IY_N() {
        return I_XOR_REF_II_N(IY);
    }

    int I_XOR_REF_II_N(int special) {
        byte tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        int tmp1 = memory.read((special + tmp) & 0xFFFF);
        regs[REG_A] = ((regs[REG_A] ^ tmp1) & 0xff);
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return 19;
    }

    int I_OR_REF_IX_N() {
        return I_OR_REF_II_N(IX);
    }

    int I_OR_REF_IY_N() {
        return I_OR_REF_II_N(IY);
    }

    int I_OR_REF_II_N(int special) {
        byte tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int tmp1 = memory.read((special + tmp) & 0xFFFF);
        regs[REG_A] = ((regs[REG_A] | tmp1) & 0xff);
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return 19;
    }

    int I_CP_REF_IX_N() {
        return I_CP_REF_II_N(IX);
    }

    int I_CP_REF_IY_N() {
        return I_CP_REF_II_N(IY);
    }

    int I_CP_REF_II_N(int special) {
        byte offset = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int value = readByte((special + offset) & 0xFFFF);

        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        int result = sum & 0xFF;
        flags = TABLE_SUB[result] | (TABLE_CHP[sum ^ value ^ oldA]);
        return 19;
    }

    int I_POP_IX() {
        IX = readWord(SP);
        SP = (SP + 2) & 0xFFFF;
        return 14;
    }

    int I_POP_IY() {
        IY = readWord(SP);
        SP = (SP + 2) & 0xFFFF;
        return 14;
    }

    int I_EX_REF_SP_IX() {
        int tmp = readWord(SP);
        int tmp1 = IX;
        IX = tmp;
        writeWord(SP, tmp1);
        return 23;
    }

    int I_EX_REF_SP_IY() {
        int tmp = readWord(SP);
        int tmp1 = IY;
        IY = tmp;
        writeWord(SP, tmp1);
        return 23;
    }

    int I_PUSH_IX() {
        SP = (SP - 2) & 0xFFFF;
        writeWord(SP, IX);
        return 15;
    }

    int I_PUSH_IY() {
        SP = (SP - 2) & 0xFFFF;
        writeWord(SP, IY);
        return 15;
    }

    int I_JP_REF_IX() {
        PC = IX;
        return 8;
    }

    int I_JP_REF_IY() {
        PC = IY;
        return 8;
    }

    int I_LD_SP_IX() {
        SP = IX;
        return 10;
    }

    int I_LD_SP_IY() {
        SP = IY;
        return 10;
    }

    int I_RLC_REF_IX_N_R(byte operand) {
        return I_RLC_REF_II_N_R(operand, IX);
    }

    int I_RLC_REF_IY_N_R(byte operand) {
        return I_RLC_REF_II_N_R(operand, IY);
    }

    int I_RLC_REF_II_N_R(byte operand, int special) {
        int address = (special + operand) & 0xFFFF;
        int value = readByte(address);
        int bit7 = (value >>> 7) & 1;

        value = (((value << 1) | bit7) & 0xFF);
        memory.write(address, (byte) value);
        flags = TABLE_SZ[value] | EmulatorTables.PARITY_TABLE[value] | bit7;

        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = value & 0xFF;
        return 23;
    }

    int I_RRC_REF_IX_N_R(byte operand) {
        return I_RRC_REF_II_N_R(operand, IX);
    }

    int I_RRC_REF_IY_N_R(byte operand) {
        return I_RRC_REF_II_N_R(operand, IY);
    }

    int I_RRC_REF_II_N_R(byte operand, int special) {
        int tmp = (special + operand) & 0xffff;
        int tmp1 = memory.read(tmp);

        int tmp2 = tmp1 & 1;
        tmp1 = (((tmp1 >>> 1) & 0x7F) | (tmp2 << 7)) & 0xFF;

        memory.write(tmp, (byte) (tmp1 & 0xFF));
        flags = TABLE_SZ[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;

        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = tmp1 & 0xFF;
        return 23;
    }

    int I_RL_REF_IX_N_R(byte operand) {
        return I_RL_REF_II_N_R(operand, IX);
    }

    int I_RL_REF_IY_N_R(byte operand) {
        return I_RL_REF_II_N_R(operand, IY);
    }

    int I_RL_REF_II_N_R(byte operand, int special) {
        int tmp = (special + operand) & 0xffff;
        int tmp1 = memory.read(tmp);

        int tmp2 = (tmp1 >>> 7) & 1;
        tmp1 = ((((tmp1 << 1) & 0xFF) | flags & FLAG_C) & 0xFF);
        memory.write(tmp, (byte) (tmp1 & 0xFF));

        flags = TABLE_SZ[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;
        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = tmp1 & 0xFF;
        return 23;
    }

    int I_RR_REF_IX_N_R(byte operand) {
        return I_RR_REF_II_N_R(operand, IX);
    }

    int I_RR_REF_IY_N_R(byte operand) {
        return I_RR_REF_II_N_R(operand, IY);
    }

    int I_RR_REF_II_N_R(byte operand, int special) {
        int address = (special + operand) & 0xffff;
        int value = readByte(address);

        int bit0 = value & 1;
        value = ((((value >> 1) & 0xFF) | (flags & FLAG_C) << 7) & 0xFF);
        memory.write(address, (byte) (value & 0xFF));

        flags = TABLE_SZ[value] | EmulatorTables.PARITY_TABLE[value] | bit0;
        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = value & 0xFF;
        return 23;
    }

    int I_SLA_REF_IX_N_R(byte operand) {
        return I_SLA_REF_II_N_R(operand, IX);
    }

    int I_SLA_REF_IY_N_R(byte operand) {
        return I_SLA_REF_II_N_R(operand, IY);
    }

    int I_SLA_REF_II_N_R(byte operand, int special) {
        int tmp = (special + operand) & 0xFFFF;
        int tmp1 = memory.read(tmp);

        int tmp2 = (tmp1 >>> 7) & 1;
        tmp1 = (tmp1 << 1) & 0xFE;
        memory.write(tmp, (byte) tmp1);

        flags = TABLE_SZ[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;
        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = tmp1 & 0xFF;
        return 23;
    }

    int I_SRA_REF_IX_N_R(byte operand) {
        return I_SRA_REF_II_N_R(operand, IX);
    }

    int I_SRA_REF_IY_N_R(byte operand) {
        return I_SRA_REF_II_N_R(operand, IY);
    }

    int I_SRA_REF_II_N_R(byte operand, int special) {
        int tmp = (special + operand) & 0xffff;
        int tmp1 = memory.read(tmp);

        int tmp2 = tmp1 & 1;
        tmp1 = (tmp1 >> 1) & 0xFF | (tmp1 & 0x80);
        memory.write(tmp, (byte) tmp1);

        flags = TABLE_SZ[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;
        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = tmp1 & 0xFF;
        return 23;
    }

    int I_SLL_REF_IX_N_R(byte operand) {
        return I_SLL_REF_II_N_R(operand, IX);
    }

    int I_SLL_REF_IY_N_R(byte operand) {
        return I_SLL_REF_II_N_R(operand, IY);
    }

    int I_SLL_REF_II_N_R(byte operand, int special) {
        int tmp = (special + operand) & 0xffff;
        int tmp1 = memory.read(tmp);

        int tmp2 = (tmp1 >>> 7) & 1;
        tmp1 = (tmp1 << 1) & 0xFF | tmp1 & 1;
        memory.write(tmp, (byte) tmp1);

        flags = TABLE_SZ[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;
        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = tmp1 & 0xFF;
        return 23;
    }

    int I_SRL_REF_IX_N_R(byte operand) {
        return I_SRL_REF_II_N_R(operand, IX);
    }

    int I_SRL_REF_IY_N_R(byte operand) {
        return I_SRL_REF_II_N_R(operand, IY);
    }

    int I_SRL_REF_II_N_R(byte operand, int special) {
        int tmp = (special + operand) & 0xffff;
        int tmp1 = memory.read(tmp);

        int tmp2 = tmp1 & 1;
        tmp1 = (tmp1 >>> 1) & 0x7F;
        memory.write(tmp, (byte) tmp1);

        flags = TABLE_SZ[tmp1] | EmulatorTables.PARITY_TABLE[tmp1] | tmp2;
        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = tmp1 & 0xFF;
        return 23;
    }

    int I_BIT_N_REF_IX_N(byte operand) {
        return I_BIT_N_REF_II_N(operand, IX);
    }

    int I_BIT_N_REF_IY_N(byte operand) {
        return I_BIT_N_REF_II_N(operand, IY);
    }

    int I_BIT_N_REF_II_N(byte operand, int special) {
        int bitNumber = (lastOpcode >>> 3) & 7;
        int tmp1 = memory.read((special + operand) & 0xFFFF);
        flags = ((flags & FLAG_C) | FLAG_H | (((tmp1 & (1 << bitNumber)) == 0) ? (FLAG_Z | FLAG_PV) : 0));
        if (bitNumber == 7) {
            flags |= (((tmp1 & (1 << 7)) == 0x80) ? FLAG_S : 0);
        }
        return 20;
    }

    int I_RES_N_REF_IX_N_R(byte operand) {
        return I_RES_N_REF_II_N_R(operand, IX);
    }

    int I_RES_N_REF_IY_N_R(byte operand) {
        return I_RES_N_REF_II_N_R(operand, IY);
    }

    int I_RES_N_REF_II_N_R(byte operand, int special) {
        int tmp2 = (lastOpcode >>> 3) & 7;
        int tmp3 = (special + operand) & 0xffff;
        int tmp1 = memory.read(tmp3);
        tmp1 = (tmp1 & (~(1 << tmp2)));
        memory.write(tmp3, (byte) (tmp1 & 0xff));
        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = tmp1 & 0xFF;
        return 23;
    }

    int I_SET_N_REF_IX_N_R(byte operand) {
        return I_SET_N_REF_II_N_R(operand, IX);
    }

    int I_SET_N_REF_IY_N_R(byte operand) {
        return I_SET_N_REF_II_N_R(operand, IY);
    }

    int I_SET_N_REF_II_N_R(byte operand, int special) {
        int tmp2 = (lastOpcode >>> 3) & 7;
        int tmp3 = (special + operand) & 0xffff;
        int tmp1 = memory.read(tmp3);
        tmp1 = (tmp1 | (1 << tmp2));
        memory.write(tmp3, (byte) (tmp1 & 0xff));

        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = tmp1 & 0xFF;
        return 23;
    }

    int I_INC_IXH() {
        IX = ((I_INC(IX >>> 8) << 8) | (IX & 0xFF)) & 0xFFFF;
        return 8;
    }

    int I_INC_IYH() {
        IY = ((I_INC(IY >>> 8) << 8) | (IY & 0xFF)) & 0xFFFF;
        return 8;
    }

    int I_INC_IXL() {
        IX = ((IX & 0xFF00) | I_INC(IX & 0xFF)) & 0xFFFF;
        return 8;
    }

    int I_INC_IYL() {
        IY = ((IY & 0xFF00) | I_INC(IY & 0xFF)) & 0xFFFF;
        return 8;
    }

    int I_INC(int value) {
        int sum = (value + 1) & 0x1FF;
        int sumByte = sum & 0xFF;
        flags = TABLE_SZ[sumByte] | (TABLE_HP[sum ^ 1 ^ value]) | (flags & FLAG_C);
        return sumByte;
    }

    int I_DEC_IXH() {
        IX = ((I_DEC_IIH(IX) << 8) | (IX & 0xFF)) & 0xFFFF;
        return 8;
    }

    int I_DEC_IYH() {
        IY = ((I_DEC_IIH(IY) << 8) | (IY & 0xFF)) & 0xFFFF;
        return 8;
    }

    int I_DEC_IIH(int special) {
        int reg = special >>> 8;
        int sum = (reg - 1) & 0x1FF;
        int sumByte = sum & 0xFF;
        flags = TABLE_SUB[sumByte] | (TABLE_HP[sum ^ 1 ^ reg]) | (flags & FLAG_C);
        return sumByte;
    }

    int I_DEC_IXL() {
        IX = ((IX & 0xFF00) | (I_DEC_IIL(IX) & 0xFF)) & 0xFFFF;
        return 8;
    }

    int I_DEC_IYL() {
        IY = ((IY & 0xFF00) | (I_DEC_IIL(IY) & 0xFF)) & 0xFFFF;
        return 8;
    }

    int I_DEC_IIL(int special) {
        int reg = special & 0xFF;
        int sum = (reg - 1) & 0x1FF;
        int sumByte = sum & 0xFF;
        flags = TABLE_SUB[sumByte] | (TABLE_HP[sum ^ 1 ^ reg]) | (flags & FLAG_C);
        return sumByte;
    }

    int I_LD_IXH_N() {
        IX = ((readByte(PC) << 8) | (IX & 0xFF)) & 0xFFFF;
        PC = (PC + 1) & 0xFFFF;
        return 11;
    }

    int I_LD_IYH_N() {
        IY = ((readByte(PC) << 8) | (IY & 0xFF)) & 0xFFFF;
        PC = (PC + 1) & 0xFFFF;
        return 11;
    }

    int I_LD_IXL_N() {
        IX = ((IX & 0xFF00) | (readByte(PC) & 0xFF)) & 0xFFFF;
        PC = (PC + 1) & 0xFFFF;
        return 11;
    }

    int I_LD_IYL_N() {
        IY = ((IY & 0xFF00) | (readByte(PC) & 0xFF)) & 0xFFFF;
        PC = (PC + 1) & 0xFFFF;
        return 11;
    }

    int I_LD_R_IXH() {
        return I_LD_R_IIH(IX);
    }

    int I_LD_R_IYH() {
        return I_LD_R_IIH(IY);
    }

    int I_LD_R_IIH(int special) {
        int reg = ((lastOpcode >>> 2) & 0x0F) / 2; // 4,5,6 not supported
        regs[reg] = special >>> 8;
        return 8;
    }

    int I_LD_R_IXL() {
        return I_LD_R_IIL(IX);
    }

    int I_LD_R_IYL() {
        return I_LD_R_IIL(IY);
    }

    int I_LD_R_IIL(int special) {
        int reg = ((lastOpcode >>> 2) & 0x0F) / 2; // 4,5,6 not supported
        regs[reg] = special & 0xFF;
        return 8;
    }

    int I_LD_IXH_R() {
        int reg = lastOpcode & 7; // 4,5,6 not supported
        IX = (IX & 0xFF) | ((regs[reg] << 8) & 0xFF00);
        return 8;
    }

    int I_LD_IYH_R() {
        int reg = lastOpcode & 7; // 4,5,6 not supported
        IY = (IY & 0xFF) | ((regs[reg] << 8) & 0xFF00);
        return 8;
    }

    int I_LD_IIH_IIH() {
        return 8;
    }

    int I_LD_IXH_IXL() {
        IX = (IX & 0xFF) | ((IX << 8) & 0xFF00);
        return 8;
    }

    int I_LD_IYH_IYL() {
        IY = (IY & 0xFF) | ((IY << 8) & 0xFF00);
        return 8;
    }

    int I_LD_IXL_R() {
        int reg = lastOpcode & 7; // 4,5,6 not supported
        IX = (IX & 0xFF00) | (regs[reg] & 0xFF);
        return 8;
    }

    int I_LD_IYL_R() {
        int reg = lastOpcode & 7; // 4,5,6 not supported
        IY = (IY & 0xFF00) | (regs[reg] & 0xFF);
        return 8;
    }

    int I_LD_IXL_IXH() {
        IX = (IX & 0xFF00) | (IX >>> 8);
        return 8;
    }

    int I_LD_IYL_IYH() {
        IY = (IY & 0xFF00) | (IY >>> 8);
        return 8;
    }

    int I_LD_IIL_IIL() {
        return 8;
    }

    int I_ADD_A_IXH() {
        return I_ADD_A(IX >>> 8);
    }

    int I_ADD_A_IYH() {
        return I_ADD_A(IY >>> 8);
    }

    int I_ADD_A_IXL() {
        return I_ADD_A(IX & 0xFF);
    }

    int I_ADD_A_IYL() {
        return I_ADD_A(IY & 0xFF);
    }

    int I_ADD_A(int value) {
        int oldA = regs[REG_A];
        int sum = (oldA + value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]);
        return 8;
    }

    int I_ADC_A_IXH() {
        return I_ADC_A(IX >>> 8);
    }

    int I_ADC_A_IYH() {
        return I_ADC_A(IY >>> 8);
    }

    int I_ADC_A_IXL() {
        return I_ADC_A(IX & 0xFF);
    }

    int I_ADC_A_IYL() {
        return I_ADC_A(IY & 0xFF);
    }

    int I_ADC_A(int value) {
        int oldA = regs[REG_A];
        int sum = (oldA + value + (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]);
        return 8;
    }

    int I_SUB_IXH() {
        return I_SUB(IX >>> 8);
    }

    int I_SUB_IYH() {
        return I_SUB(IY >>> 8);
    }

    int I_SUB_IXL() {
        return I_SUB(IX & 0xFF);
    }

    int I_SUB_IYL() {
        return I_SUB(IY & 0xFF);
    }

    int I_SUB(int value) {
        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;

        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA];
        return 8;
    }

    int I_SBC_A_IXH() {
        return I_SBC_A(IX >>> 8);
    }

    int I_SBC_A_IYH() {
        return I_SBC_A(IY >>> 8);
    }

    int I_SBC_A_IXL() {
        return I_SBC_A(IX & 0xFF);
    }

    int I_SBC_A_IYL() {
        return I_SBC_A(IY & 0xFF);
    }

    int I_SBC_A(int value) {
        int oldA = regs[REG_A];
        int sum = (oldA - value - (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA];
        return 8;
    }

    int I_AND_IXH() {
        return I_AND(IX >>> 8);
    }

    int I_AND_IYH() {
        return I_AND(IY >>> 8);
    }

    int I_AND_IXL() {
        return I_AND(IX & 0xFF);
    }

    int I_AND_IYL() {
        return I_AND(IY & 0xFF);
    }

    int I_AND(int value) {
        regs[REG_A] = (regs[REG_A] & value) & 0xFF;
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return 8;
    }

    int I_XOR_IXH() {
        return I_XOR(IX >>> 8);
    }

    int I_XOR_IYH() {
        return I_XOR(IY >>> 8);
    }

    int I_XOR_IXL() {
        return I_XOR(IX & 0xFF);
    }

    int I_XOR_IYL() {
        return I_XOR(IY & 0xFF);
    }

    int I_XOR(int value) {
        regs[REG_A] = ((regs[REG_A] ^ value) & 0xFF);
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return 8;
    }

    int I_OR_IXH() {
        return I_OR(IX >>> 8);
    }

    int I_OR_IXL() {
        return I_OR(IX & 0xFF);
    }

    int I_OR_IYH() {
        return I_OR(IY >>> 8);
    }

    int I_OR_IYL() {
        return I_OR(IY & 0xFF);
    }

    int I_OR(int value) {
        regs[REG_A] = (regs[REG_A] | value) & 0xFF;
        flags = EmulatorTables.AND_OR_XOR_TABLE[regs[REG_A]];
        return 8;
    }

    int I_CP_IXH() {
        return I_CP(IX >>> 8);
    }

    int I_CP_IXL() {
        return I_CP(IX & 0xFF);
    }

    int I_CP_IYH() {
        return I_CP(IY >>> 8);
    }

    int I_CP_IYL() {
        return I_CP(IY & 0xFF);
    }

    int I_CP(int value) {
        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        int result = sum & 0xFF;
        flags = TABLE_SUB[result] | (TABLE_CHP[sum ^ value ^ oldA]);
        return 8;
    }
}
