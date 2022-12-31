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
import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.emustudio.plugins.cpu.intel8080.DispatchTables.DISPATCH_TABLE;

public class EmulatorEngine implements CpuEngine {
    public static final int REG_A = 7, REG_B = 0, REG_C = 1, REG_D = 2, REG_E = 3, REG_H = 4, REG_L = 5;
    public static final int FLAG_S = 0x80, FLAG_Z = 0x40, FLAG_AC = 0x10, FLAG_P = 0x4, FLAG_C = 0x1;
    private static final Logger LOGGER = LoggerFactory.getLogger(EmulatorEngine.class);
    private final static int[] CONDITION = new int[]{
            FLAG_Z, FLAG_Z, FLAG_C, FLAG_C, FLAG_P, FLAG_P, FLAG_S, FLAG_S
    };
    private final static int[] CONDITION_VALUES = new int[]{
            0, FLAG_Z, 0, FLAG_C, 0, FLAG_P, 0, FLAG_S
    };
    private final static Map<Integer, Integer> RST_MAP = Map.of(
            0xC7, 0,
            0xCF, 0x8,
            0xD7, 0x10,
            0xDF, 0x18,
            0xE7, 0x20,
            0xEF, 0x28,
            0xF7, 0x30,
            0xFF, 0x38
    );
    private final MemoryContext<Byte> memory;
    private final Context8080Impl context;
    private final List<FrequencyChangedListener> frequencyChangedListeners = new CopyOnWriteArrayList<>();
    public boolean INTE = false; // enabling / disabling of interrupts
    public int PC = 0; // program counter
    public int SP = 0; // stack pointer
    public int[] regs = new int[8];
    public short flags = 2; // registers
    public volatile CPU.RunState currentRunState = CPU.RunState.STATE_STOPPED_NORMAL;
    private boolean isINT = false;
    private short b1 = 0; // the raw interrupt instruction
    private short b2 = 0;
    private short b3 = 0;
    private int lastOpcode;
    private long executedCycles = 0;

    private volatile DispatchListener dispatchListener;

    public EmulatorEngine(MemoryContext<Byte> memory, Context8080Impl context) {
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
        try {
            dispatch();
        } catch (Exception e) {
            throw e;
        } catch (Throwable e) {
            throw new Exception(e);
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
                    return CPU.RunState.STATE_STOPPED_BAD_INSTR;
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

        /* if the interrupt is waiting, the instruction is represented by bytes (b1,b2,b3) which represents either
         * rst or a call instruction from an external peripheral device
         */
        if (isINT) {
            isINT = false;
            if (INTE) {
                Integer maybeAddress = RST_MAP.get(b1 & 0xFF);
                if (maybeAddress != null) { // RST
                    SP = (SP - 2) & 0xFFFF;
                    writeWord(SP, PC);
                    PC = maybeAddress;
                    return 11;
                } else if (b1 == 0xCD) {  // CALL
                    SP = (SP - 2) & 0xFFFF;
                    writeWord(SP, (PC + 2) & 0xFFFF);
                    PC = ((b3 & 0xFF) << 8) | (b2 & 0xFF);
                    return 17;
                }
            }
        }

        try {
            lastOpcode = readByte(PC);
        } catch (NullPointerException e) {
            LOGGER.error("NPE; PC=" + Integer.toHexString(PC), e);
            currentRunState = CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
            return 0;
        }
        PC = (PC + 1) & 0xFFFF;

        try {
            MethodHandle instr = DISPATCH_TABLE[lastOpcode];
            if (instr != null) {
                return (int) instr.invokeExact(this);
            }
            currentRunState = CPU.RunState.STATE_STOPPED_BAD_INSTR;
            return 0;
        } finally {
            if (tmpListener != null) {
                tmpListener.afterDispatch();
            }
        }
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

    public int I_NOP() {
        return 4;
    }

    public int I_RLC() {
        int temp = (regs[REG_A] & 0x80) >>> 7;

        flags &= (~FLAG_C);
        flags |= temp;

        regs[REG_A] = (regs[REG_A] << 1 | temp) & 0xFF;
        return 4;
    }

    public int I_RRC() {
        int temp = regs[REG_A] & 0x01;

        flags &= (~FLAG_C);
        flags |= temp;

        regs[REG_A] = ((regs[REG_A] >>> 1) | (temp << 7)) & 0xFF;
        return 4;
    }

    public int I_RAL() {
        int temp = regs[REG_A] << 1;
        regs[REG_A] = temp & 0xFF;
        regs[REG_A] |= (flags & FLAG_C);

        flags &= (~FLAG_C);
        flags |= EmulatorTables.CARRY_TABLE[temp];
        return 4;
    }

    public int I_RAR() {
        int newCarry = regs[REG_A] & 1;
        regs[REG_A] = regs[REG_A] >>> 1;
        if ((flags & FLAG_C) == FLAG_C) {
            regs[REG_A] |= 0x80;
        }
        flags &= (~FLAG_C);
        flags |= newCarry;

        return 4;
    }

    public int I_SHLD() {
        int DAR = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        memory.write(DAR, new Byte[]{(byte) regs[REG_L], (byte) regs[REG_H]}, 2);
        return 16;
    }

    public int I_DAA() {
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

    public int I_LHLD() {
        int DAR = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        regs[REG_L] = readByte(DAR);
        regs[REG_H] = readByte(DAR + 1);
        return 16;
    }

    public int I_CMA() {
        regs[REG_A] = ~regs[REG_A];
        regs[REG_A] &= 0xFF;
        return 4;
    }

    public int I_STA() {
        int DAR = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        memory.write(DAR, (byte) regs[REG_A]);
        return 13;
    }

    public int I_STC() {
        flags |= FLAG_C;
        return 4;
    }

    public int I_LDA() {
        int DAR = readWord(PC);
        PC = (PC + 2) & 0xFFFF;
        regs[REG_A] = readByte(DAR);
        return 13;
    }

    public int I_CMC() {
        if ((flags & FLAG_C) != 0) {
            flags &= (~FLAG_C);
        } else {
            flags |= FLAG_C;
        }
        return 4;
    }

    public int I_HLT() {
        currentRunState = CPU.RunState.STATE_STOPPED_NORMAL;
        return 7;
    }

    public int I_JMP() {
        PC = readWord(PC);
        return 10;
    }

    public int I_ADI() {
        int DAR = regs[REG_A];
        int diff = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_A] += diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(DAR, diff);

        regs[REG_A] &= 0xFF;
        return 7;
    }

    public int I_RET() {
        PC = readWord(SP);
        SP = (SP + 2) & 0xFFFF;
        return 10;
    }

    public int I_CALL() {
        SP = (SP - 2) & 0xFFFF;
        writeWord(SP, (PC + 2) & 0xFFFF);
        PC = readWord(PC);
        return 17;
    }

    public int I_ACI() {
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

    public int I_OUT() throws IOException {
        int DAR = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        context.fireIO(DAR, false, (byte) regs[REG_A]);
        return 10;
    }

    public int I_SUI() {
        int DAR = regs[REG_A];
        int diff = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_A] -= diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(DAR, (-diff) & 0xFF);

        regs[REG_A] &= 0xFF;
        return 7;
    }

    public int I_IN() throws IOException {
        int DAR = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        regs[REG_A] = context.fireIO(DAR, true, (byte) 0) & 0xFF;
        return 10;
    }

    public int I_SBI() {
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

    public int I_XTHL() {
        int DAR = readWord(SP);
        writeWord(SP, (regs[REG_H] << 8) | regs[REG_L]);
        regs[REG_H] = (DAR >>> 8) & 0xFF;
        regs[REG_L] = DAR & 0xFF;
        return 18;
    }

    public int I_ANI() {
        regs[REG_A] &= readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        flags = EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]];
        return 7;
    }

    public int I_PCHL() {
        PC = (regs[REG_H] << 8) | regs[REG_L];
        return 5;
    }

    public int I_XCHG() {
        int x = regs[REG_H];
        int y = regs[REG_L];
        regs[REG_H] = regs[REG_D];
        regs[REG_L] = regs[REG_E];
        regs[REG_D] = x;
        regs[REG_E] = y;
        return 4;
    }

    public int I_XRI() {
        regs[REG_A] ^= readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        flags = EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]];
        return 7;
    }

    public int I_DI() {
        INTE = false;
        return 4;
    }

    public int I_ORI() {
        regs[REG_A] |= readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        flags = EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]];
        return 7;
    }

    public int I_SPHL() {
        SP = ((regs[REG_H] << 8) | regs[REG_L]) & 0xFFFF;
        return 5;
    }

    public int I_EI() {
        INTE = true;
        return 4;
    }

    public int I_CPI() {
        int X = regs[REG_A];
        int DAR = regs[REG_A] & 0xFF;
        int diff = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        DAR -= diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[DAR & 0x1FF];
        auxCarry(X, (-diff) & 0xFF);

        return 7;
    }

    public int I_MOV() {
        putreg((lastOpcode >>> 3) & 0x07, getreg(lastOpcode & 0x07));
        if (((lastOpcode & 0x07) == 6) || (((lastOpcode >>> 3) & 0x07) == 6)) {
            return 7;
        } else {
            return 5;
        }
    }

    public int I_MVI() {
        putreg((lastOpcode >>> 3) & 0x07, readByte(PC));
        PC = (PC + 1) & 0xFFFF;
        if (((lastOpcode >>> 3) & 0x07) == 6) {
            return 10;
        } else {
            return 7;
        }
    }

    public int I_LXI() {
        putpair((lastOpcode >>> 4) & 0x03, readWord(PC));
        PC = (PC + 2) & 0xFFFF;
        return 10;
    }

    public int I_LDAX() {
        int address = getpair((lastOpcode >>> 4) & 0x03);
        putreg(7, readByte(address));
        return 7;
    }

    public int I_STAX() {
        memory.write(getpair((lastOpcode >>> 4) & 0x03), (byte) getreg(7));
        return 7;
    }

    public int I_CMP() {
        int X = regs[REG_A];
        int DAR = X & 0xFF;
        int diff = getreg(lastOpcode & 0x07);
        DAR -= diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[DAR & 0x1FF];
        auxCarry(X, (-diff) & 0xFF);

        return ((lastOpcode & 0x07) == 6) ? 7 : 4;
    }

    public int I_JMP_COND() {
        int index = (lastOpcode >>> 3) & 0x07;
        if ((flags & CONDITION[index]) == CONDITION_VALUES[index]) {
            PC = readWord(PC);
        } else {
            PC = (PC + 2) & 0xFFFF;
        }
        return 10;
    }

    public int I_CALL_COND() {
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

    public int I_RET_COND() {
        int index = (lastOpcode >>> 3) & 0x07;
        if ((flags & CONDITION[index]) == CONDITION_VALUES[index]) {
            PC = readWord(SP);
            SP = (SP + 2) & 0xFFFF;
        }
        return 10;
    }

    public int I_RST() {
        SP = (SP - 2) & 0xFFFF;
        writeWord(SP, PC);
        PC = lastOpcode & 0x38;
        return 11;
    }

    public int I_PUSH() {
        int DAR = getpush((lastOpcode >>> 4) & 0x03);
        SP = (SP - 2) & 0xFFFF;
        writeWord(SP, DAR);
        return 11;
    }

    public int I_POP() {
        int DAR = readWord(SP);
        SP = (SP + 2) & 0xFFFF;
        putpush((lastOpcode >>> 4) & 0x03, DAR);
        return 10;
    }

    public int I_ADD() {
        int X = regs[REG_A];
        int diff = getreg(lastOpcode & 0x07);
        regs[REG_A] += diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(X, diff);

        regs[REG_A] &= 0xFF;
        return ((lastOpcode & 0x07) == 6) ? 7 : 4;
    }

    public int I_ADC() {
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

    public int I_SUB() {
        int X = regs[REG_A];
        int diff = getreg(lastOpcode & 0x07);
        regs[REG_A] -= diff;

        flags = EmulatorTables.SIGN_ZERO_PARITY_CARRY_TABLE[regs[REG_A] & 0x1FF];
        auxCarry(X, (-diff) & 0xFF);

        regs[REG_A] &= 0xFF;
        return ((lastOpcode & 0x07) == 6) ? 7 : 4;
    }

    public int I_SBB() {
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

    public int I_INR() {
        int DAR = (getreg((lastOpcode >>> 3) & 0x07) + 1) & 0xFF;
        flags = (short) (EmulatorTables.INC_TABLE[DAR] | (flags & FLAG_C));
        putreg((lastOpcode >>> 3) & 0x07, DAR);
        return 5;
    }

    public int I_DCR() {
        int DAR = (getreg((lastOpcode >>> 3) & 0x07) - 1) & 0xFF;
        flags = (short) (EmulatorTables.DEC_TABLE[DAR] | (flags & FLAG_C));
        putreg((lastOpcode >>> 3) & 0x07, DAR);
        return 5;
    }

    public int I_INX() {
        int DAR = (getpair((lastOpcode >>> 4) & 0x03) + 1) & 0xFFFF;
        putpair((lastOpcode >>> 4) & 0x03, DAR);
        return 5;
    }

    public int I_DCX() {
        int DAR = (getpair((lastOpcode >>> 4) & 0x03) - 1) & 0xFFFF;
        putpair((lastOpcode >>> 4) & 0x03, DAR);
        return 5;
    }

    public int I_DAD() {
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

    public int I_ANA() {
        regs[REG_A] &= getreg(lastOpcode & 0x07);
        flags = EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]];
        return 4;
    }

    public int I_XRA() {
        regs[REG_A] ^= getreg(lastOpcode & 0x07);
        flags = EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]];
        return 4;
    }

    public int I_ORA() {
        regs[REG_A] |= getreg(lastOpcode & 0x07);
        flags = EmulatorTables.SIGN_ZERO_PARITY_TABLE[regs[REG_A]];
        return 4;
    }
}
