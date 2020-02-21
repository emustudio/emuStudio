package net.sf.emustudio.brainduck.cpu.impl;

import emulib.plugins.cpu.CPU;
import net.sf.emustudio.brainduck.memory.RawMemoryContext;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public class EmulatorEngine {
    final static short I_STOP = 0; // ;
    final static short I_INC = 1; // >
    final static short I_DEC = 2; // <
    final static short I_INCV = 3; // +
    final static short I_DECV = 4; // -
    final static short I_PRINT = 5; // .
    final static short I_READ = 6; // ,
    final static short I_LOOP_START = 7; // [
    final static short I_LOOP_END = 8; // ]
    final static short I_COPY_AND_CLEAR = 0xA1; // any copyloop, including clear
    final static short I_SCANLOOP = 0xA2; // [<] or [>]

    private final RawMemoryContext memory;
    private final short[] rawMemory;
    private final BrainCPUContextImpl context;
    private final Deque<Integer> loopPointers = new ArrayDeque<>();
    private final Profiler profiler;

    public volatile int IP, P; // registers of the CPU

    EmulatorEngine(RawMemoryContext memory, BrainCPUContextImpl context, Profiler profiler) {
        this.memory = Objects.requireNonNull(memory);
        this.rawMemory = memory.getRawMemory();
        this.context = Objects.requireNonNull(context);
        this.profiler = Objects.requireNonNull(profiler);
    }

    public void reset(int adr) {
        IP = adr; // initialize program counter

        // find closest "free" address which does not contain a program
        try {
            while (rawMemory[adr++] != 0) {
            }
        } catch (IndexOutOfBoundsException e) {
            // we get here if "adr" would point to nonexistant memory location,
            // ie. when we go through all memory to the end without a result
            adr = 0;
        }
        P = adr; // assign to the P register the address we have found
        profiler.profileAndOptimize(adr);
    }

    public int getP() {
        return P;
    }

    @SuppressWarnings("empty-statement")
    CPU.RunState step(boolean optimize) throws IOException {
        short OP;

        // FETCH
        int argument = 1;

        Profiler.CachedOperation operation = profiler.findCachedOperation(IP);
        if (optimize && operation != null) {
            OP = operation.operation;
            IP = operation.nextIP;
            argument = operation.argument;
        } else {
            OP = rawMemory[IP++];
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
                rawMemory[P] = (short) ((rawMemory[P] + argument) & 0xFF);
                break;
            case I_DECV: /* - */
                rawMemory[P] = (short) ((rawMemory[P] - argument) & 0xFF);
                break;
            case I_PRINT: /* . */
                while (argument > 0) {
                    context.writeToDevice(rawMemory[P]);
                    argument--;
                }
                break;
            case I_READ: /* , */
                while (argument > 0) {
                    rawMemory[P] = context.readFromDevice();
                    argument--;
                }
                break;
            case I_LOOP_START: /* [ */
                int startingBrace = IP - 1;
                if (rawMemory[P] != 0) {
                    loopPointers.push(startingBrace);
                    break;
                }
                IP = profiler.findLoopEnd(startingBrace);
                break;
            case I_LOOP_END: /* ] */
                int tmpIP = loopPointers.pop();
                if (rawMemory[P] != 0) {
                    IP = tmpIP;
                }
                break;
            case I_COPY_AND_CLEAR: // [>+<-] or [>-<-] or [<+>-] or [<->-] or [-] or combinations
                for (Profiler.CopyLoop copyLoop : operation.copyLoops) {
                    if (copyLoop.specialOP == I_PRINT) {
                        context.writeToDevice(rawMemory[P]);
                    } else if (copyLoop.specialOP == I_READ) {
                        rawMemory[P] = context.readFromDevice();
                    } else {
                        rawMemory[P + copyLoop.relativePosition] = (short)
                            ((rawMemory[P] * copyLoop.factor + rawMemory[P + copyLoop.relativePosition]) & 0xFF);
                    }
                }
                rawMemory[P] = 0;
                break;
            case I_SCANLOOP: // [<] or [>] or combinations
                for (; rawMemory[P] != 0; P += operation.argument) ;
                break;
            default: /* invalid instruction */
                return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    public int getLoopLevel() {
        return loopPointers.size();
    }

}
