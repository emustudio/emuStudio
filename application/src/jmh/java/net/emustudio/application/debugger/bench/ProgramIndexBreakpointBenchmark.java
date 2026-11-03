/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.bench;

import net.emustudio.application.debugger.index.ProgramIndex;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Presence/candidate queries exclude expression evaluation and hit-counter work. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
public class ProgramIndexBreakpointBenchmark {
    @State(Scope.Thread)
    public static class Rules {
        @Param({"0", "1", "1000", "10000"}) public int rules;
        @Param({"single", "banked", "sparse"}) public String layout;
        ProgramIndex index;
        ProgramIndex.Snapshot snapshot;
        int size, banks, common, cursor;
        final RuleSink sink = new RuleSink();

        @Setup public void setup() {
            size = layout.equals("sparse") ? Integer.MAX_VALUE : 65536;
            banks = layout.equals("single") ? 1 : 4;
            common = layout.equals("single") ? size : size - 16384;
            index = create();
            snapshot = index.snapshot();
        }

        ProgramIndex create() {
            ProgramIndex created = ProgramIndex.create("rules", size, banks, common);
            try (ProgramIndex.BreakpointUpdate update = created.updateBreakpoints(created.snapshot())) {
                for (int i = 0; i < rules; i++) update.put(i, address(i), i % 3 != 2, condition(i));
                update.commit();
            }
            return created;
        }

        int address(int rule) {
            if (!layout.equals("single") && rule % 16 <= 1) return common + rule / 16;
            return layout.equals("sparse") ? rule * 100003 : rule * 3;
        }
        String condition(int rule) { return rule % 4 == 0 ? "A == 1" : ""; }
        int hitRule() {
            if (rules == 0) return 0;
            int rule = (cursor++ & Integer.MAX_VALUE) % rules;
            return rule % 3 == 2 ? rule - 1 : rule;
        }
    }

    @Benchmark public boolean indexedHit(Rules state) {
        int rule = state.hitRule();
        return state.snapshot.hasExecutionBreakpoint(state.address(rule));
    }

    @Benchmark public boolean indexedMiss(Rules state) {
        int address = state.size - 1 - (state.cursor++ & 127);
        return state.snapshot.hasExecutionBreakpoint(address);
    }

    @Benchmark public long candidates(Rules state) {
        int rule = state.hitRule();
        state.sink.checksum = 0;
        state.snapshot.forEachExecutionBreakpoint(state.address(rule), state.sink);
        return state.sink.checksum;
    }

    /** Independent presence oracle; old DebugSession had no presence-only query with this contract. */
    @Benchmark public boolean scanPresenceOracle(Rules state) {
        int rule = state.hitRule(), address = state.address(rule);
        for (int i = 0; i < state.rules; i++)
            if (i % 3 != 2 && state.address(i) == address) return true;
        return false;
    }

    @Benchmark @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public Object fullRebuild(Rules state) { return state.create(); }

    @Benchmark @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public ProgramIndex.CommitResult editOneRule(Rules state) {
        try (ProgramIndex.BreakpointUpdate update = state.index.updateBreakpoints(state.index.snapshot())) {
            if (state.rules == 0) update.remove(0);
            else update.put(0, state.address(0), (state.cursor++ & 1) == 0, "");
            return update.commit();
        }
    }

    static final class RuleSink implements Consumer<ProgramIndex.Breakpoint> {
        long checksum;
        @Override public void accept(ProgramIndex.Breakpoint rule) { checksum += rule.id() + 1; }
    }
}
