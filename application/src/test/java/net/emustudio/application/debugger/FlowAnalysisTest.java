/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FlowAnalysisTest {
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
        FlowAnalysis.Edge call = analysis.predecessors(4).get(0);
        assertEquals(0, call.from);
        assertEquals(4, call.to);
        assertEquals("call", call.kind);
        assertEquals("next", analysis.predecessors(1).get(0).kind);
        assertEquals(1, analysis.callSites(4).size());
        assertTrue(analysis.callSites(1).isEmpty());
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
        assertEquals(3, analysis.predecessors(0).size());
        assertEquals(1, analysis.callSites(0).size());
        assertFalse(analysis.truncated());
        verify(disassembler).getFlow(0);
        verify(disassembler).getFlow(2);
    }

    @Test
    public void boundRetainsKnownUndecodedTargetAndMarksPartialFlow() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "NOP", InstructionFlow.Kind.LINEAR, 1, true, List.of());

        FlowAnalysis analysis = reachable(disassembler, 0, 1, index(4));

        assertEquals(List.of(0), addresses(analysis));
        assertTrue(analysis.truncated());
        assertNull(analysis.snapshot().instructionAt(0, 1));
        assertEquals("next", analysis.predecessors(1).get(0).kind);
        verify(disassembler, never()).getFlow(1);
    }

    @Test
    public void coveringDataRegionFromAnyOwnerStopsDecodeAtReachableTarget() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "JP 3", InstructionFlow.Kind.JUMP, 1, false, List.of(3));
        ProgramIndex index = index(6);
        try (ProgramIndex.ContentUpdate update = index.updateContent(index.snapshot())) {
            update.putAnnotation(8, 10, new ProgramIndex.Range(0, 2, 5), ProgramIndex.Kinds.DATA, "table");
            assertEquals(ProgramIndex.CommitResult.PUBLISHED, update.commit());
        }

        FlowAnalysis analysis = reachable(disassembler, 0, 10, index);

        assertEquals(List.of(0), addresses(analysis));
        assertEquals(3, analysis.predecessors(3).get(0).to);
        assertFalse(analysis.truncated());
        verify(disassembler, never()).getFlow(3);
    }

    @Test
    public void indirectJumpIsUnresolvedWithoutInventedEdgeOrTruncation() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "JP (HL)", InstructionFlow.Kind.JUMP, 1, false, List.of());

        FlowAnalysis analysis = reachable(disassembler, 0, 10, index(4));

        assertEquals(List.of(0), addresses(analysis));
        assertTrue(edges(analysis).isEmpty());
        assertTrue(analysis.snapshot().instructionAt(0, 0).unresolvedSuccessors());
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
        assertEquals(1, analysis.predecessors(8).size());
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
    public void sweepUsesExplicitExtentsAndQueriesIncomingWithoutRedecoding() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "JP 4", InstructionFlow.Kind.JUMP, 2, false, List.of(4));
        instruction(disassembler, 2, "CALL 4", InstructionFlow.Kind.CALL, 4, true, List.of(4));
        instruction(disassembler, 4, "HALT", InstructionFlow.Kind.STOP, 5, false, List.of());
        ProgramIndex index = index(5);

        FlowAnalysis analysis = FlowAnalysis.sweep(disassembler, 10, index, 0).publish();
        clearInvocations(disassembler);

        assertEquals(List.of(0, 2, 4), addresses(analysis));
        assertEquals(3, analysis.predecessors(4).size());
        assertEquals(1, analysis.callSites(4).size());
        assertFalse(analysis.truncated());
        verifyNoInteractions(disassembler);
    }

    @Test
    public void sweepMarksItsWorkBoundRatherThanClaimingCompleteMemoryCoverage() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "NOP", InstructionFlow.Kind.LINEAR, 1, true, List.of());

        FlowAnalysis analysis = FlowAnalysis.sweep(disassembler, 1, index(65536), 0).publish();

        assertTrue(analysis.truncated());
        assertEquals(List.of(0), addresses(analysis));
        verify(disassembler, never()).getFlow(1);
    }

    @Test
    public void malformedDecoderWithManyInvalidTargetsStillHonorsWorkBound() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "JUMP", InstructionFlow.Kind.JUMP, 1, false,
                Collections.nCopies(1000, -1));

        FlowAnalysis analysis = reachable(disassembler, 0, 1, index(4));

        assertTrue(analysis.truncated());
        assertTrue(edges(analysis).isEmpty());
        assertEquals(List.of(0), addresses(analysis));
    }

    @Test
    public void bankedAndCommonEndpointsUsePhysicalBanksIndependentOfUiSelection() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "CALL 6", InstructionFlow.Kind.CALL, 2, false, List.of(6));
        instruction(disassembler, 6, "JP 0", InstructionFlow.Kind.JUMP, 7, false, List.of(0));
        ProgramIndex index = ProgramIndex.create("banked", 8, 2, 4);

        FlowAnalysis analysis = FlowAnalysis.reachable(disassembler, 0, 10, index, 1).publish();

        assertNotNull(analysis.snapshot().instructionAt(1, 0));
        assertNull(analysis.snapshot().instructionAt(0, 0));
        assertSame(analysis.snapshot().instructionAt(0, 6), analysis.snapshot().instructionAt(1, 6));
        FlowAnalysis.Edge call = analysis.predecessors(6).get(0);
        assertEquals(0, call.from);
        assertEquals(6, call.to);
        FlowAnalysis.Edge branch = analysis.predecessors(0).get(0);
        assertEquals(6, branch.from);
        assertEquals(0, branch.to);
    }

    @Test
    public void mutationOfNonFirstCellMakesCurrentFlowUnavailableButRetainsOldSnapshot() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "LD A,1", InstructionFlow.Kind.LINEAR, 2, true, List.of());
        ProgramIndex index = index(4);
        FlowAnalysis analysis = reachable(disassembler, 0, 1, index);

        assertEquals(2, analysis.snapshot().instructionAt(0, 0).cells().end);
        index.memoryChanged(); // Adapter reports a write to either covered cell, including address 1.

        assertFalse(analysis.isCurrent(index));
        assertEquals(ProgramIndex.FlowState.STALE, index.snapshot().flowState());
        assertNull(index.snapshot().instructionAt(0, 0));
        assertNotNull(analysis.snapshot().instructionAt(0, 0));
        assertFalse(analysis.isCurrent(index(4)));
    }

    @Test
    public void changedContentOrMemoryRejectsPendingResultAndDoesNotReplayIt() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "HALT", InstructionFlow.Kind.STOP, 1, false, List.of());
        ProgramIndex index = index(4);
        FlowAnalysis.Pending pending = FlowAnalysis.reachable(disassembler, 0, 10, index, 0);
        try (ProgramIndex.ContentUpdate update = index.updateContent(index.snapshot())) {
            update.putAnnotation(1, 1, new ProgramIndex.Range(0, 0, 1), ProgramIndex.Kinds.COMMENT, "changed");
            update.commit();
        }
        assertThrows(IllegalStateException.class, pending::publish);
        assertEquals(ProgramIndex.FlowState.NOT_BUILT, index.snapshot().flowState());
        assertThrows(IllegalStateException.class, pending::publish);

        FlowAnalysis.Pending memoryPending = FlowAnalysis.reachable(disassembler, 0, 10, index, 0);
        index.memoryChanged();
        assertThrows(IllegalStateException.class, memoryPending::publish);
    }

    @Test
    public void independentBreakpointChangeSurvivesGraphPublicationAndDoesNotStaleUiView() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "HALT", InstructionFlow.Kind.STOP, 1, false, List.of());
        ProgramIndex index = index(4);
        FlowAnalysis.Pending pending = FlowAnalysis.reachable(disassembler, 0, 10, index, 0);
        try (ProgramIndex.BreakpointUpdate update = index.updateBreakpoints(index.snapshot())) {
            update.put(7, 0, true, "");
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

        FlowAnalysis.Pending pending = FlowAnalysis.reachable(capturedDecoder, 0, 10, index, capturedBase, 0);

        assertThrows(IllegalStateException.class, pending::publish);
        assertNull(index.snapshot().instructionAt(0, 0));
    }

    @Test
    public void invalidBoundsAreRejectedBeforeUsingDisassembler() {
        Disassembler disassembler = mock(Disassembler.class);
        ProgramIndex index = index(4);

        assertThrows(IllegalArgumentException.class, () -> FlowAnalysis.reachable(disassembler, -1, 1, index, 0));
        assertThrows(IllegalArgumentException.class, () -> FlowAnalysis.reachable(disassembler, 4, 1, index, 0));
        assertThrows(IllegalArgumentException.class, () -> FlowAnalysis.reachable(disassembler, 0, 0, index, 0));
        assertThrows(IllegalArgumentException.class, () -> FlowAnalysis.reachable(disassembler, 0, 100001, index, 0));
        assertThrows(IllegalArgumentException.class, () -> FlowAnalysis.reachable(disassembler, 0, 1, index, 1));

        verifyNoInteractions(disassembler);
    }

    private static ProgramIndex index(int size) { return ProgramIndex.create("test", size, 1, size); }

    private static FlowAnalysis reachable(Disassembler disassembler, int start, int limit, ProgramIndex index) {
        return FlowAnalysis.reachable(disassembler, start, limit, index, 0).publish();
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

    private static void instruction(Disassembler disassembler, int address, String mnemonic,
                                    InstructionFlow.Kind kind, int next, boolean fallThrough,
                                    List<Integer> targets) throws InvalidInstructionException {
        when(disassembler.getFlow(address)).thenReturn(Optional.of(new InstructionFlow(kind, next, fallThrough, targets)));
        when(disassembler.getNextInstructionPosition(address)).thenReturn(next > address ? next : address + 1);
        when(disassembler.disassemble(address)).thenReturn(new DisassembledInstruction(address, mnemonic, ""));
    }
}
