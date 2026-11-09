/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck;

import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.cpu.Disassembler;
import net.emustudio.emulib.plugin.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.plugin.memory.AbstractMemoryContext;
import net.emustudio.plugins.cpu.brainduck.gui.DecoderImpl;
import net.emustudio.plugins.cpu.brainduck.gui.DisassemblerImpl;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public class EmulatorEngine {
    final static byte I_STOP = 0; // ;
    final static byte I_INC = 1; // >
    final static byte I_DEC = 2; // <
    final static byte I_INCV = 3; // +
    final static byte I_DECV = 4; // -
    final static byte I_PRINT = 5; // .
    final static byte I_READ = 6; // ,
    final static byte I_LOOP_START = 7; // [
    final static byte I_LOOP_END = 8; // ]
    final static byte I_COPY_AND_CLEAR = (byte) 0xA1; // any copyloop, including clear
    final static byte I_SCANLOOP = (byte) 0xA2; // [<] or [>]

    private final Byte[] rawMemory;
    public final MachineDebugging debugger;
    boolean debugStopped;
    private boolean recording;
    private final BrainCPUContextImpl context;
    private final Deque<Integer> loopPointers = new ArrayDeque<>();
    private final Profiler profiler;

    public volatile int IP, P; // registers of the CPU

    /** The engine executes raw bank zero even when a different bank is selected in the memory UI. */
    private static MemoryContext<Byte> rawBankZero(ByteMemoryContext memory, Byte[] raw) {
        return new AbstractMemoryContext<Byte>() {
            @Override public Byte read(int address) { return raw[address]; }
            @Override public Byte[] read(int address, int count) {
                java.util.Objects.checkFromToIndex(address, address, raw.length);
                if (count < 0) throw new IndexOutOfBoundsException("Negative read count");
                int available = Math.min(count, raw.length - address);
                return java.util.Arrays.copyOfRange(raw, address, address + available);
            }
            @Override public void write(int address, Byte value) { throw new UnsupportedOperationException("Inspection only"); }
            @Override public void write(int address, Byte[] values, int count) { throw new UnsupportedOperationException("Inspection only"); }
            @Override public void clear() { throw new UnsupportedOperationException("Inspection only"); }
            @Override public Class<Byte> getCellTypeClass() { return Byte.class; }
            @Override public int getSize() { return raw.length; }

        };
    }

    private static Disassembler recordingDisassembler(MemoryContext<Byte> memory) {
        DecoderImpl decoder = new DecoderImpl(memory);
        return new Disassembler() {
            @Override public DisassembledInstruction disassemble(int address) {
                // Generated disassemblers cache by address; each capture must see current code.
                return new DisassemblerImpl(memory, decoder).disassemble(address);
            }
            @Override public int getNextInstructionPosition(int address) {
                return new DisassemblerImpl(memory, decoder).getNextInstructionPosition(address);
            }
        };
    }

    EmulatorEngine(ByteMemoryContext memory, BrainCPUContextImpl context, Profiler profiler) {
        this.rawMemory = Objects.requireNonNull(Objects.requireNonNull(memory.getRawMemory())[0]);
        this.context = Objects.requireNonNull(context);
        this.profiler = Objects.requireNonNull(profiler);
        debugger = new MachineDebugging("BrainDuck", rawBankZero(memory, rawMemory), recordingDisassembler(rawBankZero(memory, rawMemory)));
        debugger.register("IP", 32, () -> Integer.toUnsignedLong(IP), value -> IP = (int) value);
        debugger.register("P", 32, () -> Integer.toUnsignedLong(P), value -> P = (int) value);
        debugger.inspect("LOOP_DEPTH", loopPointers::size);
        debugger.register("CELL", 8, () -> rawMemory[P] & 255, value -> rawMemory[P] = (byte) value);
    }

    public void reset(int adr) {
        IP = adr; // initialize program counter
        loopPointers.clear();
        profiler.reset();

        // find closest "free" address which does not contain a program
        try {
            while (rawMemory[adr++] != 0) {
            }
        } catch (IndexOutOfBoundsException e) {
            // we get here if "adr" would point to nonexistant memory location,
            // i.e. when we go through all memory to the end without a result
            adr = 0;
        }
        P = adr; // assign to the P register the address we have found
        profiler.profileAndOptimize(adr);
    }

    public int getP() {
        return P;
    }

    CPU.RunState step(boolean optimize) {
        debugStopped = false;
        debugger.boundary();
        if (!debugger.begin(IP)) {
            debugStopped = true;
            return CPU.RunState.STATE_STOPPED_BREAK;
        }
        boolean completed = false;
        CPU.RunState result = CPU.RunState.STATE_STOPPED_BAD_INSTR;
        recording = debugger.isActive();
        try {
            result = stepInstruction(!debugger.isActive() && optimize); // Observed execution retains each guest instruction.
            completed = result != CPU.RunState.STATE_STOPPED_BAD_INSTR
                    && result != CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
        } finally {
            recording = false;
            debugStopped = debugger.end(completed) && completed
                    && result == CPU.RunState.STATE_STOPPED_BREAK;
        }
        return result;
    }

    @SuppressWarnings("empty-statement")
    private CPU.RunState stepInstruction(boolean optimize) {
        short OP;

        // FETCH
        int argument = 1;

        Profiler.CachedOperation operation = profiler.findCachedOperation(IP);
        if (optimize && operation != null) {
            OP = operation.operation;
            IP = operation.nextIP;
            argument = operation.argument;
        } else {
            OP = readRaw(IP++);
        }

        // DECODE
        switch (OP) {
            case I_STOP: /* ; */
                return CPU.RunState.STATE_STOPPED_NORMAL;
            case I_INC: /* >  */
                P += argument;
                if (P > rawMemory.length) {
                    return CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
                }
                break;
            case I_DEC: /* < */
                P -= argument;
                if (P < 0) {
                    return CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
                }
                break;
            case I_INCV: /* + */
                writeRaw(P, (byte) ((readRaw(P) + argument) & 0xFF));
                break;
            case I_DECV: /* - */
                writeRaw(P, (byte) ((readRaw(P) - argument) & 0xFF));
                break;
            case I_PRINT: /* . */
                while (argument > 0) {
                    context.writeToDevice(readRaw(P));
                    argument--;
                }
                break;
            case I_READ: /* , */
                while (argument > 0) {
                    writeRaw(P, context.readFromDevice());
                    argument--;
                }
                break;
            case I_LOOP_START: /* [ */
                int startingBrace = IP - 1;
                if (readRaw(P) != 0) {
                    loopPointers.push(startingBrace);
                    break;
                }
                IP = profiler.findLoopEnd(startingBrace);
                break;
            case I_LOOP_END: /* ] */
                int tmpIP = loopPointers.pop();
                if (readRaw(P) != 0) {
                    IP = tmpIP;
                }
                break;
            case I_COPY_AND_CLEAR: // [>+<-] or [>-<-] or [<+>-] or [<->-] or [-] or combinations
                Profiler.CachedOperation copyLoopOperation = Objects.requireNonNull(operation);
                for (Profiler.CopyLoop copyLoop : copyLoopOperation.copyLoops) {
                    if (copyLoop.specialOP == I_PRINT) {
                        context.writeToDevice(readRaw(P));
                    } else if (copyLoop.specialOP == I_READ) {
                        writeRaw(P, context.readFromDevice());
                    } else {
                        rawMemory[P + copyLoop.relativePosition] = (byte) ((rawMemory[P] * copyLoop.factor + rawMemory[P + copyLoop.relativePosition]) & 0xFF);
                    }
                }
                rawMemory[P] = 0;
                break;
            case I_SCANLOOP: // [<] or [>] or combinations
                int scanStep = Objects.requireNonNull(operation).argument;
                for (; rawMemory[P] != 0; P += scanStep) ;
                break;
            default: /* invalid instruction */
                return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private byte readRaw(int address) {
        byte value = rawMemory[address];
        if (recording) debugger.access(address, value & 255, false);
        return value;
    }

    private void writeRaw(int address, byte value) {
        rawMemory[address] = value;
        if (recording) debugger.access(address, value & 255, true);
    }

    public int getLoopLevel() {
        return loopPointers.size();
    }

}
