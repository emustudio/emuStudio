/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.plugin.debugger.ExecutedBlock;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class FlowAnalysisTest {
    private final FlowAnalysis.Observations observations = new FlowAnalysis.Observations();

    @Test
    public void repeatedSingleStepsKeepEveryExecutionAndNewBranchDestination() {
        DecodedInstruction first = DecodedInstruction.of(100, 1, "FIRST", "01");
        DecodedInstruction branch = DecodedInstruction.of(101, 1, "BRANCH", "02");
        observations.executed(first, OptionalInt.of(101));
        observations.executed(branch, OptionalInt.of(100));
        observations.executed(first, OptionalInt.of(101));
        Set<String> before = edges(observations);
        observations.executed(branch, OptionalInt.of(100));
        observations.executed(first, OptionalInt.of(101));
        observations.executed(branch, OptionalInt.of(200));
        observations.executed(branch, OptionalInt.empty());
        observations.executed(branch, OptionalInt.empty());

        assertEquals(8, observations.revision());
        assertEquals(101, observations.lastExecutedAddress());
        assertEquals(Set.of("100:101", "101:100", "101:200"), edges(observations));
        assertEquals(Set.of("100:101", "101:100"), before);
        assertEquals(-1, observations.nextInstruction(101));
    }

    @Test
    public void previouslyMergedSingleStepRestoresFactsChangedByBlockExecution() {
        DecodedInstruction original = DecodedInstruction.of(100, 1, "ORIGINAL", "00");
        observations.executed(DecodedInstruction.of(101, 1, "ORIGINAL", "00"), OptionalInt.empty());
        observations.executed(original, OptionalInt.of(101));
        List<DecodedInstruction> changes = List.of(
                DecodedInstruction.of(100, 2, "ORIGINAL", "00"),
                DecodedInstruction.of(100, 1, "CHANGED", "00"),
                DecodedInstruction.of(100, 1, "ORIGINAL", "FF"));
        for (DecodedInstruction changed : changes) {
            observations.executed(new ExecutedBlock(List.of(changed),
                    List.of(new ExecutedBlock.Transition(100, 101)), 100, 101));
            assertEquals(List.of(List.of(100), List.of(101)), observations.instructionRuns());
            observations.executed(original, OptionalInt.of(101));
            assertEquals(List.of(List.of(100, 101)), observations.instructionRuns());
        }
        assertEquals(Set.of("100:101"), edges(observations));
    }

    @Test
    public void resetAllowsPreviouslyMergedSingleStepToRestoreAllObservations() {
        DecodedInstruction instruction = DecodedInstruction.of(100, 1, "NOP", "00");
        observations.executed(instruction, OptionalInt.of(200));
        observations.executed(instruction, OptionalInt.of(200));
        observations.reset();
        observations.executed(instruction, OptionalInt.of(200));

        assertEquals(List.of(100), addresses(observations));
        assertEquals(Set.of("100:200"), edges(observations));
        assertEquals(100, observations.lastExecutedAddress());
        assertEquals(4, observations.revision());
    }

    @Test
    public void compactBlockMatchesIndividualExecutionIncludingLoopsAndFinalAddress() {
        DecodedInstruction first = DecodedInstruction.of(100, 1, "FIRST", "01");
        DecodedInstruction loop = DecodedInstruction.of(101, 1, "LOOP", "02");
        DecodedInstruction last = DecodedInstruction.of(200, 1, "LAST", "03");
        observations.executed(first, OptionalInt.of(101));
        observations.executed(loop, OptionalInt.of(100));
        observations.executed(first, OptionalInt.of(101));
        observations.executed(loop, OptionalInt.of(200));
        observations.executed(last, OptionalInt.of(300));
        FlowAnalysis.Observations expected = observations;
        FlowAnalysis.Observations compact = new FlowAnalysis.Observations();

        compact.executed(new ExecutedBlock(List.of(last, first, loop), List.of(
                new ExecutedBlock.Transition(100, 101), new ExecutedBlock.Transition(101, 100),
                new ExecutedBlock.Transition(101, 200), new ExecutedBlock.Transition(200, 300)), 200, 300));

        FlowAnalysis.Observations actual = compact;
        assertEquals(addresses(expected), addresses(actual));
        assertEquals(edges(expected), edges(actual));
        assertEquals(200, compact.lastExecutedAddress());
        assertEquals(1, compact.revision());
    }

    @Test
    public void repeatedBlockAdvancesRevisionAndLastAddressWhilePreservingNewSingleStepEdges() {
        DecodedInstruction first = DecodedInstruction.of(100, 1, "FIRST", "01");
        DecodedInstruction last = DecodedInstruction.of(101, 1, "LAST", "02");
        ExecutedBlock block = new ExecutedBlock(List.of(first, last), List.of(
                new ExecutedBlock.Transition(100, 101), new ExecutedBlock.Transition(101, 200)), 101, 200);
        observations.executed(block);
        Set<String> before = edges(observations);
        observations.executed(DecodedInstruction.of(100, 1, "FIRST", "01"), OptionalInt.of(300));

        observations.executed(block);

        assertEquals(3, observations.revision());
        assertEquals(101, observations.lastExecutedAddress());
        assertEquals(Set.of("100:101", "100:300", "101:200"), edges(observations));
        assertEquals(Set.of("100:101", "101:200"), before);
    }

    @Test
    public void repeatedBlockRestoresFactsAfterLengthMnemonicOrEncodingChanges() {
        DecodedInstruction first = DecodedInstruction.of(100, 1, "NOP", "00");
        DecodedInstruction last = DecodedInstruction.of(101, 1, "NOP", "00");
        ExecutedBlock block = new ExecutedBlock(List.of(first, last), List.of(
                new ExecutedBlock.Transition(100, 101), new ExecutedBlock.Transition(101, 102)), 101, 102);
        List<DecodedInstruction> changes = List.of(
                DecodedInstruction.of(100, 2, "NOP", "00"),
                DecodedInstruction.of(100, 1, "CHANGED", "00"),
                DecodedInstruction.of(100, 1, "NOP", "FF"));
        observations.executed(block);

        for (DecodedInstruction changed : changes) {
            observations.executed(changed, OptionalInt.of(101));
            assertEquals(List.of(List.of(100), List.of(101)), observations.instructionRuns());

            observations.executed(block);

            assertEquals(List.of(List.of(100, 101)), observations.instructionRuns());
        }
        assertEquals(7, observations.revision());
    }

    @Test
    public void resetAllowsPreviouslyMergedBlockToRestoreAllObservations() {
        ExecutedBlock block = new ExecutedBlock(List.of(DecodedInstruction.of(100, 1, "NOP", "00")),
                List.of(new ExecutedBlock.Transition(100, 200)), 100, 200);
        observations.executed(block);
        observations.reset();
        assertTrue(addresses(observations).isEmpty());
        assertTrue(edges(observations).isEmpty());

        observations.executed(block);

        assertEquals(List.of(100), addresses(observations));
        assertEquals(Set.of("100:200"), edges(observations));
        assertEquals(100, observations.lastExecutedAddress());
        assertEquals(3, observations.revision());
    }

    @Test
    public void incidentEdgesIncludeIncomingOutgoingAndSelfLoopOnceInAddressOrder() {
        observed(200, 1, "INCOMING", "00", 100);
        observed(100, 1, "OUTGOING", "00", 300);
        observed(100, 1, "LOOP", "00", 100);
        observed(100, 1, "LOOP", "00", 100);
        observed(10, 1, "INCOMING", "00", 100);
        observed(100, 1, "NEXT", "00", 101);
        observed(101, 1, "UNRELATED", "00", 102);

        List<FlowAnalysis.Edge> edges = observations.edgesAt(100);

        assertEquals(List.of("10:100", "100:100", "100:101", "100:300", "200:100"),
                edges.stream().map(edge -> edge.from + ":" + edge.to).collect(Collectors.toList()));
        assertTrue(observations.edgesAt(999).isEmpty());
    }

    @Test
    public void incidentEdgeSnapshotsAreImmutableAndResetClearsBothDirections() {
        observed(100, 1, "FIRST", "00", 200);
        List<FlowAnalysis.Edge> captured = observations.edgesAt(200);
        assertThrows(UnsupportedOperationException.class, () -> captured.clear());

        observed(200, 1, "SECOND", "00", 300);
        observations.reset();

        assertEquals(1, captured.size());
        assertEquals(100, captured.get(0).from);
        assertEquals(200, captured.get(0).to);
        assertTrue(observations.edgesAt(100).isEmpty());
        assertTrue(observations.edgesAt(200).isEmpty());
        assertTrue(observations.edgesAt(300).isEmpty());
    }

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
        FlowAnalysis.Observations analysis = observations;

        assertTrue(addresses(analysis).isEmpty());
        assertTrue(edges(analysis).isEmpty());
    }

    @Test
    public void observedNextPcIsKeptWithoutDecodingItsDestination() {
        observed(100, 3, "EXECUTED", "AA BB CC", 200);

        FlowAnalysis.Observations analysis = observations;

        assertEquals(List.of(100), addresses(analysis));
        assertEquals(Set.of("100:200"), edges(analysis));
    }

    @Test
    public void differentExecutedPathsFromSameAddressAccumulate() {
        observed(100, 3, "TEST", "AA BB CC", 103);
        observed(100, 3, "TEST", "AA BB CC", 200);
        observed(103, 1, "NEXT", "00", 300);
        observed(200, 1, "OTHER", "01", 100);

        FlowAnalysis.Observations analysis = observations;

        assertEquals(Set.of("100:103", "100:200", "103:300", "200:100"), edges(analysis));
        assertEquals(List.of(100, 103, 200), addresses(analysis));
    }

    @Test
    public void historyIncludesAllExecutedAddressesWithoutViewportLimit() {
        int count = 1500;
        for (int address = 0; address < count; address++)
            observed(address, 1, "NOP", "00", address == count - 1 ? 0 : address + 1);

        FlowAnalysis.Observations analysis = observations;

        assertEquals(count, addresses(analysis).size());
        assertEquals(count, edges(analysis).size());
        assertTrue(edges(analysis).contains((count - 1) + ":0"));
    }

    @Test
    public void repeatedObservationDeduplicatesEdgeButSelfLoopsRemain() {
        observed(100, 1, "LOOP", "00", 100);
        observed(100, 1, "LOOP", "00", 100);

        assertEquals(Set.of("100:100"), edges(observations));
    }

    @Test
    public void foldingRequiresActualSequentialEdgesAndSingleObservedPath() {
        observed(0, 1, "NOP", "00", 1);
        observed(1, 1, "NOP", "00", 2);
        observed(2, 1, "NOP", "00", 3);
        assertEquals(List.of(List.of(0, 1, 2)), observations.instructionRuns());

        observed(1, 1, "NOP", "00", 10);


        assertEquals(List.of(List.of(0, 1), List.of(2)), observations.instructionRuns());
        assertEquals(Set.of("0:1", "1:2", "1:10", "2:3"), edges(observations));
    }

    private void observed(int address, int length, String mnemonic, String encoding, int next) {
        observations.executed(DecodedInstruction.of(address, length, mnemonic, encoding), OptionalInt.of(next));
    }
    private static List<Integer> addresses(FlowAnalysis.Observations observations) {
        return observations.instructionsBefore(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }
    private static Set<String> edges(FlowAnalysis.Observations observations) {
        Set<String> result = new TreeSet<>();
        for (int address : addresses(observations)) {
            for (FlowAnalysis.Edge edge : observations.edgesAt(address)) result.add(edge.from + ":" + edge.to);
        }
        return result;
    }
}
