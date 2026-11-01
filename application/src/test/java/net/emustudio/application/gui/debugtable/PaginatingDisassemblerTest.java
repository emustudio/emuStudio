/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
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
        new PaginatingDisassembler(null, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateInstanceEmptyAddressSpaceThrows() {
        new PaginatingDisassembler(mock(CallFlow.class), 0);
    }

    @Test
    public void testInstructionsPerPageCurrentRowAndCurrentInstructionMarkerCanChange() {
        callFlow = mock(CallFlow.class);
        when(callFlow.getLongestInstructionSize()).thenReturn(2);

        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, MEMORY_SIZE);

        assertEquals(INSTR_PER_PAGE, asm.getInstructionsPerPage());
        asm.setInstructionsPerPage(9);
        assertEquals(9, asm.getInstructionsPerPage());
        assertEquals(4, asm.getCurrentInstructionRow());
        assertTrue(asm.isRowAtCurrentInstruction(4));
        assertFalse(asm.isRowAtCurrentInstruction(3));
    }

    @Test
    public void boundaryPageFillsFromFirstKnownInstructionAndNextPageOverlapsOnce() {
        int[] successors = new int[256];
        java.util.Arrays.fill(successors, -1);
        int at = 0;
        for (int n = 0; at < 250; n++) {
            int next = at + n % 3 + 1;
            successors[at] = next;
            at = next;
        }
        PaginatingDisassembler asm = new PaginatingDisassembler(new CallFlow(makeDisassembler(successors)), 256);
        asm.setInstructionsPerPage(9);
        assertEquals(0, asm.rowToLocation(0, 0));
        assertEquals(0, asm.getCurrentInstructionRow());
        int last = asm.rowToLocation(0, 8);
        assertTrue(last > 0);
        asm.pageNext();
        assertEquals(last, asm.rowToLocation(0, 0));
        assertTrue(asm.rowToLocation(0, 8) > last);
        asm.pagePrevious();
        assertEquals(0, asm.rowToLocation(0, 0));
        assertEquals(last, asm.rowToLocation(0, 8));
    }

    @Test
    public void boundaryRowsRemainValidAfterResizeAndFirstLastPageTravel() {
        CallFlow flow = new CallFlow(makeDisassembler(80, 2));
        flow.updateCache(0);
        PaginatingDisassembler asm = new PaginatingDisassembler(flow, 80);
        asm.setInstructionsPerPage(7);
        assertEquals(0, asm.rowToLocation(4, 0));
        assertEquals(4, asm.rowToLocation(4, asm.getCurrentInstructionRow()));
        assertEquals(12, asm.rowToLocation(4, 6));
        asm.setInstructionsPerPage(11);
        assertEquals(0, asm.rowToLocation(4, 0));
        assertEquals(20, asm.rowToLocation(4, 10));
        asm.pageLast();
        assertTrue(asm.rowToLocation(4, 0) >= 0);
        asm.pageFirst();
        assertEquals(0, asm.rowToLocation(4, 0));
    }

    @Test(timeout = 2000)
    public void singleVisibleRowCanPageToBothAddressBoundaries() {
        PaginatingDisassembler asm = new PaginatingDisassembler(new CallFlow(makeDisassembler(8, 2)), 8);
        asm.setInstructionsPerPage(1);
        assertEquals(0, asm.rowToLocation(0, 0));
        asm.pageNext();
        assertEquals(2, asm.rowToLocation(0, 0));
        asm.pageLast();
        assertEquals(6, asm.rowToLocation(0, 0));
        asm.pageFirst();
        assertEquals(0, asm.rowToLocation(0, 0));
    }

    @Test
    public void steppedCpuRetainsPartialFirstPageOverlapAfterNeighborCacheIsInvalidated() {
        CallFlow flow = new CallFlow(makeDisassembler(MEMORY_SIZE, 2));
        flow.updateCache(0);
        PaginatingDisassembler asm = new PaginatingDisassembler(flow, MEMORY_SIZE);
        asm.rowToLocation(50, 0);
        asm.rowToLocation(50, INSTR_PER_PAGE - 1);
        asm.pagePrevious();
        assertEquals(0, asm.rowToLocation(52, 0));
        assertEquals(32, asm.rowToLocation(52, 16));
        assertEquals(-1, asm.rowToLocation(52, 17));
        asm.pageNext();
        assertEquals(32, asm.rowToLocation(52, 0));
    }

    @Test
    public void overlapOnlyPreviousPageCanReturnToCurrentPageAfterCpuMovesNearZero() {
        CallFlow flow = new CallFlow(makeDisassembler(MEMORY_SIZE, 2));
        flow.updateCache(0);
        PaginatingDisassembler asm = new PaginatingDisassembler(flow, MEMORY_SIZE);
        asm.rowToLocation(50, 0);
        asm.rowToLocation(50, INSTR_PER_PAGE - 1);
        asm.pagePrevious();
        assertEquals(0, asm.rowToLocation(2, 0));
        assertEquals(-1, asm.rowToLocation(2, INSTR_PER_PAGE - 1));
        asm.pageNext();
        assertEquals(0, asm.getPageIndex());
        assertEquals(0, asm.rowToLocation(2, 0));
    }

    @Test
    public void singleVisibleRowCanPageToPredecessorOfPositiveCpuAddress() {
        CallFlow flow = new CallFlow(makeDisassembler(8, 2));
        flow.updateCache(0);
        PaginatingDisassembler asm = new PaginatingDisassembler(flow, 8);
        asm.setInstructionsPerPage(1);
        assertEquals(4, asm.rowToLocation(4, 0));
        asm.pagePrevious();
        assertEquals(2, asm.rowToLocation(4, 0));
        asm.pagePrevious();
        assertEquals(0, asm.rowToLocation(4, 0));
        asm.pageNext();
        assertEquals(2, asm.rowToLocation(4, 0));
    }

    @Test
    public void testPageCurrentReturnsToPageZero() {
        CallFlow callFlow = new CallFlow(makeDisassembler(MEMORY_SIZE, 1));
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, MEMORY_SIZE);

        asm.rowToLocation(CURRENT_INSTR, INSTR_PER_PAGE - 1);
        asm.pageNext();
        assertEquals(1, asm.getPageIndex());

        asm.pageCurrent();

        assertEquals(0, asm.getPageIndex());
    }

    @Test
    public void testFlushCacheAddsInclusiveUpperBoundOffset() {
        callFlow = mock(CallFlow.class);
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, MEMORY_SIZE);

        asm.flushCache(2, 6);

        verify(callFlow).flushCache(2, 7);
    }

    @Test
    public void testRowAboveHalfReturnsMinusOneAtAddressSpaceEnd() {
        callFlow = mock(CallFlow.class);
        when(callFlow.getLongestInstructionSize()).thenReturn(1);
        when(callFlow.getLocations(0, 0)).thenReturn(new ArrayList<>(Collections.singletonList(0)));

        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, 1);

        assertEquals(-1, asm.rowToLocation(0, INSTR_PER_HALF_PAGE + 1));
        verify(callFlow).updateCache(0);
    }

    @Test
    public void testRowAboveHalfReturnsMinusOneWhenNoInstructionsAreKnown() {
        callFlow = mock(CallFlow.class);
        when(callFlow.getLongestInstructionSize()).thenReturn(1);
        when(callFlow.getLocations(0, 5)).thenReturn(Collections.emptyList());

        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, 6);

        assertEquals(-1, asm.rowToLocation(0, INSTR_PER_HALF_PAGE + 1));
    }

    @Test
    public void testCachedNextAndPreviousPagesCanBeReused() {
        CallFlow callFlow = new CallFlow(makeDisassembler(MEMORY_SIZE, 1));
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, MEMORY_SIZE);

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

        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, MEMORY_SIZE);

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

        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, MEMORY_SIZE);

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

        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, MEMORY_SIZE);

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
        PaginatingDisassembler asm = new PaginatingDisassembler(new CallFlow(makeDisassembler(MEMORY_SIZE, 4)), MEMORY_SIZE);
        asm.rowToLocation(0, 0);
        int page1min = (INSTR_PER_PAGE - 1) * 4;
        assertEquals(page1min, asm.rowToLocation(0, INSTR_PER_PAGE - 1));
        asm.pageNext();
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
        CallFlow flow = new CallFlow(makeDisassembler(MEMORY_SIZE, LONGEST_INSTR));
        flow.updateCache(0);
        PaginatingDisassembler asm = new PaginatingDisassembler(flow, MEMORY_SIZE);
        int pageZeroMin = asm.rowToLocation(50, 0);
        asm.rowToLocation(50, INSTR_PER_PAGE - 1);
        asm.pagePrevious();
        assertEquals(0, asm.rowToLocation(50, 0));
        assertEquals(pageZeroMin, asm.rowToLocation(50, pageZeroMin / LONGEST_INSTR));
        assertEquals(-1, asm.rowToLocation(50, INSTR_PER_PAGE - 1));
        asm.pageNext();
        assertEquals(pageZeroMin, asm.rowToLocation(50, 0));
    }

    @Test
    public void testPageMinusOneFirstRowNotEnoughInstructions() {
        CallFlow callFlow = new CallFlow(makeDisassembler(MEMORY_SIZE, LONGEST_INSTR));
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, MEMORY_SIZE);

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
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, MEMORY_SIZE);

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
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, MEMORY_SIZE);

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
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, MEMORY_SIZE);

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
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, MEMORY_SIZE);

        callFlow.updateCache(0);

        asm.rowToLocation(CURRENT_INSTR, 0);

        asm.pagePrevious();
        asm.pagePrevious();

        assertEquals(-1, asm.getPageIndex());
    }

    @Test(timeout = 1000)
    public void testLastPageThenAnotherPageNextIsIgnored() {
        CallFlow callFlow = new CallFlow(makeDisassembler(5 * CURRENT_INSTR, 1));
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, 5 * CURRENT_INSTR);

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
        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, 5 * CURRENT_INSTR);

        asm.rowToLocation(CURRENT_INSTR, INSTR_PER_PAGE - 1);
        asm.pageLast();

        assertTrue(asm.getPageIndex() > 0);

        asm.pageFirst();

        assertEquals(0, asm.getPageIndex());
    }
}
