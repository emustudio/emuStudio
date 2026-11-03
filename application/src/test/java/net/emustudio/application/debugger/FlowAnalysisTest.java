/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.cpu.InstructionFlow;
import net.emustudio.emulib.plugins.cpu.InvalidInstructionException;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FlowAnalysisTest {
    @Test
    public void callVisitsTargetAndFallThroughAndReportsIncomingEdges() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "CALL 4", InstructionFlow.Kind.CALL, 1, true, List.of(4));
        instruction(disassembler, 1, "HALT", InstructionFlow.Kind.STOP, 2, false, List.of());
        instruction(disassembler, 4, "RET", InstructionFlow.Kind.RETURN, 5, false, List.of());

        FlowAnalysis analysis = new FlowAnalysis(disassembler, 0, 10, null, 0);

        assertEquals(List.of(0, 4, 1), List.copyOf(analysis.instructions.keySet()));
        assertEquals("CALL 4", analysis.listing.get(0));
        assertEquals("RET", analysis.listing.get(4));
        assertEquals(2, analysis.edges.size());
        FlowAnalysis.Edge call = analysis.callers(4).get(0);
        assertEquals(0, call.from);
        assertEquals(4, call.to);
        assertEquals("call", call.kind);
        assertEquals("next", analysis.callers(1).get(0).kind);
        assertFalse(analysis.truncated);
    }

    @Test
    public void branchCycleIsDecodedOnlyOncePerAddress() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "JP 2", InstructionFlow.Kind.JUMP, 1, false, List.of(2));
        instruction(disassembler, 2, "JP 0", InstructionFlow.Kind.JUMP, 3, false, List.of(0));

        FlowAnalysis analysis = new FlowAnalysis(disassembler, 0, 10, null, 0);

        assertEquals(List.of(0, 2), List.copyOf(analysis.instructions.keySet()));
        assertEquals("branch", analysis.callers(0).get(0).kind);
        assertFalse(analysis.truncated);
        verify(disassembler).getFlow(0);
        verify(disassembler).getFlow(2);
    }

    @Test
    public void instructionLimitLeavesRemainingReachabilityMarkedTruncated() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "NOP", InstructionFlow.Kind.LINEAR, 1, true, List.of());

        FlowAnalysis analysis = new FlowAnalysis(disassembler, 0, 1, null, 0);

        assertEquals(List.of(0), List.copyOf(analysis.instructions.keySet()));
        assertTrue(analysis.truncated);
        verify(disassembler, never()).getFlow(1);
    }

    @Test
    public void dataAnnotationStopsDecodingAtReachableTarget() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "JP 2", InstructionFlow.Kind.JUMP, 1, false, List.of(2));
        AnnotationIndex annotations = new AnnotationIndex(new MemoryAnalysis(new byte[][] {{0, 0, 0}}, 3));
        annotations.put(new AnnotationIndex.Entry(0, 2, "table", "", false, "DATA"));

        FlowAnalysis analysis = new FlowAnalysis(disassembler, 0, 10, annotations, 0);

        assertEquals(List.of(0), List.copyOf(analysis.instructions.keySet()));
        assertEquals(2, analysis.edges.get(0).to);
        assertFalse(analysis.truncated);
        verify(disassembler, never()).getFlow(2);
    }

    @Test
    public void indirectJumpDoesNotInventTargetOrFallThrough() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "JP (HL)", InstructionFlow.Kind.JUMP, 1, false, List.of());

        FlowAnalysis analysis = new FlowAnalysis(disassembler, 0, 10, null, 0);

        assertEquals(1, analysis.instructions.size());
        assertTrue(analysis.edges.isEmpty());
        assertFalse(analysis.truncated);
    }

    @Test
    public void invalidTargetDoesNotPreventOtherPendingTargetFromBeingAnalyzed() throws Exception {
        Disassembler disassembler = mock(Disassembler.class);
        instruction(disassembler, 0, "CALL 8", InstructionFlow.Kind.CALL, 1, true, List.of(8));
        when(disassembler.getFlow(8)).thenThrow(new IndexOutOfBoundsException("outside memory"));
        instruction(disassembler, 1, "HALT", InstructionFlow.Kind.STOP, 2, false, List.of());

        FlowAnalysis analysis = new FlowAnalysis(disassembler, 0, 10, null, 0);

        assertEquals(List.of(0, 1), List.copyOf(analysis.instructions.keySet()));
        assertFalse(analysis.truncated);
    }

    @Test
    public void invalidBoundsAreRejectedBeforeUsingDisassembler() {
        Disassembler disassembler = mock(Disassembler.class);

        assertThrows(IllegalArgumentException.class, () -> new FlowAnalysis(disassembler, -1, 1, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new FlowAnalysis(disassembler, 0, 0, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new FlowAnalysis(disassembler, 0, 10001, null, 0));

        verifyNoInteractions(disassembler);
    }

    private static void instruction(Disassembler disassembler, int address, String mnemonic,
                                    InstructionFlow.Kind kind, int next, boolean fallThrough,
                                    List<Integer> targets) throws InvalidInstructionException {
        when(disassembler.getFlow(address)).thenReturn(Optional.of(new InstructionFlow(kind, next, fallThrough, targets)));
        when(disassembler.disassemble(address)).thenReturn(new DisassembledInstruction(address, mnemonic, ""));
    }
}
