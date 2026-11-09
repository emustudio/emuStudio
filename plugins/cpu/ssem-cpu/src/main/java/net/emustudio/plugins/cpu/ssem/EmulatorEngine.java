/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.emulib.plugin.cpu.*;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.Numbers;
import net.emustudio.emulib.Numbers.Strategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class EmulatorEngine {
    public final static double INSTRUCTION_TIME_MS = 1.44;
    public final static double INSTRUCTIONS_PER_SECOND = 1.0 / (INSTRUCTION_TIME_MS / 1000.0);

    private final static int INSTRUCTION_CYCLES = 100; // artificial number
    public final static double FREQUENCY_KHZ = (INSTRUCTIONS_PER_SECOND / 1000.0) * INSTRUCTION_CYCLES;
    final static int LINE_MASK = 0b11111000;
    private final static Logger LOGGER = LoggerFactory.getLogger(EmulatorEngine.class);
    private static final Method[] DISPATCH_TABLE = new Method[8];

    static {
        try {
            DISPATCH_TABLE[0] = EmulatorEngine.class.getDeclaredMethod("op_jmp", int.class);
            DISPATCH_TABLE[1] = EmulatorEngine.class.getDeclaredMethod("op_sub", int.class);
            DISPATCH_TABLE[2] = EmulatorEngine.class.getDeclaredMethod("op_ldn", int.class);
            DISPATCH_TABLE[3] = EmulatorEngine.class.getDeclaredMethod("op_cmp", int.class);
            DISPATCH_TABLE[4] = EmulatorEngine.class.getDeclaredMethod("op_jpr", int.class);
            DISPATCH_TABLE[6] = EmulatorEngine.class.getDeclaredMethod("op_sto", int.class);
            DISPATCH_TABLE[7] = EmulatorEngine.class.getDeclaredMethod("op_stp", int.class);
        } catch (NoSuchMethodException e) {
            LOGGER.error("Could not set up dispatch table. The emulator won't work correctly", e);
        }
    }

    public final AtomicInteger Acc = new AtomicInteger();
    public final AtomicInteger CI = new AtomicInteger();
    private MemoryContext<Byte> memory;
    public final MachineDebugging debugger;
    private final AccurateFrequencyRunner preciseRunner = new AccurateFrequencyRunner();

    private static Decoder recordingDecoder(MemoryContext<Byte> executionMemory) {
        return new DecoderImpl(MachineDebugging.inspectionMemory(executionMemory));
    }

    EmulatorEngine(MemoryContext<Byte> memory) {
        this.memory = Objects.requireNonNull(memory);
        debugger = new MachineDebugging("SSEM", memory, recordingDecoder(memory));
        debugger.register("PC", 32, () -> Integer.toUnsignedLong(CI.get() + 4), value -> CI.set((int) value - 4));
        debugger.register("CI", 32, () -> Integer.toUnsignedLong(CI.get()), value -> CI.set((int) value));
        debugger.register("ACC", 32, () -> Integer.toUnsignedLong(Acc.get()), value -> Acc.set((int) value));
        MemoryContext<Byte> instrumented = debugger.instrumentedMemory();
        debugger.onActivityChanged(active -> this.memory = active ? instrumented : memory);
    }

    void reset(int startingPos) {
        Acc.set(0);
        CI.set(startingPos);
    }

    CPU.RunState step() {
        if (!debugger.begin(CI.get() + 4)) return CPU.RunState.STATE_STOPPED_BREAK;
        boolean completed = false;
        CPU.RunState result;
        boolean stop;
        try {
            result = stepInstruction();
            completed = result != CPU.RunState.STATE_STOPPED_BAD_INSTR
                    && result != CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
        } finally {
            stop = debugger.end(completed, CI.get() + 4);
        }
        return stop && result == CPU.RunState.STATE_RUNNING ? CPU.RunState.STATE_STOPPED_BREAK : result;
    }

    private CPU.RunState stepInstruction() {
        try {
            Byte[] instruction = memory.read(CI.addAndGet(4), 4);
            if (instruction.length != 4) throw new IndexOutOfBoundsException("Truncated instruction");
            int lineAddress = Numbers.reverseBits(instruction[0] & LINE_MASK, 8) * 4;
            int opcode = instruction[1] & 7;

            Method instr = DISPATCH_TABLE[opcode];
            if (instr == null) {
                return CPU.RunState.STATE_STOPPED_BAD_INSTR;
            }
            return (CPU.RunState) instr.invoke(this, lineAddress);

        } catch (InvocationTargetException | IllegalAccessException e) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
    }

    private CPU.RunState op_jmp(int lineAddress) {
        int oldCi = CI.get() - 4;
        int newLineAddress = readLineAddress(lineAddress);
        CI.set(newLineAddress);
        if (newLineAddress == oldCi) {
            // endless loop detected;
            return CPU.RunState.STATE_STOPPED_BREAK;
        }
        return CPU.RunState.STATE_RUNNING;
    }

    private CPU.RunState op_jpr(int lineAddress) {
        CI.addAndGet(readInt(lineAddress) * 4);
        return CPU.RunState.STATE_RUNNING;
    }

    private CPU.RunState op_ldn(int lineAddress) {
        Acc.set(-readInt(lineAddress));
        return CPU.RunState.STATE_RUNNING;
    }

    private CPU.RunState op_sto(int lineAddress) {
        writeInt(lineAddress, Acc.get());
        return CPU.RunState.STATE_RUNNING;
    }

    private CPU.RunState op_sub(int lineAddress) {
        Acc.addAndGet(-readInt(lineAddress));
        return CPU.RunState.STATE_RUNNING;
    }

    private CPU.RunState op_cmp(int lineAddress) {
        if (Acc.get() < 0) {
            CI.addAndGet(4);
        }
        return CPU.RunState.STATE_RUNNING;
    }

    private CPU.RunState op_stp(int lineAddress) {
        return CPU.RunState.STATE_STOPPED_NORMAL;
    }

    private int readLineAddress(int lineAddress) {
        return 4 * Numbers.reverseBits(memory.read(lineAddress) & LINE_MASK, 8);
    }

    private int readInt(int line) {
        Byte[] word = memory.read(line, 4);
        return Numbers.readInt(word, Strategy.REVERSE_BITS);
    }

    private void writeInt(int lineAddress, int value) {
        Byte[] word = new Byte[4];
        Numbers.writeInt(value, word, Strategy.REVERSE_BITS);
        memory.write(lineAddress, word);
    }

    CPU.RunState run() {
        // 1 instruction takes 1.44 ms (~700 instructions per second)
        // frequency = 1 / (INSTRUCTION_TIME_MS / 1000) = 694.44 Hz = 0.69444 kHz
        // We need integer number of kHZ here, so we need to adjust number of instruction cycles to be > 1.
        // Let's say 1 instruction = 100 cycles.
        // Then, frequency = 100 * 694.44 Hz = 69444 Hz = 69.444 kHz

        return preciseRunner.run(
                () -> FREQUENCY_KHZ,
                () -> {
                    try {
                        preciseRunner.addExecutedCycles(INSTRUCTION_CYCLES);
                        return step();
                    } catch (IndexOutOfBoundsException e) {
                        LOGGER.error("Unexpected error", e);
                        return CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
                    } catch (IllegalArgumentException e) {
                        LOGGER.debug("Unexpected error", e);
                        if (e.getCause() != null && e.getCause() instanceof IndexOutOfBoundsException) {
                            return CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
                        } else {
                            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
                        }
                    } catch (Throwable e) {
                        LOGGER.error("Unexpected error", e);
                        return CPU.RunState.STATE_STOPPED_BAD_INSTR;
                    }
                }
        );
    }
}
