/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
package net.sf.emustudio.ssem.cpu;

import emulib.plugins.cpu.CPU;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.NumberUtils;
import emulib.runtime.NumberUtils.Strategy;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class EmulatorEngine {
    public final static int INSTRUCTIONS_PER_SECOND = 700;
    
    private final CPU cpu;
    private final MemoryContext<Byte> memory;
    
    public volatile int Acc;
    public volatile int CI;

    private volatile long averageInstructionNanos;
    
    public EmulatorEngine(MemoryContext<Byte> memory, CPU cpu) {
        this.memory = Objects.requireNonNull(memory);
        this.cpu = Objects.requireNonNull(cpu);
    }
    
    public void reset(int startingPos) {
        Acc = 0;
        CI = startingPos;
    }
    
    public CPU.RunState step() {
        Byte[] instruction = memory.readWord(CI);
        CI += 4;
        
        int line = NumberUtils.reverseBits(instruction[0], 8) * 4;
        int opcode = instruction[1] & 7;

        switch (opcode) {
            case 0: // JMP
                CI = 4 * readInt(line);
                break;
            case 4: // JPR
                CI = CI + 4 * readInt(line);
                break;
            case 2: // LDN
                Acc = -readInt(line);
                break;
            case 6: // STO
                writeInt(line, Acc);
                break;
            case 1: // SUB
                Acc = Acc - readInt(line);
                break;
            case 3: // CMP / SKN
                if (Acc < 0) {
                    CI += 4;
                }
                break;
            case 7: // STP / HLT
                return CPU.RunState.STATE_STOPPED_NORMAL;
            default:
                return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        return CPU.RunState.STATE_STOPPED_BREAK;
    }
    
    private int readInt(int line) {
        Byte[] word = memory.readWord(line);
        return NumberUtils.readInt(word, Strategy.REVERSE_BITS);
    }
    
    private void writeInt(int line, int value) {
        Byte[] word = new Byte[4];
        NumberUtils.writeInt(value, word, Strategy.REVERSE_BITS);
        memory.writeWord(line, word);
    }

    public CPU.RunState run() {
        if (averageInstructionNanos == 0) {
            measureAverageInstructionNanos();
        }
        CPU.RunState currentRunState = CPU.RunState.STATE_STOPPED_BREAK;
        
        long waitNanos = TimeUnit.SECONDS.toNanos(1) / averageInstructionNanos;
        while (!Thread.currentThread().isInterrupted() && currentRunState == CPU.RunState.STATE_STOPPED_BREAK) {
            try {
                if (cpu.isBreakpointSet(CI)) {
                    return CPU.RunState.STATE_STOPPED_BREAK;
                }
                currentRunState = step();
            } catch (IllegalArgumentException e) {
                if (e.getCause() != null && e.getCause() instanceof IndexOutOfBoundsException) {
                    return CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
                }
                return CPU.RunState.STATE_STOPPED_BAD_INSTR;
            } catch (IndexOutOfBoundsException e) {
                return CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
            }
            if (waitNanos > 0) {
                LockSupport.parkNanos(waitNanos);
            }
        }
        return currentRunState;
    }
    
    private void fakeStep() {
        Byte[] instruction = memory.readWord(CI);
        
        int line = NumberUtils.reverseBits(instruction[0], 8);
        int opcode = instruction[1] & 3;
        CI++;

        switch (opcode) {
            case 0: break;
            case 1: break;
            case 2: break;
            case 3: break;
            case 4: break;
            case 6: break;
            case 7: break;
        }
        
        Acc -= memory.read(line);
    }
    
    
    private void measureAverageInstructionNanos() {
        int oldCI = CI;
        int oldAcc = Acc;
        
        long start = System.nanoTime();
        for (int i = 0; i < INSTRUCTIONS_PER_SECOND; i++) {
            fakeStep();
        }
        long elapsed = System.nanoTime() - start;
        
        averageInstructionNanos = elapsed / INSTRUCTIONS_PER_SECOND;
        
        CI = oldCI;
        Acc = oldAcc;
    }
}
