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
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.emulib.plugins.cpu.*;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.helpers.Bits;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.emulib.runtime.helpers.NumberUtils.Strategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static net.emustudio.plugins.cpu.ssem.DecoderImpl.LINE;

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
    private final MemoryContext<Byte> memory;
    private final Decoder decoder;
    private final Function<Integer, Boolean> isBreakpointSet;
    private final Bits emptyBits = new Bits(0, 0);
    private final AccurateFrequencyRunner preciseRunner = new AccurateFrequencyRunner();

    EmulatorEngine(MemoryContext<Byte> memory, Function<Integer, Boolean> isBreakpointSet) {
        this.memory = Objects.requireNonNull(memory);
        this.isBreakpointSet = Objects.requireNonNull(isBreakpointSet);
        this.decoder = new DecoderImpl(memory);
    }

    void reset(int startingPos) {
        Acc.set(0);
        CI.set(startingPos);
    }

    CPU.RunState step() {
        try {
            DecodedInstruction instruction = decoder.decode(CI.addAndGet(4));
            int lineAddress = Optional.ofNullable(instruction.getBits(LINE)).orElse(emptyBits).reverseBits().bits * 4;
            int opcode = instruction.getImage()[1] & 7;

            Method instr = DISPATCH_TABLE[opcode];
            if (instr == null) {
                return CPU.RunState.STATE_STOPPED_BAD_INSTR;
            }
            return (CPU.RunState) instr.invoke(this, lineAddress);

        } catch (InvalidInstructionException | InvocationTargetException | IllegalAccessException e) {
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
        // 1 instruction takes 1.44 ms (~700 instructions per second)
        // frequency = 1 / (INSTRUCTION_TIME_MS / 1000) = 694.44 Hz = 0.69444 kHz
        // We need integer number of kHZ here, so we need to adjust number of instruction cycles to be > 1.
        // Let's say 1 instruction = 100 cycles.
        // Then, frequency = 100 * 694.44 Hz = 69444 Hz = 69.444 kHz

        return preciseRunner.run(
                () -> FREQUENCY_KHZ,
                () -> {
                    try {
                        if (isBreakpointSet.apply(CI.get())) {
                            return CPU.RunState.STATE_STOPPED_BREAK;
                        }
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
