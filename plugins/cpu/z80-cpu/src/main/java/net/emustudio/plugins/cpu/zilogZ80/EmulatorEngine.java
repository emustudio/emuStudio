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
            } else if (!pendingInterrupts.isEmpty()) {
                pendingInterrupts.poll(); // if interrupts are disabled, ignore it; otherwise stack overflow
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
        memory.write(regs[REG_B] << 8 | regs[REG_C], (byte) regs[REG_A]);
        memptr = (regs[REG_A] << 8) | 1;
        //	Note for *BM1: MEMPTR_low = (rp + 1) & #FF,  MEMPTR_hi = 0
        return 7;
    }

    int I_LD_BC_NN() {
        return I_LD_RP_NN(REG_C, REG_B);
    }

    int I_LD_DE_NN() {
        return I_LD_RP_NN(REG_E, REG_D);
    }

    int I_LD_HL_NN() {
        return I_LD_RP_NN(REG_L, REG_H);
    }

    int I_LD_SP_NN() {
        SP = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        return 10;
    }

    int I_LD_RP_NN(int low, int high) {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        regs[high] = tmp >>> 8;
        regs[low] = tmp & 0xFF;
        return 10;
    }

    int I_INC_BC() {
        return I_INC_RP(REG_C, REG_B, regs[REG_B] << 8 | regs[REG_C]);
    }

    int I_INC_DE() {
        return I_INC_RP(REG_E, REG_D, regs[REG_D] << 8 | regs[REG_E]);
    }

    int I_INC_HL() {
        return I_INC_RP(REG_L, REG_H, regs[REG_H] << 8 | regs[REG_L]);
    }

    int I_INC_SP() {
        SP = (SP + 1) & 0xFFFF;
        return 6;
    }

    int I_INC_IX() {
        IX = (IX + 1) & 0xFFFF;
        return 10;
    }

    int I_INC_IY() {
        IY = (IY + 1) & 0xFFFF;
        return 10;
    }

    int I_INC_RP(int low, int high, int value) {
        int result = (value + 1) & 0xFFFF;
        regs[high] = result >>> 8;
        regs[low] = result & 0xFF;
        return 6;
    }

    int I_INC_B() {
        regs[REG_B] = I_INC(regs[REG_B]);
        return 4;
    }

    int I_INC_C() {
        regs[REG_C] = I_INC(regs[REG_C]);
        return 4;
    }

    int I_INC_D() {
        regs[REG_D] = I_INC(regs[REG_D]);
        return 4;
    }

    int I_INC_E() {
        regs[REG_E] = I_INC(regs[REG_E]);
        return 4;
    }

    int I_INC_H() {
        regs[REG_H] = I_INC(regs[REG_H]);
        return 4;
    }

    int I_INC_L() {
        regs[REG_L] = I_INC(regs[REG_L]);
        return 4;
    }

    int I_INC_A() {
        regs[REG_A] = I_INC(regs[REG_A]);
        return 4;
    }

    int I_INC_REF_HL() {
        int value = memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF;
        memory.write((regs[REG_H] << 8) | regs[REG_L], (byte) I_INC(value));
        return 11;
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

    int I_DEC_BC() {
        return I_DEC_RP(REG_C, REG_B, regs[REG_B] << 8 | regs[REG_C]);
    }

    int I_DEC_DE() {
        return I_DEC_RP(REG_E, REG_D, regs[REG_D] << 8 | regs[REG_E]);
    }

    int I_DEC_HL() {
        return I_DEC_RP(REG_L, REG_H, regs[REG_H] << 8 | regs[REG_L]);
    }

    int I_DEC_SP() {
        SP = (SP - 1) & 0xFFFF;
        return 6;
    }

    int I_DEC_IX() {
        IX = (IX - 1) & 0xFFFF;
        return 10;
    }

    int I_DEC_IY() {
        IY = (IY - 1) & 0xFFFF;
        return 10;
    }

    int I_DEC_RP(int low, int high, int value) {
        value = (value - 1) & 0xFFFF;
        regs[high] = value >>> 8;
        regs[low] = value & 0xFF;
        return 6;
    }

    int I_DEC_B() {
        regs[REG_B] = I_DEC(regs[REG_B]);
        return 4;
    }

    int I_DEC_C() {
        regs[REG_C] = I_DEC(regs[REG_C]);
        return 4;
    }

    int I_DEC_D() {
        regs[REG_D] = I_DEC(regs[REG_D]);
        return 4;
    }

    int I_DEC_E() {
        regs[REG_E] = I_DEC(regs[REG_E]);
        return 4;
    }

    int I_DEC_H() {
        regs[REG_H] = I_DEC(regs[REG_H]);
        return 4;
    }

    int I_DEC_L() {
        regs[REG_L] = I_DEC(regs[REG_L]);
        return 4;
    }

    int I_DEC_A() {
        regs[REG_A] = I_DEC(regs[REG_A]);
        return 4;
    }

    int I_DEC_REF_HL() {
        int value = memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF;
        memory.write((regs[REG_H] << 8) | regs[REG_L], (byte) I_DEC(value));
        return 11;
    }

    int I_DEC_IXH() {
        IX = ((I_DEC(IX >>> 8) << 8) | (IX & 0xFF)) & 0xFFFF;
        return 8;
    }

    int I_DEC_IYH() {
        IY = ((I_DEC(IY >>> 8) << 8) | (IY & 0xFF)) & 0xFFFF;
        return 8;
    }

    int I_DEC_IXL() {
        IX = ((IX & 0xFF00) | (I_DEC(IX & 0xFF) & 0xFF)) & 0xFFFF;
        return 8;
    }

    int I_DEC_IYL() {
        IY = ((IY & 0xFF00) | (I_DEC(IY & 0xFF) & 0xFF)) & 0xFFFF;
        return 8;
    }

    int I_DEC_REF_IX_N() {
        return I_DEC_REF_II_N(IX);
    }

    int I_DEC_REF_IY_N() {
        return I_DEC_REF_II_N(IY);
    }

    int I_DEC(int value) {
        int sum = (value - 1) & 0x1FF;
        int sumByte = sum & 0xFF;
        flags = TABLE_SUB[sumByte] | (TABLE_HP[sum ^ 1 ^ value]) | (flags & FLAG_C) | TABLE_XY[sumByte];
        Q = flags;
        return sumByte;
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

    int I_LD_B_N() {
        return I_LD_R_N(REG_B);
    }

    int I_LD_C_N() {
        return I_LD_R_N(REG_C);
    }

    int I_LD_D_N() {
        return I_LD_R_N(REG_D);
    }

    int I_LD_E_N() {
        return I_LD_R_N(REG_E);
    }

    int I_LD_H_N() {
        return I_LD_R_N(REG_H);
    }

    int I_LD_L_N() {
        return I_LD_R_N(REG_L);
    }

    int I_LD_A_N() {
        return I_LD_R_N(REG_A);
    }

    int I_LD_REF_HL_N() {
        memory.write((regs[REG_H] << 8) | regs[REG_L], memory.read(PC));
        PC = (PC + 1) & 0xFFFF;
        return 10;
    }

    int I_LD_R_N(int reg) {
        regs[reg] = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        return 7;
    }

    int I_LD_REF_IX_D_N() {
        return I_LD_REF_II_D_N(IX);
    }

    int I_LD_REF_IY_D_N() {
        return I_LD_REF_II_D_N(IY);
    }

    int I_LD_REF_II_D_N(int special) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        byte number = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (special + disp) & 0xFFFF;
        memptr = address;
        memory.write(address, number);
        return 19;
    }

    int I_ADD_HL_BC() {
        return I_ADD_HL_RP(regs[REG_B] << 8 | regs[REG_C]);
    }

    int I_ADD_HL_DE() {
        return I_ADD_HL_RP(regs[REG_D] << 8 | regs[REG_E]);
    }

    int I_ADD_HL_HL() {
        return I_ADD_HL_RP(regs[REG_H] << 8 | regs[REG_L]);
    }

    int I_ADD_HL_SP() {
        return I_ADD_HL_RP(SP);
    }

    int I_ADD_IX_BC() {
        return I_ADD_IX_RP(regs[REG_B] << 8 | regs[REG_C]);
    }

    int I_ADD_IX_DE() {
        return I_ADD_IX_RP(regs[REG_D] << 8 | regs[REG_E]);
    }

    int I_ADD_IX_IX() {
        return I_ADD_IX_RP(IX);
    }

    int I_ADD_IX_SP() {
        return I_ADD_IX_RP(SP);
    }

    int I_ADD_IY_BC() {
        return I_ADD_IY_RP(regs[REG_B] << 8 | regs[REG_C]);
    }

    int I_ADD_IY_DE() {
        return I_ADD_IY_RP(regs[REG_D] << 8 | regs[REG_E]);
    }

    int I_ADD_IY_IY() {
        return I_ADD_IY_RP(IY);
    }

    int I_ADD_IY_SP() {
        return I_ADD_IY_RP(SP);
    }

    int I_ADD_HL_RP(int rp) {
        int res = I_ADD_SRC_RP((regs[REG_H] << 8) | regs[REG_L], rp);
        regs[REG_H] = (res >>> 8) & 0xFF;
        regs[REG_L] = res & 0xFF;
        return 11;
    }

    int I_ADD_IX_RP(int rp) {
        memptr = (IX + 1) & 0xFFFF;
        IX = I_ADD_SRC_RP2(IX, rp);
        return 15;
    }

    int I_ADD_IY_RP(int rp) {
        memptr = (IY + 1) & 0xFFFF;
        IY = I_ADD_SRC_RP2(IY, rp);
        return 15;
    }

    int I_ADD_SRC_RP(int src, int rp) {
        int res = I_ADD_SRC_RP2(src, rp);
        Q = flags;
        return res & 0xFFFF;
    }

    int I_ADD_SRC_RP2(int src, int rp) {
        memptr = (src + 1) & 0xFFFF;
        int res = src + rp;
        flags = (flags & FLAG_SZP) |
                (((src ^ res ^ rp) >>> 8) & FLAG_H) |
                ((res >>> 16) & FLAG_C) | TABLE_XY[(res >>> 8) & 0xFF];
        return res & 0xFFFF;
    }

    int I_POP_BC() {
        return I_POP_RP(REG_C, REG_B);
    }

    int I_POP_DE() {
        return I_POP_RP(REG_E, REG_D);
    }

    int I_POP_HL() {
        return I_POP_RP(REG_L, REG_H);
    }

    int I_POP_AF() {
        int value = readWord(SP);
        SP = (SP + 2) & 0xFFFF;
        regs[REG_A] = value >>> 8;
        flags = value & 0xFF;
        return 10;
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

    int I_POP_RP(int low, int high) {
        int value = readWord(SP);
        SP = (SP + 2) & 0xFFFF;
        regs[low] = value & 0xFF;
        regs[high] = value >>> 8;
        return 10;
    }

    int I_PUSH_BC() {
        return I_PUSH_RP((regs[REG_B] << 8) | regs[REG_C]);
    }

    int I_PUSH_DE() {
        return I_PUSH_RP((regs[REG_D] << 8) | regs[REG_E]);
    }

    int I_PUSH_HL() {
        return I_PUSH_RP((regs[REG_H] << 8) | regs[REG_L]);
    }

    int I_PUSH_AF() {
        return I_PUSH_RP((regs[REG_A] << 8) | (flags & 0xFF));
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

    int I_PUSH_RP(int value) {
        SP = (SP - 2) & 0xffff;
        writeWord(SP, value);
        return 11;
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

    int I_ADD_A_B() {
        return I_ADD_A(regs[REG_B]);
    }

    int I_ADD_A_C() {
        return I_ADD_A(regs[REG_C]);
    }

    int I_ADD_A_D() {
        return I_ADD_A(regs[REG_D]);
    }

    int I_ADD_A_E() {
        return I_ADD_A(regs[REG_E]);
    }

    int I_ADD_A_H() {
        return I_ADD_A(regs[REG_H]);
    }

    int I_ADD_A_IXH() {
        return I_ADD_A(IX >>> 8) + 4;
    }

    int I_ADD_A_IYH() {
        return I_ADD_A(IY >>> 8) + 4;
    }

    int I_ADD_A_L() {
        return I_ADD_A(regs[REG_L]);
    }

    int I_ADD_A_IXL() {
        return I_ADD_A(IX & 0xFF) + 4;
    }

    int I_ADD_A_IYL() {
        return I_ADD_A(IY & 0xFF) + 4;
    }

    int I_ADD_A_REF_HL() {
        return I_ADD_A(memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF) + 3;
    }

    int I_ADD_A_REF_IX_D() {
        return I_ADD_A_REF_XY_D(IX);
    }

    int I_ADD_A_REF_IY_D() {
        return I_ADD_A_REF_XY_D(IY);
    }

    int I_ADD_A_A() {
        return I_ADD_A(regs[REG_A]);
    }

    int I_ADD_A(int value) {
        int oldA = regs[REG_A];
        int sum = (oldA + value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 4;
    }

    int I_ADD_A_REF_XY_D(int xy) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        int value = memory.read(address) & 0xFF;
        int oldA = regs[REG_A];
        int sum = (oldA + value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
        return 19;
    }


    int I_ADC_A_B() {
        return I_ADC_A(regs[REG_B]);
    }

    int I_ADC_A_C() {
        return I_ADC_A(regs[REG_C]);
    }

    int I_ADC_A_D() {
        return I_ADC_A(regs[REG_D]);
    }

    int I_ADC_A_E() {
        return I_ADC_A(regs[REG_E]);
    }

    int I_ADC_A_H() {
        return I_ADC_A(regs[REG_H]);
    }

    int I_ADC_A_IXH() {
        return I_ADC_A(IX >>> 8) + 4;
    }

    int I_ADC_A_IYH() {
        return I_ADC_A(IY >>> 8) + 4;
    }

    int I_ADC_A_L() {
        return I_ADC_A(regs[REG_L]);
    }

    int I_ADC_A_IXL() {
        return I_ADC_A(IX & 0xFF) + 4;
    }

    int I_ADC_A_IYL() {
        return I_ADC_A(IY & 0xFF) + 4;
    }

    int I_ADC_A_REF_HL() {
        return I_ADC_A(memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF) + 3;
    }

    int I_ADC_A_REF_IX_D() {
        return I_ADC_A_REF_XY_D(IX);
    }

    int I_ADC_A_REF_IY_D() {
        return I_ADC_A_REF_XY_D(IY);
    }

    int I_ADC_A_A() {
        return I_ADC_A(regs[REG_A]);
    }

    int I_ADC_A(int value) {
        int oldA = regs[REG_A];
        int sum = (oldA + value + (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 4;
    }

    int I_ADC_A_REF_XY_D(int xy) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        int value = memory.read(address) & 0xFF;
        int oldA = regs[REG_A];
        int sum = (oldA + value + (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
        return 19;
    }

    int I_SUB_B() {
        return I_SUB(regs[REG_B]);
    }

    int I_SUB_C() {
        return I_SUB(regs[REG_C]);
    }

    int I_SUB_D() {
        return I_SUB(regs[REG_D]);
    }

    int I_SUB_E() {
        return I_SUB(regs[REG_E]);
    }

    int I_SUB_H() {
        return I_SUB(regs[REG_H]);
    }

    int I_SUB_IXH() {
        return I_SUB(IX >>> 8) + 4;
    }

    int I_SUB_IYH() {
        return I_SUB(IY >>> 8) + 4;
    }

    int I_SUB_L() {
        return I_SUB(regs[REG_L]);
    }

    int I_SUB_IXL() {
        return I_SUB(IX & 0xFF) + 4;
    }

    int I_SUB_IYL() {
        return I_SUB(IY & 0xFF) + 4;
    }

    int I_SUB_REF_HL() {
        return I_SUB(memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF) + 3;
    }

    int I_SUB_REF_IX_D() {
        return I_SUB_REF_XY_D(IX);
    }

    int I_SUB_REF_IY_D() {
        return I_SUB_REF_XY_D(IY);
    }

    int I_SUB_A() {
        return I_SUB(regs[REG_A]);
    }

    int I_SUB(int value) {
        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SUB[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 4;
    }

    int I_SUB_REF_XY_D(int xy) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        int value = memory.read(address) & 0xFF;
        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA] | TABLE_XY[regs[REG_A]];
        return 19;
    }

    int I_SBC_A_B() {
        return I_SBC_A(regs[REG_B]);
    }

    int I_SBC_A_C() {
        return I_SBC_A(regs[REG_C]);
    }

    int I_SBC_A_D() {
        return I_SBC_A(regs[REG_D]);
    }

    int I_SBC_A_E() {
        return I_SBC_A(regs[REG_E]);
    }

    int I_SBC_A_H() {
        return I_SBC_A(regs[REG_H]);
    }

    int I_SBC_A_IXH() {
        return I_SBC_A(IX >>> 8) + 4;
    }

    int I_SBC_A_IYH() {
        return I_SBC_A(IY >>> 8) + 4;
    }

    int I_SBC_A_L() {
        return I_SBC_A(regs[REG_L]);
    }

    int I_SBC_A_IXL() {
        return I_SBC_A(IX & 0xFF) + 4;
    }

    int I_SBC_A_IYL() {
        return I_SBC_A(IY & 0xFF) + 4;
    }

    int I_SBC_A_REF_HL() {
        return I_SBC_A(memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF) + 3;
    }

    int I_SBC_A_REF_IX_D() {
        return I_SBC_A_REF_XY_D(IX);
    }

    int I_SBC_A_REF_IY_D() {
        return I_SBC_A_REF_XY_D(IY);
    }

    int I_SBC_A_A() {
        return I_SBC_A(regs[REG_A]);
    }

    int I_SBC_A(int value) {
        int oldA = regs[REG_A];
        int sum = (oldA - value - (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA] | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 4;
    }

    int I_SBC_A_REF_XY_D(int xy) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        int value = memory.read(address) & 0xFF;
        int oldA = regs[REG_A];
        int sum = (oldA - value - (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA] | TABLE_XY[regs[REG_A]];
        return 19;
    }

    int I_AND_B() {
        return I_AND(regs[REG_B]);
    }

    int I_AND_C() {
        return I_AND(regs[REG_C]);
    }

    int I_AND_D() {
        return I_AND(regs[REG_D]);
    }

    int I_AND_E() {
        return I_AND(regs[REG_E]);
    }

    int I_AND_H() {
        return I_AND(regs[REG_H]);
    }

    int I_AND_IXH() {
        return I_AND(IX >>> 8) + 4; // TODO: not Q
    }

    int I_AND_IYH() {
        return I_AND(IY >>> 8) + 4; // TODO: not Q
    }

    int I_AND_L() {
        return I_AND(regs[REG_L]);
    }

    int I_AND_IXL() {
        return I_AND(IX & 0xFF) + 4; // TODO: not Q
    }

    int I_AND_IYL() {
        return I_AND(IY & 0xFF) + 4; // TODO: not Q
    }

    int I_AND_REF_HL() {
        return I_AND(memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF) + 3;
    }

    int I_AND_REF_IX_D() {
        return I_AND_REF_XY_D(IX);
    }

    int I_AND_REF_IY_D() {
        return I_AND_REF_XY_D(IY);
    }

    int I_AND_A() {
        return I_AND(regs[REG_A]);
    }

    int I_AND(int value) {
        regs[REG_A] = (regs[REG_A] & value) & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | FLAG_H | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 4;
    }

    int I_AND_REF_XY_D(int xy) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        int value = memory.read(address) & 0xFF;
        regs[REG_A] = (regs[REG_A] & value) & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | FLAG_H | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        return 19;
    }

    int I_XOR_B() {
        return I_XOR(regs[REG_B]);
    }

    int I_XOR_C() {
        return I_XOR(regs[REG_C]);
    }

    int I_XOR_D() {
        return I_XOR(regs[REG_D]);
    }

    int I_XOR_E() {
        return I_XOR(regs[REG_E]);
    }

    int I_XOR_H() {
        return I_XOR(regs[REG_H]);
    }

    int I_XOR_IXH() {
        return I_XOR(IX >>> 8) + 4; // TODO: not Q
    }

    int I_XOR_IYH() {
        return I_XOR(IY >>> 8) + 4; // TODO: not Q
    }

    int I_XOR_L() {
        return I_XOR(regs[REG_L]);
    }

    int I_XOR_IXL() {
        return I_XOR(IX & 0xFF) + 4; // TODO: not Q
    }

    int I_XOR_IYL() {
        return I_XOR(IY & 0xFF) + 4; // TODO: not Q
    }

    int I_XOR_REF_HL() {
        return I_XOR(memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF) + 3;
    }

    int I_XOR_REF_IX_D() {
        return I_XOR_REF_XY_D(IX);
    }

    int I_XOR_REF_IY_D() {
        return I_XOR_REF_XY_D(IY);
    }

    int I_XOR_A() {
        return I_XOR(regs[REG_A]);
    }

    int I_XOR(int value) {
        regs[REG_A] = ((regs[REG_A] ^ value) & 0xff);
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 4;
    }

    int I_XOR_REF_XY_D(int xy) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        byte value = memory.read(address);
        regs[REG_A] = ((regs[REG_A] ^ value) & 0xff);
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        return 19;
    }

    int I_OR_B() {
        return I_OR(regs[REG_B]);
    }

    int I_OR_C() {
        return I_OR(regs[REG_C]);
    }

    int I_OR_D() {
        return I_OR(regs[REG_D]);
    }

    int I_OR_E() {
        return I_OR(regs[REG_E]);
    }

    int I_OR_H() {
        return I_OR(regs[REG_H]);
    }

    int I_OR_IXH() {
        return I_OR(IX >>> 8) + 4;
    }

    int I_OR_IXL() {
        return I_OR(IX & 0xFF) + 4;
    }

    int I_OR_L() {
        return I_OR(regs[REG_L]);
    }

    int I_OR_IYH() {
        return I_OR(IY >>> 8) + 4;
    }

    int I_OR_IYL() {
        return I_OR(IY & 0xFF) + 4;
    }

    int I_OR_REF_HL() {
        return I_OR(memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF) + 3;
    }

    int I_OR_REF_IX_D() {
        return I_OR_REF_XY_D(IX);
    }

    int I_OR_REF_IY_D() {
        return I_OR_REF_XY_D(IY);
    }

    int I_OR_A() {
        return I_OR(regs[REG_A]);
    }

    int I_OR(int value) {
        regs[REG_A] = (regs[REG_A] | value) & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 4;
    }

    int I_OR_REF_XY_D(int xy) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        byte value = memory.read(address);
        regs[REG_A] = ((regs[REG_A] | value) & 0xff);
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        return 19;
    }

    int I_CP_B() {
        return I_CP_R(regs[REG_B]);
    }

    int I_CP_C() {
        return I_CP_R(regs[REG_C]);
    }

    int I_CP_D() {
        return I_CP_R(regs[REG_D]);
    }

    int I_CP_E() {
        return I_CP_R(regs[REG_E]);
    }

    int I_CP_H() {
        return I_CP_R(regs[REG_H]);
    }

    int I_CP_IXH() {
        return I_CP_R(IX >>> 8) + 4;
    }

    int I_CP_IYH() {
        return I_CP_R(IY >>> 8) + 4;
    }

    int I_CP_L() {
        return I_CP_R(regs[REG_L]);
    }

    int I_CP_IXL() {
        return I_CP_R(IX & 0xFF) + 4;
    }

    int I_CP_IYL() {
        return I_CP_R(IY & 0xFF) + 4;
    }

    int I_CP_REF_HL() {
        return I_CP_R(memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF) + 3;
    }

    int I_CP_REF_IX_D() {
        return I_CP_REF_XY_D(IX);
    }

    int I_CP_REF_IY_D() {
        return I_CP_REF_XY_D(IY);
    }

    int I_CP_A() {
        return I_CP_R(regs[REG_A]);
    }

    int I_CP_R(int value) {
        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        int result = sum & 0xFF;
        // F5 and F3 flags are set from the subtrahend instead of from the result.
        flags = TABLE_SUB[result] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[value];
        Q = flags;
        return 4;
    }

    int I_CP_REF_XY_D(int xy) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        int value = memory.read(address) & 0xFF;
        int sum = (regs[REG_A] - value) & 0x1FF;
        int result = sum & 0xFF;
        flags = TABLE_SUB[result] | (TABLE_CHP[sum ^ value ^ regs[REG_A]]) | TABLE_XY[value];
        return 19;
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

    int I_AND_N() {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_A] = (regs[REG_A] & tmp) & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | FLAG_H | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 7;
    }

    int I_XOR_N() {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_A] = ((regs[REG_A] ^ tmp) & 0xFF);
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 7;
    }

    int I_OR_N() {
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_A] = (regs[REG_A] | tmp) & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        Q = flags;
        return 7;
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
        return 4;
    }

    int I_SCF() {
        flags = (flags & FLAG_SZP) | (((lastQ ^ flags) | regs[REG_A]) & (FLAG_X | FLAG_Y)) | FLAG_C;
        Q = flags;
        return 4;
    }

    int I_CCF() {
        flags = (flags & FLAG_SZP)
                | ((flags & FLAG_C) == 0 ? FLAG_C : FLAG_H)
                | (((lastQ ^ flags) | regs[REG_A]) & (FLAG_X | FLAG_Y));
        Q = flags;
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
        return I_SUB(v) + 4;
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

    int I_LD_REF_NN_BC() {
        int addr = readWord(PC);
        memptr = (addr + 1) & 0xFFFF;
        PC = (PC + 2) & 0xFFFF;
        writeWord(addr, (regs[REG_B] << 8) | regs[REG_C]);
        return 20;
    }
    int I_LD_REF_NN_DE() {
        int addr = readWord(PC);
        memptr = (addr + 1) & 0xFFFF;
        PC = (PC + 2) & 0xFFFF;
        writeWord(addr, (regs[REG_D] << 8) | regs[REG_E]);
        return 20;
    }
    int I_LD_REF_NN_HL() {
        int addr = readWord(PC);
        memptr = (addr + 1) & 0xFFFF;
        PC = (PC + 2) & 0xFFFF;
        writeWord(addr, (regs[REG_H] << 8) | regs[REG_L]);
        return 16;
    }
    int I_ED_LD_REF_NN_HL() {
        return I_LD_REF_NN_HL() + 4;
    }
    int I_LD_REF_NN_SP() {
        int addr = readWord(PC);
        memptr = (addr + 1) & 0xFFFF;
        PC = (PC + 2) & 0xFFFF;
        writeWord(addr, SP);
        return 20;
    }
    int I_LD_REF_NN_A() {
        int addr = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        memptr = (regs[REG_A] << 8) | ((addr + 1) & 0xFF);
        //	Note for *BM1: MEMPTR_low = (addr + 1) & #FF,  MEMPTR_hi = 0
        memory.write(addr, (byte) regs[REG_A]);
        return 13;
    }
    int I_LD_REF_NN_IX() {
        return I_LD_REF_NN_XY(IX);
    }
    int I_LD_REF_NN_IY() {
        return I_LD_REF_NN_XY(IY);
    }

    int I_LD_REF_NN_XY(int xy) {
        int tmp = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        writeWord(tmp, xy);
        return 16;
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

    int I_LD_HL_REF_NN() {
        int addr = readWord(PC);
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = readWord(addr);
        putpair(2, tmp1, false);
        return 16;
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

    int I_LD_B_B() {
        return I_LD_R_R(REG_B, REG_B);
    }

    int I_LD_B_C() {
        return I_LD_R_R(REG_B, REG_C);
    }

    int I_LD_B_D() {
        return I_LD_R_R(REG_B, REG_D);
    }

    int I_LD_B_E() {
        return I_LD_R_R(REG_B, REG_E);
    }

    int I_LD_B_H() {
        return I_LD_R_R(REG_B, REG_H);
    }

    int I_LD_B_L() {
        return I_LD_R_R(REG_B, REG_L);
    }

    int I_LD_B_A() {
        return I_LD_R_R(REG_B, REG_A);
    }

    int I_LD_B_REF_HL() {
        return I_LD_R_REF_HL(REG_B);
    }

    int I_LD_B_IXH() {
        regs[REG_B] = (IX >>> 8);
        return 8;
    }

    int I_LD_B_IXL() {
        regs[REG_B] = (IX & 0xFF);
        return 8;
    }

    int I_LD_B_IYH() {
        regs[REG_B] = (IY >>> 8);
        return 8;
    }

    int I_LD_B_IYL() {
        regs[REG_B] = (IY & 0xFF);
        return 8;
    }

    int I_LD_B_REF_IX_D() {
        return I_LD_R_REF_XY_D(REG_B, IX);
    }

    int I_LD_B_REF_IY_D() {
        return I_LD_R_REF_XY_D(REG_B, IY);
    }

    int I_LD_C_B() {
        return I_LD_R_R(REG_C, REG_B);
    }

    int I_LD_C_C() {
        return I_LD_R_R(REG_C, REG_C);
    }

    int I_LD_C_D() {
        return I_LD_R_R(REG_C, REG_D);
    }

    int I_LD_C_E() {
        return I_LD_R_R(REG_C, REG_E);
    }

    int I_LD_C_H() {
        return I_LD_R_R(REG_C, REG_H);
    }

    int I_LD_C_L() {
        return I_LD_R_R(REG_C, REG_L);
    }

    int I_LD_C_A() {
        return I_LD_R_R(REG_C, REG_A);
    }

    int I_LD_C_REF_HL() {
        return I_LD_R_REF_HL(REG_C);
    }

    int I_LD_C_IXH() {
        regs[REG_C] = (IX >>> 8);
        return 8;
    }

    int I_LD_C_IXL() {
        regs[REG_C] = (IX & 0xFF);
        return 8;
    }

    int I_LD_C_IYH() {
        regs[REG_C] = (IY >>> 8);
        return 8;
    }

    int I_LD_C_IYL() {
        regs[REG_C] = (IY & 0xFF);
        return 8;
    }

    int I_LD_C_REF_IX_D() {
        return I_LD_R_REF_XY_D(REG_C, IX);
    }

    int I_LD_C_REF_IY_D() {
        return I_LD_R_REF_XY_D(REG_C, IY);
    }

    int I_LD_D_B() {
        return I_LD_R_R(REG_D, REG_B);
    }

    int I_LD_D_C() {
        return I_LD_R_R(REG_D, REG_C);
    }

    int I_LD_D_D() {
        return I_LD_R_R(REG_D, REG_D);
    }

    int I_LD_D_E() {
        return I_LD_R_R(REG_D, REG_E);
    }

    int I_LD_D_H() {
        return I_LD_R_R(REG_D, REG_H);
    }

    int I_LD_D_L() {
        return I_LD_R_R(REG_D, REG_L);
    }

    int I_LD_D_A() {
        return I_LD_R_R(REG_D, REG_A);
    }

    int I_LD_D_REF_HL() {
        return I_LD_R_REF_HL(REG_D);
    }

    int I_LD_D_IXH() {
        regs[REG_D] = (IX >>> 8);
        return 8;
    }

    int I_LD_D_IXL() {
        regs[REG_D] = (IX & 0xFF);
        return 8;
    }

    int I_LD_D_IYH() {
        regs[REG_D] = (IY >>> 8);
        return 8;
    }

    int I_LD_D_IYL() {
        regs[REG_D] = (IY & 0xFF);
        return 8;
    }

    int I_LD_D_REF_IX_D() {
        return I_LD_R_REF_XY_D(REG_D, IX);
    }

    int I_LD_D_REF_IY_D() {
        return I_LD_R_REF_XY_D(REG_D, IY);
    }

    int I_LD_E_B() {
        return I_LD_R_R(REG_E, REG_B);
    }

    int I_LD_E_C() {
        return I_LD_R_R(REG_E, REG_C);
    }

    int I_LD_E_D() {
        return I_LD_R_R(REG_E, REG_D);
    }

    int I_LD_E_E() {
        return I_LD_R_R(REG_E, REG_E);
    }

    int I_LD_E_H() {
        return I_LD_R_R(REG_E, REG_H);
    }

    int I_LD_E_L() {
        return I_LD_R_R(REG_E, REG_L);
    }

    int I_LD_E_A() {
        return I_LD_R_R(REG_E, REG_A);
    }

    int I_LD_E_REF_HL() {
        return I_LD_R_REF_HL(REG_E);
    }

    int I_LD_E_IXH() {
        regs[REG_E] = (IX >>> 8);
        return 8;
    }

    int I_LD_E_IXL() {
        regs[REG_E] = (IX & 0xFF);
        return 8;
    }

    int I_LD_E_IYH() {
        regs[REG_E] = (IY >>> 8);
        return 8;
    }

    int I_LD_E_IYL() {
        regs[REG_E] = (IY & 0xFF);
        return 8;
    }

    int I_LD_E_REF_IX_D() {
        return I_LD_R_REF_XY_D(REG_E, IX);
    }

    int I_LD_E_REF_IY_D() {
        return I_LD_R_REF_XY_D(REG_E, IY);
    }

    int I_LD_H_B() {
        return I_LD_R_R(REG_H, REG_B);
    }

    int I_LD_H_C() {
        return I_LD_R_R(REG_H, REG_C);
    }

    int I_LD_H_D() {
        return I_LD_R_R(REG_H, REG_D);
    }

    int I_LD_H_E() {
        return I_LD_R_R(REG_H, REG_E);
    }

    int I_LD_H_H() {
        return I_LD_R_R(REG_H, REG_H);
    }

    int I_LD_H_L() {
        return I_LD_R_R(REG_H, REG_L);
    }

    int I_LD_H_A() {
        return I_LD_R_R(REG_H, REG_A);
    }

    int I_LD_H_REF_HL() {
        return I_LD_R_REF_HL(REG_H);
    }

    int I_LD_IXH_B() {
        IX = (regs[REG_B] << 8) | (IX & 0xFF);
        return 8;
    }

    int I_LD_IXH_C() {
        IX = (regs[REG_C] << 8) | (IX & 0xFF);
        return 8;
    }

    int I_LD_IXH_D() {
        IX = (regs[REG_D] << 8) | (IX & 0xFF);
        return 8;
    }

    int I_LD_IXH_E() {
        IX = (regs[REG_E] << 8) | (IX & 0xFF);
        return 8;
    }

    int I_LD_IXH_IXH() {
        return 8;
    }

    int I_LD_IXH_IXL() {
        IX = (IX & 0xFF) | ((IX << 8) & 0xFF00);
        return 8;
    }

    int I_LD_IXH_A() {
        IX = (regs[REG_A] << 8) | (IX & 0xFF);
        return 8;
    }

    int I_LD_IYH_B() {
        IY = (regs[REG_B] << 8) | (IY & 0xFF);
        return 8;
    }

    int I_LD_IYH_C() {
        IY = (regs[REG_C] << 8) | (IY & 0xFF);
        return 8;
    }

    int I_LD_IYH_D() {
        IY = (regs[REG_D] << 8) | (IY & 0xFF);
        return 8;
    }

    int I_LD_IYH_E() {
        IY = (regs[REG_E] << 8) | (IY & 0xFF);
        return 8;
    }

    int I_LD_IYH_A() {
        IY = (regs[REG_A] << 8) | (IY & 0xFF);
        return 8;
    }

    int I_LD_IYH_IYH() {
        return 8;
    }

    int I_LD_IYH_IYL() {
        IY = (IY & 0xFF) | ((IY << 8) & 0xFF00);
        return 8;
    }

    int I_LD_H_REF_IX_D() {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (IX + disp) & 0xFFFF;
        memptr = address;
        regs[REG_H] = (memory.read(address) & 0xFF);
        return 19;
    }

    int I_LD_H_REF_IY_D() {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (IY + disp) & 0xFFFF;
        memptr = address;
        regs[REG_H] = (memory.read(address) & 0xFF);
        return 19;
    }

    int I_LD_L_B() {
        return I_LD_R_R(REG_L, REG_B);
    }

    int I_LD_L_C() {
        return I_LD_R_R(REG_L, REG_C);
    }

    int I_LD_L_D() {
        return I_LD_R_R(REG_L, REG_D);
    }

    int I_LD_L_E() {
        return I_LD_R_R(REG_L, REG_E);
    }

    int I_LD_L_H() {
        return I_LD_R_R(REG_L, REG_H);
    }

    int I_LD_L_L() {
        return I_LD_R_R(REG_L, REG_L);
    }

    int I_LD_L_A() {
        return I_LD_R_R(REG_L, REG_A);
    }

    int I_LD_L_REF_HL() {
        return I_LD_R_REF_HL(REG_L);
    }

    int I_LD_IXL_B() {
        IX = regs[REG_B] | (IX & 0xFF00);
        return 8;
    }

    int I_LD_IXL_C() {
        IX = regs[REG_C] | (IX & 0xFF00);
        return 8;
    }

    int I_LD_IXL_D() {
        IX = regs[REG_D] | (IX & 0xFF00);
        return 8;
    }

    int I_LD_IXL_E() {
        IX = regs[REG_E] | (IX & 0xFF00);
        return 8;
    }

    int I_LD_IXL_IXH() {
        IX = (IX & 0xFF00) | (IX >>> 8);
        return 8;
    }

    int I_LD_IXL_IXL() {
        return 8;
    }

    int I_LD_L_REF_IX_D() {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (IX + disp) & 0xFFFF;
        memptr = address;
        regs[REG_L] = (memory.read(address) & 0xFF);
        return 19;
    }

    int I_LD_IXL_A() {
        IX = regs[REG_A] | (IX & 0xFF00);
        return 8;
    }

    int I_LD_IYL_B() {
        IY = regs[REG_B] | (IY & 0xFF00);
        return 8;
    }

    int I_LD_IYL_C() {
        IY = regs[REG_C] | (IY & 0xFF00);
        return 8;
    }

    int I_LD_IYL_D() {
        IY = regs[REG_D] | (IY & 0xFF00);
        return 8;
    }

    int I_LD_IYL_E() {
        IY = regs[REG_E] | (IY & 0xFF00);
        return 8;
    }

    int I_LD_IYL_IYH() {
        IY = (IY & 0xFF00) | (IY >>> 8);
        return 8;
    }

    int I_LD_IYL_IYL() {
        return 8;
    }

    int I_LD_IYL_A() {
        IY = regs[REG_A] | (IY & 0xFF00);
        return 8;
    }

    int I_LD_L_REF_IY_D() {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (IY + disp) & 0xFFFF;
        memptr = address;
        regs[REG_L] = (memory.read(address) & 0xFF);
        return 19;
    }

    int I_LD_A_B() {
        return I_LD_R_R(REG_A, REG_B);
    }

    int I_LD_A_C() {
        return I_LD_R_R(REG_A, REG_C);
    }

    int I_LD_A_D() {
        return I_LD_R_R(REG_A, REG_D);
    }

    int I_LD_A_E() {
        return I_LD_R_R(REG_A, REG_E);
    }

    int I_LD_A_H() {
        return I_LD_R_R(REG_A, REG_H);
    }

    int I_LD_A_L() {
        return I_LD_R_R(REG_A, REG_L);
    }

    int I_LD_A_A() {
        return I_LD_R_R(REG_A, REG_A);
    }

    int I_LD_A_REF_HL() {
        return I_LD_R_REF_HL(REG_A);
    }

    int I_LD_A_IXH() {
        regs[REG_A] = (IX >>> 8);
        return 8;
    }

    int I_LD_A_IXL() {
        regs[REG_A] = (IX & 0xFF);
        return 8;
    }

    int I_LD_A_IYH() {
        regs[REG_A] = (IY >>> 8);
        return 8;
    }

    int I_LD_A_IYL() {
        regs[REG_A] = (IY & 0xFF);
        return 8;
    }

    int I_LD_A_REF_IX_D() {
        return I_LD_R_REF_XY_D(REG_A, IX);
    }

    int I_LD_A_REF_IY_D() {
        return I_LD_R_REF_XY_D(REG_A, IY);
    }

    int I_LD_REF_HL_B() {
        return I_LD_REF_HL_R(REG_B);
    }

    int I_LD_REF_HL_C() {
        return I_LD_REF_HL_R(REG_C);
    }

    int I_LD_REF_HL_D() {
        return I_LD_REF_HL_R(REG_D);
    }

    int I_LD_REF_HL_E() {
        return I_LD_REF_HL_R(REG_E);
    }

    int I_LD_REF_HL_H() {
        return I_LD_REF_HL_R(REG_H);
    }

    int I_LD_REF_HL_L() {
        return I_LD_REF_HL_R(REG_L);
    }

    int I_LD_REF_HL_A() {
        return I_LD_REF_HL_R(REG_A);
    }

    int I_LD_R_R(int dstReg, int srcReg) {
        regs[dstReg] = regs[srcReg];
        return 4;
    }

    int I_LD_R_REF_HL(int dstReg) {
        regs[dstReg] = memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF;
        return 7;
    }

    int I_LD_REF_HL_R(int srcReg) {
        memory.write((regs[REG_H] << 8) | regs[REG_L], (byte) regs[srcReg]);
        return 7;
    }

    int I_LD_R_REF_XY_D(int reg, int xy) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        regs[reg] = memory.read(address) & 0xFF;
        return 19;
    }

    int I_LD_REF_IX_D_B() {
        return I_LD_REF_XY_D_R(regs[REG_B], IX);
    }

    int I_LD_REF_IX_D_C() {
        return I_LD_REF_XY_D_R(regs[REG_C], IX);
    }

    int I_LD_REF_IX_D_D() {
        return I_LD_REF_XY_D_R(regs[REG_D], IX);
    }

    int I_LD_REF_IX_D_E() {
        return I_LD_REF_XY_D_R(regs[REG_E], IX);
    }

    int I_LD_REF_IX_D_IXH() {
        return I_LD_REF_XY_D_R(IX >>> 8, IX);
    }

    int I_LD_REF_IX_D_IXL() {
        return I_LD_REF_XY_D_R(IX & 0xFF, IX);
    }

    int I_LD_REF_IX_D_A() {
        return I_LD_REF_XY_D_R(regs[REG_A], IX);
    }

    int I_LD_REF_IY_D_B() {
        return I_LD_REF_XY_D_R(regs[REG_B], IY);
    }

    int I_LD_REF_IY_D_C() {
        return I_LD_REF_XY_D_R(regs[REG_C], IY);
    }

    int I_LD_REF_IY_D_D() {
        return I_LD_REF_XY_D_R(regs[REG_D], IY);
    }

    int I_LD_REF_IY_D_E() {
        return I_LD_REF_XY_D_R(regs[REG_E], IY);
    }

    int I_LD_REF_IY_D_IYH() {
        return I_LD_REF_XY_D_R(IY >>> 8, IY);
    }

    int I_LD_REF_IY_D_IYL() {
        return I_LD_REF_XY_D_R(IY & 0xFF, IY);
    }

    int I_LD_REF_IY_D_A() {
        return I_LD_REF_XY_D_R(regs[REG_A], IY);
    }

    int I_LD_REF_XY_D_R(int value, int xy) {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        memory.write(address, (byte) value);
        return 19;
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
}
