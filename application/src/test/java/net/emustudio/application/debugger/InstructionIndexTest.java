/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.runtime.annotations.MemoryAnnotations;
import net.emustudio.application.debugger.index.ProgramIndex;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class InstructionIndexTest {
    @Test
    public void logicalBoundariesFollowPublishedRevisionsWhileCapturedViewsRemainStable() {
        ProgramIndex index = bankedIndex();
        InstructionIndex instructions = new InstructionIndex(index::snapshot);
        InstructionIndex.View first = instructions.snapshot();
        assertEquals(2, first.nextAddress(0));
        assertEquals(10, first.nextAddress(8));
        assertSame(first, instructions.snapshot());

        try (ProgramIndex.ContentUpdate update = index.updateContent(index.snapshot())) {
            update.replaceFlow(false);
            update.putInstruction(new ProgramIndex.Range(0, 3), ProgramIndex.InstructionKind.LINEAR);
            update.putInstruction(new ProgramIndex.Range(8, 10), ProgramIndex.InstructionKind.LINEAR);
            update.commit();
        }
        InstructionIndex.View second = instructions.snapshot();
        assertNotSame(first, second);
        assertEquals(3, second.nextAddress(0));
        assertEquals(10, second.nextAddress(8));
        assertEquals(2, first.nextAddress(0));

        assertEquals(3, instructions.snapshot().nextAddress(0));
    }

    @Test
    public void instructionCoverageRetainsItsLogicalEnd() {
        ProgramIndex index = bankedIndex();
        try (ProgramIndex.ContentUpdate update = index.updateContent(index.snapshot())) {
            update.replaceFlow(false);
            update.putInstruction(new ProgramIndex.Range(7, 10), ProgramIndex.InstructionKind.LINEAR);
            update.commit();
        }
        InstructionIndex instructions = new InstructionIndex(index::snapshot);
        assertEquals(10, instructions.snapshot().nextAddress(7));
        assertEquals(-1, instructions.snapshot().nextAddress(8));
    }

    @Test
    public void operandMutationInvalidatesCurrentCoverageButPreservesHistoricalView() {
        ProgramIndex index = bankedIndex();
        InstructionIndex instructions = new InstructionIndex(index::snapshot);
        InstructionIndex.View before = instructions.snapshot();
        index.memoryChanged();
        InstructionIndex.View after = instructions.snapshot();
        assertNotSame(before, after);
        assertEquals(-1, after.nextAddress(0));
        assertEquals(2, before.nextAddress(0));
    }

    @Test
    public void breakpointEditsDoNotInvalidateInstructionPages() {
        ProgramIndex index = bankedIndex();
        InstructionIndex instructions = new InstructionIndex(index::snapshot);
        InstructionIndex.View before = instructions.snapshot();
        try (ProgramIndex.BreakpointUpdate update = index.updateBreakpoints(index.snapshot())) {
            update.put(1, 0, true);
            update.commit();
        }
        assertSame(before, instructions.snapshot());
    }

    @Test
    public void metadataAndImageChangesInvalidateViewsEvenWithMatchingRevisionNumbers() {
        AtomicReference<ProgramIndex> index = new AtomicReference<>(bankedIndex());
        InstructionIndex instructions = new InstructionIndex(() -> index.get().snapshot());
        InstructionIndex.View before = instructions.snapshot();
        index.set(bankedIndex()); // New image/layout, with the same revision numbers.
        assertNotSame(before, instructions.snapshot());
        before = instructions.snapshot();
        index.get().annotationsChanged();
        assertNotSame(before, instructions.snapshot());
        assertEquals(-1, instructions.snapshot().nextAddress(0));
        InstructionIndex.View metadata = instructions.snapshot();
        index.set(bankedIndex());
        assertNotSame(metadata, instructions.snapshot());
        assertEquals(2, instructions.snapshot().nextAddress(0));
    }

    @Test
    public void unknownAddressesStayUnknown() {
        ProgramIndex index = ProgramIndex.create("shared", 16, new MemoryAnnotations());
        InstructionIndex instructions = new InstructionIndex(index::snapshot);
        InstructionIndex.View first = instructions.snapshot();
        assertSame(first, instructions.snapshot());
        assertEquals(-1, first.nextAddress(-1));
        assertEquals(-1, first.nextAddress(0));
        assertEquals(-1, first.nextAddress(16));
        assertEquals(-1, first.nextAddress(Integer.MAX_VALUE));
    }

    private static ProgramIndex bankedIndex() {
        ProgramIndex index = ProgramIndex.create("banked", 16, new MemoryAnnotations());
        try (ProgramIndex.ContentUpdate update = index.updateContent(index.snapshot())) {
            update.replaceFlow(false);
            update.putInstruction(new ProgramIndex.Range(0, 2), ProgramIndex.InstructionKind.LINEAR);
            update.putInstruction(new ProgramIndex.Range(8, 10), ProgramIndex.InstructionKind.LINEAR);
            update.commit();
        }
        return index;
    }
}
