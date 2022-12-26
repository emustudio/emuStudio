/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.gui.debugtable;

import org.junit.Ignore;
import org.junit.Test;

import static net.emustudio.application.gui.debugtable.MockHelper.*;
import static net.emustudio.application.gui.debugtable.PaginatingDisassembler.INSTR_PER_PAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PaginatingDisassemblerTest {
    private CallFlow callFlow;

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceNullDisassemblerThrows() {
        new PaginatingDisassembler(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateInstanceNegativeMemorySizeThrows() {
        new PaginatingDisassembler(mock(CallFlow.class), -1);
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
