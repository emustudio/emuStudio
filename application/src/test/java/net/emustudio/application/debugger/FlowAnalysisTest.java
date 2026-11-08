/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.runtime.annotations.MemoryAnnotations;
import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.cpu.InstructionFlow;
import net.emustudio.emulib.plugins.cpu.InvalidInstructionException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FlowAnalysisTest {
    @Test
    public void identicalRunsStopAtProtectedInstructionsAndLeaveAnalysisIntact() throws Exception {
        Disassembler decoder = nopDecoder(6);
        FlowAnalysis analysis = reachable(decoder, 0, 10, index(8));

        List<List<Integer>> runs = analysis.instructionRuns(Set.of(0, 1, 2, 3, 4, 5), Set.of(0, 3));

        assertEquals(List.of(List.of(0), List.of(1, 2), List.of(3), List.of(4, 5)), runs);
        assertEquals(List.of(0, 1, 2, 3, 4, 5), addresses(analysis));
        assertEquals(6, edges(analysis).size());
    }

    @Test
    public void branchTargetsStaySeparateFromIdenticalNeighbors() throws Exception {
        Disassembler decoder = nopDecoder(6);
        instruction(decoder, 6, "JP 3", InstructionFlow.Kind.JUMP, 7, false, List.of(3));
        FlowAnalysis analysis = reachable(decoder, 0, 10, index(8));

        List<List<Integer>> runs = analysis.instructionRuns(Set.of(0, 1, 2, 3, 4, 5, 6), Set.of(0));

        assertEquals(List.of(List.of(0), List.of(1, 2), List.of(3), List.of(4, 5), List.of(6)), runs);
    }

    @Test
    public void equalMnemonicWithDifferentEncodingDoesNotFold() throws Exception {
        Disassembler decoder = nopDecoder(4);
        when(decoder.disassemble(2)).thenReturn(new DisassembledInstruction(2, "NOP", "DD00"));
        FlowAnalysis analysis = reachable(decoder, 0, 10, index(6));

        List<List<Integer>> runs = analysis.instructionRuns(Set.of(0, 1, 2, 3), Set.of());

        assertEquals(List.of(List.of(0, 1), List.of(2), List.of(3)), runs);
    }

    @Test
    public void missingEncodingAndDifferentMnemonicDoNotFold() throws Exception {
        Disassembler decoder = nopDecoder(4);
        when(decoder.disassemble(1)).thenReturn(new DisassembledInstruction(1, "NOP", ""));
        when(decoder.disassemble(3)).thenReturn(new DisassembledInstruction(3, "INC A", "00"));
        FlowAnalysis analysis = reachable(decoder, 0, 10, index(6));

        List<List<Integer>> runs = analysis.instructionRuns(Set.of(0, 1, 2, 3), Set.of());

        assertEquals(List.of(List.of(0), List.of(1), List.of(2), List.of(3)), runs);
    }

    @Test
    public void onlyContiguousLinearFallThroughInstructionsFold() throws Exception {
        Disassembler decoder = nopDecoder(5);
        instruction(decoder, 1, "NOP", InstructionFlow.Kind.CALL, 2, true, List.of(4));
        when(decoder.disassemble(1)).thenReturn(new DisassembledInstruction(1, "NOP", "00"));
        FlowAnalysis analysis = reachable(decoder, 0, 10, index(8));

        List<List<Integer>> runs = analysis.instructionRuns(Set.of(0, 1, 3, 4), Set.of());

        assertEquals(List.of(List.of(0), List.of(1), List.of(3), List.of(4)), runs);
    }

    private static Disassembler nopDecoder(int count) throws Exception {
        Disassembler decoder = mock(Disassembler.class);
        for (int at = 0; at < count; at++) {
            instruction(decoder, at, "NOP", InstructionFlow.Kind.LINEAR, at + 1, true, List.of());
            when(decoder.disassemble(at)).thenReturn(new DisassembledInstruction(at, "NOP", "00"));
        }
        return decoder;
    }

    @Test
    public void analysisStartsAtRequestedAddressAndFollowsBackwardBranches() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 3, "JP 1", InstructionFlow.Kind.JUMP, 4, true, List.of(1));
        instruction(disassembler, 1, "RET", InstructionFlow.Kind.RETURN, 2, false, List.of());
        instruction(disassembler, 4, "HALT", InstructionFlow.Kind.STOP, 5, false, List.of());

        FlowAnalysis analysis = reachable(disassembler, 3, 10, index(8));

        assertEquals(List.of(1, 3, 4), addresses(analysis));
        assertEquals(3, predecessors(analysis, 1).get(0).from);
        verify(disassembler, never()).getFlow(0);
        verify(disassembler, never()).getFlow(2);
    }

    @Test
    public void callVisitsTargetAndFallThroughAndDistinguishesIncomingFromCalls() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "CALL 4", InstructionFlow.Kind.CALL, 1, true, List.of(4));
        instruction(disassembler, 1, "HALT", InstructionFlow.Kind.STOP, 2, false, List.of());
        instruction(disassembler, 4, "RET", InstructionFlow.Kind.RETURN, 5, false, List.of());

        FlowAnalysis analysis = reachable(disassembler, 0, 10, index(8));

        assertEquals(List.of(0, 1, 4), addresses(analysis));
        assertEquals("CALL 4", analysis.mnemonic(0));
        assertEquals("RET", analysis.mnemonic(4));
        assertEquals(2, edges(analysis).size());
        FlowAnalysis.Edge call = predecessors(analysis, 4).get(0);
        assertEquals(0, call.from);
        assertEquals(4, call.to);
        assertEquals("call", call.kind);
        assertEquals("next", predecessors(analysis, 1).get(0).kind);
        assertEquals(1, callSites(analysis, 4).size());
        assertTrue(callSites(analysis, 1).isEmpty());
        assertFalse(analysis.truncated());
    }

    @Test
    public void loopsAndDuplicateTargetsDecodeOnceAndKeepDistinctEdgeKinds() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "CALL 0", InstructionFlow.Kind.CALL, 0, true, List.of(0, 0, 2));
        instruction(disassembler, 2, "JP 0", InstructionFlow.Kind.JUMP, 3, false, List.of(0));

        FlowAnalysis analysis = reachable(disassembler, 0, 10, index(4));

        assertEquals(List.of(0, 2), addresses(analysis));
        assertEquals(4, edges(analysis).size());
        assertEquals(3, predecessors(analysis, 0).size());
        assertEquals(1, callSites(analysis, 0).size());
        assertFalse(analysis.truncated());
        verify(disassembler).getFlow(0);
        verify(disassembler).getFlow(2);
    }

    @Test
    public void boundRetainsKnownUndecodedTargetAndMarksPartialFlow() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "NOP", InstructionFlow.Kind.LINEAR, 1, true, List.of());

        ProgramIndex index = index(4);
        FlowAnalysis analysis = reachable(disassembler, 0, 1, index);

        assertEquals(List.of(0), addresses(analysis));
        assertTrue(analysis.truncated());
        assertNull(index.snapshot().instructionAt(1));
        assertEquals("next", predecessors(analysis, 1).get(0).kind);
        verify(disassembler, never()).getFlow(1);
    }

    @Test
    public void reachableDataRegionFromAnyOwnerIsAnalyzedAndMarked() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "JP 3", InstructionFlow.Kind.JUMP, 1, false, List.of(3));
        instruction(disassembler, 3, "HALT", InstructionFlow.Kind.STOP, 4, false, List.of());
        ProgramIndex index = index(6);
        index.annotationEngine().put(new net.emustudio.emulib.runtime.annotations.MemoryAnnotations.Entry(
                8, 10, 2, 5, ProgramIndex.Kinds.DATA, "table", null));

        FlowAnalysis analysis = reachable(disassembler, 0, 10, index);

        assertEquals(List.of(0, 3), addresses(analysis));
        assertEquals(3, predecessors(analysis, 3).get(0).to);
        assertFalse(analysis.truncated());
        assertFalse(analysis.isData(0));
        assertTrue(analysis.isData(3));
    }

    @Test
    public void indirectJumpIsUnresolvedWithoutInventedEdgeOrTruncation() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "JP (HL)", InstructionFlow.Kind.JUMP, 1, false, List.of());

        ProgramIndex index = index(4);
        FlowAnalysis analysis = reachable(disassembler, 0, 10, index);

        assertEquals(List.of(0), addresses(analysis));
        assertTrue(edges(analysis).isEmpty());
        assertEquals(ProgramIndex.InstructionKind.JUMP, index.snapshot().instructionAt(0).kind());
        assertFalse(analysis.truncated());
    }

    @Test
    public void undecodableAddressesAreReportedAsUnknownNodesWithReason() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "CALL 8", InstructionFlow.Kind.CALL, 1, true, List.of(8));
        when(disassembler.getFlow(8)).thenThrow(new IllegalArgumentException("bad opcode"));
        instruction(disassembler, 1, "HALT", InstructionFlow.Kind.STOP, 2, false, List.of());

        FlowAnalysis analysis = reachable(disassembler, 0, 10, index(16));

        assertEquals(java.util.Map.of(8, "bad opcode"), analysis.unknown());
    }

    @Test
    public void manyTargetsAreAllIndexedWithoutHiddenEdgeCap() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        List<Integer> targets = new java.util.ArrayList<>();
        for (int target = 1; target < 3000; target++) {
            targets.add(target);
            instruction(disassembler, target, "HALT", InstructionFlow.Kind.STOP, target + 1, false, List.of());
        }
        instruction(disassembler, 0, "SWITCH", InstructionFlow.Kind.JUMP, 1, false, targets);

        FlowAnalysis analysis = reachable(disassembler, 0, 5000, index(4096));

        assertEquals(2999, edges(analysis).size());
        assertEquals(3000, addresses(analysis).size());
        assertFalse(analysis.truncated());
    }

    @Test
    public void undecodableTargetIsNotRetriedAndDoesNotBlockFallThrough() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "CALL 8", InstructionFlow.Kind.CALL, 1, true, List.of(8, 8));
        when(disassembler.getFlow(8)).thenThrow(new IndexOutOfBoundsException("unavailable"));
        instruction(disassembler, 1, "HALT", InstructionFlow.Kind.STOP, 2, false, List.of());

        FlowAnalysis analysis = reachable(disassembler, 0, 10, index(16));

        assertEquals(List.of(0, 1), addresses(analysis));
        assertEquals(1, predecessors(analysis, 8).size());
        assertFalse(analysis.truncated());
        verify(disassembler).getFlow(8);
    }

    @Test
    public void outOfSpaceTargetsAndInvalidInstructionExtentAreNotIndexed() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "JP 9", InstructionFlow.Kind.JUMP, 1, false, List.of(-1, 9));
        FlowAnalysis analysis = reachable(disassembler, 0, 10, index(4));
        assertTrue(edges(analysis).isEmpty());

        when(disassembler.getNextInstructionPosition(0)).thenReturn(5);
        assertTrue(addresses(reachable(disassembler, 0, 10, index(4))).isEmpty());
    }

    @Test
    public void malformedDecoderInvalidTargetsAreIgnoredWithoutTruncation() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "JUMP", InstructionFlow.Kind.JUMP, 1, false,
                Collections.nCopies(1000, -1));

        FlowAnalysis analysis = reachable(disassembler, 0, 1, index(4));

        assertFalse(analysis.truncated());
        assertTrue(edges(analysis).isEmpty());
        assertEquals(List.of(0), addresses(analysis));
    }

    @Test
    public void edgesConnectCpuVisibleInstructionAddresses() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "CALL 6", InstructionFlow.Kind.CALL, 2, false, List.of(6));
        instruction(disassembler, 6, "JP 0", InstructionFlow.Kind.JUMP, 7, false, List.of(0));
        ProgramIndex index = ProgramIndex.create(8, new MemoryAnnotations());

        FlowAnalysis analysis = FlowAnalysis.reachable(disassembler, 0, 10, index).publish();

        assertNotNull(index.snapshot().instructionAt(0));
        assertNotNull(index.snapshot().instructionAt(6));
        FlowAnalysis.Edge call = predecessors(analysis, 6).get(0);
        assertEquals(0, call.from);
        assertEquals(6, call.to);
        FlowAnalysis.Edge branch = predecessors(analysis, 0).get(0);
        assertEquals(6, branch.from);
        assertEquals(0, branch.to);
    }

    @Test
    public void mutationOfNonFirstCellMakesCurrentFlowUnavailableButRetainsOldSnapshot() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "LD A,1", InstructionFlow.Kind.LINEAR, 2, true, List.of());
        ProgramIndex index = index(4);
        FlowAnalysis analysis = reachable(disassembler, 0, 1, index);

        ProgramIndex.Snapshot old = index.snapshot();
        assertEquals(2, old.instructionAt(0).cells().end);
        index.memoryChanged(); // Adapter reports a write to either covered cell, including address 1.

        assertFalse(analysis.isCurrent(index));
        assertEquals(ProgramIndex.FlowState.STALE, index.snapshot().flowState());
        assertNull(index.snapshot().instructionAt(0));
        assertNotNull(old.instructionAt(0));
        assertFalse(analysis.isCurrent(index(4)));
    }

    @Test
    public void changedContentOrMemoryRejectsPendingResultAndDoesNotReplayIt() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "HALT", InstructionFlow.Kind.STOP, 1, false, List.of());
        ProgramIndex index = index(4);
        FlowAnalysis.Pending pending = FlowAnalysis.reachable(disassembler, 0, 10, index);
        index.annotationEngine().put(new net.emustudio.emulib.runtime.annotations.MemoryAnnotations.Entry(
                1, 1, 0, 1, "comment", "changed", null));
        assertThrows(IllegalStateException.class, pending::publish);
        assertEquals(ProgramIndex.FlowState.NOT_BUILT, index.snapshot().flowState());
        assertThrows(IllegalStateException.class, pending::publish);

        FlowAnalysis.Pending memoryPending = FlowAnalysis.reachable(disassembler, 0, 10, index);
        index.memoryChanged();
        assertThrows(IllegalStateException.class, memoryPending::publish);
    }

    @Test
    public void independentBreakpointChangeSurvivesGraphPublicationAndDoesNotStaleUiView() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "HALT", InstructionFlow.Kind.STOP, 1, false, List.of());
        ProgramIndex index = index(4);
        FlowAnalysis.Pending pending = FlowAnalysis.reachable(disassembler, 0, 10, index);
        try (ProgramIndex.BreakpointUpdate update = index.updateBreakpoints(index.snapshot())) {
            update.put(7, 0, true);
            update.commit();
        }

        FlowAnalysis analysis = pending.publish();

        assertTrue(index.snapshot().hasExecutionBreakpoint(0));
        assertTrue(analysis.isCurrent(index));
        try (ProgramIndex.BreakpointUpdate update = index.updateBreakpoints(index.snapshot())) {
            update.remove(7);
            update.commit();
        }
        assertTrue(analysis.isCurrent(index));
    }

    @Test
    public void decoderCaptureCannotBeRelabeledWithNewerMemoryRevision() throws Exception {
        Disassembler capturedDecoder = mock(Disassembler.class);
        instruction(capturedDecoder, 0, "HALT", InstructionFlow.Kind.STOP, 1, false, List.of());
        ProgramIndex index = index(4);
        ProgramIndex.Snapshot capturedBase = index.snapshot();
        index.memoryChanged();

        FlowAnalysis.Pending pending = FlowAnalysis.reachable(capturedDecoder, 0, 10, index, capturedBase);

        assertThrows(IllegalStateException.class, pending::publish);
        assertNull(index.snapshot().instructionAt(0));
    }

    @Test
    public void invalidBoundsAreRejectedBeforeUsingDisassembler() {
        Disassembler disassembler = mock(Disassembler.class);
        ProgramIndex index = index(4);

        assertThrows(IllegalArgumentException.class, () -> FlowAnalysis.reachable(disassembler, -1, 1, index));
        assertThrows(IllegalArgumentException.class, () -> FlowAnalysis.reachable(disassembler, 4, 1, index));
        assertThrows(IllegalArgumentException.class, () -> FlowAnalysis.reachable(disassembler, 0, 0, index));

        verifyNoInteractions(disassembler);
    }

    private static ProgramIndex index(int size) { return ProgramIndex.create(size, new MemoryAnnotations()); }

    private static FlowAnalysis reachable(Disassembler disassembler, int start, int limit, ProgramIndex index) {
        return FlowAnalysis.reachable(disassembler, start, limit, index).publish();
    }

    private static List<Integer> addresses(FlowAnalysis analysis) {
        List<Integer> result = new ArrayList<>();
        analysis.forEachInstruction(instruction -> result.add(instruction.cells().start));
        return result;
    }

    private static List<String> edges(FlowAnalysis analysis) {
        List<String> result = new ArrayList<>();
        analysis.forEachEdge((from, to, kind) -> result.add(from + ":" + to + ":" + kind));
        return result;
    }

    private static List<FlowAnalysis.Edge> predecessors(FlowAnalysis analysis, int address) {
        List<FlowAnalysis.Edge> result = new ArrayList<>();
        analysis.forEachEdge((from, to, kind) -> {
            if (to == address) result.add(new FlowAnalysis.Edge(from, to, kind));
        });
        return result;
    }

    private static List<FlowAnalysis.Edge> callSites(FlowAnalysis analysis, int address) {
        List<FlowAnalysis.Edge> result = new ArrayList<>();
        analysis.forEachEdge((from, to, kind) -> {
            if (to == address && kind == ProgramIndex.EdgeKind.CALL) result.add(new FlowAnalysis.Edge(from, to, kind));
        });
        return result;
    }

    private static void instruction(Disassembler disassembler, int address, String mnemonic,
                                    InstructionFlow.Kind kind, int next, boolean fallThrough,
                                    List<Integer> targets) throws InvalidInstructionException {
        when(disassembler.getFlow(address)).thenReturn(Optional.of(new InstructionFlow(kind, next, fallThrough, targets)));
        when(disassembler.getNextInstructionPosition(address)).thenReturn(next > address ? next : address + 1);
        when(disassembler.disassemble(address)).thenReturn(new DisassembledInstruction(address, mnemonic, ""));
    }
}
