/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;

import static net.emustudio.application.gui.debugtable.MockHelper.*;
import static net.emustudio.application.gui.debugtable.PaginatingDisassembler.INSTR_PER_PAGE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PaginatingDisassemblerTest {
    private CallFlow callFlow;

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceNullDisassemblerThrows() {
        new PaginatingDisassembler(null, () -> 0);
    }

    @Test
    public void testInstructionsPerPageCurrentRowAndCurrentInstructionMarkerCanChange() {
        callFlow = mock(CallFlow.class);
        when(callFlow.getLongestInstructionSize()).thenReturn(2);

        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, () -> MEMORY_SIZE);

        assertEquals(INSTR_PER_PAGE, asm.getInstructionsPerPage());
        asm.setInstructionsPerPage(9);
        assertEquals(9, asm.getInstructionsPerPage());
        assertEquals(4, asm.getCurrentInstructionRow());
        assertTrue(asm.isRowAtCurrentInstruction(4));
        assertFalse(asm.isRowAtCurrentInstruction(3));
    }

    @Test
    public void testPageCurrentReturnsToPageZero() {
        CallFlow callFlow = new CallFlow(makeDisassembler(MEMORY_SIZE, 1));
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, () -> MEMORY_SIZE);

        asm.rowToLocation(CURRENT_INSTR, INSTR_PER_PAGE - 1);
        asm.pageNext();
        assertEquals(1, asm.getPageIndex());

        asm.pageCurrent();

        assertEquals(0, asm.getPageIndex());
    }

    @Test
    public void testFlushCacheAddsInclusiveUpperBoundOffset() {
        callFlow = mock(CallFlow.class);
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, () -> MEMORY_SIZE);

        asm.flushCache(2, 6);

        verify(callFlow).flushCache(2, 7);
    }

    @Test
    public void testRowAboveHalfReturnsMinusOneForEmptyMemory() {
        callFlow = mock(CallFlow.class);
        when(callFlow.getLongestInstructionSize()).thenReturn(1);

        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, () -> 0);

        assertEquals(-1, asm.rowToLocation(0, INSTR_PER_HALF_PAGE + 1));
        verify(callFlow).updateCache(0);
    }

    @Test
    public void testRowAboveHalfReturnsMinusOneWhenNoInstructionsAreKnown() {
        callFlow = mock(CallFlow.class);
        when(callFlow.getLongestInstructionSize()).thenReturn(1);
        when(callFlow.getLocations(0, 5)).thenReturn(Collections.emptyList());

        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, () -> 6);

        assertEquals(-1, asm.rowToLocation(0, INSTR_PER_HALF_PAGE + 1));
    }

    @Test
    public void testCachedNextAndPreviousPagesCanBeReused() {
        CallFlow callFlow = new CallFlow(makeDisassembler(MEMORY_SIZE, 1));
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, () -> MEMORY_SIZE);

        asm.rowToLocation(CURRENT_INSTR, INSTR_PER_PAGE - 1);
        asm.pageNext();
        asm.pagePrevious();
        asm.pageNext();
        assertEquals(1, asm.getPageIndex());

        PaginatingDisassembler previousAsm = makeDisassemblerWithFixedSizedInstructions(
                CURRENT_INSTR, -1, 1, false
        );
        assertEquals(-1, previousAsm.getPageIndex());
        previousAsm.pageNext();
        previousAsm.pagePrevious();
        assertEquals(-1, previousAsm.getPageIndex());
    }

    @Test
    public void testPageZeroCurrentInstruction() {
        callFlow = mock(CallFlow.class);

        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, () -> MEMORY_SIZE);

        assertEquals(50, asm.rowToLocation(50, INSTR_PER_HALF_PAGE));
        verify(callFlow).updateCache(50);
    }

    @Test
    public void testPageZeroOneBelowCurrentInstructionIsUnknown() {
        int page0curr = CURRENT_INSTR;
        int page0min = page0curr - HALF_PAGE_MAX_BYTES;

        callFlow = mockCallFlow(
                page0min,
                page0curr,
                CURRENT_INSTR
        );

        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, () -> MEMORY_SIZE);

        assertEquals(
                -1,
                asm.rowToLocation(CURRENT_INSTR, INSTR_PER_HALF_PAGE - 1)
        );
    }

    @Test
    public void testPageZeroOneAboveCurrentInstructionIsUnknown() {
        int page0curr = CURRENT_INSTR;
        int page0max = page0curr + HALF_PAGE_MAX_BYTES;

        callFlow = mockCallFlow(
                page0curr,
                page0max,
                CURRENT_INSTR
        );

        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, () -> MEMORY_SIZE);

        assertEquals(
                -1,
                asm.rowToLocation(CURRENT_INSTR, INSTR_PER_HALF_PAGE + 1)
        );
    }

    @Test
    public void testPageZeroOneBelowCurrentInstructionIsKnown() {
        PaginatingDisassembler asm = makeDisassemblerWithFixedSizedInstructions(
                CURRENT_INSTR, 0, 1, true
        );

        assertEquals(
                CURRENT_INSTR - 1,
                asm.rowToLocation(CURRENT_INSTR, INSTR_PER_HALF_PAGE - 1)
        );
    }

    @Test
    public void testPageZeroFirstRow() {
        PaginatingDisassembler asm = makeDisassemblerWithFixedSizedInstructions(
                CURRENT_INSTR, 0, LONGEST_INSTR, true
        );

        int page0min = CURRENT_INSTR - HALF_PAGE_MAX_BYTES;
        assertEquals(
                page0min,
                asm.rowToLocation(CURRENT_INSTR, 0)
        );
    }

    @Test
    public void testPageZeroLastRow() {
        PaginatingDisassembler asm = makeDisassemblerWithFixedSizedInstructions(
                CURRENT_INSTR, 0, LONGEST_INSTR, true
        );

        int page0max = CURRENT_INSTR + HALF_PAGE_MAX_BYTES;
        assertEquals(
                page0max,
                asm.rowToLocation(CURRENT_INSTR, INSTR_PER_PAGE - 1)
        );
    }

    @Test
    public void testPageOneFirstRow() {
        PaginatingDisassembler asm = makeDisassemblerWithFixedSizedInstructions(
                CURRENT_INSTR, 1, LONGEST_INSTR, true
        );

        int page1min = CURRENT_INSTR + HALF_PAGE_MAX_BYTES;
        assertEquals(
                page1min, // last instruction from previous page is present here
                asm.rowToLocation(CURRENT_INSTR, 0)
        );
    }

    @Test
    public void testPageOneLastRow() {
        PaginatingDisassembler asm = makeDisassemblerWithFixedSizedInstructions(
                CURRENT_INSTR, 1, LONGEST_INSTR, true
        );

        int page1max = CURRENT_INSTR + LONGEST_INSTR * (INSTR_PER_PAGE - 1) + HALF_PAGE_MAX_BYTES;
        assertEquals(
                page1max, // last instruction from previous page is present here
                asm.rowToLocation(CURRENT_INSTR, INSTR_PER_PAGE - 1)
        );
    }

    @Test
    public void testPageTwoFirstRow() {
        PaginatingDisassembler asm = makeDisassemblerWithFixedSizedInstructions(
                CURRENT_INSTR, 2, LONGEST_INSTR, true
        );

        int page2min = CURRENT_INSTR + LONGEST_INSTR * (INSTR_PER_PAGE - 1) + HALF_PAGE_MAX_BYTES;
        assertEquals(
                page2min,
                asm.rowToLocation(CURRENT_INSTR, 0)
        );
    }

    @Test
    public void testPageTwoLastRow() {
        PaginatingDisassembler asm = makeDisassemblerWithFixedSizedInstructions(
                CURRENT_INSTR, 2, LONGEST_INSTR, true
        );

        int page2max = CURRENT_INSTR + 2 * LONGEST_INSTR * (INSTR_PER_PAGE - 1) + HALF_PAGE_MAX_BYTES;
        assertEquals(
                page2max,
                asm.rowToLocation(CURRENT_INSTR, INSTR_PER_PAGE - 1)
        );
    }

    @Test
    public void testPageOneFirstRowWhenCurrentInstructionIs0AndInstructionSizeIs4() {
        PaginatingDisassembler asm = makeDisassemblerWithFixedSizedInstructions(
                0, 1, 4, true
        );

        int page1min = INSTR_PER_HALF_PAGE * 4;
        assertEquals(
                page1min,
                asm.rowToLocation(0, 0)
        );
    }

    @Test
    public void testCurrentLocationDifferenceBetweenPagesIsCorrect() {
        // pages do overlap by 1 instruction:
        //   pagePrevMax = pageNextMin

        PaginatingDisassembler asm = makeDisassemblerWithFixedSizedInstructions(
                CURRENT_INSTR, 2, LONGEST_INSTR, true
        );

        int page2curr = asm.rowToLocation(CURRENT_INSTR, INSTR_PER_HALF_PAGE);
        asm.pagePrevious();
        int page1curr = asm.rowToLocation(CURRENT_INSTR, INSTR_PER_HALF_PAGE);
        asm.pagePrevious();
        int page0curr = asm.rowToLocation(CURRENT_INSTR, INSTR_PER_HALF_PAGE);

        assertEquals((INSTR_PER_PAGE - 1) * LONGEST_INSTR, page2curr - page1curr);
        assertEquals((INSTR_PER_PAGE - 1) * LONGEST_INSTR, page1curr - page0curr);
    }

    @Test
    public void testPageMinusOneLastRowNotEnoughInstructions() {
        PaginatingDisassembler asm = makeDisassemblerWithFixedSizedInstructions(
                10, -1, LONGEST_INSTR, false
        );

        int pageM1max = 10 - LONGEST_INSTR * INSTR_PER_HALF_PAGE;
        int missingInstructions = 10 - LONGEST_INSTR * (INSTR_PER_PAGE - 1);

        assertTrue(missingInstructions < 0);
        assertEquals(
                pageM1max - missingInstructions, // prefer number of instructions shown must fit
                asm.rowToLocation(10, INSTR_PER_PAGE - 1)
        );
    }

    @Test
    public void testPageMinusOneFirstRowNotEnoughInstructions() {
        CallFlow callFlow = new CallFlow(makeDisassembler(MEMORY_SIZE, LONGEST_INSTR));
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, () -> MEMORY_SIZE);

        asm.rowToLocation(CURRENT_INSTR, 0);
        asm.rowToLocation(CURRENT_INSTR, INSTR_PER_HALF_PAGE);
        asm.rowToLocation(CURRENT_INSTR, INSTR_PER_PAGE - 1);

        assertEquals(
                -1, // prefer number of instructions shown must fit
                asm.rowToLocation(50, 0)
        );
    }

    @Test
    @Ignore
    public void testPageMinusOneCurrentRowNotEnoughInstructions() {
        PaginatingDisassembler asm = makeDisassemblerWithFixedSizedInstructions(
                10, -1, LONGEST_INSTR, false
        );

        assertEquals(
                0,
                asm.rowToLocation(10, INSTR_PER_HALF_PAGE)
        );
    }

    @Test
    public void testPageZeroInstructionStepped() {
        CallFlow callFlow = new CallFlow(makeDisassembler(MEMORY_SIZE, 1));
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, () -> MEMORY_SIZE);

        assertEquals(-1, asm.rowToLocation(CURRENT_INSTR, 0));
        assertEquals(CURRENT_INSTR, asm.rowToLocation(CURRENT_INSTR, INSTR_PER_HALF_PAGE));
        assertEquals(
                CURRENT_INSTR + INSTR_PER_HALF_PAGE, asm.rowToLocation(CURRENT_INSTR, INSTR_PER_PAGE - 1)
        );

        assertEquals(CURRENT_INSTR, asm.rowToLocation(CURRENT_INSTR + 1, INSTR_PER_HALF_PAGE - 1));
        assertEquals(CURRENT_INSTR + 1, asm.rowToLocation(CURRENT_INSTR + 1, INSTR_PER_HALF_PAGE));
        assertEquals(
                CURRENT_INSTR + INSTR_PER_HALF_PAGE + 1,
                asm.rowToLocation(CURRENT_INSTR + 1, INSTR_PER_PAGE - 1)
        );
    }

    @Test
    public void testPageOneInstructionStepped() {
        CallFlow callFlow = new CallFlow(makeDisassembler(MEMORY_SIZE, 1));
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, () -> MEMORY_SIZE);

        asm.rowToLocation(CURRENT_INSTR, INSTR_PER_PAGE - 1);
        asm.pageNext();

        assertEquals(CURRENT_INSTR + INSTR_PER_HALF_PAGE, asm.rowToLocation(CURRENT_INSTR, 0));
        assertEquals(CURRENT_INSTR + INSTR_PER_PAGE - 1, asm.rowToLocation(CURRENT_INSTR, INSTR_PER_HALF_PAGE));
        assertEquals(
                CURRENT_INSTR + INSTR_PER_PAGE - 1 + INSTR_PER_HALF_PAGE,
                asm.rowToLocation(CURRENT_INSTR, INSTR_PER_PAGE - 1)
        );

        // instruction step

        assertEquals(
                CURRENT_INSTR + INSTR_PER_HALF_PAGE + 1,
                asm.rowToLocation(CURRENT_INSTR + 1, 0)
        );
        assertEquals(
                CURRENT_INSTR + INSTR_PER_PAGE,
                asm.rowToLocation(CURRENT_INSTR + 1, INSTR_PER_HALF_PAGE)
        );
        assertEquals(
                CURRENT_INSTR + INSTR_PER_PAGE + INSTR_PER_HALF_PAGE,
                asm.rowToLocation(CURRENT_INSTR + 1, INSTR_PER_PAGE - 1)
        );
    }

    @Test
    public void testPageMinusOneInstructionStepped() {
        CallFlow callFlow = new CallFlow(makeDisassembler(MEMORY_SIZE, 1));
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, () -> MEMORY_SIZE);

        callFlow.updateCache(0);

        asm.rowToLocation(CURRENT_INSTR, 0);
        asm.pagePrevious();

        assertEquals(CURRENT_INSTR - INSTR_PER_PAGE + 1 - INSTR_PER_HALF_PAGE, asm.rowToLocation(CURRENT_INSTR, 0));
        assertEquals(CURRENT_INSTR - INSTR_PER_PAGE + 1, asm.rowToLocation(CURRENT_INSTR, INSTR_PER_HALF_PAGE));
        assertEquals(
                CURRENT_INSTR - INSTR_PER_HALF_PAGE,
                asm.rowToLocation(CURRENT_INSTR, INSTR_PER_PAGE - 1)
        );

        // instruction step
        assertEquals(
                CURRENT_INSTR - INSTR_PER_PAGE + 2 - INSTR_PER_HALF_PAGE,
                asm.rowToLocation(CURRENT_INSTR + 1, 0)
        );
        assertEquals(
                CURRENT_INSTR - INSTR_PER_PAGE + 2,
                asm.rowToLocation(CURRENT_INSTR + 1, INSTR_PER_HALF_PAGE)
        );
        assertEquals(
                CURRENT_INSTR - INSTR_PER_HALF_PAGE + 1,
                asm.rowToLocation(CURRENT_INSTR + 1, INSTR_PER_PAGE - 1)
        );
    }

    @Test
    public void testPageMinusOneMinLocationIsNotKnownThenAnotherPreviousPageIsIgnored() {
        CallFlow callFlow = new CallFlow(makeDisassembler(MEMORY_SIZE, 1));
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, () -> MEMORY_SIZE);

        callFlow.updateCache(0);

        asm.rowToLocation(CURRENT_INSTR, 0);

        asm.pagePrevious();
        asm.pagePrevious();

        assertEquals(-1, asm.getPageIndex());
    }

    @Test(timeout = 1000)
    public void testLastPageThenAnotherPageNextIsIgnored() {
        CallFlow callFlow = new CallFlow(makeDisassembler(5 * CURRENT_INSTR, 1));
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, () -> 5 * CURRENT_INSTR);

        asm.rowToLocation(CURRENT_INSTR, INSTR_PER_PAGE - 1);
        asm.pageLast();
        int pageIndex = asm.getPageIndex();

        assertTrue(pageIndex > 0);

        asm.pageNext();

        assertEquals(pageIndex, asm.getPageIndex());
    }

    @Test(timeout = 1000)
    public void testLastPageThenFirstPageReturnsBack() {
        CallFlow callFlow = new CallFlow(makeDisassembler(5 * CURRENT_INSTR, 1));
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, () -> 5 * CURRENT_INSTR);

        asm.rowToLocation(CURRENT_INSTR, INSTR_PER_PAGE - 1);
        asm.pageLast();

        assertTrue(asm.getPageIndex() > 0);

        asm.pageFirst();

        assertEquals(0, asm.getPageIndex());
    }
}
