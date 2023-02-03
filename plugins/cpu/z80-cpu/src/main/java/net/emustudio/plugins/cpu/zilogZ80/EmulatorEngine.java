/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
import net.emustudio.emulib.plugins.cpu.TimedEventsProcessor;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.helpers.SleepUtils;
import net.emustudio.plugins.cpu.intel8080.api.CpuEngine;
import net.emustudio.plugins.cpu.intel8080.api.DispatchListener;
import net.emustudio.plugins.cpu.intel8080.api.FrequencyChangedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static net.emustudio.plugins.cpu.zilogZ80.DispatchTables.*;
import static net.emustudio.plugins.cpu.zilogZ80.EmulatorTables.*;

/**
 * Main implementation class for CPU emulation CPU works in a separate thread
 * (parallel with other hardware)
 */
// TODO: set frequency runtime
public class EmulatorEngine implements CpuEngine {
    public static final int REG_A = 7, REG_B = 0, REG_C = 1, REG_D = 2, REG_E = 3, REG_H = 4, REG_L = 5;
    public static final int FLAG_S = 0x80, FLAG_Z = 0x40, FLAG_Y = 0x20, FLAG_H = 0x10, FLAG_X = 0x8, FLAG_PV = 0x4, FLAG_N = 0x02, FLAG_C = 0x1;
    public static final int FLAG_SZP = FLAG_S | FLAG_Z | FLAG_PV;
    private final static Logger LOGGER = LoggerFactory.getLogger(EmulatorEngine.class);
    private final static int[] CONDITION = new int[]{
            FLAG_Z, FLAG_Z, FLAG_C, FLAG_C, FLAG_PV, FLAG_PV, FLAG_S, FLAG_S
    };
    private final static int[] CONDITION_VALUES = new int[]{
            0, FLAG_Z, 0, FLAG_C, 0, FLAG_PV, 0, FLAG_S
    };

    private final ContextZ80Impl context;
    private final TimedEventsProcessor tep;
    private final MemoryContext<Byte> memory;
    private final List<FrequencyChangedListener> frequencyChangedListeners = new CopyOnWriteArrayList<>();
    private final AtomicLong executedCycles = new AtomicLong(0);

    public final int[] regs = new int[8];
    public final int[] regs2 = new int[8];
    public final boolean[] IFF = new boolean[2]; // interrupt enable flip-flops

    public int flags = 2;
    public int flags2 = 2;

    // special registers
    public int PC = 0, SP = 0, IX = 0, IY = 0;
    public int I = 0, R = 0; // interrupt r., refresh r.
    public int memptr = 0; // internal register, https://gist.github.com/drhelius/8497817
    public int Q = 0; // internal register
    public int lastQ = 0; // internal register

    private final Queue<byte[]> pendingInterrupts = new ConcurrentLinkedQueue<>(); // must be thread-safe; can cause stack overflow
    // non-maskable interrupts are always executed
    private final AtomicBoolean pendingNonMaskableInterrupt = new AtomicBoolean();

    public byte interruptMode = 0;
    private boolean interruptSkip; // when EI enabled, skip next instruction interrupt

    private int lastOpcode;
    private RunState currentRunState = RunState.STATE_STOPPED_NORMAL;

    private volatile DispatchListener dispatchListener;

    public EmulatorEngine(MemoryContext<Byte> memory, ContextZ80Impl context) {
        this.memory = Objects.requireNonNull(memory);
        this.context = Objects.requireNonNull(context);
        this.tep = context.getTimedEventsProcessorNow();
        LOGGER.info("Sleep precision: " + SleepUtils.SLEEP_PRECISION + " nanoseconds.");
    }

    @SuppressWarnings("unused")
    public static String intToFlags(int flags) {
        String flagsString = "";
        if ((flags & FLAG_S) == FLAG_S) {
            flagsString += "S";
        }
        if ((flags & FLAG_Z) == FLAG_Z) {
            flagsString += "Z";
        }
        if ((flags & FLAG_Y) == FLAG_Y) {
            flagsString += "Y";
        }
        if ((flags & FLAG_H) == FLAG_H) {
            flagsString += "H";
        }
        if ((flags & FLAG_X) == FLAG_X) {
            flagsString += "X";
        }
        if ((flags & FLAG_PV) == FLAG_PV) {
            flagsString += "P";
        }
        if ((flags & FLAG_N) == FLAG_N) {
            flagsString += "N";
        }
        if ((flags & FLAG_C) == FLAG_C) {
            flagsString += "C";
        }
        return flagsString;
    }

    @Override
    public void setDispatchListener(DispatchListener dispatchListener) {
        this.dispatchListener = dispatchListener;
    }

    @Override
    public long getAndResetExecutedCycles() {
        return executedCycles.getAndSet(0);
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

    public void requestMaskableInterrupt(byte[] data) {
        if (currentRunState == RunState.STATE_RUNNING) {
            pendingInterrupts.add(data);
        }
    }

    public void requestNonMaskableInterrupt() {
        pendingNonMaskableInterrupt.set(true);
    }

    void reset(int startPos) {
        IX = IY = 0;
        SP = 0xFFFF;
        flags = 0xFF;
        I = R = 0;
        memptr = 0;
        Q = lastQ = 0;
        Arrays.fill(regs, 0);
        Arrays.fill(regs2, 0);
        flags = 0;
        flags2 = 0;
        interruptMode = 0;
        IFF[0] = false;
        IFF[1] = false;
        pendingNonMaskableInterrupt.set(false);
        PC = startPos;
        pendingInterrupts.clear();
        currentRunState = RunState.STATE_STOPPED_BREAK;
    }

    CPU.RunState step() throws Exception {
        currentRunState = CPU.RunState.STATE_STOPPED_BREAK;
        try {
            dispatch();
        } catch (Throwable e) {
            throw new Exception(e);
        }
        return currentRunState;
    }

    public CPU.RunState run(CPU cpu) {
        long startTime, endTime;
        int cycles_executed;
        // In Z80, 1 t-state = 250 ns = 0.25 microseconds = 0.00025 milliseconds
        // in 10 milliseconds = 10 / 0.00025 = 40000 t-states are executed uncontrollably
        // in 1 millisecond = 1 / 0.00025 = 4000 t-states :(

        int checkTimeSlice = (int) Math.ceil(SleepUtils.SLEEP_PRECISION / 1000000.0); // milliseconds
        int cycles_to_execute = checkTimeSlice * context.getCPUFrequency();
        int cycles;
        long slice = checkTimeSlice * 1000000L; // nanoseconds

        currentRunState = CPU.RunState.STATE_RUNNING;
        while (!Thread.currentThread().isInterrupted() && (currentRunState == CPU.RunState.STATE_RUNNING)) {
            startTime = System.nanoTime();
            cycles_executed = 0;
            while ((cycles_executed < cycles_to_execute) && !Thread.currentThread().isInterrupted() && (currentRunState == CPU.RunState.STATE_RUNNING)) {
                try {
                    cycles = dispatch();
                    cycles_executed += cycles;
                    executedCycles.addAndGet(cycles);
                    tep.advanceClock(cycles);
                    if (cpu.isBreakpointSet(PC)) {
                        throw new Breakpoint();
                    }
                } catch (Breakpoint e) {
                    return CPU.RunState.STATE_STOPPED_BREAK;
                } catch (IndexOutOfBoundsException e) {
                    LOGGER.error("Unexpected error", e);
                    return CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
                } catch (Throwable e) {
                    LOGGER.error("Unexpected error", e);
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
            lastQ = Q;
            Q = 0;
            if (pendingNonMaskableInterrupt.getAndSet(false)) {
                writeWord((SP - 2) & 0xFFFF, PC);
                SP = (SP - 2) & 0xffff;
                PC = 0x66;
                return 12;
            }
            if (interruptSkip) {
                interruptSkip = false; // See EI
            } else if (IFF[0] && !pendingInterrupts.isEmpty()) {
                return doInterrupt();
            }
            return DISPATCH(DISPATCH_TABLE);
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
        lastOpcode = memory.read(PC) & 0xFF;
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

        lastOpcode = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        incrementR();

        MethodHandle instr = table[lastOpcode];
        if (instr != null) {
            return (int) instr.invokeExact(this, operand);
        }
        return 4;
    }

    private int doInterrupt() throws Throwable {
        byte[] dataBus = pendingInterrupts.poll();
        int cycles = 0;

        IFF[0] = IFF[1] = false;
        switch (interruptMode) {
            case 0:
                cycles += 11;
                RunState old_runstate = currentRunState;
                if (dataBus != null && dataBus.length > 0) {
                    lastOpcode = dataBus[0] & 0xFF; // TODO: if dataBus had more bytes, they're ignored (except call).
                    if (lastOpcode == 0xCD) {  /* CALL */
                        SP = (SP - 2) & 0xFFFF;
                        writeWord(SP, PC);
                        PC = ((dataBus[2] & 0xFF) << 8) | (dataBus[1] & 0xFF);
                        memptr = PC;
                        return cycles + 17;
                    }

                    dispatch(); // must ignore halt
                    if (currentRunState == RunState.STATE_STOPPED_NORMAL) {
                        currentRunState = old_runstate;
                    }
                }
                break;
            case 1:
                cycles += 12;
                writeWord((SP - 2) & 0xFFFF, PC);
                SP = (SP - 2) & 0xffff;
                PC = 0x38;
                memptr = PC;
                break;
            case 2:
                cycles += 13;
                if (dataBus != null && dataBus.length > 0) {
                    SP = (SP - 2) & 0xFFFF;
                    if (memory.read(PC) == 0x76) {
                        // jump over HALT
                        writeWord(SP, (PC + 1) & 0xFFFF);
                    } else {
                        writeWord(SP, PC);
                    }
                    PC = readWord(((I << 8) | (dataBus[0] & 0xFF)) & 0xFFFF);
                    memptr = PC;
                }
                break;
        }
        return cycles;
    }

    private int getreg(int reg) {
        if (reg == 6) {
            return memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF;
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

    private int readWord(int address) {
        Byte[] read = memory.read(address, 2);
        return ((read[1] << 8) | (read[0] & 0xFF)) & 0xFFFF;
    }

    private void writeWord(int address, int value) {
        memory.write(address, new Byte[]{(byte) (value & 0xFF), (byte) ((value >>> 8) & 0xFF)}, 2);
    }

    private void incrementR() {
        R = (R & 0x80) | (((R & 0x7F) + 1) & 0x7F);
    }

    int I_NOP() {
        return 4;
    }

    int I_LD_REF_BC_A() {
        memory.write(getpair(0, false), (byte) regs[REG_A]);
        memptr = (regs[REG_A] << 8) | 1;
        //	Note for *BM1: MEMPTR_low = (rp + 1) & #FF,  MEMPTR_hi = 0
        return 7;
    }

    int I_INC_RP() {
        int tmp = (lastOpcode >>> 4) & 0x03;
        putpair(tmp, (getpair(tmp, true) + 1) & 0xFFFF, true);
        return 6;
    }

    int I_ADD_HL_RP() {
        int rp = getpair((lastOpcode >>> 4) & 0x03, true);
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        memptr = (hl + 1) & 0xFFFF;

        int res = hl + rp;
        flags = (flags & FLAG_SZP) |
                (((hl ^ res ^ rp) >>> 8) & FLAG_H) |
                ((res >>> 16) & FLAG_C) | TABLE_XY[(res >>> 8) & 0xFF];
        Q = flags;

        regs[REG_H] = (res >>> 8) & 0xFF;
        regs[REG_L] = res & 0xFF;

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
        flags = TABLE_SZ[sumByte] | (TABLE_HP[sum ^ 1 ^ regValue]) | (flags & FLAG_C) | TABLE_XY[sumByte];
        Q = flags;
        putreg(reg, sumByte);
        return (reg == 6) ? 11 : 4;
    }

    int I_DEC_R() {
        int reg = (lastOpcode >>> 3) & 0x07;
        int regValue = getreg(reg);
        int sum = (regValue - 1) & 0x1FF;
        int sumByte = sum & 0xFF;
        flags = TABLE_SUB[sumByte] | (TABLE_HP[sum ^ 1 ^ regValue]) | (flags & FLAG_C) | TABLE_XY[sumByte];
        Q = flags;
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
        memptr = PC;
        return 11;
    }

    int I_ADD_A_R() {
        int value = getreg(lastOpcode & 0x07);
        int oldA = regs[REG_A];
        int sum = (oldA + value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
        Q = flags;
        return (lastOpcode == 0x86) ? 7 : 4;
    }

    int I_ADD_A_N() {
        int value = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        int oldA = regs[REG_A];
        int sum = (oldA + value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 7;
    }

    int I_ADC_A_R() {
        int value = getreg(lastOpcode & 0x07);
        int oldA = regs[REG_A];
        int sum = (oldA + value + (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
        Q = flags;
        return (lastOpcode == 0x8E) ? 7 : 4;
    }

    int I_ADC_A_N() {
        int value = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        int oldA = regs[REG_A];
        int sum = (oldA + value + (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 7;
    }

    int I_SUB_R() {
        int value = getreg(lastOpcode & 0x07);

        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;

        flags = TABLE_SUB[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
        Q = flags;
        return (lastOpcode == 0x96) ? 7 : 4;
    }

    int I_SBC_A_R() {
        int value = getreg(lastOpcode & 0x07);

        int oldA = regs[REG_A];
        int sum = (oldA - value - (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;

        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA] | TABLE_XY[regs[REG_A]];
        Q = flags;
        return (lastOpcode == 0x9E) ? 7 : 4;
    }

    int I_SBC_A_N() {
        int value = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;

        int oldA = regs[REG_A];
        int sum = (oldA - value - (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;

        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA] | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 7;
    }

    int I_AND_R() {
        regs[REG_A] = (regs[REG_A] & getreg(lastOpcode & 7)) & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | FLAG_H | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        Q = flags;
        return (lastOpcode == 0xA6) ? 7 : 4;
    }

    int I_AND_N() {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        regs[REG_A] = (regs[REG_A] & tmp) & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | FLAG_H | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 7;
    }

    int I_XOR_R() {
        regs[REG_A] = ((regs[REG_A] ^ getreg(lastOpcode & 7)) & 0xff);
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        Q = flags;
        return (lastOpcode == 0xAE) ? 7 : 4;
    }

    int I_XOR_N() {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        regs[REG_A] = ((regs[REG_A] ^ tmp) & 0xFF);
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 7;
    }

    int I_OR_R() {
        regs[REG_A] = (regs[REG_A] | getreg(lastOpcode & 7)) & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        Q = flags;
        return (lastOpcode == 0xB6) ? 7 : 4;
    }

    int I_OR_N() {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        regs[REG_A] = (regs[REG_A] | tmp) & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 7;
    }

    int I_CP_R() {
        int value = getreg(lastOpcode & 7);
        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        int result = sum & 0xFF;
        // F5 and F3 flags are set from the subtrahend instead of from the result.
        flags = TABLE_SUB[result] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[value];
        Q = flags;
        return (lastOpcode == 0xBE) ? 7 : 4;
    }

    int I_CP_N() {
        int value = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;

        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        int result = sum & 0xFF;
        flags = (TABLE_SUB[result] | (TABLE_CHP[sum ^ value ^ oldA])) | TABLE_XY[value];
        Q = flags;
        return 7;
    }

    int I_RLCA() {
        regs[REG_A] = TABLE_RLCA[regs[REG_A]];
        flags = (flags & FLAG_SZP) | (regs[REG_A] & (FLAG_X | FLAG_Y | FLAG_C));
        Q = flags;
        return 4;
    }

    int I_EX_AF_AFF() {
        regs[REG_A] ^= regs2[REG_A];
        regs2[REG_A] ^= regs[REG_A];
        regs[REG_A] ^= regs2[REG_A];
        flags ^= flags2;
        flags2 ^= flags;
        flags ^= flags2;
        return 4;
    }

    int I_LD_A_REF_BC() {
        int bc = regs[REG_B] << 8 | regs[REG_C];
        regs[REG_A] = memory.read(bc) & 0xFF;
        memptr = bc;
        return 7;
    }

    int I_RRCA() {
        flags = (flags & FLAG_SZP) | (regs[REG_A] & FLAG_C);
        regs[REG_A] = TABLE_RRCA[regs[REG_A]];
        flags |= TABLE_XY[regs[REG_A]];
        Q = flags;
        return 4;
    }

    int I_DJNZ() {
        byte addr = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_B] = (regs[REG_B] - 1) & 0xFF;
        if (regs[REG_B] != 0) {
            PC = (PC + addr) & 0xFFFF;
            memptr = PC;
            return 13;
        }
        return 8;
    }

    int I_LD_REF_DE_A() {
        memory.write(getpair(1, false), (byte) regs[REG_A]);
        memptr = (regs[REG_A] << 8) | 2;
        //_memPtr = A * 256 + ((DE + 1) & 0xFF);
        //	Note for *BM1: MEMPTR_low = (rp + 1) & #FF,  MEMPTR_hi = 0
        return 7;
    }

    int I_RLA() {
        int res = ((regs[REG_A] << 1) | (flags & FLAG_C)) & 0xFF;
        int flagC = ((regs[REG_A] & 0x80) == 0x80) ? FLAG_C : 0;
        regs[REG_A] = res;
        flags = (flags & FLAG_SZP) | flagC | TABLE_XY[res];
        Q = flags;
        return 4;
    }

    int I_LD_A_REF_DE() {
        int de = regs[REG_D] << 8 | regs[REG_E];
        regs[REG_A] = memory.read(de) & 0xFF;
        memptr = de;
        return 7;
    }

    int I_RRA() {
        int res = ((regs[REG_A] >>> 1) | (flags << 7)) & 0xFF;
        int flagC = regs[REG_A] & FLAG_C;
        flags = (flags & FLAG_SZP) | flagC | TABLE_XY[res];
        Q = flags;
        regs[REG_A] = res;
        return 4;
    }

    int I_DAA() {
        // The following algorithm is from comp.sys.sinclair's FAQ.
        int c, d;

        int a = regs[REG_A];
        if (a > 0x99 || ((flags & FLAG_C) != 0)) {
            c = FLAG_C;
            d = 0x60;
        } else {
            c = d = 0;
        }

        if ((a & 0x0f) > 0x09 || ((flags & FLAG_H) != 0)) {
            d += 0x06;
        }

        regs[REG_A] = ((flags & FLAG_N) != 0 ? regs[REG_A] - d : regs[REG_A] + d) & 0xFF;
        flags = TABLE_SZ[regs[REG_A]]
                | PARITY_TABLE[regs[REG_A]]
                | TABLE_XY[regs[REG_A]]
                | ((regs[REG_A] ^ a) & FLAG_H)
                | (flags & FLAG_N)
                | c;
        Q = flags;
        return 4;
    }

    int I_CPL() {
        regs[REG_A] = (~regs[REG_A]) & 0xFF;
        flags = (flags & (FLAG_SZP | FLAG_C))
                | FLAG_H | FLAG_N
                | (regs[REG_A] & (FLAG_X | FLAG_Y));
        Q = flags;

//        flags = (flags & (FLAG_SZP | FLAG_C))
//                | FLAG_H | FLAG_N
//                | TABLE_XY[regs[REG_A]];
        return 4;
    }

    int I_SCF() {
        flags = (flags & FLAG_SZP) | (((lastQ ^ flags) | regs[REG_A]) & (FLAG_X | FLAG_Y)) | FLAG_C;
        Q = flags;

        //flags = (flags & FLAG_SZP) | TABLE_XY[regs[REG_A]] | FLAG_C;
        return 4;
    }

    int I_CCF() {
        flags = (flags & FLAG_SZP)
                | ((flags & FLAG_C) == 0 ? FLAG_C : FLAG_H)
                | (((lastQ ^ flags) | regs[REG_A]) & (FLAG_X | FLAG_Y));
        Q = flags;

//        int c = flags & FLAG_C;
        //      flags = (flags & FLAG_SZP) | (c << 4)
        //            | TABLE_XY[regs[REG_A]]
        //          | (c ^ FLAG_C);
        return 4;
    }

    int I_RET() {
        PC = readWord(SP);
        memptr = PC;
        SP = (SP + 2) & 0xFFFF;
        return 10;
    }

    int I_EXX() {
        // https://www.baeldung.com/java-swap-two-variables
        regs[REG_B] ^= regs2[REG_B];
        regs2[REG_B] ^= regs[REG_B];
        regs[REG_B] ^= regs2[REG_B];

        regs[REG_C] ^= regs2[REG_C];
        regs2[REG_C] ^= regs[REG_C];
        regs[REG_C] ^= regs2[REG_C];

        regs[REG_D] ^= regs2[REG_D];
        regs2[REG_D] ^= regs[REG_D];
        regs[REG_D] ^= regs2[REG_D];

        regs[REG_E] ^= regs2[REG_E];
        regs2[REG_E] ^= regs[REG_E];
        regs[REG_E] ^= regs2[REG_E];

        regs[REG_H] ^= regs2[REG_H];
        regs2[REG_H] ^= regs[REG_H];
        regs[REG_H] ^= regs2[REG_H];

        regs[REG_L] ^= regs2[REG_L];
        regs2[REG_L] ^= regs[REG_L];
        regs[REG_L] ^= regs2[REG_L];
        return 4;
    }

    int I_EX_REF_SP_HL() {
        byte value = memory.read(SP);
        int SP_plus1 = (SP + 1) & 0xFFFF;
        byte value1 = memory.read(SP_plus1);
        memory.write(SP, (byte) regs[REG_L]);
        memory.write(SP_plus1, (byte) regs[REG_H]);
        regs[REG_L] = value & 0xFF;
        regs[REG_H] = value1 & 0xFF;
        memptr = (regs[REG_H] << 8) | regs[REG_L];
        return 19;
    }

    int I_JP_REF_HL() {
        PC = ((regs[REG_H] << 8) | regs[REG_L]);
        return 4;
    }

    int I_EX_DE_HL() {
        regs[REG_D] ^= regs[REG_H];
        regs[REG_H] ^= regs[REG_D];
        regs[REG_D] ^= regs[REG_H];

        regs[REG_E] ^= regs[REG_L];
        regs[REG_L] ^= regs[REG_E];
        regs[REG_E] ^= regs[REG_L];
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
        // https://www.smspower.org/forums/2511-LDILDIRLDDLDDRCRCInZEXALL
        // interrupts are not allowed until after the *next* instruction after EI.
        // This is used to prevent interrupts from occurring between an EI/RETI pair used at the end of interrupt handlers.
        if (!IFF[0]) {
            interruptSkip = true;
        }
        IFF[0] = IFF[1] = true;
        return 4;
    }

    int I_IN_R_REF_C() {
        int reg = (lastOpcode >>> 3) & 0x7;
        putreg(reg, context.readIO((regs[REG_B] << 8) | regs[REG_C]));
        memptr = (((regs[REG_B] << 8) | regs[REG_C]) + 1) & 0xFFFF;
        flags = (flags & FLAG_C) | TABLE_SZ[regs[reg]] | PARITY_TABLE[regs[reg]] | TABLE_XY[regs[reg]];
        Q = flags;
        return 12;
    }

    int I_OUT_REF_C_R() {
        int reg = (lastOpcode >>> 3) & 0x7;
        memptr = (((regs[REG_B] << 8) | regs[REG_C]) + 1) & 0xFFFF;
        context.writeIO((regs[REG_B] << 8) | regs[REG_C], (byte) getreg(reg));
        return 12;
    }

    int I_SBC_HL_RP() {
        int rp = getpair((lastOpcode >>> 4) & 0x03, true);
        int hl = ((regs[REG_H] << 8) | regs[REG_L]) & 0xFFFF;
        memptr = (hl + 1) & 0xFFFF;
        int res = hl - rp - (flags & FLAG_C);

        flags = (((hl ^ res ^ rp) >>> 8) & FLAG_H) | FLAG_N |
                ((res >>> 16) & FLAG_C) |
                ((res >>> 8) & (FLAG_S | FLAG_Y | FLAG_X)) |
                (((res & 0xFFFF) != 0) ? 0 : FLAG_Z) |
                (((rp ^ hl) & (hl ^ res) & 0x8000) >>> 13);
        Q = flags;

        regs[REG_H] = (res >>> 8) & 0xFF;
        regs[REG_L] = res & 0xFF;
        return 15;
    }

    int I_ADC_HL_RP() {
        int rp = getpair((lastOpcode >>> 4) & 0x03, true);
        int hl = (regs[REG_H] << 8 | regs[REG_L]) & 0xFFFF;
        memptr = (hl + 1) & 0xFFFF;
        int res = hl + rp + (flags & FLAG_C);

        flags = (((hl ^ res ^ rp) >>> 8) & FLAG_H) |
                ((res >>> 16) & FLAG_C) |
                ((res >>> 8) & (FLAG_S | FLAG_Y | FLAG_X)) |
                (((res & 0xFFFF) != 0) ? 0 : FLAG_Z) |
                (((rp ^ hl ^ 0x8000) & (rp ^ res) & 0x8000) >>> 13);
        Q = flags;

        regs[REG_H] = (res >>> 8) & 0xFF;
        regs[REG_L] = (res & 0xFF);
        return 11;
    }

    int I_NEG() {
        int v = regs[REG_A];
        regs[REG_A] = 0;
        return I_SUB(v);
    }

    int I_RETN() {
        IFF[0] = IFF[1];
        PC = readWord(SP);
        SP = (SP + 2) & 0xffff;
        return 14;
    }

    int I_IM_0() {
        interruptMode = 0;
        return 8;
    }

    int I_LD_I_A() {
        I = regs[REG_A];
        return 9;
    }

    int I_RETI() {
        IFF[0] = IFF[1];
        PC = readWord(SP);
        memptr = PC;
        SP = (SP + 2) & 0xffff;
        return 14;
    }

    int I_LD_R_A() {
        R = regs[REG_A];
        return 9;
    }

    int I_IM_1() {
        interruptMode = 1;
        return 8;
    }

    int I_IM_2() {
        interruptMode = 2;
        return 8;
    }

    int I_LD_A_I() {
        regs[REG_A] = I & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (IFF[1] ? FLAG_PV : 0) | (flags & FLAG_C) | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 9;
    }

    int I_LD_A_R() {
        regs[REG_A] = R & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (IFF[1] ? FLAG_PV : 0) | (flags & FLAG_C) | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 9;
    }

    int I_RRD() {
        int regA = regs[REG_A] & 0x0F;
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        memptr = (hl + 1) & 0xFFFF;
        int value = memory.read(hl);
        regs[REG_A] = ((regs[REG_A] & 0xF0) | (value & 0x0F));
        value = ((value >>> 4) & 0x0F) | (regA << 4);
        memory.write(hl, (byte) (value & 0xff));
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | (flags & FLAG_C) | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 18;
    }

    int I_RLD() {
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int value = memory.read(hl);
        memptr = (hl + 1) & 0xFFFF;
        int tmp1 = (value >>> 4) & 0x0F;
        value = ((value << 4) & 0xF0) | (regs[REG_A] & 0x0F);
        regs[REG_A] = ((regs[REG_A] & 0xF0) | tmp1);
        memory.write((regs[REG_H] << 8) | regs[REG_L], (byte) (value & 0xff));
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | (flags & FLAG_C) | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 18;
    }

    int I_IN_REF_C() {
        int tmp = context.readIO((regs[REG_B] << 8) | regs[REG_C]) & 0xFF;
        flags = TABLE_SZ[tmp] | PARITY_TABLE[tmp] | (flags & FLAG_C) | TABLE_XY[tmp];
        return 12;
    }

    int I_OUT_REF_C_0() {
        context.writeIO((regs[REG_B] << 8) | regs[REG_C], (byte) 0);
        return 12;
    }

    int I_CPI() {
        int a, n, z, f, hl, bc;
        a = regs[REG_A];
        hl = (regs[REG_H] << 8) | regs[REG_L];
        bc = (((regs[REG_B] << 8) | regs[REG_C]) - 1) & 0xFFFF;

        n = memory.read(hl) & 0xFF;
        z = a - n;

        hl = (hl + 1) & 0xFFFF;

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        f = (a ^ n ^ z) & FLAG_H;

        n = z - (f >> 4);
        f |= (n << 4) & FLAG_Y;
        f |= n & FLAG_X;

        f |= TABLE_SZ[z & 0xFF];
        f |= (bc != 0) ? FLAG_PV : 0;
        flags = f | FLAG_N | (flags & FLAG_C);
        Q = flags;
        return 16;
    }

    int I_CPIR() {
        int bc = (regs[REG_B] << 8) | regs[REG_C];
        int hl = (regs[REG_H] << 8) | regs[REG_L];

        int n = memory.read(hl) & 0xFF;
        int z = regs[REG_A] - n;

        hl = (hl + 1) & 0xFFFF;
        int cycles;
        if ((--bc) != 0 && (z != 0)) {
            cycles = 21;
            PC = (PC - 2) & 0xFFFF;
        } else {
            cycles = 16;
        }

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        int f = (regs[REG_A] ^ n ^ z) & FLAG_H;
        n = z - (f >>> 4);
        f |= ((n << 4) & FLAG_Y);
        f |= (n & FLAG_X);
        f |= TABLE_SZ[z & 0xFF];
        f |= (bc != 0) ? FLAG_PV : 0;

        flags = f | FLAG_N | (flags & FLAG_C);
        Q = flags;
        return cycles;
    }

    int I_CPD() {
        int a = regs[REG_A];
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int bc = (((regs[REG_B] << 8) | regs[REG_C]) - 1) & 0xFFFF;

        int n = memory.read(hl) & 0xFF;
        hl = (hl - 1) & 0xFFFF;

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        int z = a - n;
        int f = (a ^ n ^ z) & FLAG_H;

        n = z - (f >> 4);
        f |= (n << 4) & FLAG_Y;
        f |= n & FLAG_X;

        f |= TABLE_SZ[z & 0xFF];
        f |= (bc != 0) ? FLAG_PV : 0;
        flags = f | FLAG_N | (flags & FLAG_C);
        Q = flags;
        return 16;
    }

    int I_CPDR() {
        int a = regs[REG_A];
        int bc = (regs[REG_B] << 8) | regs[REG_C];
        int hl = (regs[REG_H] << 8) | regs[REG_L];

        int n = memory.read(hl) & 0xFF;
        int z = a - n;

        hl = (hl - 1) & 0xFFFF;
        int cycles;
        if ((--bc) != 0 && (z != 0)) {
            cycles = 21;
            PC = (PC - 2) & 0xFFFF;
        } else {
            cycles = 16;
        }

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        int f = (a ^ n ^ z) & FLAG_H;
        n = z - (f >>> 4);
        f |= ((n << 4) & FLAG_Y);
        f |= (n & FLAG_X);
        f |= TABLE_SZ[z & 0xFF];
        f |= (bc != 0) ? FLAG_PV : 0;

        flags = f | FLAG_N | (flags & FLAG_C);
        Q = flags;
        return cycles;
    }

    int I_LDD() {
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int de = (regs[REG_D] << 8) | regs[REG_E];

        byte io = memory.read(hl--);
        memory.write(de--, io);

        int bc = (((regs[REG_B] << 8) | regs[REG_C]) - 1) & 0xFFFF;

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_D] = (de >>> 8) & 0xFF;
        regs[REG_E] = de & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        int result = regs[REG_A] + io;

        flags = (flags & (FLAG_S | FLAG_Z | FLAG_C)) |
                ((result << 4) & FLAG_Y) | (result & FLAG_X) | (bc != 0 ? FLAG_PV : 0);
        Q = flags;
        return 16;
    }

    int I_LDDR() {
        int bc = (regs[REG_B] << 8) | regs[REG_C];
        int de = (regs[REG_D] << 8) | regs[REG_E];
        int hl = (regs[REG_H] << 8) | regs[REG_L];

        byte value = memory.read(hl);
        memory.write(de, value);

        bc = (bc - 1) & 0xFFFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        hl = (hl - 1) & 0xFFFF;
        de = (de - 1) & 0xFFFF;
        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_D] = (de >>> 8) & 0xFF;
        regs[REG_E] = de & 0xFF;

        // https://github.com/hoglet67/Z80Decoder/wiki/Undocumented-Flags#interrupted-block-instructions
        flags = (flags & (FLAG_S | FLAG_Z | FLAG_C)) | (bc != 0 ? FLAG_PV : 0);
        if (bc != 0) {
            PC = (PC - 2) & 0xFFFF;
            memptr = (PC + 1) & 0xFFFF;
            flags |= TABLE_XY[PC >>> 8];
            Q = flags;
            return 21;
        }

        value += regs[REG_A];
        flags |= (value & FLAG_X) | ((value & 0x02) != 0 ? FLAG_Y : 0);
        Q = flags;
        return 16;
    }

    int I_LDI() {
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int de = (regs[REG_D] << 8) | regs[REG_E];

        byte io = memory.read(hl++);
        memory.write(de++, io);

        int bc = ((((regs[REG_B] << 8) | regs[REG_C]) & 0xFFFF) - 1) & 0xFFFF;

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_D] = (de >>> 8) & 0xFF;
        regs[REG_E] = de & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        int result = regs[REG_A] + (io & 0xFF);

        flags = (flags & (FLAG_S | FLAG_Z | FLAG_C)) |
                ((result << 4) & FLAG_Y) | (result & FLAG_X) | (bc != 0 ? FLAG_PV : 0);
        Q = flags;
        return 16;
    }

    int I_LDIR() {
        int bc = (regs[REG_B] << 8) | regs[REG_C];
        int de = (regs[REG_D] << 8) | regs[REG_E];
        int hl = (regs[REG_H] << 8) | regs[REG_L];

        byte value = memory.read(hl);
        memory.write(de, value);

        bc = (bc - 1) & 0xFFFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        hl = (hl + 1) & 0xFFFF;
        de = (de + 1) & 0xFFFF;
        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_D] = (de >>> 8) & 0xFF;
        regs[REG_E] = de & 0xFF;

        // https://github.com/hoglet67/Z80Decoder/wiki/Undocumented-Flags#interrupted-block-instructions
        flags = (flags & (FLAG_S | FLAG_Z | FLAG_C)) | (bc != 0 ? FLAG_PV : 0);
        if (bc != 0) {
            PC = (PC - 2) & 0xFFFF;
            memptr = (PC + 1) & 0xFFFF;
            flags |= TABLE_XY[PC >>> 8];
            Q = flags;
            return 21;
        }

        value += regs[REG_A];
        flags |= (value & FLAG_X) | ((value & 0x02) != 0 ? FLAG_Y : 0);
        Q = flags;
        return 16;
    }

    int I_INI() {
        int bc = (regs[REG_B] << 8) | regs[REG_C];
        byte value = context.readIO(bc);
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        memory.write(hl, value);

        hl++;
        int decB = (regs[REG_B] - 1) & 0xFF;
        regs[REG_B] = decB;
        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        memptr = (bc + 1) & 0xFFFF;

        // from zxpoly
        int tmp = (value + regs[REG_C] + 1) & 0xFF;
        flags = ((value & 0x80) >>> 6) // N
                | (tmp < (value & 0xFF) ? (FLAG_H | FLAG_C) : 0)
                | TABLE_SZ[regs[REG_B]]
                | TABLE_XY[regs[REG_B]]
                | PARITY_TABLE[(tmp & 7) ^ decB];
        Q = flags;
        return 16;
    }

    int I_INIR() {
        int bc = (regs[REG_B] << 8) | regs[REG_C];
        int hl = (regs[REG_H] << 8) | regs[REG_L];

        byte value = context.readIO(bc);
        memory.write(hl, value);

        hl = (hl + 1) & 0xFFFF;
        int decB = (regs[REG_B] - 1) & 0xFF;
        regs[REG_B] = decB;
        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        memptr = (bc + 1) & 0xFFFF;

        // from zxpoly
        int tmp = (value + regs[REG_C] + 1) & 0xFF;
        flags = ((value & 0x80) >>> 6) // N
                | (tmp < (value & 0xFF) ? (FLAG_H | FLAG_C) : 0)
                | TABLE_SZ[regs[REG_B]]
                | TABLE_XY[regs[REG_B]]
                | PARITY_TABLE[(tmp & 7) ^ decB];
        Q = flags;

        if (decB == 0) {
            return 16;
        }
        PC = (PC - 2) & 0xFFFF;
        flags = (flags & ~(FLAG_X | FLAG_Y)) | ((PC >>> 8) & (FLAG_X | FLAG_Y));

        int flagP = flags & FLAG_PV;
        int flagH = flags & FLAG_H;

        if ((flags & FLAG_C) == FLAG_C) {
            if ((value & 0x80) == 0) {
                flagP = flagP ^ PARITY_TABLE[(decB + 1) & 0x7] ^ FLAG_PV;
                flagH = (decB & 0x0F) == 0x0F ? FLAG_H : 0;
            } else {
                flagP = flagP ^ PARITY_TABLE[(decB - 1) & 0x7] ^ FLAG_PV;
                flagH = (decB & 0x0F) == 0 ? FLAG_H : 0;
            }
        } else {
            flagP = flagP ^ PARITY_TABLE[decB & 0x07] ^ FLAG_PV;
        }
        flags = ((flags & ~(FLAG_PV | FLAG_H)) | flagP | flagH);
        return 21;
    }

    int I_IND() {
        int bc = (regs[REG_B] << 8) | regs[REG_C];
        byte value = context.readIO(bc);
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        memory.write(hl, value);

        hl = (hl - 1) & 0xFFFF;
        int decB = (regs[REG_B] - 1) & 0xFF;
        regs[REG_B] = decB;
        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        memptr = (bc - 1) & 0xFFFF;

        // from zxpoly
        int tmp = (value + regs[REG_C] - 1) & 0xFF;
        flags = ((value & 0x80) >>> 6) // N
                | (tmp < (value & 0xFF) ? (FLAG_H | FLAG_C) : 0)
                | TABLE_SZ[regs[REG_B]]
                | TABLE_XY[regs[REG_B]]
                | PARITY_TABLE[(tmp & 7) ^ decB];

        Q = flags;
        return 16;
    }

    int I_INDR() {
        int bc = (regs[REG_B] << 8) | regs[REG_C];
        int hl = (regs[REG_H] << 8) | regs[REG_L];

        byte value = context.readIO(bc);
        memory.write(hl, value);

        hl = (hl - 1) & 0xFFFF;
        int decB = (regs[REG_B] - 1) & 0xFF;
        regs[REG_B] = decB;
        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        memptr = (bc - 1) & 0xFFFF;

        // from zxpoly
        int tmp = (value + regs[REG_C] - 1) & 0xFF;
        flags = ((value & 0x80) >>> 6) // N
                | (tmp < (value & 0xFF) ? (FLAG_H | FLAG_C) : 0)
                | TABLE_SZ[regs[REG_B]]
                | TABLE_XY[regs[REG_B]]
                | PARITY_TABLE[(tmp & 7) ^ decB];
        Q = flags;

        if (decB == 0) {
            return 16;
        }
        PC = (PC - 2) & 0xFFFF;
        flags = (flags & ~(FLAG_X | FLAG_Y)) | ((PC >>> 8) & (FLAG_X | FLAG_Y));

        int flagP = flags & FLAG_PV;
        int flagH = flags & FLAG_H;

        if ((flags & FLAG_C) == FLAG_C) {
            if ((value & 0x80) == 0) {
                flagP = flagP ^ PARITY_TABLE[(decB + 1) & 0x7] ^ FLAG_PV;
                flagH = (decB & 0x0F) == 0x0F ? FLAG_H : 0;
            } else {
                flagP = flagP ^ PARITY_TABLE[(decB - 1) & 0x7] ^ FLAG_PV;
                flagH = (decB & 0x0F) == 0 ? FLAG_H : 0;
            }
        } else {
            flagP = flagP ^ PARITY_TABLE[decB & 0x07] ^ FLAG_PV;
        }
        flags = ((flags & ~(FLAG_PV | FLAG_H)) | flagP | flagH);
        return 21;
    }

    int I_OUTI() {
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        byte value = memory.read(hl);
        int B = regs[REG_B];
        int decB = (B - 1) & 0xFF;

        context.writeIO((decB << 8) | regs[REG_C], value);

        hl++;
        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_B] = decB;
        memptr = (((decB << 8) | regs[REG_C]) + 1) & 0xFFFF;

        // from zxpoly
        int tmp = (value + regs[REG_L]) & 0xFF;
        flags = ((value & 0x80) >>> 6) // N
                | (tmp < (value & 0xFF) ? (FLAG_H | FLAG_C) : 0)
                | TABLE_SZ[regs[REG_B]]
                | TABLE_XY[regs[REG_B]]
                | PARITY_TABLE[(tmp & 7) ^ decB];

        Q = flags;
        return 16;
    }

    int I_OTIR() {
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        byte value = memory.read(hl);
        int B = regs[REG_B];
        int decB = (B - 1) & 0xFF;

        context.writeIO((decB << 8) | regs[REG_C], value);

        hl++;
        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_B] = decB;
        memptr = (((decB << 8) | regs[REG_C]) + 1) & 0xFFFF;

        // from zxpoly
        int tmp = (value + regs[REG_L]) & 0xFF;
        flags = ((value & 0x80) >>> 6) // N
                | (tmp < (value & 0xFF) ? (FLAG_H | FLAG_C) : 0)
                | TABLE_SZ[decB]
                | TABLE_XY[decB]
                | PARITY_TABLE[(tmp & 7) ^ decB];
        Q = flags;

        if (decB == 0) {
            return 16;
        }
        PC = (PC - 2) & 0xFFFF;
        flags = (flags & ~(FLAG_X | FLAG_Y)) | ((PC >>> 8) & (FLAG_X | FLAG_Y));

        int flagP = flags & FLAG_PV;
        int flagH = flags & FLAG_H;

        if ((flags & FLAG_C) == FLAG_C) {
            if ((value & 0x80) == 0) {
                flagP = flagP ^ PARITY_TABLE[(decB + 1) & 0x7] ^ FLAG_PV;
                flagH = (decB & 0x0F) == 0x0F ? FLAG_H : 0;
            } else {
                flagP = flagP ^ PARITY_TABLE[(decB - 1) & 0x7] ^ FLAG_PV;
                flagH = (decB & 0x0F) == 0 ? FLAG_H : 0;
            }
        } else {
            flagP = flagP ^ PARITY_TABLE[decB & 0x07] ^ FLAG_PV;
        }
        flags = ((flags & ~(FLAG_PV | FLAG_H)) | flagP | flagH);
        return 21;
    }

    int I_OUTD() {
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        byte value = memory.read(hl);
        int B = regs[REG_B];
        int decB = (B - 1) & 0xFF;

        context.writeIO((decB << 8) | regs[REG_C], value);

        hl--;
        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_B] = decB;
        memptr = (((decB << 8) | regs[REG_C]) + 1) & 0xFFFF;

        // from zxpoly
        int tmp = (value + regs[REG_L]) & 0xFF;
        flags = ((value & 0x80) >>> 6) // N
                | (tmp < (value & 0xFF) ? (FLAG_H | FLAG_C) : 0)
                | TABLE_SZ[decB]
                | TABLE_XY[decB]
                | PARITY_TABLE[(tmp & 7) ^ decB];
        Q = flags;
        return 16;
    }

    int I_OTDR() {
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        byte value = memory.read(hl);
        int B = regs[REG_B];
        int decB = (B - 1) & 0xFF;

        context.writeIO((decB << 8) | regs[REG_C], value);

        hl--;
        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_B] = decB;
        memptr = (((decB << 8) | regs[REG_C]) + 1) & 0xFFFF;

        // from zxpoly
        int tmp = (value + regs[REG_L]) & 0xFF;
        flags = ((value & 0x80) >>> 6) // N
                | (tmp < (value & 0xFF) ? (FLAG_H | FLAG_C) : 0)
                | TABLE_SZ[decB]
                | TABLE_XY[decB]
                | PARITY_TABLE[(tmp & 7) ^ decB];
        Q = flags;

        if (decB == 0) {
            return 16;
        }
        PC = (PC - 2) & 0xFFFF;

        flags = (flags & ~(FLAG_X | FLAG_Y)) | ((PC >>> 8) & (FLAG_X | FLAG_Y));

        int flagP = flags & FLAG_PV;
        int flagH = flags & FLAG_H;

        if ((flags & FLAG_C) == FLAG_C) {
            if ((value & 0x80) == 0) {
                flagP = flagP ^ PARITY_TABLE[(decB + 1) & 0x7] ^ FLAG_PV;
                flagH = (decB & 0x0F) == 0x0F ? FLAG_H : 0;
            } else {
                flagP = flagP ^ PARITY_TABLE[(decB - 1) & 0x7] ^ FLAG_PV;
                flagH = (decB & 0x0F) == 0 ? FLAG_H : 0;
            }
        } else {
            flagP = flagP ^ PARITY_TABLE[decB & 0x07] ^ FLAG_PV;
        }
        flags = ((flags & ~(FLAG_PV | FLAG_H)) | flagP | flagH);
        return 21;
    }

    int I_LD_REF_NN_RP() {
        int addr = readWord(PC);
        memptr = (addr + 1) & 0xFFFF;
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = getpair((lastOpcode >>> 4) & 3, true);
        writeWord(addr, tmp1);
        return 20;
    }

    int I_LD_RP_REF_NN() {
        int addr = readWord(PC);
        memptr = (addr + 1) & 0xFFFF;
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = readWord(addr);
        putpair((lastOpcode >>> 4) & 3, tmp1, true);
        return 20;
    }

    int I_JR_CC_N() {
        int addr = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;

        if (getCC1((lastOpcode >>> 3) & 3)) {
            PC = (PC + (byte) addr) & 0xFFFF;
            memptr = PC;
            return 12;
        }
        return 7;
    }

    int I_JR_N() {
        int addr = memory.read(PC);
        PC = (PC + 1 + (byte) addr) & 0xFFFF;
        memptr = PC;
        return 12;
    }

    int I_OUT_REF_N_A() {
        int port = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        memptr = (regs[REG_A] << 8) | ((port + 1) & 0xFF);
        //	Note for *BM1: MEMPTR_low = (port + 1) & #FF,  MEMPTR_hi = 0
        context.writeIO((regs[REG_A] << 8) | port, (byte) regs[REG_A]);
        return 11;
    }

    int I_IN_A_REF_N() {
        int port = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        //_memPtr = (A << 8) + _memory.ReadByte(PC) + _memory.ReadByte(PC + 1) * 256 + 1;
        int aport = (regs[REG_A] << 8) | port;
        regs[REG_A] = context.readIO(aport) & 0xFF;
        memptr = (aport + 1) & 0xFFFF;
        return 11;
    }

    int I_SUB_N() {
        int value = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;

        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;

        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA] | TABLE_XY[regs[REG_A]];
        return 7;
    }

    int I_LD_RP_NN() {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        putpair((lastOpcode >>> 4) & 3, tmp, true);
        return 10;
    }

    int I_JP_CC_NN() {
        int addr = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = (lastOpcode >>> 3) & 7;
        if ((flags & CONDITION[tmp1]) == CONDITION_VALUES[tmp1]) {
            PC = addr;
        }
        memptr = PC;
        return 10;
    }

    int I_CALL_CC_NN() {
        int addr = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        memptr = PC;

        int tmp1 = (lastOpcode >>> 3) & 7;
        if ((flags & CONDITION[tmp1]) == CONDITION_VALUES[tmp1]) {
            SP = (SP - 2) & 0xffff;
            writeWord(SP, PC);
            PC = addr;
            memptr = PC;
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
        int addr = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = readWord(addr);
        putpair(2, tmp1, false);
        return 16;
    }

    int I_LD_REF_NN_A() {
        int addr = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        memptr = (regs[REG_A] << 8) | ((addr + 1) & 0xFF);
        //	Note for *BM1: MEMPTR_low = (addr + 1) & #FF,  MEMPTR_hi = 0
        memory.write(addr, (byte) regs[REG_A]);
        return 13;
    }

    int I_LD_A_REF_NN() {
        int addr = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        memptr = (addr + 1) & 0xFFFF;
        regs[REG_A] = (memory.read(addr) & 0xff);
        return 13;
    }

    int I_JP_NN() {
        PC = readWord(PC);
        memptr = PC;
        return 10;
    }

    int I_CALL_NN() {
        int addr = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        SP = (SP - 2) & 0xffff;
        writeWord(SP, PC);
        PC = addr;
        memptr = PC;
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
        if (IFF[0]) {
            PC = (PC - 1) & 0xFFFF; // endless loop if interrupts are enabled
        } else {
            currentRunState = RunState.STATE_STOPPED_NORMAL;
        }
        return 4;
    }

    int I_RLC_R() {
        int reg = lastOpcode & 7;
        int regValue = getreg(reg) & 0xFF;

        int flagC = ((regValue & 0x80) != 0) ? FLAG_C : 0;
        regValue = ((regValue << 1) | (regValue >>> 7)) & 0xFF;
        putreg(reg, regValue);
        flags = TABLE_SZ[regValue] | PARITY_TABLE[regValue] | flagC | TABLE_XY[regValue];
        Q = flags;

        if (reg == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_RRC_R() {
        int reg = lastOpcode & 7;
        int regValue = getreg(reg) & 0xFF;
        int flagC = regValue & FLAG_C;
        regValue = ((regValue >>> 1) | (regValue << 7)) & 0xFF;
        putreg(reg, regValue);
        flags = TABLE_SZ[regValue] | PARITY_TABLE[regValue] | flagC | TABLE_XY[regValue];
        Q = flags;

        if (reg == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_RL_R() {
        int reg = lastOpcode & 7;
        int regValue = getreg(reg) & 0xFF;
        int flagC = ((regValue & 0x80) == 0x80) ? FLAG_C : 0;
        regValue = ((regValue << 1) | (flags & FLAG_C)) & 0xFF;
        putreg(reg, regValue);
        flags = TABLE_SZ[regValue] | PARITY_TABLE[regValue] | flagC | TABLE_XY[regValue];
        Q = flags;

        if (reg == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_RR_R() {
        int reg = lastOpcode & 7;
        int regValue = getreg(reg) & 0xFF;
        int flagC = regValue & FLAG_C;
        regValue = ((regValue >>> 1) | (flags << 7)) & 0xFF;
        putreg(reg, regValue);
        flags = TABLE_SZ[regValue] | PARITY_TABLE[regValue] | flagC | TABLE_XY[regValue];
        Q = flags;

        if (reg == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_SLA_R() {
        int reg = lastOpcode & 7;
        int regValue = getreg(reg) & 0xFF;

        int flagC = ((regValue & 0x80) == 0x80) ? FLAG_C : 0;
        regValue = (regValue << 1) & 0xFF;
        putreg(reg, regValue);
        flags = TABLE_SZ[regValue] | PARITY_TABLE[regValue] | flagC | TABLE_XY[regValue];
        Q = flags;

        if (reg == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_SRA_R() {
        int reg = lastOpcode & 7;
        int regValue = getreg(reg) & 0xFF;
        int flagC = regValue & FLAG_C;
        regValue = ((regValue >>> 1) | (regValue & 0x80)) & 0xFF;
        putreg(reg, regValue);
        flags = TABLE_SZ[regValue] | PARITY_TABLE[regValue] | flagC | TABLE_XY[regValue];
        Q = flags;

        if (reg == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_SLL_R() {
        int reg = lastOpcode & 7;
        int regValue = getreg(reg) & 0xFF;
        int flagC = ((regValue & 0x80) != 0) ? FLAG_C : 0;
        regValue = ((regValue << 1) | 0x01) & 0xFF;
        putreg(reg, regValue);
        flags = TABLE_SZ[regValue] | PARITY_TABLE[regValue] | flagC | TABLE_XY[regValue];
        Q = flags;

        if (reg == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_SRL_R() {
        int reg = lastOpcode & 7;
        int regValue = getreg(reg) & 0xFF;
        int flagC = regValue & FLAG_C;
        regValue = (regValue >>> 1) & 0xFF;
        putreg(reg, regValue);
        flags = TABLE_SZ[regValue] | PARITY_TABLE[regValue] | flagC | TABLE_XY[regValue];
        Q = flags;

        if (reg == 6) {
            return 15;
        } else {
            return 8;
        }
    }

    int I_BIT_N_R() {
        int bit = (lastOpcode >>> 3) & 7;
        int reg = lastOpcode & 7;
        int regValue = getreg(reg) & 0xFF;
        int result = (1 << bit) & regValue;

        flags = ((result != 0) ? (result & FLAG_S) : (FLAG_Z | FLAG_PV))
                | TABLE_XY[regValue]
                | FLAG_H
                | (flags & FLAG_C);

        if (reg == 6) {
            flags &= (~FLAG_X);
            flags &= (~FLAG_Y);
            flags |= ((memptr >>> 8) & (FLAG_X | FLAG_Y));
            Q = flags;
            return 12;
        } else {
            Q = flags;
            return 8;
        }
    }

    int I_RES_N_R() {
        int bit = (lastOpcode >>> 3) & 7;
        int reg = lastOpcode & 7;
        int regValue = getreg(reg) & 0xFF;
        regValue = (regValue & (~(1 << bit)));
        putreg(reg, regValue);
        if (reg == 6) {
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
        memptr = (IX + 1) & 0xFFFF;
        IX = I_ADD_II_RP(IX);
        return 15;
    }

    int I_ADD_IY_RP() {
        memptr = (IY + 1) & 0xFFFF;
        IY = I_ADD_II_RP(IY);
        return 15;
    }

    int I_ADD_II_RP(int special) {
        int dstRp;
        int rp = (lastOpcode >>> 4) & 0x03;
        switch (rp) {
            case 3:
                dstRp = SP;
                break;
            case 2:
                dstRp = special;
                break;
            default:
                int index = rp * 2;
                dstRp = regs[index] << 8 | regs[index + 1];
        }

        int res = special + dstRp;
        flags = (flags & FLAG_SZP) |
                (((dstRp ^ res ^ special) >>> 8) & FLAG_H) |
                ((res >>> 16) & FLAG_C) | TABLE_XY[(res >>> 8) & 0xFF];
        return res & 0xFFFF;
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
        int value = memory.read(address) & 0xFF;
        memptr = address;

        int sum = (value + 1) & 0x1FF;
        int sumByte = sum & 0xFF;
        flags = TABLE_SZ[sumByte] | (TABLE_HP[sum ^ 1 ^ value]) | (flags & FLAG_C) | TABLE_XY[sumByte];

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
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (special + disp) & 0xFFFF;
        int value = memory.read(address) & 0xFF;
        memptr = address;

        int sum = (value - 1) & 0x1FF;
        int sumByte = sum & 0xFF;

        flags = TABLE_SUB[sumByte] | (TABLE_HP[sum ^ 1 ^ value]) | (flags & FLAG_C) | TABLE_XY[sumByte];
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
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        byte number = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (special + disp) & 0xFFFF;
        memptr = address;
        memory.write(address, number);
        return 19;
    }

    int I_LD_R_REF_IX_N() {
        return I_LD_R_REF_II_N(IX);
    }

    int I_LD_R_REF_IY_N() {
        return I_LD_R_REF_II_N(IY);
    }

    int I_LD_R_REF_II_N(int special) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int tmp1 = (lastOpcode >>> 3) & 7;
        int address = (special + disp) & 0xFFFF;
        memptr = address;
        putreg(tmp1, memory.read(address));
        return 19;
    }

    int I_LD_REF_IX_N_R() {
        return I_LD_REF_II_N_R(IX);
    }

    int I_LD_REF_IY_N_R() {
        return I_LD_REF_II_N_R(IY);
    }

    int I_LD_REF_II_N_R(int special) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (special + disp) & 0xFFFF;
        memptr = address;
        memory.write(address, (byte) getreg(lastOpcode & 7));
        return 19;
    }

    int I_ADD_A_REF_IX_N() {
        return I_ADD_A_REF_II_N(IX);
    }

    int I_ADD_A_REF_IY_N() {
        return I_ADD_A_REF_II_N(IY);
    }

    int I_ADD_A_REF_II_N(int special) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (special + disp) & 0xFFFF;
        memptr = address;
        int value = memory.read(address) & 0xFF;
        int oldA = regs[REG_A];
        int sum = (oldA + value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
        return 19;
    }

    int I_ADC_A_REF_IX_N() {
        return I_ADC_A_REF_II_N(IX);
    }

    int I_ADC_A_REF_IY_N() {
        return I_ADC_A_REF_II_N(IY);
    }

    int I_ADC_A_REF_II_N(int special) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (special + disp) & 0xFFFF;
        memptr = address;
        int value = memory.read(address) & 0xFF;
        int oldA = regs[REG_A];
        int sum = (oldA + value + (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
        return 19;
    }

    int I_SUB_REF_IX_N() {
        return I_SUB_REF_II_N(IX);
    }

    int I_SUB_REF_IY_N() {
        return I_SUB_REF_II_N(IY);
    }

    int I_SUB_REF_II_N(int special) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (special + disp) & 0xFFFF;
        memptr = address;
        int value = memory.read(address) & 0xFF;
        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA] | TABLE_XY[regs[REG_A]];
        return 19;
    }

    int I_SBC_A_REF_IX_N() {
        return I_SBC_A_REF_II_N(IX);
    }

    int I_SBC_A_REF_IY_N() {
        return I_SBC_A_REF_II_N(IY);
    }

    int I_SBC_A_REF_II_N(int special) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (special + disp) & 0xFFFF;
        memptr = address;
        int value = memory.read(address) & 0xFF;
        int oldA = regs[REG_A];
        int sum = (oldA - value - (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA] | TABLE_XY[regs[REG_A]];
        return 19;
    }

    int I_AND_REF_IX_N() {
        return I_AND_REF_II_N(IX);
    }

    int I_AND_REF_IY_N() {
        return I_AND_REF_II_N(IY);
    }

    int I_AND_REF_II_N(int special) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (special + disp) & 0xFFFF;
        memptr = address;
        int value = memory.read(address);
        regs[REG_A] = (regs[REG_A] & value) & 0xFF;
        flags = PARITY_TABLE[regs[REG_A]] | TABLE_SZ[regs[REG_A]] | FLAG_H | TABLE_XY[regs[REG_A]];
        return 19;
    }

    int I_XOR_REF_IX_N() {
        return I_XOR_REF_II_N(IX);
    }

    int I_XOR_REF_IY_N() {
        return I_XOR_REF_II_N(IY);
    }

    int I_XOR_REF_II_N(int special) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (special + disp) & 0xFFFF;
        memptr = address;
        byte value = memory.read(address);
        regs[REG_A] = ((regs[REG_A] ^ value) & 0xff);
        flags = PARITY_TABLE[regs[REG_A]] | TABLE_SZ[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        return 19;
    }

    int I_OR_REF_IX_N() {
        return I_OR_REF_II_N(IX);
    }

    int I_OR_REF_IY_N() {
        return I_OR_REF_II_N(IY);
    }

    int I_OR_REF_II_N(int special) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (special + disp) & 0xFFFF;
        memptr = address;
        byte value = memory.read(address);
        regs[REG_A] = ((regs[REG_A] | value) & 0xff);
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        return 19;
    }

    int I_CP_REF_IX_N() {
        return I_CP_REF_II_N(IX);
    }

    int I_CP_REF_IY_N() {
        return I_CP_REF_II_N(IY);
    }

    int I_CP_REF_II_N(int special) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (special + disp) & 0xFFFF;
        memptr = address;
        int value = memory.read(address) & 0xFF;
        int sum = (regs[REG_A] - value) & 0x1FF;
        int result = sum & 0xFF;
        flags = TABLE_SUB[result] | (TABLE_CHP[sum ^ value ^ regs[REG_A]]) | TABLE_XY[value];
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
        memptr = IX;
        writeWord(SP, tmp1);
        return 23;
    }

    int I_EX_REF_SP_IY() {
        int tmp = readWord(SP);
        int tmp1 = IY;
        IY = tmp;
        memptr = IY;
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
        int addr = (special + operand) & 0xFFFF;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;

        int c = ((addrValue & 0x80) != 0) ? FLAG_C : 0;
        int res = ((addrValue << 1) | (addrValue >>> 7)) & 0xFF;
        memory.write(addr, (byte) res);
        flags = TABLE_SZ[res] | PARITY_TABLE[res] | c | TABLE_XY[res];

        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = res & 0xFF;
        return 23;
    }

    int I_RRC_REF_IX_N_R(byte operand) {
        return I_RRC_REF_II_N_R(operand, IX);
    }

    int I_RRC_REF_IY_N_R(byte operand) {
        return I_RRC_REF_II_N_R(operand, IY);
    }

    int I_RRC_REF_II_N_R(byte operand, int special) {
        int addr = (special + operand) & 0xffff;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;

        int c = addrValue & 1;
        int res = (((addrValue >>> 1) & 0x7F) | (c << 7)) & 0xFF;
        memory.write(addr, (byte) (res & 0xFF));
        flags = TABLE_SZ[res] | EmulatorTables.PARITY_TABLE[res] | c | TABLE_XY[res];

        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = res & 0xFF;
        return 23;
    }

    int I_RL_REF_IX_N_R(byte operand) {
        return I_RL_REF_II_N_R(operand, IX);
    }

    int I_RL_REF_IY_N_R(byte operand) {
        return I_RL_REF_II_N_R(operand, IY);
    }

    int I_RL_REF_II_N_R(byte operand, int special) {
        int addr = (special + operand) & 0xffff;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;

        int c = (addrValue >>> 7) & 1;
        int res = ((((addrValue << 1) & 0xFF) | flags & FLAG_C) & 0xFF);
        memory.write(addr, (byte) (res & 0xFF));

        flags = TABLE_SZ[res] | EmulatorTables.PARITY_TABLE[res] | c | TABLE_XY[res];
        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = res & 0xFF;
        return 23;
    }

    int I_RR_REF_IX_N_R(byte operand) {
        return I_RR_REF_II_N_R(operand, IX);
    }

    int I_RR_REF_IY_N_R(byte operand) {
        return I_RR_REF_II_N_R(operand, IY);
    }

    int I_RR_REF_II_N_R(byte operand, int special) {
        int addr = (special + operand) & 0xFFFF;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;

        int c = addrValue & 1;
        int res = ((((addrValue >> 1) & 0xFF) | (flags & FLAG_C) << 7) & 0xFF);
        memory.write(addr, (byte) (res & 0xFF));

        flags = TABLE_SZ[res] | EmulatorTables.PARITY_TABLE[res] | c | TABLE_XY[res];
        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = res & 0xFF;
        return 23;
    }

    int I_SLA_REF_IX_N_R(byte operand) {
        return I_SLA_REF_II_N_R(operand, IX);
    }

    int I_SLA_REF_IY_N_R(byte operand) {
        return I_SLA_REF_II_N_R(operand, IY);
    }

    int I_SLA_REF_II_N_R(byte operand, int special) {
        int addr = (special + operand) & 0xFFFF;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;

        int c = (addrValue >>> 7) & 1;
        int res = (addrValue << 1) & 0xFE;
        memory.write(addr, (byte) res);
        flags = TABLE_SZ[res] | EmulatorTables.PARITY_TABLE[res] | c | TABLE_XY[res];

        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = res & 0xFF;
        return 23;
    }

    int I_SRA_REF_IX_N_R(byte operand) {
        return I_SRA_REF_II_N_R(operand, IX);
    }

    int I_SRA_REF_IY_N_R(byte operand) {
        return I_SRA_REF_II_N_R(operand, IY);
    }

    int I_SRA_REF_II_N_R(byte operand, int special) {
        int addr = (special + operand) & 0xFFFF;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;

        int c = addrValue & 1;
        int res = (addrValue >> 1) & 0xFF | (addrValue & 0x80);
        memory.write(addr, (byte) res);

        flags = TABLE_SZ[res] | EmulatorTables.PARITY_TABLE[res] | c | TABLE_XY[res];
        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = res & 0xFF;
        return 23;
    }

    int I_SLL_REF_IX_N_R(byte operand) {
        return I_SLL_REF_II_N_R(operand, IX);
    }

    int I_SLL_REF_IY_N_R(byte operand) {
        return I_SLL_REF_II_N_R(operand, IY);
    }

    int I_SLL_REF_II_N_R(byte operand, int special) {
        int addr = (special + operand) & 0xFFFF;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;

        int c = ((addrValue & 0x80) != 0) ? FLAG_C : 0;
        int res = ((addrValue << 1) | 0x01) & 0xFF;

        memory.write(addr, (byte) res);
        flags = TABLE_SZ[res] | PARITY_TABLE[res] | c | TABLE_XY[res];

        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = res;
        return 23;
    }

    int I_SRL_REF_IX_N_R(byte operand) {
        return I_SRL_REF_II_N_R(operand, IX);
    }

    int I_SRL_REF_IY_N_R(byte operand) {
        return I_SRL_REF_II_N_R(operand, IY);
    }

    int I_SRL_REF_II_N_R(byte operand, int special) {
        int addr = (special + operand) & 0xFFFF;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;

        int c = ((addrValue & 0x01) != 0) ? FLAG_C : 0;
        int res = (addrValue >>> 1) & 0xFF;
        memory.write(addr, (byte) res);

        flags = TABLE_SZ[res] | PARITY_TABLE[res] | c | TABLE_XY[res];
        // regs[6] is unused, so it's ok to set it
        regs[lastOpcode & 7] = res;
        return 23;
    }

    int I_BIT_N_REF_IX_N(byte operand) {
        return I_BIT_N_REF_II_N(operand, IX);
    }

    int I_BIT_N_REF_IY_N(byte operand) {
        return I_BIT_N_REF_II_N(operand, IY);
    }

    int I_BIT_N_REF_II_N(byte operand, int special) {
        int bit = (lastOpcode >>> 3) & 7;
        int address = (special + operand) & 0xFFFF;
        memptr = address;
        byte addrValue = memory.read(address);
        int result = (1 << bit) & addrValue;

        flags = ((flags & FLAG_C)
                | FLAG_H
                | ((result == 0) ? (FLAG_Z | FLAG_PV) : 0))
                | TABLE_XY[special & 0xFF];
        if (bit == 7) {
            flags |= ((result == 0x80) ? FLAG_S : 0);
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
        int bitNumber = (lastOpcode >>> 3) & 7;
        int addr = (special + operand) & 0xFFFF;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;
        int res = (addrValue & (~(1 << bitNumber)));
        memory.write(addr, (byte) (res & 0xff));
        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = res & 0xFF;
        return 23;
    }

    int I_SET_N_REF_IX_N_R(byte operand) {
        return I_SET_N_REF_II_N_R(operand, IX);
    }

    int I_SET_N_REF_IY_N_R(byte operand) {
        return I_SET_N_REF_II_N_R(operand, IY);
    }

    int I_SET_N_REF_II_N_R(byte operand, int special) {
        int bitNumber = (lastOpcode >>> 3) & 7;
        int addr = (special + operand) & 0xFFFF;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;

        int res = (addrValue | (1 << bitNumber)) & 0xFF;
        memory.write(addr, (byte) res);

        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = res;
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
        flags = TABLE_SZ[sumByte] | (TABLE_HP[sum ^ 1 ^ value]) | (flags & FLAG_C) | TABLE_XY[sumByte];
        Q = flags;
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
        flags = TABLE_SUB[sumByte] | (TABLE_HP[sum ^ 1 ^ reg]) | (flags & FLAG_C) | TABLE_XY[sumByte];
        Q = flags;
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
        flags = TABLE_SUB[sumByte] | (TABLE_HP[sum ^ 1 ^ reg]) | (flags & FLAG_C) | TABLE_XY[sumByte];
        Q = flags;
        return sumByte;
    }

    int I_LD_IXH_N() {
        IX = (((memory.read(PC) & 0xFF) << 8) | (IX & 0xFF)) & 0xFFFF;
        PC = (PC + 1) & 0xFFFF;
        return 11;
    }

    int I_LD_IYH_N() {
        IY = (((memory.read(PC) & 0xFF) << 8) | (IY & 0xFF)) & 0xFFFF;
        PC = (PC + 1) & 0xFFFF;
        return 11;
    }

    int I_LD_IXL_N() {
        IX = ((IX & 0xFF00) | (memory.read(PC) & 0xFF)) & 0xFFFF;
        PC = (PC + 1) & 0xFFFF;
        return 11;
    }

    int I_LD_IYL_N() {
        IY = ((IY & 0xFF00) | (memory.read(PC) & 0xFF)) & 0xFFFF;
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
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
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
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
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

        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA] | TABLE_XY[regs[REG_A]];
        Q = flags;
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
        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA] | TABLE_XY[regs[REG_A]];
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
        flags = PARITY_TABLE[regs[REG_A]] | TABLE_SZ[regs[REG_A]] | FLAG_H | TABLE_XY[regs[REG_A]];
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
        regs[REG_A] = (regs[REG_A] ^ value) & 0xFF;
        flags = PARITY_TABLE[regs[REG_A]] | TABLE_SZ[regs[REG_A]] | TABLE_XY[regs[REG_A]];
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
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
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
        flags = TABLE_SUB[result] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[value & 0xFF];
        return 8;
    }
}
