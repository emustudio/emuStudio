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
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.helpers.SleepUtils;
import net.emustudio.plugins.cpu.intel8080.api.CpuEngine;
import net.emustudio.plugins.cpu.intel8080.api.DispatchListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
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
    private static final int FLAG_SZC = FLAG_S | FLAG_Z | FLAG_C;
    private static final int FLAG_XY = FLAG_X | FLAG_Y;

    private final static Logger LOGGER = LoggerFactory.getLogger(EmulatorEngine.class);
    private final static int[] CONDITION = new int[]{
            FLAG_Z, FLAG_Z, FLAG_C, FLAG_C, FLAG_PV, FLAG_PV, FLAG_S, FLAG_S
    };
    private final static int[] CONDITION_VALUES = new int[]{
            0, FLAG_Z, 0, FLAG_C, 0, FLAG_PV, 0, FLAG_S
    };

    private final ContextZ80Impl context;
    private final MemoryContext<Byte> memory;
    private final AtomicLong executedCyclesPerSlot = new AtomicLong(0);

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

    public void addExecutedCyclesPerTimeSlice(long tstates) {
        executedCyclesPerSlot.addAndGet(tstates);
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

    @SuppressWarnings("BusyWait")
    public CPU.RunState run(CPU cpu) {
        // In Z80, 1 t-state = 250 ns = 0.25 microseconds = 0.00025 milliseconds
        // in 1 millisecond time slice = 1 / 0.00025 = 4000 t-states are executed uncontrollably

        final long slotNanos = SleepUtils.SLEEP_PRECISION;
        final double slotMicros = slotNanos / 1_000.0;
        final int cyclesPerSlot = (int) (slotMicros * context.getCPUFrequency() / 1000.0); // frequency in kHZ -> MHz

        currentRunState = CPU.RunState.STATE_RUNNING;
        long delayNanos = SleepUtils.SLEEP_PRECISION;

        long startTime = System.nanoTime();
        executedCyclesPerSlot.set(0);
        while (!Thread.currentThread().isInterrupted() && (currentRunState == CPU.RunState.STATE_RUNNING)) {
            try {
                if (delayNanos > 0) {
                    Thread.sleep(TimeUnit.NANOSECONDS.toMillis(delayNanos));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long endTime = System.nanoTime();
            long targetCycles = (endTime - startTime) / slotNanos * cyclesPerSlot;

            while ((executedCyclesPerSlot.get() < targetCycles) && !Thread.currentThread().isInterrupted() && (currentRunState == CPU.RunState.STATE_RUNNING)) {
                try {
                    dispatch();
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

            long computationTime = System.nanoTime() - endTime;
            delayNanos = slotNanos - computationTime;
        }
        return currentRunState;
    }

    private void advanceCycles(int cycles) {
        executedCyclesPerSlot.addAndGet(cycles);
        for (int i = 0; i < cycles; i++) {
            context.passedCycles(1); // make it precise to the bones
        }
    }

    private void dispatch() throws Throwable {
        DispatchListener tmpListener = dispatchListener;
        if (tmpListener != null) {
            tmpListener.beforeDispatch();
        }

        try {
            lastQ = Q;
            Q = 0;
            if (pendingNonMaskableInterrupt.getAndSet(false)) {
                if (memory.read(PC) == 0x76) {
                    // jump over HALT - this is probably wrong
                    writeWord((SP - 2) & 0xFFFF, (PC + 1) & 0xFFFF);
                } else {
                    writeWord((SP - 2) & 0xFFFF, PC);
                }

                SP = (SP - 2) & 0xffff;
                PC = 0x66;
                advanceCycles(12);
                return;
            }
            if (interruptSkip) {
                interruptSkip = false; // See EI
            } else if (IFF[0] && !pendingInterrupts.isEmpty()) {
                doInterrupt();
            } else if (!pendingInterrupts.isEmpty()) {
                pendingInterrupts.poll(); // if interrupts are disabled, ignore it; otherwise stack overflow
            }
            DISPATCH(DISPATCH_TABLE);
        } finally {
            if (tmpListener != null) {
                tmpListener.afterDispatch();
            }
        }
    }

    void CB_DISPATCH() throws Throwable {
        DISPATCH(DISPATCH_TABLE_CB);
    }

    void DD_DISPATCH() throws Throwable {
        DISPATCH(DISPATCH_TABLE_DD);
    }

    void DD_CB_DISPATCH() throws Throwable {
        SPECIAL_CB_DISPATCH(DISPATCH_TABLE_DD_CB);
    }

    void ED_DISPATCH() throws Throwable {
        DISPATCH(DISPATCH_TABLE_ED);
    }

    void FD_DISPATCH() throws Throwable {
        DISPATCH(DISPATCH_TABLE_FD);
    }

    void FD_CB_DISPATCH() throws Throwable {
        SPECIAL_CB_DISPATCH(DISPATCH_TABLE_FD_CB);
    }

    private void DISPATCH(MethodHandle[] table) throws Throwable {
        lastOpcode = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        incrementR();
        advanceCycles(4);

        MethodHandle instr = table[lastOpcode];
        if (instr != null) {
            instr.invokeExact(this);
        }
    }

    void SPECIAL_CB_DISPATCH(MethodHandle[] table) throws Throwable {
        // pc:4,pc+1:4,pc+2:3,pc+3:3,pc+3:1 x 2
        byte operand = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(3);

        lastOpcode = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        incrementR();
        advanceCycles(5);

        MethodHandle instr = table[lastOpcode];
        if (instr != null) {
            instr.invokeExact(this, operand);
        }
    }

    private void doInterrupt() throws Throwable {
        byte[] dataBus = pendingInterrupts.poll();

        IFF[0] = IFF[1] = false;
        switch (interruptMode) {
            case 0:
                advanceCycles(11);
                RunState old_runstate = currentRunState;
                if (dataBus != null && dataBus.length > 0) {
                    lastOpcode = dataBus[0] & 0xFF; // TODO: if dataBus had more bytes, they're ignored (except call).
                    if (lastOpcode == 0xCD) {  /* CALL */
                        advanceCycles(4 + 7); // fetch
                        SP = (SP - 2) & 0xFFFF;
                        memory.write(SP, (byte) (PC & 0xFF));
                        advanceCycles(3);
                        memory.write((SP + 1) & 0xFFFF, (byte) (PC >>> 8));

                        PC = ((dataBus[2] & 0xFF) << 8) | (dataBus[1] & 0xFF);
                        memptr = PC;
                        advanceCycles(3);
                        return;
                    }

                    dispatch(); // must ignore halt
                    if (currentRunState == RunState.STATE_STOPPED_NORMAL) {
                        currentRunState = old_runstate;
                    }
                }
                break;
            case 1:
                advanceCycles(12);
                if (memory.read(PC) == 0x76) {
                    // jump over HALT
                    writeWord((SP - 2) & 0xFFFF, (PC + 1) & 0xFFFF);
                } else {
                    writeWord((SP - 2) & 0xFFFF, PC);
                }
                SP = (SP - 2) & 0xffff;
                PC = 0x38;
                memptr = PC;
                break;
            case 2:
                advanceCycles(13);
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
    }

    private int getReg(int reg) {
        if (reg == 6) {
            return memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF;
        }
        return regs[reg];
    }

    private void putReg(int reg, int val) {
        if (reg == 6) {
            memory.write((regs[REG_H] << 8) | regs[REG_L], (byte) (val & 0xFF));
        } else {
            regs[reg] = val & 0xFF;
        }
    }

    private void putPair(int reg, int val) {
        int high = (val >>> 8) & 0xFF;
        int low = val & 0xFF;
        int index = reg * 2;

        if (reg == 3) {
            SP = val;
        } else {
            regs[index] = high;
            regs[index + 1] = low;
        }
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
        R = (R & 0x80) | ((R + 1) & 0x7F);
    }

    void I_NOP() {
        // pc:4
    }

    void I_LD_REF_BC_A() {
        // pc:4,ss:3
        int bc = regs[REG_B] << 8 | regs[REG_C];
        memory.write(bc, (byte) regs[REG_A]);
        memptr = (regs[REG_A] << 8) | ((bc + 1) & 0xFF);
        advanceCycles(3);
    }

    void I_LD_BC_NN() {
        I_LD_RP_NN(REG_C, REG_B);
    }

    void I_LD_DE_NN() {
        I_LD_RP_NN(REG_E, REG_D);
    }

    void I_LD_HL_NN() {
        I_LD_RP_NN(REG_L, REG_H);
    }

    void I_LD_SP_NN() {
        int tmp = memory.read(PC) & 0xFF;
        advanceCycles(3);
        SP = ((memory.read((PC + 1) & 0xFFFF) << 8) | tmp) & 0xFFFF;
        PC = (PC + 2) & 0xFFFF;
        advanceCycles(3);
    }

    void I_LD_RP_NN(int low, int high) {
        // pc:4,pc+1:3,pc+2:3
        regs[low] = memory.read(PC) & 0xFF;
        advanceCycles(3);
        regs[high] = memory.read((PC + 1) & 0xFFFF) & 0xFF;
        PC = (PC + 2) & 0xFFFF;
        advanceCycles(3);
    }

    void I_INC_BC() {
        I_INC_RP(REG_C, REG_B, regs[REG_B] << 8 | regs[REG_C]);
    }

    void I_INC_DE() {
        I_INC_RP(REG_E, REG_D, regs[REG_D] << 8 | regs[REG_E]);
    }

    void I_INC_HL() {
        I_INC_RP(REG_L, REG_H, regs[REG_H] << 8 | regs[REG_L]);
    }

    void I_INC_SP() {
        SP = (SP + 1) & 0xFFFF;
        advanceCycles(2);
    }

    void I_INC_IX() {
        IX = (IX + 1) & 0xFFFF;
        advanceCycles(2);
    }

    void I_INC_IY() {
        IY = (IY + 1) & 0xFFFF;
        advanceCycles(2);
    }

    void I_INC_RP(int low, int high, int value) {
        // pc:6
        int result = (value + 1) & 0xFFFF;
        regs[high] = result >>> 8;
        regs[low] = result & 0xFF;
        advanceCycles(2);
    }

    void I_INC_B() {
        regs[REG_B] = I_INC(regs[REG_B]);
    }

    void I_INC_C() {
        regs[REG_C] = I_INC(regs[REG_C]);
    }

    void I_INC_D() {
        regs[REG_D] = I_INC(regs[REG_D]);
    }

    void I_INC_E() {
        regs[REG_E] = I_INC(regs[REG_E]);
    }

    void I_INC_H() {
        regs[REG_H] = I_INC(regs[REG_H]);
    }

    void I_INC_L() {
        regs[REG_L] = I_INC(regs[REG_L]);
    }

    void I_INC_A() {
        regs[REG_A] = I_INC(regs[REG_A]);
    }

    void I_INC_REF_HL() {
        // pc:4,hl:3,hl:1,hl(write):3
        int value = memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF;
        advanceCycles(4);
        memory.write((regs[REG_H] << 8) | regs[REG_L], (byte) I_INC(value));
        advanceCycles(3);
    }

    void I_INC_IXH() {
        IX = ((I_INC(IX >>> 8) << 8) | (IX & 0xFF)) & 0xFFFF;
    }

    void I_INC_IYH() {
        IY = ((I_INC(IY >>> 8) << 8) | (IY & 0xFF)) & 0xFFFF;
    }

    void I_INC_IXL() {
        IX = ((IX & 0xFF00) | I_INC(IX & 0xFF)) & 0xFFFF;
    }

    void I_INC_IYL() {
        IY = ((IY & 0xFF00) | I_INC(IY & 0xFF)) & 0xFFFF;
    }

    int I_INC(int value) {
        // pc:4
        int sum = (value + 1) & 0x1FF;
        int sumByte = sum & 0xFF;
        flags = TABLE_SZ[sumByte] | (TABLE_HP[sum ^ 1 ^ value]) | (flags & FLAG_C) | TABLE_XY[sumByte];
        Q = flags;
        return sumByte;
    }

    void I_DEC_BC() {
        I_DEC_RP(REG_C, REG_B, regs[REG_B] << 8 | regs[REG_C]);
    }

    void I_DEC_DE() {
        I_DEC_RP(REG_E, REG_D, regs[REG_D] << 8 | regs[REG_E]);
    }

    void I_DEC_HL() {
        I_DEC_RP(REG_L, REG_H, regs[REG_H] << 8 | regs[REG_L]);
    }

    void I_DEC_SP() {
        SP = (SP - 1) & 0xFFFF;
        advanceCycles(2);
    }

    void I_DEC_IX() {
        IX = (IX - 1) & 0xFFFF;
        advanceCycles(2);
    }

    void I_DEC_IY() {
        IY = (IY - 1) & 0xFFFF;
        advanceCycles(2);
    }

    void I_DEC_RP(int low, int high, int value) {
        // pc:6
        value = (value - 1) & 0xFFFF;
        regs[high] = value >>> 8;
        regs[low] = value & 0xFF;
        advanceCycles(2);
    }

    void I_DEC_B() {
        regs[REG_B] = I_DEC(regs[REG_B]);
    }

    void I_DEC_C() {
        regs[REG_C] = I_DEC(regs[REG_C]);
    }

    void I_DEC_D() {
        regs[REG_D] = I_DEC(regs[REG_D]);
    }

    void I_DEC_E() {
        regs[REG_E] = I_DEC(regs[REG_E]);
    }

    void I_DEC_H() {
        regs[REG_H] = I_DEC(regs[REG_H]);
    }

    void I_DEC_L() {
        regs[REG_L] = I_DEC(regs[REG_L]);
    }

    void I_DEC_A() {
        regs[REG_A] = I_DEC(regs[REG_A]);
    }

    void I_DEC_REF_HL() {
        // pc:4,hl:3,hl:1,hl(write):3
        int value = memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF;
        advanceCycles(4);
        memory.write((regs[REG_H] << 8) | regs[REG_L], (byte) I_DEC(value));
        advanceCycles(3);
    }

    void I_DEC_IXH() {
        IX = ((I_DEC(IX >>> 8) << 8) | (IX & 0xFF)) & 0xFFFF;
    }

    void I_DEC_IYH() {
        IY = ((I_DEC(IY >>> 8) << 8) | (IY & 0xFF)) & 0xFFFF;
    }

    void I_DEC_IXL() {
        IX = ((IX & 0xFF00) | (I_DEC(IX & 0xFF) & 0xFF)) & 0xFFFF;
    }

    void I_DEC_IYL() {
        IY = ((IY & 0xFF00) | (I_DEC(IY & 0xFF) & 0xFF)) & 0xFFFF;
    }

    void I_DEC_REF_IX_N() {
        I_DEC_REF_II_N(IX);
    }

    void I_DEC_REF_IY_N() {
        I_DEC_REF_II_N(IY);
    }

    int I_DEC(int value) {
        // pc:4
        int sum = (value - 1) & 0x1FF;
        int sumByte = sum & 0xFF;
        flags = TABLE_SUB[sumByte] | (TABLE_HP[sum ^ 1 ^ value]) | (flags & FLAG_C) | TABLE_XY[sumByte];
        Q = flags;
        return sumByte;
    }

    void I_DEC_REF_II_N(int special) {
        // pc:4,pc+1:4,pc+2:3,pc+2:1 x 5,ii+n:3,ii+n:1,ii+n(write):3
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(8);
        int address = (special + disp) & 0xFFFF;
        int value = memory.read(address) & 0xFF;
        memptr = address;
        advanceCycles(4);

        int sum = (value - 1) & 0x1FF;
        int sumByte = sum & 0xFF;

        flags = TABLE_SUB[sumByte] | (TABLE_HP[sum ^ 1 ^ value]) | (flags & FLAG_C) | TABLE_XY[sumByte];
        memory.write(address, (byte) sumByte);
        advanceCycles(3);
    }

    void I_LD_B_N() {
        I_LD_R_N(REG_B);
    }

    void I_LD_C_N() {
        I_LD_R_N(REG_C);
    }

    void I_LD_D_N() {
        I_LD_R_N(REG_D);
    }

    void I_LD_E_N() {
        I_LD_R_N(REG_E);
    }

    void I_LD_H_N() {
        I_LD_R_N(REG_H);
    }

    void I_LD_L_N() {
        I_LD_R_N(REG_L);
    }

    void I_LD_A_N() {
        I_LD_R_N(REG_A);
    }

    void I_LD_REF_HL_N() {
        //pc:4,pc+1:3,hl:3
        byte n = memory.read(PC);
        advanceCycles(3);
        memory.write((regs[REG_H] << 8) | regs[REG_L], n);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(3);
    }

    void I_LD_R_N(int reg) {
        // pc:4,pc+1:3
        regs[reg] = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(3);
    }

    void I_LD_REF_IX_D_N() {
        I_LD_REF_II_D_N(IX);
    }

    void I_LD_REF_IY_D_N() {
        I_LD_REF_II_D_N(IY);
    }

    void I_LD_REF_II_D_N(int special) {
        // pc:4,pc+1:4,pc+2:3,pc+3:3,pc+3:1 x 2,ii+n:3
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(3);

        byte number = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(5);
        int address = (special + disp) & 0xFFFF;
        memptr = address;
        memory.write(address, number);
        advanceCycles(3);
    }

    void I_ADD_HL_BC() {
        I_ADD_HL_RP(regs[REG_B] << 8 | regs[REG_C]);
    }

    void I_ADD_HL_DE() {
        I_ADD_HL_RP(regs[REG_D] << 8 | regs[REG_E]);
    }

    void I_ADD_HL_HL() {
        I_ADD_HL_RP(regs[REG_H] << 8 | regs[REG_L]);
    }

    void I_ADD_HL_SP() {
        I_ADD_HL_RP(SP);
    }

    void I_ADD_IX_BC() {
        I_ADD_IX_RP(regs[REG_B] << 8 | regs[REG_C]);
    }

    void I_ADD_IX_DE() {
        I_ADD_IX_RP(regs[REG_D] << 8 | regs[REG_E]);
    }

    void I_ADD_IX_IX() {
        I_ADD_IX_RP(IX);
    }

    void I_ADD_IX_SP() {
        I_ADD_IX_RP(SP);
    }

    void I_ADD_IY_BC() {
        I_ADD_IY_RP(regs[REG_B] << 8 | regs[REG_C]);
    }

    void I_ADD_IY_DE() {
        I_ADD_IY_RP(regs[REG_D] << 8 | regs[REG_E]);
    }

    void I_ADD_IY_IY() {
        I_ADD_IY_RP(IY);
    }

    void I_ADD_IY_SP() {
        I_ADD_IY_RP(SP);
    }

    void I_ADD_HL_RP(int rp) {
        // pc:11
        int res = I_ADD_SRC_RP((regs[REG_H] << 8) | regs[REG_L], rp);
        regs[REG_H] = (res >>> 8) & 0xFF;
        regs[REG_L] = res & 0xFF;
        advanceCycles(7);
    }

    void I_ADD_IX_RP(int rp) {
        // pc:4,pc+1:11
        memptr = (IX + 1) & 0xFFFF;
        IX = I_ADD_SRC_RP2(IX, rp);
        advanceCycles(7);
    }

    void I_ADD_IY_RP(int rp) {
        memptr = (IY + 1) & 0xFFFF;
        IY = I_ADD_SRC_RP2(IY, rp);
        advanceCycles(7);
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

    void I_POP_BC() {
        I_POP_RP(REG_C, REG_B);
    }

    void I_POP_DE() {
        I_POP_RP(REG_E, REG_D);
    }

    void I_POP_HL() {
        I_POP_RP(REG_L, REG_H);
    }

    void I_POP_AF() {
        int value = memory.read(SP) & 0xFF;
        advanceCycles(3);
        value = ((memory.read((SP + 1) & 0xFFFF) << 8) | value) & 0xFFFF;
        SP = (SP + 2) & 0xFFFF;
        regs[REG_A] = value >>> 8;
        flags = value & 0xFF;
        advanceCycles(3);
    }

    void I_POP_IX() {
        IX = memory.read(SP) & 0xFF;
        advanceCycles(3);
        IX = ((memory.read((SP + 1) & 0xFFFF) << 8) | IX) & 0xFFFF;
        SP = (SP + 2) & 0xFFFF;
        advanceCycles(3);
    }

    void I_POP_IY() {
        IY = memory.read(SP) & 0xFF;
        advanceCycles(3);
        IY = ((memory.read((SP + 1) & 0xFFFF) << 8) | IY) & 0xFFFF;
        SP = (SP + 2) & 0xFFFF;
        advanceCycles(3);
    }

    void I_POP_RP(int low, int high) {
        // pc:4,sp:3,sp+1:3
        int value = memory.read(SP) & 0xFF;
        advanceCycles(3);
        value = ((memory.read((SP + 1) & 0xFFFF) << 8) | value) & 0xFFFF;
        SP = (SP + 2) & 0xFFFF;
        regs[low] = value & 0xFF;
        regs[high] = value >>> 8;
        advanceCycles(3);
    }

    void I_PUSH_BC() {
        I_PUSH_RP((regs[REG_B] << 8) | regs[REG_C]);
    }

    void I_PUSH_DE() {
        I_PUSH_RP((regs[REG_D] << 8) | regs[REG_E]);
    }

    void I_PUSH_HL() {
        I_PUSH_RP((regs[REG_H] << 8) | regs[REG_L]);
    }

    void I_PUSH_AF() {
        I_PUSH_RP((regs[REG_A] << 8) | (flags & 0xFF));
    }

    void I_PUSH_IX() {
        // pc:5,sp-1:3,sp-2:3
        SP = (SP - 2) & 0xFFFF;
        memory.write((SP + 1) & 0xFFFF, (byte) (IX >>> 8));
        advanceCycles(3);
        memory.write(SP, (byte) (IX & 0xFF));
        advanceCycles(3);
    }

    void I_PUSH_IY() {
        // pc:5,sp-1:3,sp-2:3
        SP = (SP - 2) & 0xFFFF;
        memory.write((SP + 1) & 0xFFFF, (byte) (IY >>> 8));
        advanceCycles(3);
        memory.write(SP, (byte) (IY & 0xFF));
        advanceCycles(3);
    }

    void I_PUSH_RP(int value) {
        // pc:5,sp-1:3,sp-2:3
        advanceCycles(1);
        SP = (SP - 2) & 0xffff;
        memory.write((SP + 1) & 0xFFFF, (byte) (value >>> 8));
        advanceCycles(3);
        memory.write(SP, (byte) (value & 0xFF));
        advanceCycles(3);
    }

    void I_RET_CC() {
        //pc:5,[sp:3,sp+1:3]
        advanceCycles(1);
        int cc = (lastOpcode >>> 3) & 7;
        if ((flags & CONDITION[cc]) == CONDITION_VALUES[cc]) {
            int tmp = memory.read(SP) & 0xFF;
            SP = (SP + 1) & 0xFFFF;
            advanceCycles(3);
            PC = ((memory.read(SP) << 8) | tmp) & 0xFFFF;
            SP = (SP + 1) & 0xffff;
            advanceCycles(3);
            memptr = PC;
        }
    }

    void I_RST() {
        // pc:5,sp-1:3,sp-2:3
        advanceCycles(1);
        SP = (SP - 1) & 0xFFFF;
        memory.write(SP, (byte) (PC >>> 8));
        advanceCycles(3);
        SP = (SP - 1) & 0xffff;
        memory.write(SP, (byte) (PC & 0xFF));

        PC = lastOpcode & 0x38;
        memptr = PC;
        advanceCycles(3);
    }

    void I_ADD_A_B() {
        I_ADD_A(regs[REG_B]);
    }

    void I_ADD_A_C() {
        I_ADD_A(regs[REG_C]);
    }

    void I_ADD_A_D() {
        I_ADD_A(regs[REG_D]);
    }

    void I_ADD_A_E() {
        I_ADD_A(regs[REG_E]);
    }

    void I_ADD_A_H() {
        I_ADD_A(regs[REG_H]);
    }

    void I_ADD_A_IXH() {
        I_ADD_A(IX >>> 8);
        advanceCycles(4);
    }

    void I_ADD_A_IYH() {
        I_ADD_A(IY >>> 8);
        advanceCycles(4);
    }

    void I_ADD_A_L() {
        I_ADD_A(regs[REG_L]);
    }

    void I_ADD_A_IXL() {
        I_ADD_A(IX & 0xFF);
    }

    void I_ADD_A_IYL() {
        I_ADD_A(IY & 0xFF);
    }

    void I_ADD_A_REF_HL() {
        // pc:4,hl:3
        I_ADD_A(memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF);
        advanceCycles(3);
    }

    void I_ADD_A_REF_IX_D() {
        I_ADD_A_REF_XY_D(IX);
    }

    void I_ADD_A_REF_IY_D() {
        I_ADD_A_REF_XY_D(IY);
    }

    void I_ADD_A_A() {
        I_ADD_A(regs[REG_A]);
    }

    void I_ADD_A(int value) {
        // pc:4
        int oldA = regs[REG_A];
        int sum = (oldA + value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
        Q = flags;
    }

    void I_ADD_A_REF_XY_D(int xy) {
        // pc:4,pc+1:4,pc+2:3,pc+2:1 x 5,ii+n:3
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(8);
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        int value = memory.read(address) & 0xFF;
        int oldA = regs[REG_A];
        int sum = (oldA + value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
        advanceCycles(3);
    }


    void I_ADC_A_B() {
        I_ADC_A(regs[REG_B]);
    }

    void I_ADC_A_C() {
        I_ADC_A(regs[REG_C]);
    }

    void I_ADC_A_D() {
        I_ADC_A(regs[REG_D]);
    }

    void I_ADC_A_E() {
        I_ADC_A(regs[REG_E]);
    }

    void I_ADC_A_H() {
        I_ADC_A(regs[REG_H]);
    }

    void I_ADC_A_IXH() {
        I_ADC_A(IX >>> 8);
    }

    void I_ADC_A_IYH() {
        I_ADC_A(IY >>> 8);
    }

    void I_ADC_A_L() {
        I_ADC_A(regs[REG_L]);
    }

    void I_ADC_A_IXL() {
        I_ADC_A(IX & 0xFF);
    }

    void I_ADC_A_IYL() {
        I_ADC_A(IY & 0xFF);
    }

    void I_ADC_A_REF_HL() {
        // pc:4,hl:3
        I_ADC_A(memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF);
        advanceCycles(3);
    }

    void I_ADC_A_REF_IX_D() {
        I_ADC_A_REF_XY_D(IX);
    }

    void I_ADC_A_REF_IY_D() {
        I_ADC_A_REF_XY_D(IY);
    }

    void I_ADC_A_A() {
        I_ADC_A(regs[REG_A]);
    }

    void I_ADC_A(int value) {
        // pc:4
        int oldA = regs[REG_A];
        int sum = (oldA + value + (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
        Q = flags;
    }

    void I_ADC_A_REF_XY_D(int xy) {
        // pc:4,pc+1:4,pc+2:3,pc+2:1 x 5,ii+n:3
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(8);
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        int value = memory.read(address) & 0xFF;
        int oldA = regs[REG_A];
        int sum = (oldA + value + (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
        advanceCycles(3);
    }

    void I_SUB_B() {
        I_SUB(regs[REG_B]);
    }

    void I_SUB_C() {
        I_SUB(regs[REG_C]);
    }

    void I_SUB_D() {
        I_SUB(regs[REG_D]);
    }

    void I_SUB_E() {
        I_SUB(regs[REG_E]);
    }

    void I_SUB_H() {
        I_SUB(regs[REG_H]);
    }

    void I_SUB_IXH() {
        I_SUB(IX >>> 8);
    }

    void I_SUB_IYH() {
        I_SUB(IY >>> 8);
    }

    void I_SUB_L() {
        I_SUB(regs[REG_L]);
    }

    void I_SUB_IXL() {
        I_SUB(IX & 0xFF);
    }

    void I_SUB_IYL() {
        I_SUB(IY & 0xFF);
    }

    void I_SUB_REF_HL() {
        // pc:4,hl:3
        I_SUB(memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF);
        advanceCycles(3);
    }

    void I_SUB_REF_IX_D() {
        I_SUB_REF_XY_D(IX);
    }

    void I_SUB_REF_IY_D() {
        I_SUB_REF_XY_D(IY);
    }

    void I_SUB_A() {
        I_SUB(regs[REG_A]);
    }

    void I_SUB(int value) {
        // pc:4
        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SUB[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
        Q = flags;
    }

    void I_SUB_REF_XY_D(int xy) {
        // pc:4,pc+1:4,pc+2:3,pc+2:1 x 5,ii+n:3
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(8);
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        int value = memory.read(address) & 0xFF;
        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA] | TABLE_XY[regs[REG_A]];
        advanceCycles(3);
    }

    void I_SBC_A_B() {
        I_SBC_A(regs[REG_B]);
    }

    void I_SBC_A_C() {
        I_SBC_A(regs[REG_C]);
    }

    void I_SBC_A_D() {
        I_SBC_A(regs[REG_D]);
    }

    void I_SBC_A_E() {
        I_SBC_A(regs[REG_E]);
    }

    void I_SBC_A_H() {
        I_SBC_A(regs[REG_H]);
    }

    void I_SBC_A_IXH() {
        I_SBC_A(IX >>> 8);
    }

    void I_SBC_A_IYH() {
        I_SBC_A(IY >>> 8);
    }

    void I_SBC_A_L() {
        I_SBC_A(regs[REG_L]);
    }

    void I_SBC_A_IXL() {
        I_SBC_A(IX & 0xFF);
    }

    void I_SBC_A_IYL() {
        I_SBC_A(IY & 0xFF);
    }

    void I_SBC_A_REF_HL() {
        // pc:4,hl:3
        I_SBC_A(memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF);
        advanceCycles(3);
    }

    void I_SBC_A_REF_IX_D() {
        I_SBC_A_REF_XY_D(IX);
    }

    void I_SBC_A_REF_IY_D() {
        I_SBC_A_REF_XY_D(IY);
    }

    void I_SBC_A_A() {
        I_SBC_A(regs[REG_A]);
    }

    void I_SBC_A(int value) {
        // pc:4
        int oldA = regs[REG_A];
        int sum = (oldA - value - (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA] | TABLE_XY[regs[REG_A]];
        Q = flags;
    }

    void I_SBC_A_REF_XY_D(int xy) {
        // pc:4,pc+1:4,pc+2:3,pc+2:1 x 5,ii+n:3
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(8);
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        int value = memory.read(address) & 0xFF;
        int oldA = regs[REG_A];
        int sum = (oldA - value - (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA] | TABLE_XY[regs[REG_A]];
        advanceCycles(3);
    }

    void I_AND_B() {
        I_AND(regs[REG_B]);
    }

    void I_AND_C() {
        I_AND(regs[REG_C]);
    }

    void I_AND_D() {
        I_AND(regs[REG_D]);
    }

    void I_AND_E() {
        I_AND(regs[REG_E]);
    }

    void I_AND_H() {
        I_AND(regs[REG_H]);
    }

    void I_AND_IXH() {
        int oldQ = Q;
        I_AND(IX >>> 8);
        Q = oldQ;
    }

    void I_AND_IYH() {
        int oldQ = Q;
        I_AND(IY >>> 8);
        Q = oldQ;
    }

    void I_AND_L() {
        I_AND(regs[REG_L]);
    }

    void I_AND_IXL() {
        int oldQ = Q;
        I_AND(IX & 0xFF);
        Q = oldQ;
    }

    void I_AND_IYL() {
        int oldQ = Q;
        I_AND(IY & 0xFF);
        Q = oldQ;
    }

    void I_AND_REF_HL() {
        // pc:4,hl:3
        I_AND(memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF);
        advanceCycles(3);
    }

    void I_AND_REF_IX_D() {
        I_AND_REF_XY_D(IX);
    }

    void I_AND_REF_IY_D() {
        I_AND_REF_XY_D(IY);
    }

    void I_AND_A() {
        I_AND(regs[REG_A]);
    }

    void I_AND(int value) {
        // pc:4
        regs[REG_A] = regs[REG_A] & value;
        flags = TABLE_SZ[regs[REG_A]] | FLAG_H | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        Q = flags;
    }

    void I_AND_REF_XY_D(int xy) {
        // pc:4,pc+1:4,pc+2:3,pc+2:1 x 5,ii+n:3
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(8);
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        int value = memory.read(address) & 0xFF;
        regs[REG_A] = (regs[REG_A] & value) & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | FLAG_H | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        advanceCycles(3);
    }

    void I_XOR_B() {
        I_XOR(regs[REG_B]);
    }

    void I_XOR_C() {
        I_XOR(regs[REG_C]);
    }

    void I_XOR_D() {
        I_XOR(regs[REG_D]);
    }

    void I_XOR_E() {
        I_XOR(regs[REG_E]);
    }

    void I_XOR_H() {
        I_XOR(regs[REG_H]);
    }

    void I_XOR_IXH() {
        int oldQ = Q;
        I_XOR(IX >>> 8);
        Q = oldQ;
    }

    void I_XOR_IYH() {
        int oldQ = Q;
        I_XOR(IY >>> 8);
        Q = oldQ;
    }

    void I_XOR_L() {
        I_XOR(regs[REG_L]);
    }

    void I_XOR_IXL() {
        int oldQ = Q;
        I_XOR(IX & 0xFF);
        Q = oldQ;
    }

    void I_XOR_IYL() {
        int oldQ = Q;
        I_XOR(IY & 0xFF);
        Q = oldQ;
    }

    void I_XOR_REF_HL() {
        // pc:4,hl:3
        I_XOR(memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF);
        advanceCycles(3);
    }

    void I_XOR_REF_IX_D() {
        I_XOR_REF_XY_D(IX);
    }

    void I_XOR_REF_IY_D() {
        I_XOR_REF_XY_D(IY);
    }

    void I_XOR_A() {
        I_XOR(regs[REG_A]);
    }

    void I_XOR(int value) {
        // pc:4
        regs[REG_A] = ((regs[REG_A] ^ value) & 0xff);
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        Q = flags;
    }

    void I_XOR_REF_XY_D(int xy) {
        // pc:4,pc+1:4,pc+2:3,pc+2:1 x 5,ii+n:3
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(8);
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        byte value = memory.read(address);
        regs[REG_A] = ((regs[REG_A] ^ value) & 0xff);
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        advanceCycles(3);
    }

    void I_OR_B() {
        I_OR(regs[REG_B]);
    }

    void I_OR_C() {
        I_OR(regs[REG_C]);
    }

    void I_OR_D() {
        I_OR(regs[REG_D]);
    }

    void I_OR_E() {
        I_OR(regs[REG_E]);
    }

    void I_OR_H() {
        I_OR(regs[REG_H]);
    }

    void I_OR_IXH() {
        I_OR(IX >>> 8);
    }

    void I_OR_IXL() {
        I_OR(IX & 0xFF);
    }

    void I_OR_L() {
        I_OR(regs[REG_L]);
    }

    void I_OR_IYH() {
        I_OR(IY >>> 8);
    }

    void I_OR_IYL() {
        I_OR(IY & 0xFF);
    }

    void I_OR_REF_HL() {
        // pc:4,hl:3
        I_OR(memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF);
        advanceCycles(3);
    }

    void I_OR_REF_IX_D() {
        I_OR_REF_XY_D(IX);
    }

    void I_OR_REF_IY_D() {
        I_OR_REF_XY_D(IY);
    }

    void I_OR_A() {
        I_OR(regs[REG_A]);
    }

    void I_OR(int value) {
        regs[REG_A] = (regs[REG_A] | value) & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        Q = flags;
    }

    void I_OR_REF_XY_D(int xy) {
        // pc:4,pc+1:4,pc+2:3,pc+2:1 x 5,ii+n:3
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(8);
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        byte value = memory.read(address);
        regs[REG_A] = ((regs[REG_A] | value) & 0xff);
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        advanceCycles(3);
    }

    void I_CP_B() {
        I_CP_R(regs[REG_B]);
    }

    void I_CP_C() {
        I_CP_R(regs[REG_C]);
    }

    void I_CP_D() {
        I_CP_R(regs[REG_D]);
    }

    void I_CP_E() {
        I_CP_R(regs[REG_E]);
    }

    void I_CP_H() {
        I_CP_R(regs[REG_H]);
    }

    void I_CP_IXH() {
        I_CP_R(IX >>> 8);
    }

    void I_CP_IYH() {
        I_CP_R(IY >>> 8);
    }

    void I_CP_L() {
        I_CP_R(regs[REG_L]);
    }

    void I_CP_IXL() {
        I_CP_R(IX & 0xFF);
    }

    void I_CP_IYL() {
        I_CP_R(IY & 0xFF);
    }

    void I_CP_REF_HL() {
        // pc:4,hl:3
        I_CP_R(memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF);
        advanceCycles(3);
    }

    void I_CP_REF_IX_D() {
        I_CP_REF_XY_D(IX);
    }

    void I_CP_REF_IY_D() {
        I_CP_REF_XY_D(IY);
    }

    void I_CP_A() {
        I_CP_R(regs[REG_A]);
    }

    void I_CP_R(int value) {
        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        int result = sum & 0xFF;
        // F5 and F3 flags are set from the subtrahend instead of from the result.
        flags = TABLE_SUB[result] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[value];
        Q = flags;
    }

    void I_CP_REF_XY_D(int xy) {
        // pc:4,pc+1:4,pc+2:3,pc+2:1 x 5,ii+n:3
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(8);
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        int value = memory.read(address) & 0xFF;
        int sum = (regs[REG_A] - value) & 0x1FF;
        int result = sum & 0xFF;
        flags = TABLE_SUB[result] | (TABLE_CHP[sum ^ value ^ regs[REG_A]]) | TABLE_XY[value];
        advanceCycles(3);
    }

    void I_ADD_A_N() {
        // pc:4,pc+1:3
        int value = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        int oldA = regs[REG_A];
        int sum = (oldA + value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
        Q = flags;
        advanceCycles(3);
    }

    void I_ADC_A_N() {
        // pc:4,pc+1:3
        int value = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        int oldA = regs[REG_A];
        int sum = (oldA + value + (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (TABLE_CHP[sum ^ value ^ oldA]) | TABLE_XY[regs[REG_A]];
        Q = flags;
        advanceCycles(3);
    }

    void I_SBC_A_N() {
        // pc:4,pc+1:3
        int value = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        int oldA = regs[REG_A];
        int sum = (oldA - value - (flags & FLAG_C)) & 0x1FF;
        regs[REG_A] = sum & 0xFF;
        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA] | TABLE_XY[regs[REG_A]];
        Q = flags;
        advanceCycles(3);
    }

    void I_AND_N() {
        // pc:4,pc+1:3
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_A] = (regs[REG_A] & tmp) & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | FLAG_H | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        Q = flags;
        advanceCycles(3);
    }

    void I_XOR_N() {
        // pc:4,pc+1:3
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_A] = ((regs[REG_A] ^ tmp) & 0xFF);
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        Q = flags;
        advanceCycles(3);
    }

    void I_OR_N() {
        // pc:4,pc+1:3
        int tmp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_A] = (regs[REG_A] | tmp) & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | TABLE_XY[regs[REG_A]];
        Q = flags;
        advanceCycles(3);
    }

    void I_CP_N() {
        // pc:4,pc+1:3
        int value = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        int result = sum & 0xFF;
        flags = (TABLE_SUB[result] | (TABLE_CHP[sum ^ value ^ oldA])) | TABLE_XY[value];
        Q = flags;
        advanceCycles(3);
    }

    void I_RLCA() {
        // pc:4
        regs[REG_A] = TABLE_RLCA[regs[REG_A]];
        flags = (flags & FLAG_SZP) | (regs[REG_A] & (FLAG_X | FLAG_Y | FLAG_C));
        Q = flags;
    }

    void I_EX_AF_AFF() {
        // pc:4
        regs[REG_A] ^= regs2[REG_A];
        regs2[REG_A] ^= regs[REG_A];
        regs[REG_A] ^= regs2[REG_A];
        flags ^= flags2;
        flags2 ^= flags;
        flags ^= flags2;
    }

    void I_LD_A_REF_BC() {
        // pc:4,ss:3
        int bc = regs[REG_B] << 8 | regs[REG_C];
        regs[REG_A] = memory.read(bc) & 0xFF;
        memptr = bc;
        advanceCycles(3);
    }

    void I_RRCA() {
        // pc:4
        flags = (flags & FLAG_SZP) | (regs[REG_A] & FLAG_C);
        regs[REG_A] = TABLE_RRCA[regs[REG_A]];
        flags |= TABLE_XY[regs[REG_A]];
        Q = flags;
    }

    void I_DJNZ() {
        // pc:5,pc+1:3,[pc+1:1 x 5]
        advanceCycles(1);
        byte addr = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(3);
        regs[REG_B] = (regs[REG_B] - 1) & 0xFF;
        if (regs[REG_B] != 0) {
            PC = (PC + addr) & 0xFFFF;
            memptr = PC;
            advanceCycles(5);
        }
    }

    void I_LD_REF_DE_A() {
        // pc:4,ss:3
        int de = regs[REG_D] << 8 | regs[REG_E];
        memory.write(de, (byte) regs[REG_A]);
        memptr = (regs[REG_A] << 8) | ((de + 1) & 0xFF);
        advanceCycles(3);
    }

    void I_RLA() {
        // pc:4
        int res = ((regs[REG_A] << 1) | (flags & FLAG_C)) & 0xFF;
        int flagC = ((regs[REG_A] & 0x80) == 0x80) ? FLAG_C : 0;
        regs[REG_A] = res;
        flags = (flags & FLAG_SZP) | flagC | TABLE_XY[res];
        Q = flags;
    }

    void I_LD_A_REF_DE() {
        // pc:4,ss:3
        int de = regs[REG_D] << 8 | regs[REG_E];
        regs[REG_A] = memory.read(de) & 0xFF;
        memptr = (de + 1) & 0xFFFF;
        advanceCycles(3);
    }

    void I_RRA() {
        // pc:4
        int res = ((regs[REG_A] >>> 1) | (flags << 7)) & 0xFF;
        int flagC = regs[REG_A] & FLAG_C;
        flags = (flags & FLAG_SZP) | flagC | TABLE_XY[res];
        Q = flags;
        regs[REG_A] = res;
    }

    void I_DAA() {
        // pc:4
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
    }

    void I_CPL() {
        // pc:4
        regs[REG_A] = (~regs[REG_A]) & 0xFF;
        flags = (flags & (FLAG_SZP | FLAG_C))
                | FLAG_H | FLAG_N
                | (regs[REG_A] & FLAG_XY);
        Q = flags;
    }

    void I_SCF() {
        // pc:4
        flags = (flags & FLAG_SZP) | (((lastQ ^ flags) | regs[REG_A]) & FLAG_XY) | FLAG_C;
        Q = flags;
    }

    void I_CCF() {
        // pc:4
        flags = (flags & FLAG_SZP)
                | ((flags & FLAG_C) == 0 ? FLAG_C : FLAG_H)
                | (((lastQ ^ flags) | regs[REG_A]) & FLAG_XY);
        Q = flags;
    }

    void I_RET() {
        // pc:4,sp:3,sp+1:3
        int tmp = memory.read(SP) & 0xFF;
        advanceCycles(3);
        PC = ((memory.read((SP + 1) & 0xFFFF) << 8) | tmp) & 0xFFFF;
        memptr = PC;
        SP = (SP + 2) & 0xFFFF;
        advanceCycles(3);
    }

    void I_EXX() {
        // pc:4
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
    }

    void I_EX_REF_SP_HL() {
        // pc:4,sp:3,sp+1:4,sp(write):3,sp+1(write):3,sp+1(write):1 x 2
        byte value = memory.read(SP);
        advanceCycles(3);
        int SP_plus1 = (SP + 1) & 0xFFFF;
        byte value1 = memory.read(SP_plus1);
        advanceCycles(4);
        memory.write(SP, (byte) regs[REG_L]);
        advanceCycles(3);
        memory.write(SP_plus1, (byte) regs[REG_H]);
        regs[REG_L] = value & 0xFF;
        regs[REG_H] = value1 & 0xFF;
        memptr = (regs[REG_H] << 8) | regs[REG_L];
        advanceCycles(5);
    }

    void I_JP_REF_HL() {
        // pc:4
        PC = ((regs[REG_H] << 8) | regs[REG_L]);
    }

    void I_EX_DE_HL() {
        // pc:4
        regs[REG_D] ^= regs[REG_H];
        regs[REG_H] ^= regs[REG_D];
        regs[REG_D] ^= regs[REG_H];

        regs[REG_E] ^= regs[REG_L];
        regs[REG_L] ^= regs[REG_E];
        regs[REG_E] ^= regs[REG_L];
    }

    void I_DI() {
        // pc:4
        IFF[0] = IFF[1] = false;
    }

    void I_LD_SP_HL() {
        // pc:6
        SP = ((regs[REG_H] << 8) | regs[REG_L]);
        advanceCycles(2);
    }

    void I_EI() {
        // pc:4
        // https://www.smspower.org/forums/2511-LDILDIRLDDLDDRCRCInZEXALL
        // interrupts are not allowed until after the *next* instruction after EI.
        // This is used to prevent interrupts from occurring between an EI/RETI pair used at the end of interrupt handlers.
        if (!IFF[0]) {
            interruptSkip = true;
        }
        IFF[0] = IFF[1] = true;
    }

    void I_IN_B_REF_C() {
        I_IN_R_REF_C(REG_B);
    }

    void I_IN_C_REF_C() {
        I_IN_R_REF_C(REG_C);
    }

    void I_IN_D_REF_C() {
        I_IN_R_REF_C(REG_D);
    }

    void I_IN_E_REF_C() {
        I_IN_R_REF_C(REG_E);
    }

    void I_IN_H_REF_C() {
        I_IN_R_REF_C(REG_H);
    }

    void I_IN_L_REF_C() {
        I_IN_R_REF_C(REG_L);
    }

    void I_IN_A_REF_C() {
        I_IN_R_REF_C(REG_A);
    }

    void I_IN_R_REF_C(int reg) {
        // pc:4,pc+1:4,IO
        int bc = (regs[REG_B] << 8) | regs[REG_C];
        memptr = (bc + 1) & 0xFFFF;
        int tmp = context.readIO(bc) & 0xFF;
        regs[reg] = tmp;
        flags = TABLE_SZ[tmp] | TABLE_XY[tmp] | PARITY_TABLE[tmp] | (flags & FLAG_C);
        Q = flags;
        advanceCycles(4);
    }


    void I_OUT_REF_C_B() {
        I_OUT_REF_C_R(regs[REG_B]);
    }

    void I_OUT_REF_C_C() {
        I_OUT_REF_C_R(regs[REG_C]);
    }

    void I_OUT_REF_C_D() {
        I_OUT_REF_C_R(regs[REG_D]);
    }

    void I_OUT_REF_C_E() {
        I_OUT_REF_C_R(regs[REG_E]);
    }

    void I_OUT_REF_C_H() {
        I_OUT_REF_C_R(regs[REG_H]);
    }

    void I_OUT_REF_C_L() {
        I_OUT_REF_C_R(regs[REG_L]);
    }

    void I_OUT_REF_C_A() {
        I_OUT_REF_C_R(regs[REG_A]);
    }

    void I_OUT_REF_C_R(int reg) {
        // pc:4,pc+1:4,IO
        memptr = (regs[REG_B] << 8) | regs[REG_C];
        context.writeIO(memptr, (byte) reg);
        memptr++;
        advanceCycles(4);
    }

    void I_SBC_HL_BC() {
        I_SBC_HL_RP(regs[REG_B] << 8 | regs[REG_C]);
    }

    void I_SBC_HL_DE() {
        I_SBC_HL_RP(regs[REG_D] << 8 | regs[REG_E]);
    }

    void I_SBC_HL_HL() {
        I_SBC_HL_RP(regs[REG_H] << 8 | regs[REG_L]);
    }

    void I_SBC_HL_SP() {
        I_SBC_HL_RP(SP);
    }

    void I_SBC_HL_RP(int rp) {
        // pc:4,pc+1:11
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
        advanceCycles(7);
    }

    void I_ADC_HL_BC() {
        I_ADC_HL_RP(regs[REG_B] << 8 | regs[REG_C]);
    }

    void I_ADC_HL_DE() {
        I_ADC_HL_RP(regs[REG_D] << 8 | regs[REG_E]);
    }

    void I_ADC_HL_HL() {
        I_ADC_HL_RP(regs[REG_H] << 8 | regs[REG_L]);
    }

    void I_ADC_HL_SP() {
        I_ADC_HL_RP(SP);
    }

    void I_ADC_HL_RP(int rp) {
        // pc:4,pc+1:11
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
        advanceCycles(7);
    }

    void I_NEG() {
        // pc:4,pc+1:4
        int v = regs[REG_A];
        regs[REG_A] = 0;
        I_SUB(v);
    }

    void I_RETN() {
        // pc:4,pc+1:4,sp:3,sp+1:3
        IFF[0] = IFF[1];
        int tmp = memory.read(SP) & 0xFF;
        advanceCycles(3);
        PC = ((memory.read((SP + 1) & 0xFFFF) << 8) | tmp) & 0xFFFF;
        memptr = PC;
        SP = (SP + 2) & 0xffff;
        advanceCycles(3);
    }

    void I_IM_0() {
        // pc:4,pc+1:4
        interruptMode = 0;
    }

    void I_LD_I_A() {
        // pc:4,pc+1:5
        I = regs[REG_A];
        advanceCycles(1);
    }

    void I_RETI() {
        // pc:4,pc+1:4,sp:3,sp+1:3
        IFF[0] = IFF[1];
        int tmp = memory.read(SP) & 0xFF;
        advanceCycles(3);
        PC = ((memory.read((SP + 1) & 0xFFFF) << 8) | tmp) & 0xFFFF;
        memptr = PC;
        SP = (SP + 2) & 0xffff;
        advanceCycles(3);
    }

    void I_LD_R_A() {
        // pc:4,pc+1:5
        R = regs[REG_A];
        advanceCycles(1);
    }

    void I_IM_1() {
        // pc:4,pc+1:4
        interruptMode = 1;
    }

    void I_IM_2() {
        // pc:4,pc+1:4
        interruptMode = 2;
    }

    void I_LD_A_I() {
        // pc:4,pc+1:5
        regs[REG_A] = I & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (IFF[1] ? FLAG_PV : 0) | (flags & FLAG_C) | TABLE_XY[regs[REG_A]];
        Q = flags;
        advanceCycles(1);
    }

    void I_LD_A_R() {
        // pc:4,pc+1:5
        regs[REG_A] = R & 0xFF;
        flags = TABLE_SZ[regs[REG_A]] | (IFF[1] ? FLAG_PV : 0) | (flags & FLAG_C) | TABLE_XY[regs[REG_A]];
        Q = flags;
        advanceCycles(1);
    }

    void I_RRD() {
        // pc:4,pc+1:4,hl:3,hl:1 x 4,hl(write):3
        int regA = regs[REG_A] & 0x0F;
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        memptr = (hl + 1) & 0xFFFF;
        int value = memory.read(hl);
        advanceCycles(7);
        regs[REG_A] = ((regs[REG_A] & 0xF0) | (value & 0x0F));
        value = ((value >>> 4) & 0x0F) | (regA << 4);
        memory.write(hl, (byte) (value & 0xff));
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | (flags & FLAG_C) | TABLE_XY[regs[REG_A]];
        Q = flags;
        advanceCycles(3);
    }

    void I_RLD() {
        // pc:4,pc+1:4,hl:3,hl:1 x 4,hl(write):3
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int value = memory.read(hl);
        advanceCycles(7);
        memptr = (hl + 1) & 0xFFFF;
        int tmp1 = (value >>> 4) & 0x0F;
        value = ((value << 4) & 0xF0) | (regs[REG_A] & 0x0F);
        regs[REG_A] = ((regs[REG_A] & 0xF0) | tmp1);
        memory.write((regs[REG_H] << 8) | regs[REG_L], (byte) (value & 0xff));
        flags = TABLE_SZ[regs[REG_A]] | PARITY_TABLE[regs[REG_A]] | (flags & FLAG_C) | TABLE_XY[regs[REG_A]];
        Q = flags;
        advanceCycles(3);
    }

    void I_IN_REF_C() {
        int bc = (regs[REG_B] << 8) | regs[REG_C];
        memptr = (bc + 1) & 0xFFFF;
        int io = context.readIO(bc) & 0xFF;
        flags = TABLE_SZ[io] | PARITY_TABLE[io] | (flags & FLAG_C) | TABLE_XY[io];
        Q = flags;
        advanceCycles(4);
    }

    void I_OUT_REF_C_0() {
        int bc = (regs[REG_B] << 8) | regs[REG_C];
        memptr = (bc + 1) & 0xFFFF;
        context.writeIO(bc, (byte) 0);
        advanceCycles(4);
    }

    void I_CPI() {
        // pc:4,pc+1:4,hl:3,hl:1 x 5
        int a = regs[REG_A];
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int bc = (((regs[REG_B] << 8) | regs[REG_C]) - 1) & 0xFFFF;
        int n = memory.read(hl++) & 0xFF;
        int z = a - n;

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        int f = (a ^ n ^ z) & FLAG_H;
        n = z - (f >> 4);
        f |= (n << 4) & FLAG_Y;
        f |= n & FLAG_X;
        f |= TABLE_SZ[z & 0xFF];
        f |= (bc != 0) ? FLAG_PV : 0;
        flags = f | FLAG_N | (flags & FLAG_C);
        Q = flags;
        memptr = (memptr + 1) & 0xFFFF;
        advanceCycles(8);
    }

    void I_CPIR() {
        // pc:4,pc+1:4,hl:3,hl:1 x 5,[hl:1 x 5]
        int a = regs[REG_A];
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int bc = (((regs[REG_B] << 8) | regs[REG_C]) - 1) & 0xFFFF;
        int n = memory.read(hl++) & 0xFF;
        advanceCycles(8);
        int z = a - n;

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        int f = (a ^ n ^ z) & FLAG_H;
        n = z - (f >> 4);
        f |= (n << 4) & FLAG_Y;
        f |= n & FLAG_X;
        f |= TABLE_SZ[z & 0xFF];
        f |= (bc != 0) ? FLAG_PV : 0;
        flags = f | FLAG_N | (flags & FLAG_C);
        Q = flags;
        memptr = (memptr + 1) & 0xFFFF;

        if (bc != 0 && (z != 0)) {
            PC = (PC - 2) & 0xFFFF;
            memptr = (PC + 1) & 0xFFFF;
            flags = ((flags & (~FLAG_XY)) | ((PC >>> 8) & FLAG_XY)) & 0xFF;
            advanceCycles(5);
        }
    }

    void I_CPD() {
        // pc:4,pc+1:4,hl:3,hl:1 x 5
        int a = regs[REG_A];
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int bc = (((regs[REG_B] << 8) | regs[REG_C]) - 1) & 0xFFFF;

        int n = memory.read(hl--) & 0xFF;

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        int z = a - n;
        int f = (a ^ n ^ z) & FLAG_H;
        n = z - (f >>> 4);
        f |= (n << 4) & FLAG_Y;
        f |= n & FLAG_X;
        f |= TABLE_SZ[z & 0xFF];
        f |= (bc != 0) ? FLAG_PV : 0;
        flags = f | FLAG_N | (flags & FLAG_C);
        Q = flags;
        memptr = (memptr - 1) & 0xFFFF;
        advanceCycles(8);
    }

    void I_CPDR() {
        // pc:4,pc+1:4,hl:3,hl:1 x 5,[hl:1 x 5]
        int a = regs[REG_A];
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int bc = (((regs[REG_B] << 8) | regs[REG_C]) - 1) & 0xFFFF;

        int n = memory.read(hl--) & 0xFF;
        advanceCycles(8);

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        int z = a - n;
        int f = (a ^ n ^ z) & FLAG_H;
        n = z - (f >>> 4);
        f |= (n << 4) & FLAG_Y;
        f |= n & FLAG_X;
        f |= TABLE_SZ[z & 0xFF];
        f |= (bc != 0) ? FLAG_PV : 0;
        flags = f | FLAG_N | (flags & FLAG_C);
        Q = flags;
        memptr = (memptr - 1) & 0xFFFF;

        if (bc != 0 && (z != 0)) {
            PC = (PC - 2) & 0xFFFF;
            memptr = (PC + 1) & 0xFFFF;
            flags = ((flags & (~FLAG_XY)) | ((PC >>> 8) & FLAG_XY)) & 0xFF;
            advanceCycles(5);
        }
    }

    void I_LDD() {
        // pc:4,pc+1:4,hl:3,de:3,de:1 x 2
        int bc = (((regs[REG_B] << 8) | regs[REG_C]) - 1) & 0xFFFF;
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int de = (regs[REG_D] << 8) | regs[REG_E];

        byte io = memory.read(hl--);
        advanceCycles(3);
        memory.write(de--, io);

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_D] = (de >>> 8) & 0xFF;
        regs[REG_E] = de & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        int result = regs[REG_A] + io;

        flags = (flags & FLAG_SZC) |
                ((result << 4) & FLAG_Y) | (result & FLAG_X) | (bc != 0 ? FLAG_PV : 0);
        Q = flags;
        advanceCycles(5);
    }

    void I_LDDR() {
        // pc:4,pc+1:4,hl:3,de:3,de:1 x 2,[de:1 x 5]
        int bc = (((regs[REG_B] << 8) | regs[REG_C]) - 1) & 0xFFFF;
        int de = (regs[REG_D] << 8) | regs[REG_E];
        int hl = (regs[REG_H] << 8) | regs[REG_L];

        byte io = memory.read(hl--);
        advanceCycles(3);
        memory.write(de--, io);
        advanceCycles(5);

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_D] = (de >>> 8) & 0xFF;
        regs[REG_E] = de & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        int result = regs[REG_A] + io;

        flags = (flags & FLAG_SZC) |
                ((result << 4) & FLAG_Y) | (result & FLAG_X) | (bc != 0 ? FLAG_PV : 0);
        Q = flags;

        // https://github.com/hoglet67/Z80Decoder/wiki/Undocumented-Flags#interrupted-block-instructions
        if (bc != 0) {
            PC = (PC - 2) & 0xFFFF;
            memptr = (PC + 1) & 0xFFFF;
            flags = ((flags & (~FLAG_XY)) | ((PC >>> 8) & FLAG_XY)) & 0xFF;
            advanceCycles(5);
        }
    }

    void I_LDI() {
        // pc:4,pc+1:4,hl:3,de:3,de:1 x 2
        int bc = (regs[REG_B] << 8) | regs[REG_C];
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int de = (regs[REG_D] << 8) | regs[REG_E];

        byte io = memory.read(hl++);
        advanceCycles(3);
        memory.write(de++, io);

        bc = (bc - 1) & 0xFFFF;

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_D] = (de >>> 8) & 0xFF;
        regs[REG_E] = de & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        int result = regs[REG_A] + (io & 0xFF);

        flags = (flags & FLAG_SZC) |
                ((result << 4) & FLAG_Y) | (result & FLAG_X) | (bc != 0 ? FLAG_PV : 0);
        Q = flags;
        advanceCycles(5);
    }

    void I_LDIR() {
        // pc:4,pc+1:4,hl:3,de:3,de:1 x 2,[de:1 x 5]
        int bc = (regs[REG_B] << 8) | regs[REG_C];
        int de = (regs[REG_D] << 8) | regs[REG_E];
        int hl = (regs[REG_H] << 8) | regs[REG_L];

        byte io = memory.read(hl++);
        advanceCycles(3);
        memory.write(de++, io);
        advanceCycles(5);

        bc = (bc - 1) & 0xFFFF;

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_D] = (de >>> 8) & 0xFF;
        regs[REG_E] = de & 0xFF;
        regs[REG_B] = (bc >>> 8) & 0xFF;
        regs[REG_C] = bc & 0xFF;

        int result = regs[REG_A] + (io & 0xFF);

        flags = (flags & FLAG_SZC) |
                ((result << 4) & FLAG_Y) | (result & FLAG_X) | (bc != 0 ? FLAG_PV : 0);
        Q = flags;

        // https://github.com/hoglet67/Z80Decoder/wiki/Undocumented-Flags#interrupted-block-instructions
        if (bc != 0) {
            PC = (PC - 2) & 0xFFFF;
            memptr = (PC + 1) & 0xFFFF;
            flags = (flags & (~FLAG_XY) | ((PC >>> 8) & FLAG_XY)) & 0xFF;
            advanceCycles(5);
        }
    }

    void I_INI() {
        // pc:4,pc+1:5,IO,hl:3
        advanceCycles(1);
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int bc = (regs[REG_B] << 8) | regs[REG_C];
        byte io = context.readIO(bc);
        advanceCycles(4);
        memory.write(hl++, io);

        int decB = (regs[REG_B] - 1) & 0xFF;
        regs[REG_B] = decB;
        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;

        // from zxpoly
        int tmp = (io + regs[REG_C] + 1) & 0xFF;
        flags = ((io & 0x80) >>> 6) // N
                | (tmp < (io & 0xFF) ? (FLAG_H | FLAG_C) : 0)
                | TABLE_SZ[decB]
                | TABLE_XY[decB]
                | PARITY_TABLE[(tmp & 7) ^ decB];
        Q = flags;
        memptr = (bc + 1) & 0xFFFF;
        advanceCycles(3);
    }

    void I_INIR() {
        // pc:4,pc+1:5,IO,hl:3,[hl:1 x 5]
        advanceCycles(1);
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int bc = (regs[REG_B] << 8) | regs[REG_C];
        byte io = context.readIO(bc);
        advanceCycles(4);
        memory.write(hl++, io);
        advanceCycles(3);

        int decB = (regs[REG_B] - 1) & 0xFF;
        regs[REG_B] = decB;
        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;

        // from zxpoly
        int tmp = (io + regs[REG_C] + 1) & 0xFF;
        flags = ((io & 0x80) >>> 6) // N
                | (tmp < (io & 0xFF) ? (FLAG_H | FLAG_C) : 0)
                | TABLE_SZ[decB]
                | TABLE_XY[decB]
                | PARITY_TABLE[(tmp & 7) ^ decB];
        Q = flags;
        memptr = (bc + 1) & 0xFFFF;

        if (decB == 0) {
            return;
        }
        PC = (PC - 2) & 0xFFFF;
        flags = ((flags & ~FLAG_XY) | ((PC >>> 8) & FLAG_XY)) & 0xFF;

        int flagP = flags & FLAG_PV;
        int flagH = flags & FLAG_H;

        if ((flags & FLAG_C) == FLAG_C) {
            if ((io & 0x80) == 0) {
                flagP = flagP ^ PARITY_TABLE[(decB + 1) & 0x7] ^ FLAG_PV;
                flagH = (decB & 0x0F) == 0x0F ? FLAG_H : 0;
            } else {
                flagP = flagP ^ PARITY_TABLE[(decB - 1) & 0x7] ^ FLAG_PV;
                flagH = (decB & 0x0F) == 0 ? FLAG_H : 0;
            }
        } else {
            flagP = flagP ^ PARITY_TABLE[decB & 0x07] ^ FLAG_PV;
        }
        flags = ((flags & ~(FLAG_PV | FLAG_H)) | flagP | flagH) & 0xFF;
        advanceCycles(5);
    }

    void I_IND() {
        // pc:4,pc+1:5,IO,hl:3
        advanceCycles(1);
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int bc = (regs[REG_B] << 8) | regs[REG_C];
        byte io = context.readIO(bc);
        advanceCycles(4);
        memory.write(hl--, io);

        int decB = (regs[REG_B] - 1) & 0xFF;
        regs[REG_B] = decB;
        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;

        // from zxpoly
        int tmp = (io + regs[REG_C] - 1) & 0xFF;
        flags = ((io & 0x80) >>> 6) // N
                | (tmp < (io & 0xFF) ? (FLAG_H | FLAG_C) : 0)
                | TABLE_SZ[decB]
                | TABLE_XY[decB]
                | PARITY_TABLE[(tmp & 7) ^ decB];
        Q = flags;
        memptr = (bc + 1) & 0xFFFF;
        advanceCycles(3);
    }

    void I_INDR() {
        // pc:4,pc+1:5,IO,hl:3,[hl:1 x 5]
        advanceCycles(1);
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        int bc = (regs[REG_B] << 8) | regs[REG_C];
        byte io = context.readIO(bc);
        advanceCycles(4);
        memory.write(hl--, io);
        advanceCycles(3);

        int decB = (regs[REG_B] - 1) & 0xFF;
        regs[REG_B] = decB;
        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;

        // from zxpoly
        int tmp = (io + regs[REG_C] - 1) & 0xFF;
        flags = ((io & 0x80) >>> 6) // N
                | (tmp < (io & 0xFF) ? (FLAG_H | FLAG_C) : 0)
                | TABLE_SZ[decB]
                | TABLE_XY[decB]
                | PARITY_TABLE[(tmp & 7) ^ decB];
        Q = flags;
        memptr = (bc + 1) & 0xFFFF;

        if (decB == 0) {
            return;
        }
        PC = (PC - 2) & 0xFFFF;
        flags = ((flags & ~FLAG_XY) | ((PC >>> 8) & FLAG_XY)) & 0xFF;

        int flagP = flags & FLAG_PV;
        int flagH = flags & FLAG_H;

        if ((flags & FLAG_C) == FLAG_C) {
            if ((io & 0x80) == 0) {
                flagP = flagP ^ PARITY_TABLE[(decB + 1) & 0x7] ^ FLAG_PV;
                flagH = (decB & 0x0F) == 0x0F ? FLAG_H : 0;
            } else {
                flagP = flagP ^ PARITY_TABLE[(decB - 1) & 0x7] ^ FLAG_PV;
                flagH = (decB & 0x0F) == 0 ? FLAG_H : 0;
            }
        } else {
            flagP = flagP ^ PARITY_TABLE[decB & 0x07] ^ FLAG_PV;
        }
        flags = ((flags & ~(FLAG_PV | FLAG_H)) | flagP | flagH) & 0xFF;
        advanceCycles(5);
    }

    void I_OUTI() {
        // pc:4,pc+1:5,hl:3,IO
        advanceCycles(1);
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        byte value = memory.read(hl);
        advanceCycles(3);
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
        advanceCycles(4);
    }

    void I_OTIR() {
        // pc:4,pc+1:5,hl:3,IO,[hl:1 x 5]
        advanceCycles(1);
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        byte value = memory.read(hl);
        advanceCycles(3);
        int B = regs[REG_B];
        int decB = (B - 1) & 0xFF;

        context.writeIO((decB << 8) | regs[REG_C], value);
        advanceCycles(4);

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
            return;
        }
        PC = (PC - 2) & 0xFFFF;
        flags = (flags & ~FLAG_XY) | ((PC >>> 8) & FLAG_XY);

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
        advanceCycles(5);
    }

    void I_OUTD() {
        // pc:4,pc+1:5,hl:3,IO
        advanceCycles(1);
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        byte value = memory.read(hl--);
        advanceCycles(3);
        int decB = (regs[REG_B] - 1) & 0xFF;

        context.writeIO((decB << 8) | regs[REG_C], value);

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_B] = decB;

        // from zxpoly
        int tmp = (value + regs[REG_L]) & 0xFF;
        flags = ((value & 0x80) >>> 6) // N
                | (tmp < (value & 0xFF) ? (FLAG_H | FLAG_C) : 0)
                | TABLE_SZ[decB]
                | TABLE_XY[decB]
                | PARITY_TABLE[(tmp & 7) ^ decB];
        Q = flags;
        memptr = (((decB << 8) | regs[REG_C]) - 1) & 0xFFFF;
        advanceCycles(4);
    }

    void I_OTDR() {
        // pc:4,pc+1:5,hl:3,IO,[hl:1 x 5]
        advanceCycles(1);
        int hl = (regs[REG_H] << 8) | regs[REG_L];
        byte value = memory.read(hl--);
        advanceCycles(3);
        int decB = (regs[REG_B] - 1) & 0xFF;

        context.writeIO((decB << 8) | regs[REG_C], value);
        advanceCycles(4);

        regs[REG_H] = (hl >>> 8) & 0xFF;
        regs[REG_L] = hl & 0xFF;
        regs[REG_B] = decB;

        // from zxpoly
        int tmp = (value + regs[REG_L]) & 0xFF;
        flags = ((value & 0x80) >>> 6) // N
                | (tmp < (value & 0xFF) ? (FLAG_H | FLAG_C) : 0)
                | TABLE_SZ[decB]
                | TABLE_XY[decB]
                | PARITY_TABLE[(tmp & 7) ^ decB];
        Q = flags;
        memptr = (((decB << 8) | regs[REG_C]) - 1) & 0xFFFF;

        if (decB == 0) {
            return;
        }
        PC = (PC - 2) & 0xFFFF;

        flags = ((flags & (~FLAG_XY)) | ((PC >>> 8) & FLAG_XY)) & 0xFF;

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
        flags = ((flags & ~(FLAG_PV | FLAG_H)) | flagP | flagH) & 0xFF;
        advanceCycles(5);
    }

    void I_LD_REF_NN_BC() {
        int addr = memory.read(PC) & 0xFF;
        advanceCycles(3);
        addr = ((memory.read((PC + 1) & 0xFFFF) << 8) | addr) & 0xFFFF;
        advanceCycles(3);
        memptr = (addr + 1) & 0xFFFF;
        PC = (PC + 2) & 0xFFFF;

        memory.write(addr, (byte) regs[REG_C]);
        advanceCycles(3);
        memory.write(memptr, (byte) regs[REG_B]);
        advanceCycles(3);
    }

    void I_LD_REF_NN_DE() {
        int addr = memory.read(PC) & 0xFF;
        advanceCycles(3);
        addr = ((memory.read((PC + 1) & 0xFFFF) << 8) | addr) & 0xFFFF;
        advanceCycles(3);
        memptr = (addr + 1) & 0xFFFF;
        PC = (PC + 2) & 0xFFFF;

        memory.write(addr, (byte) regs[REG_E]);
        advanceCycles(3);
        memory.write(memptr, (byte) regs[REG_D]);
        advanceCycles(3);
    }

    void I_LD_REF_NN_HL() {
        //pc:4,pc+1:3,pc+2:3,nn:3,nn+1:3
        int addr = memory.read(PC) & 0xFF;
        advanceCycles(3);
        addr = ((memory.read((PC + 1) & 0xFFFF) << 8) | addr) & 0xFFFF;
        advanceCycles(3);
        memptr = (addr + 1) & 0xFFFF;
        PC = (PC + 2) & 0xFFFF;

        memory.write(addr, (byte) regs[REG_L]);
        advanceCycles(3);
        memory.write(memptr, (byte) regs[REG_H]);
        advanceCycles(3);
    }

    void I_ED_LD_REF_NN_HL() {
        I_LD_REF_NN_HL();
    }

    void I_LD_REF_NN_SP() {
        int addr = memory.read(PC) & 0xFF;
        advanceCycles(3);
        addr = ((memory.read((PC + 1) & 0xFFFF) << 8) | addr) & 0xFFFF;
        advanceCycles(3);
        memptr = (addr + 1) & 0xFFFF;
        PC = (PC + 2) & 0xFFFF;

        memory.write(addr, (byte) (SP & 0xFF));
        advanceCycles(3);
        memory.write(memptr, (byte) (SP >>> 8));
        advanceCycles(3);
    }

    void I_LD_REF_NN_A() {
        // pc:4,pc+1:3,pc+2:3,nn:3
        int addr = memory.read(PC) & 0xFF;
        advanceCycles(3);
        addr = ((memory.read((PC + 1) & 0xFFFF) << 8) | addr) & 0xFFFF;
        advanceCycles(3);
        PC = (PC + 2) & 0xFFFF;
        memptr = (regs[REG_A] << 8) | ((addr + 1) & 0xFF);
        //	Note for *BM1: MEMPTR_low = (addr + 1) & #FF,  MEMPTR_hi = 0
        memory.write(addr, (byte) regs[REG_A]);
        advanceCycles(3);
    }

    void I_LD_REF_NN_IX() {
        I_LD_REF_NN_XY(IX);
    }

    void I_LD_REF_NN_IY() {
        I_LD_REF_NN_XY(IY);
    }

    void I_LD_REF_NN_XY(int xy) {
        int address = memory.read(PC) & 0xFF;
        advanceCycles(3);
        address = ((memory.read((PC + 1) & 0xFFFF) << 8) | address) & 0xFFFF;
        PC = (PC + 2) & 0xFFFF;
        advanceCycles(3);

        memory.write(address, (byte) (xy & 0xFF));
        advanceCycles(3);
        memptr = (address + 1) & 0xFFFF;
        memory.write(memptr, (byte) (xy >>> 8));
        advanceCycles(3);
    }

    void I_LD_RP_REF_NN() {
        //pc:4,pc+1:4,pc+2:3,pc+3:3,nn:3,nn+1:3
        int addr = memory.read(PC) & 0xFF;
        advanceCycles(3);
        addr = ((memory.read((PC + 1) & 0xFFFF) << 8) | addr) & 0xFFFF;
        advanceCycles(3);
        memptr = (addr + 1) & 0xFFFF;
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = memory.read(addr) & 0xFF;
        advanceCycles(3);
        tmp1 = ((memory.read(memptr) << 8) | tmp1) & 0xFFFF;
        putPair((lastOpcode >>> 4) & 3, tmp1);
        advanceCycles(3);
    }

    void I_JR_CC_N() {
        // pc:4,pc+1:3,[pc+1:1 x 5]
        byte offset = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(3);

        if (getCC1((lastOpcode >>> 3) & 3)) {
            PC = (PC + offset) & 0xFFFF;
            memptr = PC;
            advanceCycles(5);
        }
    }

    void I_JR_N() {
        // pc:4,pc+1:3,[pc+1:1 x 5]
        int addr = memory.read(PC);
        PC = (PC + 1 + (byte) addr) & 0xFFFF;
        memptr = PC;
        advanceCycles(8);
    }

    void I_OUT_REF_N_A() {
        // pc:4,pc+1:3,IO
        int port = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(3);
        memptr = (regs[REG_A] << 8) | ((port + 1) & 0xFF);
        //	Note for *BM1: MEMPTR_low = (port + 1) & #FF,  MEMPTR_hi = 0
        context.writeIO((regs[REG_A] << 8) | port, (byte) regs[REG_A]);
        advanceCycles(4);
    }

    void I_IN_A_REF_N() {
        // pc:4,pc+1:3,IO
        int port = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(3);
        //_memPtr = (A << 8) + _memory.ReadByte(PC) + _memory.ReadByte(PC + 1) * 256 + 1;
        int aport = (regs[REG_A] << 8) | port;
        regs[REG_A] = context.readIO(aport) & 0xFF;
        memptr = (aport + 1) & 0xFFFF;
        advanceCycles(4); // I/O
    }

    void I_SUB_N() {
        int value = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;

        int oldA = regs[REG_A];
        int sum = (oldA - value) & 0x1FF;
        regs[REG_A] = sum & 0xFF;

        flags = TABLE_SUB[regs[REG_A]] | TABLE_CHP[sum ^ value ^ oldA] | TABLE_XY[regs[REG_A]];
        advanceCycles(3);
    }

    void I_JP_CC_NN() {
        int addr = memory.read(PC) & 0xFF;
        advanceCycles(3);
        addr = ((memory.read((PC + 1) & 0xFFFF) << 8) | addr) & 0xFFFF;
        advanceCycles(3);

        memptr = addr;
        PC = (PC + 2) & 0xFFFF;

        int tmp1 = (lastOpcode >>> 3) & 7;
        if ((flags & CONDITION[tmp1]) == CONDITION_VALUES[tmp1]) {
            PC = addr;
        }
    }

    void I_CALL_CC_NN() {
        // pc:4,pc+1:3,pc+2:3,[pc+2:1,sp-1:3,sp-2:3]
        int addr = memory.read(PC) & 0xFF;
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(3);
        addr = ((memory.read(PC) << 8) | addr) & 0xFFFF;
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(4);

        memptr = addr;

        int tmp1 = (lastOpcode >>> 3) & 7;
        if ((flags & CONDITION[tmp1]) == CONDITION_VALUES[tmp1]) {
            SP = (SP - 2) & 0xffff;

            memory.write((SP + 1) & 0xFFFF, (byte) (PC >>> 8));
            advanceCycles(3);
            memory.write(SP, (byte) (PC & 0xFF));
            PC = addr;
            advanceCycles(3);
        }
    }

    void I_LD_HL_REF_NN() {
        // pc:4,pc+1:3,pc+2:3,nn:3,nn+1:3
        int addr = memory.read(PC) & 0xFF;
        advanceCycles(3);
        addr = ((memory.read((PC + 1) & 0xFFFF) << 8) | addr) & 0xFFFF;
        advanceCycles(3);
        PC = (PC + 2) & 0xFFFF;
        int tmp1 = memory.read(addr) & 0xFF;
        advanceCycles(3);
        tmp1 = ((memory.read((addr + 1) & 0xFFFF) << 8) | tmp1) & 0xFFFF;
        memptr = (addr + 1) & 0xFFFF;
        regs[REG_H] = (tmp1 >>> 8) & 0xFF;
        regs[REG_L] = tmp1 & 0xFF;
        advanceCycles(3);
    }

    void I_LD_A_REF_NN() {
        //pc:4,pc+1:3,pc+2:3,nn:3
        int addr = memory.read(PC) & 0xFF;
        advanceCycles(3);
        addr = ((memory.read((PC + 1) & 0xFFFF) << 8) | addr) & 0xFFFF;
        advanceCycles(3);
        PC = (PC + 2) & 0xFFFF;
        memptr = (addr + 1) & 0xFFFF;
        regs[REG_A] = (memory.read(addr) & 0xff);
        advanceCycles(3);
    }

    void I_JP_NN() {
        //pc:4,pc+1:3,pc+2:3
        int tmp = memory.read(PC) & 0xFF;
        advanceCycles(3);
        PC = ((memory.read((PC + 1) & 0xFFFF) << 8) | tmp) & 0xFFFF;
        memptr = PC;
        advanceCycles(3);
    }

    void I_CALL_NN() {
        // pc:4,pc+1:3,pc+2:3,[pc+2:1,sp-1:3,sp-2:3]
        int addr = memory.read(PC) & 0xFF;
        advanceCycles(3);
        addr = ((memory.read((PC + 1) & 0xFFFF) << 8) | addr) & 0xFFFF;
        advanceCycles(4);

        PC = (PC + 2) & 0xFFFF;
        SP = (SP - 2) & 0xffff;

        memory.write((SP + 1) & 0xFFFF, (byte) (PC >>> 8));
        advanceCycles(3);
        memory.write(SP, (byte) (PC & 0xFF));
        PC = addr;
        memptr = PC;
        advanceCycles(3);
    }

    void I_LD_B_B() {
    }

    void I_LD_B_C() {
        I_LD_R_R(REG_B, REG_C);
    }

    void I_LD_B_D() {
        I_LD_R_R(REG_B, REG_D);
    }

    void I_LD_B_E() {
        I_LD_R_R(REG_B, REG_E);
    }

    void I_LD_B_H() {
        I_LD_R_R(REG_B, REG_H);
    }

    void I_LD_B_L() {
        I_LD_R_R(REG_B, REG_L);
    }

    void I_LD_B_A() {
        I_LD_R_R(REG_B, REG_A);
    }

    void I_LD_B_REF_HL() {
        I_LD_R_REF_HL(REG_B);
    }

    void I_LD_B_IXH() {
        regs[REG_B] = (IX >>> 8);
    }

    void I_LD_B_IXL() {
        regs[REG_B] = (IX & 0xFF);
    }

    void I_LD_B_IYH() {
        regs[REG_B] = (IY >>> 8);
    }

    void I_LD_B_IYL() {
        regs[REG_B] = (IY & 0xFF);
    }

    void I_LD_B_REF_IX_D() {
        I_LD_R_REF_XY_D(REG_B, IX);
    }

    void I_LD_B_REF_IY_D() {
        I_LD_R_REF_XY_D(REG_B, IY);
    }

    void I_LD_C_B() {
        I_LD_R_R(REG_C, REG_B);
    }

    void I_LD_C_C() {
    }

    void I_LD_C_D() {
        I_LD_R_R(REG_C, REG_D);
    }

    void I_LD_C_E() {
        I_LD_R_R(REG_C, REG_E);
    }

    void I_LD_C_H() {
        I_LD_R_R(REG_C, REG_H);
    }

    void I_LD_C_L() {
        I_LD_R_R(REG_C, REG_L);
    }

    void I_LD_C_A() {
        I_LD_R_R(REG_C, REG_A);
    }

    void I_LD_C_REF_HL() {
        I_LD_R_REF_HL(REG_C);
    }

    void I_LD_C_IXH() {
        regs[REG_C] = (IX >>> 8);
    }

    void I_LD_C_IXL() {
        regs[REG_C] = (IX & 0xFF);
    }

    void I_LD_C_IYH() {
        regs[REG_C] = (IY >>> 8);
    }

    void I_LD_C_IYL() {
        regs[REG_C] = (IY & 0xFF);
    }

    void I_LD_C_REF_IX_D() {
        I_LD_R_REF_XY_D(REG_C, IX);
    }

    void I_LD_C_REF_IY_D() {
        I_LD_R_REF_XY_D(REG_C, IY);
    }

    void I_LD_D_B() {
        I_LD_R_R(REG_D, REG_B);
    }

    void I_LD_D_C() {
        I_LD_R_R(REG_D, REG_C);
    }

    void I_LD_D_D() {
    }

    void I_LD_D_E() {
        I_LD_R_R(REG_D, REG_E);
    }

    void I_LD_D_H() {
        I_LD_R_R(REG_D, REG_H);
    }

    void I_LD_D_L() {
        I_LD_R_R(REG_D, REG_L);
    }

    void I_LD_D_A() {
        I_LD_R_R(REG_D, REG_A);
    }

    void I_LD_D_REF_HL() {
        I_LD_R_REF_HL(REG_D);
    }

    void I_LD_D_IXH() {
        regs[REG_D] = (IX >>> 8);
    }

    void I_LD_D_IXL() {
        regs[REG_D] = (IX & 0xFF);
    }

    void I_LD_D_IYH() {
        regs[REG_D] = (IY >>> 8);
    }

    void I_LD_D_IYL() {
        regs[REG_D] = (IY & 0xFF);
    }

    void I_LD_D_REF_IX_D() {
        I_LD_R_REF_XY_D(REG_D, IX);
    }

    void I_LD_D_REF_IY_D() {
        I_LD_R_REF_XY_D(REG_D, IY);
    }

    void I_LD_E_B() {
        I_LD_R_R(REG_E, REG_B);
    }

    void I_LD_E_C() {
        I_LD_R_R(REG_E, REG_C);
    }

    void I_LD_E_D() {
        I_LD_R_R(REG_E, REG_D);
    }

    void I_LD_E_E() {
    }

    void I_LD_E_H() {
        I_LD_R_R(REG_E, REG_H);
    }

    void I_LD_E_L() {
        I_LD_R_R(REG_E, REG_L);
    }

    void I_LD_E_A() {
        I_LD_R_R(REG_E, REG_A);
    }

    void I_LD_E_REF_HL() {
        I_LD_R_REF_HL(REG_E);
    }

    void I_LD_E_IXH() {
        regs[REG_E] = (IX >>> 8);
    }

    void I_LD_E_IXL() {
        regs[REG_E] = (IX & 0xFF);
    }

    void I_LD_E_IYH() {
        regs[REG_E] = (IY >>> 8);
    }

    void I_LD_E_IYL() {
        regs[REG_E] = (IY & 0xFF);
    }

    void I_LD_E_REF_IX_D() {
        I_LD_R_REF_XY_D(REG_E, IX);
    }

    void I_LD_E_REF_IY_D() {
        I_LD_R_REF_XY_D(REG_E, IY);
    }

    void I_LD_H_B() {
        I_LD_R_R(REG_H, REG_B);
    }

    void I_LD_H_C() {
        I_LD_R_R(REG_H, REG_C);
    }

    void I_LD_H_D() {
        I_LD_R_R(REG_H, REG_D);
    }

    void I_LD_H_E() {
        I_LD_R_R(REG_H, REG_E);
    }

    void I_LD_H_H() {
    }

    void I_LD_H_L() {
        I_LD_R_R(REG_H, REG_L);
    }

    void I_LD_H_A() {
        I_LD_R_R(REG_H, REG_A);
    }

    void I_LD_H_REF_HL() {
        I_LD_R_REF_HL(REG_H);
    }

    void I_LD_IXH_B() {
        IX = ((regs[REG_B] << 8) | (IX & 0xFF)) & 0xFFFF;
    }

    void I_LD_IXH_C() {
        IX = ((regs[REG_C] << 8) | (IX & 0xFF)) & 0xFFFF;
    }

    void I_LD_IXH_D() {
        IX = ((regs[REG_D] << 8) | (IX & 0xFF)) & 0xFFFF;
    }

    void I_LD_IXH_E() {
        IX = ((regs[REG_E] << 8) | (IX & 0xFF)) & 0xFFFF;
    }

    void I_LD_IXH_IXH() {
    }

    void I_LD_IXH_IXL() {
        IX = (IX & 0xFF) | ((IX << 8) & 0xFF00);
    }

    void I_LD_IXH_A() {
        IX = ((regs[REG_A] << 8) | (IX & 0xFF)) & 0xFFFF;
    }

    void I_LD_IYH_B() {
        IY = ((regs[REG_B] << 8) | (IY & 0xFF)) & 0xFFFF;
    }

    void I_LD_IYH_C() {
        IY = ((regs[REG_C] << 8) | (IY & 0xFF)) & 0xFFFF;
    }

    void I_LD_IYH_D() {
        IY = ((regs[REG_D] << 8) | (IY & 0xFF)) & 0xFFFF;
    }

    void I_LD_IYH_E() {
        IY = ((regs[REG_E] << 8) | (IY & 0xFF)) & 0xFFFF;
    }

    void I_LD_IYH_A() {
        IY = ((regs[REG_A] << 8) | (IY & 0xFF)) & 0xFFFF;
    }

    void I_LD_IYH_IYH() {
    }

    void I_LD_IYH_IYL() {
        IY = (IY & 0xFF) | ((IY << 8) & 0xFF00);
    }

    void I_LD_H_REF_IX_D() {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(3);
        int address = (IX + disp) & 0xFFFF;
        memptr = address;
        regs[REG_H] = (memory.read(address) & 0xFF);
        advanceCycles(8);
    }

    void I_LD_H_REF_IY_D() {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(3);
        int address = (IY + disp) & 0xFFFF;
        memptr = address;
        regs[REG_H] = (memory.read(address) & 0xFF);
        advanceCycles(8);
    }

    void I_LD_L_B() {
        I_LD_R_R(REG_L, REG_B);
    }

    void I_LD_L_C() {
        I_LD_R_R(REG_L, REG_C);
    }

    void I_LD_L_D() {
        I_LD_R_R(REG_L, REG_D);
    }

    void I_LD_L_E() {
        I_LD_R_R(REG_L, REG_E);
    }

    void I_LD_L_H() {
        I_LD_R_R(REG_L, REG_H);
    }

    void I_LD_L_L() {
    }

    void I_LD_L_A() {
        I_LD_R_R(REG_L, REG_A);
    }

    void I_LD_L_REF_HL() {
        I_LD_R_REF_HL(REG_L);
    }

    void I_LD_IXL_B() {
        IX = regs[REG_B] | (IX & 0xFF00);
    }

    void I_LD_IXL_C() {
        IX = regs[REG_C] | (IX & 0xFF00);
    }

    void I_LD_IXL_D() {
        IX = regs[REG_D] | (IX & 0xFF00);
    }

    void I_LD_IXL_E() {
        IX = regs[REG_E] | (IX & 0xFF00);
    }

    void I_LD_IXL_IXH() {
        IX = (IX & 0xFF00) | (IX >>> 8);
    }

    void I_LD_IXL_IXL() {
    }

    void I_LD_L_REF_IX_D() {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(3);
        int address = (IX + disp) & 0xFFFF;
        memptr = address;
        regs[REG_L] = (memory.read(address) & 0xFF);
        advanceCycles(8);
    }

    void I_LD_IXL_A() {
        IX = regs[REG_A] | (IX & 0xFF00);
    }

    void I_LD_IYL_B() {
        IY = regs[REG_B] | (IY & 0xFF00);
    }

    void I_LD_IYL_C() {
        IY = regs[REG_C] | (IY & 0xFF00);
    }

    void I_LD_IYL_D() {
        IY = regs[REG_D] | (IY & 0xFF00);
    }

    void I_LD_IYL_E() {
        IY = regs[REG_E] | (IY & 0xFF00);
    }

    void I_LD_IYL_IYH() {
        IY = (IY & 0xFF00) | (IY >>> 8);
    }

    void I_LD_IYL_IYL() {
    }

    void I_LD_IYL_A() {
        IY = regs[REG_A] | (IY & 0xFF00);
    }

    void I_LD_L_REF_IY_D() {
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(3);
        int address = (IY + disp) & 0xFFFF;
        memptr = address;
        regs[REG_L] = (memory.read(address) & 0xFF);
        advanceCycles(8);
    }

    void I_LD_A_B() {
        I_LD_R_R(REG_A, REG_B);
    }

    void I_LD_A_C() {
        I_LD_R_R(REG_A, REG_C);
    }

    void I_LD_A_D() {
        I_LD_R_R(REG_A, REG_D);
    }

    void I_LD_A_E() {
        I_LD_R_R(REG_A, REG_E);
    }

    void I_LD_A_H() {
        I_LD_R_R(REG_A, REG_H);
    }

    void I_LD_A_L() {
        I_LD_R_R(REG_A, REG_L);
    }

    void I_LD_A_A() {
    }

    void I_LD_A_REF_HL() {
        I_LD_R_REF_HL(REG_A);
    }

    void I_LD_A_IXH() {
        regs[REG_A] = (IX >>> 8);
    }

    void I_LD_A_IXL() {
        regs[REG_A] = (IX & 0xFF);
    }

    void I_LD_A_IYH() {
        regs[REG_A] = (IY >>> 8);
    }

    void I_LD_A_IYL() {
        regs[REG_A] = (IY & 0xFF);
    }

    void I_LD_A_REF_IX_D() {
        I_LD_R_REF_XY_D(REG_A, IX);
    }

    void I_LD_A_REF_IY_D() {
        I_LD_R_REF_XY_D(REG_A, IY);
    }

    void I_LD_REF_HL_B() {
        I_LD_REF_HL_R(REG_B);
    }

    void I_LD_REF_HL_C() {
        I_LD_REF_HL_R(REG_C);
    }

    void I_LD_REF_HL_D() {
        I_LD_REF_HL_R(REG_D);
    }

    void I_LD_REF_HL_E() {
        I_LD_REF_HL_R(REG_E);
    }

    void I_LD_REF_HL_H() {
        I_LD_REF_HL_R(REG_H);
    }

    void I_LD_REF_HL_L() {
        I_LD_REF_HL_R(REG_L);
    }

    void I_LD_REF_HL_A() {
        I_LD_REF_HL_R(REG_A);
    }

    void I_LD_R_R(int dstReg, int srcReg) {
        // pc:4
        regs[dstReg] = regs[srcReg];
    }

    void I_LD_R_REF_HL(int dstReg) {
        // pc:4,ss:3
        regs[dstReg] = memory.read((regs[REG_H] << 8) | regs[REG_L]) & 0xFF;
        advanceCycles(3);
    }

    void I_LD_REF_HL_R(int srcReg) {
        // pc:4,ss:3
        memory.write((regs[REG_H] << 8) | regs[REG_L], (byte) regs[srcReg]);
        advanceCycles(3);
    }

    void I_LD_R_REF_XY_D(int reg, int xy) {
        // pc:4,pc+1:4,pc+2:3,pc+2:1 x 5,ii+n:3
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(8);
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        regs[reg] = memory.read(address) & 0xFF;
        advanceCycles(3);
    }

    void I_LD_REF_IX_D_B() {
        I_LD_REF_XY_D_R(regs[REG_B], IX);
    }

    void I_LD_REF_IX_D_C() {
        I_LD_REF_XY_D_R(regs[REG_C], IX);
    }

    void I_LD_REF_IX_D_D() {
        I_LD_REF_XY_D_R(regs[REG_D], IX);
    }

    void I_LD_REF_IX_D_E() {
        I_LD_REF_XY_D_R(regs[REG_E], IX);
    }

    void I_LD_REF_IX_D_H() {
        I_LD_REF_XY_D_R(regs[REG_H], IX);
    }

    void I_LD_REF_IX_D_L() {
        I_LD_REF_XY_D_R(regs[REG_L], IX);
    }

    void I_LD_REF_IX_D_A() {
        I_LD_REF_XY_D_R(regs[REG_A], IX);
    }

    void I_LD_REF_IY_D_B() {
        I_LD_REF_XY_D_R(regs[REG_B], IY);
    }

    void I_LD_REF_IY_D_C() {
        I_LD_REF_XY_D_R(regs[REG_C], IY);
    }

    void I_LD_REF_IY_D_D() {
        I_LD_REF_XY_D_R(regs[REG_D], IY);
    }

    void I_LD_REF_IY_D_E() {
        I_LD_REF_XY_D_R(regs[REG_E], IY);
    }

    void I_LD_REF_IY_D_H() {
        I_LD_REF_XY_D_R(regs[REG_H], IY);
    }

    void I_LD_REF_IY_D_L() {
        I_LD_REF_XY_D_R(regs[REG_L], IY);
    }

    void I_LD_REF_IY_D_A() {
        I_LD_REF_XY_D_R(regs[REG_A], IY);
    }

    void I_LD_REF_XY_D_R(int value, int xy) {
        // pc:4,pc+1:4,pc+2:3,pc+2:1 x 5,ii+n:3
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(8);
        int address = (xy + disp) & 0xFFFF;
        memptr = address;
        memory.write(address, (byte) value);
        advanceCycles(3);
    }

    void I_HALT() {
        if (IFF[0]) {
            PC = (PC - 1) & 0xFFFF; // endless loop if interrupts are enabled
        } else {
            currentRunState = RunState.STATE_STOPPED_NORMAL;
        }
    }

    void I_RLC_R() {
        // pc:4,pc+1:4
        // reg==6 : pc:4,pc+1:4,hl:3,hl:1,hl(write):3
        int reg = lastOpcode & 7;
        int regValue = getReg(reg) & 0xFF;
        if (reg == 6) {
            advanceCycles(4);
        }

        int flagC = ((regValue & 0x80) != 0) ? FLAG_C : 0;
        regValue = ((regValue << 1) | (regValue >>> 7)) & 0xFF;
        putReg(reg, regValue);
        flags = TABLE_SZ[regValue] | PARITY_TABLE[regValue] | flagC | TABLE_XY[regValue];
        Q = flags;

        if (reg == 6) {
            advanceCycles(3);
        }
    }

    void I_RRC_R() {
        // pc:4,pc+1:4
        // reg==6 : pc:4,pc+1:4,hl:3,hl:1,hl(write):3
        int reg = lastOpcode & 7;
        int regValue = getReg(reg) & 0xFF;
        if (reg == 6) {
            advanceCycles(4);
        }

        int flagC = regValue & FLAG_C;
        regValue = ((regValue >>> 1) | (regValue << 7)) & 0xFF;
        putReg(reg, regValue);

        flags = TABLE_SZ[regValue] | PARITY_TABLE[regValue] | flagC | TABLE_XY[regValue];
        Q = flags;

        if (reg == 6) {
            advanceCycles(3);
        }
    }

    void I_RL_R() {
        // pc:4,pc+1:4
        // reg==6 : pc:4,pc+1:4,hl:3,hl:1,hl(write):3
        int reg = lastOpcode & 7;
        int regValue = getReg(reg) & 0xFF;
        if (reg == 6) {
            advanceCycles(4);
        }

        int flagC = ((regValue & 0x80) == 0x80) ? FLAG_C : 0;
        regValue = ((regValue << 1) | (flags & FLAG_C)) & 0xFF;
        putReg(reg, regValue);
        flags = TABLE_SZ[regValue] | PARITY_TABLE[regValue] | flagC | TABLE_XY[regValue];
        Q = flags;

        if (reg == 6) {
            advanceCycles(3);
        }
    }

    void I_RR_R() {
        // pc:4,pc+1:4
        // reg==6 : pc:4,pc+1:4,hl:3,hl:1,hl(write):3
        int reg = lastOpcode & 7;
        int regValue = getReg(reg) & 0xFF;
        if (reg == 6) {
            advanceCycles(4);
        }
        int flagC = regValue & FLAG_C;
        regValue = ((regValue >>> 1) | (flags << 7)) & 0xFF;
        putReg(reg, regValue);
        flags = TABLE_SZ[regValue] | PARITY_TABLE[regValue] | flagC | TABLE_XY[regValue];
        Q = flags;

        if (reg == 6) {
            advanceCycles(3);
        }
    }

    void I_SLA_R() {
        // pc:4,pc+1:4
        // reg==6 : pc:4,pc+1:4,hl:3,hl:1,hl(write):3
        int reg = lastOpcode & 7;
        int regValue = getReg(reg) & 0xFF;
        if (reg == 6) {
            advanceCycles(4);
        }

        int flagC = ((regValue & 0x80) == 0x80) ? FLAG_C : 0;
        regValue = (regValue << 1) & 0xFF;
        putReg(reg, regValue);
        flags = TABLE_SZ[regValue] | PARITY_TABLE[regValue] | flagC | TABLE_XY[regValue];
        Q = flags;

        if (reg == 6) {
            advanceCycles(3);
        }
    }

    void I_SRA_R() {
        // pc:4,pc+1:4
        // reg==6 : pc:4,pc+1:4,hl:3,hl:1,hl(write):3
        int reg = lastOpcode & 7;
        int regValue = getReg(reg) & 0xFF;
        if (reg == 6) {
            advanceCycles(4);
        }

        int flagC = regValue & FLAG_C;
        regValue = ((regValue >>> 1) | (regValue & 0x80)) & 0xFF;
        putReg(reg, regValue);
        flags = TABLE_SZ[regValue] | PARITY_TABLE[regValue] | flagC | TABLE_XY[regValue];
        Q = flags;

        if (reg == 6) {
            advanceCycles(3);
        }
    }

    void I_SLL_R() {
        // pc:4,pc+1:4
        // reg==6 : pc:4,pc+1:4,hl:3,hl:1,hl(write):3
        int reg = lastOpcode & 7;
        int regValue = getReg(reg) & 0xFF;
        if (reg == 6) {
            advanceCycles(4);
        }
        int flagC = ((regValue & 0x80) != 0) ? FLAG_C : 0;
        regValue = ((regValue << 1) | 0x01) & 0xFF;
        putReg(reg, regValue);
        flags = TABLE_SZ[regValue] | PARITY_TABLE[regValue] | flagC | TABLE_XY[regValue];
        Q = flags;

        if (reg == 6) {
            advanceCycles(3);
        }
    }

    void I_SRL_R() {
        // pc:4,pc+1:4
        // reg==6 : pc:4,pc+1:4,hl:3,hl:1,hl(write):3
        int reg = lastOpcode & 7;
        int regValue = getReg(reg) & 0xFF;
        if (reg == 6) {
            advanceCycles(4);
        }
        int flagC = regValue & FLAG_C;
        regValue = (regValue >>> 1) & 0xFF;
        putReg(reg, regValue);
        flags = TABLE_SZ[regValue] | PARITY_TABLE[regValue] | flagC | TABLE_XY[regValue];
        Q = flags;

        if (reg == 6) {
            advanceCycles(3);
        }
    }

    void I_BIT_N_R() {
        // pc:4,pc+1:4
        // reg == 6: pc:4,pc+1:4,hl:3,hl:1
        int bit = (lastOpcode >>> 3) & 7;
        int reg = lastOpcode & 7;
        int regValue = getReg(reg) & 0xFF;
        if (reg == 6) {
            advanceCycles(4);
        }

        int result = (1 << bit) & regValue;

        flags = ((result != 0) ? (result & FLAG_S) : (FLAG_Z | FLAG_PV))
                | TABLE_XY[regValue]
                | FLAG_H
                | (flags & FLAG_C);

        if (reg == 6) {
            flags &= (~FLAG_X);
            flags &= (~FLAG_Y);
            flags |= ((memptr >>> 8) & FLAG_XY);
        }
        Q = flags;
    }

    void I_RES_N_R() {
        // pc:4,pc+1:4
        // reg==6 : pc:4,pc+1:4,hl:3,hl:1,hl(write):3
        int bit = (lastOpcode >>> 3) & 7;
        int reg = lastOpcode & 7;
        int regValue = getReg(reg) & 0xFF;
        if (reg == 6) {
            advanceCycles(4);
        }
        regValue = (regValue & (~(1 << bit)));
        putReg(reg, regValue);
        if (reg == 6) {
            advanceCycles(3);
        }
    }

    void I_SET_N_R() {
        // pc:4,pc+1:4
        // reg==6 : pc:4,pc+1:4,hl:3,hl:1,hl(write):3
        int bit = (lastOpcode >>> 3) & 7;
        int reg = lastOpcode & 7;
        int tmp1 = getReg(reg) & 0xFF;
        if (reg == 6) {
            advanceCycles(4);
        }
        tmp1 = (tmp1 | (1 << bit));
        putReg(reg, tmp1);
        if (reg == 6) {
            advanceCycles(3);
        }
    }

    void I_LD_IX_NN() {
        IX = memory.read(PC) & 0xFF;
        advanceCycles(3);
        IX = ((memory.read((PC + 1) & 0xFFFF) << 8) | IX) & 0xFFFF;
        PC = (PC + 2) & 0xFFFF;
        advanceCycles(3);
    }

    void I_LD_IY_NN() {
        IY = memory.read(PC) & 0xFF;
        advanceCycles(3);
        IY = ((memory.read((PC + 1) & 0xFFFF) << 8) | IY) & 0xFFFF;
        PC = (PC + 2) & 0xFFFF;
        advanceCycles(3);
    }

    void I_LD_IX_REF_NN() {
        IX = I_LD_II_REF_NN();
    }

    void I_LD_IY_REF_NN() {
        IY = I_LD_II_REF_NN();
    }

    int I_LD_II_REF_NN() {
        int tmp = memory.read(PC) & 0xFF;
        advanceCycles(3);
        tmp = ((memory.read((PC + 1) & 0xFFFF) << 8) | tmp) & 0xFFFF;
        advanceCycles(3);
        PC = (PC + 2) & 0xFFFF;

        int result = memory.read(tmp) & 0xFF;
        advanceCycles(3);
        result = ((memory.read((tmp + 1) & 0xFFFF) << 8) | result) & 0xFFFF;
        advanceCycles(3);

        return result;
    }

    void I_INC_REF_IX_N() {
        I_INC_REF_II_N(IX);
    }

    void I_INC_REF_IY_N() {
        I_INC_REF_II_N(IY);
    }

    void I_INC_REF_II_N(int special) {
        // pc:4,pc+1:4,pc+2:3,pc+2:1 x 5,ii+n:3,ii+n:1,ii+n(write):3
        byte disp = memory.read(PC);
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(8);
        int address = (special + disp) & 0xFFFF;
        int value = memory.read(address) & 0xFF;
        memptr = address;
        advanceCycles(4);

        int sum = (value + 1) & 0x1FF;
        int sumByte = sum & 0xFF;
        flags = TABLE_SZ[sumByte] | (TABLE_HP[sum ^ 1 ^ value]) | (flags & FLAG_C) | TABLE_XY[sumByte];

        memory.write(address, (byte) sumByte);
        advanceCycles(3);
    }

    void I_EX_REF_SP_IX() {
        int tmp = memory.read(SP) & 0xFF;
        advanceCycles(3);
        tmp = ((memory.read((SP + 1) & 0xFFFF) << 8) | tmp) & 0xFFFF;
        advanceCycles(3);
        int tmp1 = IX;
        IX = tmp;
        memptr = IX;

        memory.write(SP, (byte) (tmp1 & 0xFF));
        advanceCycles(3);
        memory.write((SP + 1) & 0xFFFF, (byte) (tmp1 >>> 8));
        advanceCycles(6);
    }

    void I_EX_REF_SP_IY() {
        int tmp = memory.read(SP) & 0xFF;
        advanceCycles(3);
        tmp = ((memory.read((SP + 1) & 0xFFFF) << 8) | tmp) & 0xFFFF;
        advanceCycles(3);

        int tmp1 = IY;
        IY = tmp;
        memptr = IY;

        memory.write(SP, (byte) (tmp1 & 0xFF));
        advanceCycles(3);
        memory.write((SP + 1) & 0xFFFF, (byte) (tmp1 >>> 8));
        advanceCycles(6);
    }

    void I_JP_REF_IX() {
        PC = IX;
    }

    void I_JP_REF_IY() {
        PC = IY;
    }

    void I_LD_SP_IX() {
        SP = IX;
        advanceCycles(2);
    }

    void I_LD_SP_IY() {
        SP = IY;
        advanceCycles(2);
    }

    void I_RLC_REF_IX_N_R(byte operand) {
        I_RLC_REF_II_N_R(operand, IX);
    }

    void I_RLC_REF_IY_N_R(byte operand) {
        I_RLC_REF_II_N_R(operand, IY);
    }

    void I_RLC_REF_II_N_R(byte operand, int special) {
        // 4 (dd), 4 (cb), 3 (operand), 5 (opcode),
        int addr = (special + operand) & 0xFFFF;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;
        advanceCycles(4);

        int c = ((addrValue & 0x80) != 0) ? FLAG_C : 0;
        int res = ((addrValue << 1) | (addrValue >>> 7)) & 0xFF;
        memory.write(addr, (byte) res);
        flags = TABLE_SZ[res] | PARITY_TABLE[res] | c | TABLE_XY[res];

        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = res & 0xFF;
        advanceCycles(3);
    }

    void I_RRC_REF_IX_N_R(byte operand) {
        I_RRC_REF_II_N_R(operand, IX);
    }

    void I_RRC_REF_IY_N_R(byte operand) {
        I_RRC_REF_II_N_R(operand, IY);
    }

    void I_RRC_REF_II_N_R(byte operand, int special) {
        int addr = (special + operand) & 0xffff;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;
        advanceCycles(4);

        int c = addrValue & 1;
        int res = (((addrValue >>> 1) & 0x7F) | (c << 7)) & 0xFF;
        memory.write(addr, (byte) (res & 0xFF));
        flags = TABLE_SZ[res] | EmulatorTables.PARITY_TABLE[res] | c | TABLE_XY[res];

        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = res & 0xFF;
        advanceCycles(3);
    }

    void I_RL_REF_IX_N_R(byte operand) {
        I_RL_REF_II_N_R(operand, IX);
    }

    void I_RL_REF_IY_N_R(byte operand) {
        I_RL_REF_II_N_R(operand, IY);
    }

    void I_RL_REF_II_N_R(byte operand, int special) {
        int addr = (special + operand) & 0xffff;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;
        advanceCycles(4);

        int c = (addrValue >>> 7) & 1;
        int res = ((((addrValue << 1) & 0xFF) | flags & FLAG_C) & 0xFF);
        memory.write(addr, (byte) (res & 0xFF));

        flags = TABLE_SZ[res] | EmulatorTables.PARITY_TABLE[res] | c | TABLE_XY[res];
        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = res & 0xFF;
        advanceCycles(3);
    }

    void I_RR_REF_IX_N_R(byte operand) {
        I_RR_REF_II_N_R(operand, IX);
    }

    void I_RR_REF_IY_N_R(byte operand) {
        I_RR_REF_II_N_R(operand, IY);
    }

    void I_RR_REF_II_N_R(byte operand, int special) {
        int addr = (special + operand) & 0xFFFF;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;
        advanceCycles(4);

        int c = addrValue & 1;
        int res = ((((addrValue >> 1) & 0xFF) | (flags & FLAG_C) << 7) & 0xFF);
        memory.write(addr, (byte) (res & 0xFF));

        flags = TABLE_SZ[res] | EmulatorTables.PARITY_TABLE[res] | c | TABLE_XY[res];
        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = res & 0xFF;
        advanceCycles(3);
    }

    void I_SLA_REF_IX_N_R(byte operand) {
        I_SLA_REF_II_N_R(operand, IX);
    }

    void I_SLA_REF_IY_N_R(byte operand) {
        I_SLA_REF_II_N_R(operand, IY);
    }

    void I_SLA_REF_II_N_R(byte operand, int special) {
        int addr = (special + operand) & 0xFFFF;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;
        advanceCycles(4);

        int c = (addrValue >>> 7) & 1;
        int res = (addrValue << 1) & 0xFE;
        memory.write(addr, (byte) res);
        flags = TABLE_SZ[res] | EmulatorTables.PARITY_TABLE[res] | c | TABLE_XY[res];

        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = res & 0xFF;
        advanceCycles(3);
    }

    void I_SRA_REF_IX_N_R(byte operand) {
        I_SRA_REF_II_N_R(operand, IX);
    }

    void I_SRA_REF_IY_N_R(byte operand) {
        I_SRA_REF_II_N_R(operand, IY);
    }

    void I_SRA_REF_II_N_R(byte operand, int special) {
        int addr = (special + operand) & 0xFFFF;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;
        advanceCycles(4);

        int c = addrValue & 1;
        int res = (addrValue >> 1) & 0xFF | (addrValue & 0x80);
        memory.write(addr, (byte) res);

        flags = TABLE_SZ[res] | EmulatorTables.PARITY_TABLE[res] | c | TABLE_XY[res];
        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = res & 0xFF;
        advanceCycles(3);
    }

    void I_SLL_REF_IX_N_R(byte operand) {
        I_SLL_REF_II_N_R(operand, IX);
    }

    void I_SLL_REF_IY_N_R(byte operand) {
        I_SLL_REF_II_N_R(operand, IY);
    }

    void I_SLL_REF_II_N_R(byte operand, int special) {
        int addr = (special + operand) & 0xFFFF;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;
        advanceCycles(4);

        int c = ((addrValue & 0x80) != 0) ? FLAG_C : 0;
        int res = ((addrValue << 1) | 0x01) & 0xFF;
        memory.write(addr, (byte) res);

        flags = TABLE_SZ[res] | PARITY_TABLE[res] | c | TABLE_XY[res];

        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = res;
        advanceCycles(3);
    }

    void I_SRL_REF_IX_N_R(byte operand) {
        I_SRL_REF_II_N_R(operand, IX);
    }

    void I_SRL_REF_IY_N_R(byte operand) {
        I_SRL_REF_II_N_R(operand, IY);
    }

    void I_SRL_REF_II_N_R(byte operand, int special) {
        int addr = (special + operand) & 0xFFFF;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;
        advanceCycles(4);

        int c = ((addrValue & 0x01) != 0) ? FLAG_C : 0;
        int res = (addrValue >>> 1) & 0xFF;
        memory.write(addr, (byte) res);

        flags = TABLE_SZ[res] | PARITY_TABLE[res] | c | TABLE_XY[res];
        // regs[6] is unused, so it's ok to set it
        regs[lastOpcode & 7] = res;
        advanceCycles(3);
    }

    void I_BIT_N_REF_IX_N(byte operand) {
        I_BIT_N_REF_II_N(operand, IX);
    }

    void I_BIT_N_REF_IY_N(byte operand) {
        I_BIT_N_REF_II_N(operand, IY);
    }

    void I_BIT_N_REF_II_N(byte operand, int special) {
        // pc+1:4,pc+2:3,pc+3:3,pc+3:1 x 2,ii+n:3,ii+n:1
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
        advanceCycles(4);
    }

    void I_RES_N_REF_IX_N_R(byte operand) {
        I_RES_N_REF_II_N_R(operand, IX);
    }

    void I_RES_N_REF_IY_N_R(byte operand) {
        I_RES_N_REF_II_N_R(operand, IY);
    }

    void I_RES_N_REF_II_N_R(byte operand, int special) {
        int bitNumber = (lastOpcode >>> 3) & 7;
        int addr = (special + operand) & 0xFFFF;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;
        advanceCycles(4);
        int res = (addrValue & (~(1 << bitNumber)));
        memory.write(addr, (byte) (res & 0xff));
        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = res & 0xFF;
        advanceCycles(3);
    }

    void I_SET_N_REF_IX_N_R(byte operand) {
        I_SET_N_REF_II_N_R(operand, IX);
    }

    void I_SET_N_REF_IY_N_R(byte operand) {
        I_SET_N_REF_II_N_R(operand, IY);
    }

    void I_SET_N_REF_II_N_R(byte operand, int special) {
        int bitNumber = (lastOpcode >>> 3) & 7;
        int addr = (special + operand) & 0xFFFF;
        memptr = addr;
        int addrValue = memory.read(addr) & 0xFF;
        advanceCycles(4);

        int res = (addrValue | (1 << bitNumber)) & 0xFF;
        memory.write(addr, (byte) res);

        // regs[6] is unused, so it's ok
        regs[lastOpcode & 7] = res;
        advanceCycles(3);
    }

    void I_LD_IXH_N() {
        IX = (((memory.read(PC) & 0xFF) << 8) | (IX & 0xFF)) & 0xFFFF;
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(3);
    }

    void I_LD_IYH_N() {
        IY = (((memory.read(PC) & 0xFF) << 8) | (IY & 0xFF)) & 0xFFFF;
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(3);
    }

    void I_LD_IXL_N() {
        IX = ((IX & 0xFF00) | (memory.read(PC) & 0xFF)) & 0xFFFF;
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(3);
    }

    void I_LD_IYL_N() {
        IY = ((IY & 0xFF00) | (memory.read(PC) & 0xFF)) & 0xFFFF;
        PC = (PC + 1) & 0xFFFF;
        advanceCycles(3);
    }
}
