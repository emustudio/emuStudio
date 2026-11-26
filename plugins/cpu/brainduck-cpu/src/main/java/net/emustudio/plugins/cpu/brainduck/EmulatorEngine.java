/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck;

import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.cpu.Decoder;
import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.plugin.cpu.InvalidInstructionException;
import net.emustudio.emulib.plugin.debugger.ExecutedBlock;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.plugin.memory.AbstractMemoryContext;
import net.emustudio.plugins.cpu.brainduck.gui.DecoderImpl;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public class EmulatorEngine {
    private static final int MAX_BATCH_OPERATIONS = 4096;
    private static final long BATCH_NANOS = 2_000_000L;
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
    private final Decoder decoder;
    private final OptimizedBlock[] optimizedBlocks;
    private final ExecutionBatch executionBatch;
    private long observedVersion = -1;
    private int programLimit;

    public volatile int IP, P; // registers of the CPU

    /** The engine executes raw bank zero even when a different bank is selected in the memory UI. */
    private static MemoryContext<Byte> rawBankZero(Byte[] raw) {
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

    private static Decoder recordingDecoder(MemoryContext<Byte> memory) {
        Byte[] snapshot = new Byte[memory.getSize()];
        Decoder decoder = new DecoderImpl(rawBankZero(snapshot));
        DecodedInstruction[] instructions = new DecodedInstruction[memory.getSize()];
        byte[] encodings = new byte[memory.getSize()];
        // BrainDuck instructions occupy one byte. Reuse immutable facts while that byte is unchanged.
        return address -> {
            byte opcode = memory.peek(address);
            if (instructions[address] == null || encodings[address] != opcode) {
                // Decode the sampled byte, so a concurrent editor cannot poison the cache's byte/facts pair.
                snapshot[address] = opcode;
                instructions[address] = decoder.decode(address);
                encodings[address] = opcode;
            }
            return instructions[address];
        };
    }

    EmulatorEngine(ByteMemoryContext memory, BrainCPUContextImpl context, Profiler profiler) {
        this.rawMemory = Objects.requireNonNull(Objects.requireNonNull(memory.getRawMemory())[0]);
        this.context = Objects.requireNonNull(context);
        this.profiler = Objects.requireNonNull(profiler);
        decoder = recordingDecoder(rawBankZero(rawMemory));
        optimizedBlocks = new OptimizedBlock[rawMemory.length];
        executionBatch = new ExecutionBatch(rawMemory.length);
        debugger = new MachineDebugging("BrainDuck", rawBankZero(rawMemory), decoder);
        debugger.register("IP", 4, () -> Integer.toUnsignedLong(IP));
        debugger.register("P", 4, () -> Integer.toUnsignedLong(P));
        debugger.inspect("LOOP_DEPTH", loopPointers::size);
        debugger.register("CELL", 1, () -> rawMemory[P] & 255);
    }

    public void reset(int adr) {
        IP = adr; // initialize program counter
        loopPointers.clear();
        profiler.reset();
        executionBatch.reset();

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
        programLimit = adr;
        for (int at = 0; at < optimizedBlocks.length; at++) {
            Profiler.CachedOperation operation = profiler.findCachedOperation(at);
            optimizedBlocks[at] = operation == null ? null : OptimizedBlock.capture(at, operation, rawMemory, decoder);
        }
    }

    public int getP() {
        return P;
    }

    CPU.RunState step(boolean optimize) {
        debugStopped = false;
        long beforeBeginVersion = debugger.observationVersion();
        if (!debugger.begin(IP)) {
            debugStopped = true;
            return CPU.RunState.STATE_STOPPED_BREAK;
        }
        boolean completed = false;
        CPU.RunState result = CPU.RunState.STATE_STOPPED_BAD_INSTR;
        ExecutedBlock block = null;
        boolean batching = false;
        recording = debugger.requiresAccesses();
        try {
            if (optimize && debugger.canBatchInstructions()) {
                batching = true;
                if (observedVersion != beforeBeginVersion || debugger.observationVersion() != beforeBeginVersion)
                    executionBatch.reset();
                observedVersion = beforeBeginVersion;
                executionBatch.begin();
                try { result = executeObservedBatch(); }
                finally { block = executionBatch.finish(); }
            }
            if (!batching || (block == null && result == CPU.RunState.STATE_STOPPED_BREAK))
                result = stepInstruction(!debugger.isActive() && optimize);
            completed = result != CPU.RunState.STATE_STOPPED_BAD_INSTR
                    && result != CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
        } finally {
            recording = false;
            if (block != null) debugger.endBlock(block);
            else {
                debugStopped = debugger.end(completed, IP) && completed
                        && result == CPU.RunState.STATE_STOPPED_BREAK;
                executionBatch.reset();
            }
        }
        return result;
    }

    private CPU.RunState executeObservedBatch() {
        Thread thread = Thread.currentThread();
        long started = System.nanoTime();
        // Bound pause/settings latency. I/O, stop and failing instructions retain individual callbacks.
        for (int count = 0; count < MAX_BATCH_OPERATIONS && !thread.isInterrupted(); count++) {
            if ((count & 63) == 63 && System.nanoTime() - started >= BATCH_NANOS) break;
            int address = IP;
            int pointer = P;
            if (!canExecuteInBatch(address, pointer)) break;
            OptimizedBlock optimized = optimizedBlocks[address];
            ExecutedBlock operation = optimized == null ? null : executeObservedBlock(optimized, pointer);
            if (operation != null) executionBatch.executed(optimizedBlocks[address], operation);
            else {
                DecodedInstruction instruction;
                try { instruction = decoder.decode(address); }
                catch (InvalidInstructionException failure) { break; }
                CPU.RunState state = stepInstruction(false);
                if (state != CPU.RunState.STATE_STOPPED_BREAK) return state;
                executionBatch.executed(instruction, IP);
            }
        }
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private boolean canExecuteInBatch(int address, int pointer) {
        if (address < 0 || address >= rawMemory.length) return false;
        byte opcode = rawMemory[address];
        if (opcode == I_INC) return pointer < rawMemory.length;
        if (opcode == I_DEC) return pointer > 0;
        if (pointer < 0 || pointer >= rawMemory.length) return false;
        if (opcode == I_INCV || opcode == I_DECV) return true;
        if (opcode == I_LOOP_START) return rawMemory[pointer] != 0 || profiler.findLoopEnd(address) >= 0;
        return opcode == I_LOOP_END && !loopPointers.isEmpty();
    }

    /** Null means no guest effects occurred: execute the original instruction instead. */
    private ExecutedBlock executeObservedBlock(OptimizedBlock block, int pointer) {
        if (pointer < 0 || pointer >= rawMemory.length || !block.isCurrent(rawMemory)) return null;
        Profiler.CachedOperation operation = block.operation;
        if (operation.type == Profiler.CachedOperation.TYPE.SCANLOOP) return executeObservedScan(block);
        if (!block.pointerInBounds(pointer, rawMemory.length)) return null;
        // Do not bulk-write program memory, including another cached operation's source instructions.
        if (operation.type == Profiler.CachedOperation.TYPE.COPYLOOP
                || operation.operation == I_INCV || operation.operation == I_DECV) {
            if ((long) pointer + block.minPointer < programLimit) return null;
        }
        int iterations = operation.type == Profiler.CachedOperation.TYPE.COPYLOOP ? rawMemory[pointer] & 255 : 1;
        if (iterations == 0) {
            IP = block.end;
            return block.skipped;
        }
        stepInstruction(true); // Bounds, source code and absence of I/O were checked before any effects.
        return iterations == 1 ? block.once : block.repeated;
    }

    private ExecutedBlock executeObservedScan(OptimizedBlock block) {
        if (rawMemory[P] == 0) {
            IP = block.end;
            return block.skipped;
        }
        int iterations = 0;
        // Bound uninterrupted work; every chunk ends at a real '[' boundary with an unchanged loop stack.
        while (iterations < 256 && !Thread.currentThread().isInterrupted()) {
            if (!block.pointerInBounds(P, rawMemory.length)) break;
            P += block.operation.argument;
            iterations++;
            if (rawMemory[P] == 0) {
                IP = block.end;
                return iterations == 1 ? block.once : block.repeated;
            }
        }
        if (iterations == 0) return null;
        IP = block.start;
        return block.continuing;
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
