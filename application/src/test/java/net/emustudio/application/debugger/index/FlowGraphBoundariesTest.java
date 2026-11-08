/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.index;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static org.junit.Assert.*;

public class FlowGraphBoundariesTest {
    @Test public void replacementPrunesObsoleteChainAndCapturedWindowStaysImmutable() {
        FlowGraph.Boundaries boundaries = new FlowGraph.Boundaries();
        boundaries.record(0, 2);
        boundaries.record(2, 4);
        boundaries.record(4, 5);
        boundaries.record(5, 6);
        FlowGraph.Boundaries.Window before = boundaries.capture(0, 6);
        boundaries.record(0, 5);
        assertEquals(5, boundaries.nextAt(0));
        assertEquals(-1, boundaries.nextAt(2));
        assertEquals(-1, boundaries.nextAt(4));
        assertEquals(-1, boundaries.nextAt(5));
        assertEquals(0, boundaries.previousStart(5));
        assertEquals(5, boundaries.longestInstructionSize());
        assertEquals(4, before.size());
        assertEquals(2, before.endAt(0));
        assertEquals(5, before.startAt(3));
        boundaries.record(5, 6);
        assertEquals(5, boundaries.previousStart(6));
        assertEquals(0, boundaries.previousStart(5));
    }

    @Test public void halfOpenFlushAndMaximumCellCoverageDoNotOverflow() {
        FlowGraph.Boundaries boundaries = new FlowGraph.Boundaries();
        boundaries.record(0, 1);
        boundaries.record(1023, 1024);
        boundaries.record(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        boundaries.flush(0, 1023);
        assertEquals(-1, boundaries.nextAt(0));
        assertEquals(1024, boundaries.nextAt(1023));
        FlowGraph.Boundaries.Window last = boundaries.capture(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        assertEquals(1, last.size());
        assertEquals(Integer.MAX_VALUE, last.endAt(0));
        assertThrows(IllegalArgumentException.class, () -> boundaries.record(0, 0));
        assertThrows(IllegalArgumentException.class, () -> boundaries.record(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> boundaries.record(Integer.MAX_VALUE, Integer.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> boundaries.flush(3, 2));
        assertThrows(IllegalArgumentException.class, () -> boundaries.capture(3, 2));
    }

    @Test public void randomizedInsertionsAndWindowsMatchIndependentSortedOracle() {
        FlowGraph.Boundaries boundaries = new FlowGraph.Boundaries();
        Map<Integer, Integer> oracle = new TreeMap<>();
        List<Integer> starts = new ArrayList<>();
        for (int i = 0; i < 1000; i++) starts.add(i * 3);
        Collections.shuffle(starts, new Random(454));
        for (int start : starts) {
            boundaries.record(start, start + 2);
            oracle.put(start, start + 2);
        }
        Random random = new Random(266);
        for (int test = 0; test < 300; test++) {
            int from = random.nextInt(3000), to = from + random.nextInt(3001 - from);
            FlowGraph.Boundaries.Window found = boundaries.capture(from, to);
            int count = 0, previous = -1;
            for (Map.Entry<Integer, Integer> entry : oracle.entrySet()) {
                if (entry.getKey() < from) previous = entry.getKey();
                if (entry.getKey() < from || entry.getKey() > to) continue;
                assertEquals(entry.getKey().intValue(), found.startAt(count));
                assertEquals(entry.getValue().intValue(), found.endAt(count++));
            }
            assertEquals(count, found.size());
            assertEquals(previous, boundaries.previousStart(from));
            assertEquals(oracle.getOrDefault(from, -1).intValue(), boundaries.nextAt(from));
        }
    }
}
