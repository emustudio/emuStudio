/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.bench;

import net.emustudio.application.debugger.BaselineFlowAnalysis;
import net.emustudio.application.debugger.FlowAnalysis;
import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.cpu.InstructionFlow;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/** Frozen production edge scan vs CSR, with identical explicit edge semantics. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
public class ProgramIndexGraphBenchmark {
    @State(Scope.Thread)
    public static class Graphs {
        @Param({"1", "1000", "10000"}) public int nodes;
        @Param({"single", "banked", "sparse"}) public String layout;
        ProgramIndex index;
        ProgramIndex.Snapshot snapshot;
        FlowAnalysis analysis;
        BaselineFlowAnalysis baseline;
        Disassembler decoder;
        int[] addresses, queries = new int[1024];
        int size, banks, common, bank, cursor;
        final EdgeSink sink = new EdgeSink();

        @Setup public void setup() {
            size = layout.equals("sparse") ? Integer.MAX_VALUE : 65536;
            banks = layout.equals("single") ? 1 : 4;
            common = layout.equals("single") ? size : size - 16384;
            bank = banks - 1;
            addresses = new int[nodes];
            for (int i = 0; i < nodes; i++) addresses[i] = layout.equals("sparse") ? i * 100003
                    : layout.equals("banked") && i % 8 == 0 ? common + i / 8 : i;
            decoder = new FixtureDecoder(addresses);
            index = ProgramIndex.create("graph", size, banks, common);
            analysis = FlowAnalysis.reachable(decoder, addresses[0], nodes, index, bank).publish();
            snapshot = index.snapshot();
            baseline = new BaselineFlowAnalysis(decoder, addresses[0], nodes, null, bank);
            Random random = new Random(454);
            for (int i = 0; i < queries.length; i++) queries[i] = random.nextInt(nodes);
        }

        int nextAddress() { return addresses[queries[(cursor++) & (queries.length - 1)]]; }
    }

    @Benchmark public long indexedSuccessors(Graphs state) {
        state.sink.checksum = 0;
        state.snapshot.forEachSuccessor(state.bank, state.nextAddress(), state.sink);
        return state.sink.checksum;
    }

    @Benchmark public long scanSuccessorsOracle(Graphs state) {
        int address = state.nextAddress();
        long result = 0;
        for (BaselineFlowAnalysis.Edge edge : state.baseline.edges)
            if (edge.from == address) result += (edge.from ^ edge.to) + 1L;
        return result;
    }

    @Benchmark @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public Object oldFullRebuild(Graphs state) {
        return new BaselineFlowAnalysis(state.decoder, state.addresses[0], state.nodes, null, state.bank);
    }

    @Benchmark @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public Object indexedFullRebuild(Graphs state) {
        ProgramIndex index = ProgramIndex.create("graph", state.size, state.banks, state.common);
        return FlowAnalysis.reachable(state.decoder, state.addresses[0], state.nodes, index, state.bank).publish();
    }

    static final class EdgeSink implements ProgramIndex.EdgeVisitor {
        long checksum;
        @Override public void accept(int from, int to, ProgramIndex.EdgeKind kind) {
            checksum += (from ^ to) + 1L;
        }
    }

    /** Cached decoder payloads keep setup/decoding allocations identical in both rebuild paths. */
    static final class FixtureDecoder implements Disassembler {
        final java.util.Map<Integer, Integer> positions = new java.util.HashMap<>();
        final Optional<InstructionFlow>[] flows;
        final DisassembledInstruction[] listing;

        @SuppressWarnings("unchecked")
        FixtureDecoder(int[] addresses) {
            flows = (Optional<InstructionFlow>[]) new Optional<?>[addresses.length];
            listing = new DisassembledInstruction[addresses.length];
            for (int i = 0; i < addresses.length; i++) {
                int address = addresses[i], target = addresses[(i + 1) % addresses.length];
                positions.put(address, i);
                boolean call = i % 16 == 0;
                flows[i] = Optional.of(new InstructionFlow(call ? InstructionFlow.Kind.CALL : InstructionFlow.Kind.JUMP,
                        address + 1, call && target == address + 1, List.of(target)));
                listing[i] = new DisassembledInstruction(address, call ? "CALL" : "JUMP", "00");
            }
        }

        @Override public Optional<InstructionFlow> getFlow(int address) {
            Integer position = positions.get(address);
            return position == null ? Optional.empty() : flows[position];
        }
        @Override public DisassembledInstruction disassemble(int address) { return listing[positions.get(address)]; }
        @Override public int getNextInstructionPosition(int address) { return address + 1; }
    }
}
