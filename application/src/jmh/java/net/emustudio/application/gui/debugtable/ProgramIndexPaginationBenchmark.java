/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.application.debugger.InstructionIndex;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.plugins.cpu.zilogZ80.gui.DecoderImpl;
import net.emustudio.plugins.cpu.zilogZ80.gui.DisassemblerImpl;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/** Reproduces the supplied Z80/NOP pagination workload with the real old/new table paths. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
public class ProgramIndexPaginationBenchmark {
    @AuxCounters(AuxCounters.Type.EVENTS)
    @State(Scope.Thread)
    public static class Reads {
        public long memoryCells;
        @Setup(Level.Iteration) public void reset() { memoryCells = 0; }
    }

    @State(Scope.Thread)
    public static class Pages {
        @Param({"21", "32", "64", "128"}) public int rows;
        @Param({"empty", "built"}) public String flow;
        BaselineDebugTableModelImpl baseline;
        DebugTableModelImpl indexed;
        ProgramIndex index;
        CPU cpu;
        int pc;
        long memoryCells;

        @Setup(Level.Trial) public void setup() {
            Disassembler decoder = decoder(this);
            cpu = (CPU) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {CPU.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getDisassembler": return decoder;
                            case "getAddressSpaceSize": return 65536;
                            case "getInstructionLocation": return pc;
                            case "isBreakpointSupported": return false;
                            case "getDebugger": return Optional.empty();
                            case "hashCode": return System.identityHashCode(proxy);
                            case "equals": return proxy == args[0];
                            case "toString": return "Z80 NOP pagination CPU";
                            default: throw new UnsupportedOperationException(method.getName());
                        }
                    });
            index = ProgramIndex.create("z80-nop", 65536, 1, 65536);
            if (flow.equals("built")) {
                try (ProgramIndex.ContentUpdate update = index.updateContent(index.snapshot())) {
                    update.replaceFlow(false);
                    for (int address = 0; address < 65536; address++)
                        update.putInstruction(new ProgramIndex.Range(0, address, address + 1),
                                ProgramIndex.InstructionKind.LINEAR, false);
                    update.commit();
                }
            }
            resetModels();
        }

        void resetModels() {
            pc = 0;
            baseline = new BaselineDebugTableModelImpl();
            baseline.setCPU(cpu);
            baseline.setMaxRows(rows);
            baseline.getLocationAt(rows / 2);
            indexed = new DebugTableModelImpl();
            indexed.setCPU(cpu);
            indexed.setInstructionIndex(new InstructionIndex(index::snapshot, () -> 0));
            indexed.setMaxRows(rows);
            indexed.getLocationAt(rows / 2);
            pc = 0x8000;
        }
    }

    @State(Scope.Thread)
    public static class FreshPages extends Pages {
        @Setup(Level.Invocation) public void reset() { resetModels(); }
    }

    @Benchmark public long oldRepeatedRowLocations(Pages pages, Reads reads) {
        long before = pages.memoryCells;
        long result = 0;
        for (int row = 0; row < pages.rows; row++) result += pages.baseline.getLocationAt(row);
        reads.memoryCells += pages.memoryCells - before;
        return result;
    }

    @Benchmark public long indexedRepeatedRowLocations(Pages pages, Reads reads) {
        long before = pages.memoryCells;
        long result = 0;
        for (int row = 0; row < pages.rows; row++) result += pages.indexed.getLocationAt(row);
        reads.memoryCells += pages.memoryCells - before;
        return result;
    }

    @Benchmark public void oldRepeatedTableCells(Pages pages, Reads reads, Blackhole blackhole) {
        long before = pages.memoryCells;
        for (int row = 0; row < pages.rows; row++)
            for (int column = 0; column < pages.baseline.getColumnCount(); column++)
                blackhole.consume(pages.baseline.getValueAt(row, column));
        reads.memoryCells += pages.memoryCells - before;
    }

    @Benchmark public void indexedRepeatedTableCells(Pages pages, Reads reads, Blackhole blackhole) {
        long before = pages.memoryCells;
        for (int row = 0; row < pages.rows; row++)
            for (int column = 0; column < pages.indexed.getColumnCount(); column++)
                blackhole.consume(pages.indexed.getValueAt(row, column));
        reads.memoryCells += pages.memoryCells - before;
    }

    @Benchmark @BenchmarkMode(Mode.SingleShotTime)
    public long oldFirstRowLocations(FreshPages pages, Reads reads) {
        long before = pages.memoryCells;
        long result = 0;
        for (int row = 0; row < pages.rows; row++) result += pages.baseline.getLocationAt(row);
        reads.memoryCells += pages.memoryCells - before;
        return result;
    }

    @Benchmark @BenchmarkMode(Mode.SingleShotTime)
    public long indexedFirstRowLocations(FreshPages pages, Reads reads) {
        long before = pages.memoryCells;
        long result = 0;
        for (int row = 0; row < pages.rows; row++) result += pages.indexed.getLocationAt(row);
        reads.memoryCells += pages.memoryCells - before;
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Disassembler decoder(Pages pages) {
        MemoryContext<Byte> memory = (MemoryContext<Byte>) Proxy.newProxyInstance(
                ProgramIndexPaginationBenchmark.class.getClassLoader(), new Class<?>[] {MemoryContext.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "read":
                            int address = (int) args[0];
                            if (address < 0 || address >= 65536) throw new IndexOutOfBoundsException();
                            if (args.length == 1) { pages.memoryCells++; return (byte) 0; }
                            int count = (int) args[1];
                            pages.memoryCells += count;
                            Byte[] values = new Byte[count];
                            Arrays.fill(values, (byte) 0);
                            return values;
                        case "getSize": return 65536;
                        case "getCellTypeClass": return Byte.class;
                        case "hashCode": return System.identityHashCode(proxy);
                        case "equals": return proxy == args[0];
                        case "toString": return "Counting Z80 NOP memory";
                        default: throw new UnsupportedOperationException(method.getName());
                    }
                });
        return new DisassemblerImpl(memory, new DecoderImpl(memory));
    }
}
