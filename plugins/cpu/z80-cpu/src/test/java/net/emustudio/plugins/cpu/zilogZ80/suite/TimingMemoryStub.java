/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80.suite;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;

import java.util.HashMap;
import java.util.Map;

/**
 * Memory stub that also acts as the CPU's passed-cycles listener.
 * <p>
 * Used by every Z80 test through {@code InstructionsTest}; per-test counters can be reset via
 * {@link #clearCounters()} or queried through {@link #getTotalCycles()}, {@link #getReadCount(int)}
 * and {@link #getPassiveCycleCount(int)}. Behaves identically to {@link ByteMemoryStub} when
 * counters are ignored.
 */
public class TimingMemoryStub extends ByteMemoryStub implements CPUContext.PassedCyclesListener {
    private final Map<Integer, Integer> readCounts = new HashMap<>();
    private final Map<Integer, Integer> passiveCycleCounts = new HashMap<>();
    private long totalCycles;

    public TimingMemoryStub(int wordReadingStrategy) {
        super(wordReadingStrategy);
    }

    @Override
    public Byte read(int location) {
        int masked = location & 0xFFFF;
        readCounts.merge(masked, 1, Integer::sum);
        return super.read(masked);
    }

    @Override
    public Byte[] read(int location, int count) {
        Byte[] values = new Byte[count];
        for (int i = 0; i < count; i++) {
            values[i] = read(location + i);
        }
        return values;
    }

    @Override
    public void write(int location, Byte value) {
        super.write(location & 0xFFFF, value);
    }

    @Override
    public void write(int location, Byte[] values, int count) {
        for (int i = 0; i < count; i++) {
            write(location + i, values[i]);
        }
    }

    @Override
    public void passedCycles(long cyclesDelta) {
        totalCycles += cyclesDelta;
    }

    @Override
    public void passedCycles(int address, int cycles) {
        passiveCycleCounts.merge(address & 0xFFFF, cycles, Integer::sum);
    }

    @Override
    public Class<Byte> getCellTypeClass() {
        return Byte.class;
    }

    @Override
    public void clear() {
        super.clear();
        clearCounters();
    }

    @Override
    public MemoryContextAnnotations annotations() {
        return null;
    }

    public void clearCounters() {
        readCounts.clear();
        passiveCycleCounts.clear();
        totalCycles = 0;
    }

    public long getTotalCycles() {
        return totalCycles;
    }

    public int getReadCount(int address) {
        return readCounts.getOrDefault(address & 0xFFFF, 0);
    }

    public int getPassiveCycleCount(int address) {
        return passiveCycleCounts.getOrDefault(address & 0xFFFF, 0);
    }
}

