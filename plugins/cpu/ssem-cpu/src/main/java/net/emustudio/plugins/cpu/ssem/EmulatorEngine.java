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
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.emulib.runtime.helpers.NumberUtils.Strategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class EmulatorEngine {
    private final static Logger LOGGER = LoggerFactory.getLogger(EmulatorEngine.class);

    public final static int INSTRUCTIONS_PER_SECOND = 700;
    private final static int MEMORY_CELLS = 32 * 4;
    private final static int LINE_MASK = 0b11111000;

    private final CPU cpu;
    private final MemoryContext<Byte> memory;

    public final AtomicInteger Acc = new AtomicInteger();
    public final AtomicInteger CI = new AtomicInteger();

    private volatile long averageInstructionNanos;

    EmulatorEngine(MemoryContext<Byte> memory, CPU cpu) {
        this.memory = Objects.requireNonNull(memory);
        this.cpu = Objects.requireNonNull(cpu);
    }

    void reset(int startingPos) {
        Acc.set(0);
        CI.set(startingPos);
    }

    CPU.RunState step() {
        Byte[] instruction = memory.read(CI.addAndGet(4), 4);

        byte line = (byte)(NumberUtils.reverseBits(instruction[0] & LINE_MASK, 8));
        int lineAddress = line * 4;
        int opcode = instruction[1] & 7;

        switch (opcode) {
            case 0: // JMP
                int oldCi = CI.get() - 4;
                int newLineAddress = readLineAddress(lineAddress);
                CI.set(newLineAddress);
                if (newLineAddress == oldCi) {
                    // endless loop detected;
                    return CPU.RunState.STATE_STOPPED_BREAK;
                }
                break;
            case 4: // JPR, JRP, JMR
                CI.addAndGet(readInt(lineAddress)*4);
                break;
            case 2: // LDN
                int tmp = readInt(lineAddress);
                Acc.set((tmp != 0) ? -tmp : 0);
                break;
            case 6: // STO
                writeInt(lineAddress, Acc.get());
                break;
            case 1: // SUB
                Acc.addAndGet(-readInt(lineAddress));
                break;
            case 3: // CMP, SKN
                if (Acc.get() < 0) {
                    CI.addAndGet(4);
                }
                break;
            case 7: // STP, HLT
                return CPU.RunState.STATE_STOPPED_NORMAL;
            default:
                return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        return CPU.RunState.STATE_RUNNING;
    }

    private int readLineAddress(int lineAddress) {
        return 4 * NumberUtils.reverseBits(memory.read(lineAddress) & LINE_MASK, 8);
    }

    private int readInt(int line) {
        Byte[] word = memory.read(line, 4);
        return NumberUtils.readInt(word, Strategy.REVERSE_BITS);
    }

    private void writeInt(int lineAddress, int value) {
        Byte[] word = new Byte[4];
        NumberUtils.writeInt(value, word, Strategy.REVERSE_BITS);
        memory.write(lineAddress, word);
    }

    CPU.RunState run() {
        if (averageInstructionNanos == 0) {
            measureAverageInstructionNanos();
        }
        CPU.RunState currentRunState = CPU.RunState.STATE_RUNNING;

        long waitNanos = TimeUnit.SECONDS.toNanos(1) / averageInstructionNanos;
        while (!Thread.currentThread().isInterrupted() && currentRunState == CPU.RunState.STATE_RUNNING) {
            try {
                if (cpu.isBreakpointSet(CI.get())) {
                    return CPU.RunState.STATE_STOPPED_BREAK;
                }
                currentRunState = step();
            } catch (IllegalArgumentException e) {
                LOGGER.debug("Unexpected error", e);
                if (e.getCause() != null && e.getCause() instanceof IndexOutOfBoundsException) {
                    return CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
                }
                return CPU.RunState.STATE_STOPPED_BAD_INSTR;
            } catch (IndexOutOfBoundsException e) {
                LOGGER.debug("Unexpected error", e);
                return CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
            }
            if (waitNanos > 0) {
                LockSupport.parkNanos(waitNanos);
            }
        }
        return currentRunState;
    }

    private void fakeStep() {
        Byte[] instruction = memory.read(CI.get(), 4);

        int line = NumberUtils.reverseBits(instruction[0] & LINE_MASK, 8);
        int opcode = instruction[1] & 3;
        CI.updateAndGet(ci -> (ci + 4) % MEMORY_CELLS);

        switch (opcode) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 6:
            case 7:
                break;
        }

        Acc.addAndGet(- memory.read(line % MEMORY_CELLS));
    }


    private void measureAverageInstructionNanos() {
        int oldCI = CI.get();
        int oldAcc = Acc.get();

        long start = System.nanoTime();
        for (int i = 0; i < INSTRUCTIONS_PER_SECOND; i++) {
            fakeStep();
        }
        long elapsed = System.nanoTime() - start;

        averageInstructionNanos = elapsed / INSTRUCTIONS_PER_SECOND;

        CI.set(oldCI);
        Acc.set(oldAcc);
    }
}
