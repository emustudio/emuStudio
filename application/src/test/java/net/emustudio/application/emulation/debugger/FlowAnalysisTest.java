/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CancellationException;

import static org.junit.Assert.*;

public class FlowAnalysisTest {
    private final FlowAnalysis.Observations observations = new FlowAnalysis.Observations();
    private final MemoryAnnotations annotations = new MemoryAnnotations();

    @Test
    public void lastExecutedAddressFollowsExecutionOrderAndReset() {
        assertEquals(-1, observations.lastExecutedAddress());
        observed(100, 1, "FIRST", "00", 200);
        assertEquals(100, observations.lastExecutedAddress());
        observed(200, 1, "SECOND", "00", 100);
        assertEquals(200, observations.lastExecutedAddress());
        observed(100, 1, "FIRST", "00", 200);
        assertEquals(100, observations.lastExecutedAddress());
        observations.reset();
        assertEquals(-1, observations.lastExecutedAddress());
    }

    @Test
    public void noInstructionOrDestinationIsInventedBeforeExecution() {
        FlowAnalysis analysis = view(100);

        assertTrue(addresses(analysis).isEmpty());
        assertTrue(edges(analysis).isEmpty());
        assertTrue(analysis.unknown().isEmpty());
    }

    @Test
    public void observedNextPcIsKeptWithoutDecodingItsDestination() {
        observed(100, 3, "EXECUTED", "AA BB CC", 200);

        FlowAnalysis analysis = view(100);

        assertEquals(List.of(100), addresses(analysis));
        assertEquals(Set.of("100:200"), edges(analysis));
        assertEquals("EXECUTED", analysis.mnemonic(100));
        assertNull(analysis.instructionAt(200));
        assertEquals(Map.of(200, "Observed next PC; not executed yet"), analysis.unknown());
        assertFalse(analysis.unknown().containsKey(103));
    }

    @Test
    public void differentExecutedPathsFromSameAddressAccumulate() {
        observed(100, 3, "TEST", "AA BB CC", 103);
        observed(100, 3, "TEST", "AA BB CC", 200);
        observed(103, 1, "NEXT", "00", 300);
        observed(200, 1, "OTHER", "01", 100);

        FlowAnalysis analysis = view(100);

        assertEquals(Set.of("100:103", "100:200", "103:300", "200:100"), edges(analysis));
        assertEquals(List.of(100, 103, 200), addresses(analysis));
    }

    @Test
    public void changedEncodingUpdatesMetadataWithoutRemovingAnyOldEdge() {
        observed(100, 1, "OLD", "00", 101);
        FlowAnalysis before = view(100);

        observed(100, 3, "NEW", "AA BB CC", 500);
        FlowAnalysis after = view(100);

        assertEquals("OLD", before.mnemonic(100));
        assertEquals(Set.of("100:101"), edges(before));
        assertEquals("NEW", after.mnemonic(100));
        assertEquals(103, after.nextAddress(100));
        assertEquals(Set.of("100:101", "100:500"), edges(after));
    }

    @Test
    public void rootSelectionNeverDiscardsLearnedEdges() {
        observed(100, 1, "A", "00", 200);
        observed(100, 1, "A", "00", 300);
        observed(200, 1, "B", "01", 100);
        Set<String> all = edges(view(100));

        view(200);
        view(700);

        assertEquals(all, edges(view(100)));
        assertEquals(Set.of("100:200", "100:300", "200:100"), all);
    }

    @Test
    public void viewIncludesAllReachableAddressesBeyondFormerLimit() {
        int count = 1500;
        for (int address = 0; address < count; address++)
            observed(address, 1, "NOP", "00", address == count - 1 ? 0 : address + 1);

        FlowAnalysis analysis = view(0);

        assertEquals(count, addresses(analysis).size());
        assertEquals(count, edges(analysis).size());
        assertEquals("NOP", analysis.mnemonic(count - 1));
        assertTrue(edges(analysis).contains((count - 1) + ":0"));
    }

    @Test
    public void annotationsChangePresentationWithoutClearingObservedGraph() {
        observed(100, 1, "A", "00", 200);
        FlowAnalysis analysis = view(100);
        annotations.put(new MemoryAnnotations.Entry(8, 10, 100, 101, "data", "DATA", null));

        assertTrue(analysis.isData(100));
        new UserAnnotations(256, annotations).add(new UserAnnotations.Entry(200, "user data", "DATA"));
        assertTrue(analysis.isData(200));
        annotations.put(new MemoryAnnotations.Entry(8, 11, 201, 202, "DATA", "other owner", null));
        assertFalse(analysis.isData(201));
        assertEquals(Set.of("100:200"), edges(view(100)));
        observations.reset();

        assertTrue(addresses(view(100)).isEmpty());
        assertTrue(edges(view(100)).isEmpty());
        assertEquals(1, annotations.at(100).size());
        assertTrue(analysis.isData(200));
    }

    @Test
    public void repeatedObservationDeduplicatesEdgeButSelfLoopsRemain() {
        observed(100, 1, "LOOP", "00", 100);
        observed(100, 1, "LOOP", "00", 100);

        assertEquals(Set.of("100:100"), edges(view(100)));
    }

    @Test
    public void largeAddressExtentClipsGeometryWithoutLosingObservedWrappedEdge() {
        int address = Integer.MAX_VALUE - 1;
        observed(address, 2, "EXECUTED", "00 00", 1);

        FlowAnalysis analysis = view(address);

        assertEquals(Integer.MAX_VALUE, analysis.nextAddress(address));
        assertEquals(Set.of(address + ":1"), edges(analysis));
    }

    @Test
    public void foldingRequiresActualSequentialEdgesAndSingleObservedPath() {
        observed(0, 1, "NOP", "00", 1);
        observed(1, 1, "NOP", "00", 2);
        observed(2, 1, "NOP", "00", 3);
        FlowAnalysis before = view(0);
        assertEquals(List.of(List.of(0, 1, 2)), before.instructionRuns(Set.of(0, 1, 2), Set.of()));

        observed(1, 1, "NOP", "00", 10);
        FlowAnalysis after = view(0);

        assertEquals(List.of(List.of(0, 1), List.of(2)), after.instructionRuns(Set.of(0, 1, 2), Set.of()));
        assertEquals(Set.of("0:1", "1:2", "1:10", "2:3"), edges(after));
    }

    @Test
    public void cancellationAndInvalidViewDoNotMutateObservations() {
        observed(100, 1, "NOP", "00", 200);
        assertThrows(IllegalArgumentException.class, () -> view(-1));
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class, () -> view(100));
        } finally {
            Thread.interrupted();
        }

        assertEquals(Set.of("100:200"), edges(view(100)));
    }

    private void observed(int address, int length, String mnemonic, String encoding, int next) {
        observations.executed(DecodedInstruction.of(address, length, mnemonic, encoding), OptionalInt.of(next));
    }
    private FlowAnalysis view(int root) { return observations.view(root, annotations); }
    private static List<Integer> addresses(FlowAnalysis analysis) {
        List<Integer> result = new ArrayList<>();
        analysis.forEachInstruction(instruction -> result.add(instruction.cells().start));
        return result;
    }
    private static Set<String> edges(FlowAnalysis analysis) {
        Set<String> result = new TreeSet<>();
        analysis.forEachEdge((from, to) -> result.add(from + ":" + to));
        return result;
    }
}
