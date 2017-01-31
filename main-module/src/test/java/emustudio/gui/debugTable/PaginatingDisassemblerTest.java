/*
 * (c) Copyright 2006-2017, Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emustudio.gui.debugTable;

import org.junit.Test;

import java.util.Collections;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

public class PaginatingDisassemblerTest {
    private final static int MEMORY_SIZE = 0x10000;
    public static final int[] LONG_PROGRAM = new int[]{
            1,
            3, 3,
            5, 5,
            6,
            9, 9, 9,
            10,
            11,
            12,
            13,
            14,
            15,
            16,
            17,
            18,
            19,
            20,
            21,
            22,
            23,
            24,
            25,
            26,
            27,
            28,
            29,
            30,
            31,
            32,
            33,
            34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66
    };
    public static final int[] SHORT_PROGRAM = new int[]{
            1,
            3, 3,
            5, 5,
            6,
            9, 9, 9,
            10
    };

    private DisassemblerStub makeDisassembler(int... instructions) {
        return new DisassemblerStub(MEMORY_SIZE, instructions);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceNullDisassemblerThrows() throws Exception {
        new PaginatingDisassembler(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateInstanceNegativeMemorySizeThrows() throws Exception {
        new PaginatingDisassembler(createMock(CallFlow.class), -1);
    }

    @Test
    public void testCurrentPageInstructionsAboveCurrentLocationAreNotDecoded() throws Exception {
        CallFlow callFlow = createMock(CallFlow.class);
        callFlow.updateCache(0);
        expectLastCall().anyTimes();
        expect(callFlow.getLongestInstructionSize()).andReturn(2).anyTimes();
        expect(callFlow.getLocationsInterval(0, 50)).andReturn(Collections.emptyList()).anyTimes();
        replay(callFlow);


        PaginatingDisassembler ida = new PaginatingDisassembler(callFlow, MEMORY_SIZE);

        for (int i = 0; i < PaginatingDisassembler.CURRENT_INSTRUCTION; i++) {
            assertEquals(-1, ida. rowToLocation(0, i));
        }
    }

//    @Test
//    public void testCurrentPageForCurrentLocationRowToLocationReturnsCurrentLocation() throws Exception {
//        CallFlow ida = new CallFlow(makeDisassembler(SHORT_PROGRAM), MEMORY_SIZE);
//
//        assertEquals(0, ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION));
//    }
//
//    @Test
//    public void testCurrentPageInstructionsBelowAreDecodedCorrectly() throws Exception {
//        CallFlow ida = new CallFlow(makeDisassembler(SHORT_PROGRAM), MEMORY_SIZE);
//
//        assertEquals(1, ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION + 1));
//        assertEquals(3, ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION + 2));
//        assertEquals(5, ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION + 3));
//        assertEquals(6, ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION + 4));
//        assertEquals(9, ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION + 5));
//        assertEquals(10, ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION + 6));
//    }
//
//    @Test
//    public void testCurrentPageInstructionsBelowAreDecodedCorrectlyAfterAdvance() throws Exception {
//        CallFlow ida = new CallFlow(makeDisassembler(SHORT_PROGRAM), MEMORY_SIZE);
//
//        // advance for updating cache
//        ida.rowToLocation(0, 0);
//        ida.rowToLocation(1, 0);
//        ida.rowToLocation(3, 0);
//        ida.rowToLocation(5, 0);
//        ida.rowToLocation(6, 0);
//        ida.rowToLocation(9, 0);
//
//        assertEquals(3, ida.rowToLocation(1, CallFlow.CURRENT_INSTRUCTION + 1));
//        assertEquals(5, ida.rowToLocation(1, CallFlow.CURRENT_INSTRUCTION + 2));
//        assertEquals(6, ida.rowToLocation(1, CallFlow.CURRENT_INSTRUCTION + 3));
//        assertEquals(9, ida.rowToLocation(1, CallFlow.CURRENT_INSTRUCTION + 4));
//
//        assertEquals(5, ida.rowToLocation(3, CallFlow.CURRENT_INSTRUCTION + 1));
//        assertEquals(6, ida.rowToLocation(3, CallFlow.CURRENT_INSTRUCTION + 2));
//        assertEquals(9, ida.rowToLocation(3, CallFlow.CURRENT_INSTRUCTION + 3));
//
//        assertEquals(6, ida.rowToLocation(5, CallFlow.CURRENT_INSTRUCTION + 1));
//        assertEquals(9, ida.rowToLocation(5, CallFlow.CURRENT_INSTRUCTION + 2));
//
//        assertEquals(9, ida.rowToLocation(6, CallFlow.CURRENT_INSTRUCTION + 1));
//    }
//
//
//    @Test
//    public void testCurrentPageInstructionsAboveAreDecodedCorrectlyAfterAdvance() throws Exception {
//        CallFlow ida = new CallFlow(makeDisassembler(SHORT_PROGRAM), MEMORY_SIZE);
//
//        // advance for updating cache
//        ida.rowToLocation(0, 0);
//        ida.rowToLocation(1, 0);
//        ida.rowToLocation(3, 0);
//        ida.rowToLocation(5, 0);
//        ida.rowToLocation(6, 0);
//        ida.rowToLocation(9, 0);
//
//        assertEquals(0, ida.rowToLocation(1, CallFlow.CURRENT_INSTRUCTION - 1));
//        assertEquals(-1, ida.rowToLocation(1, CallFlow.CURRENT_INSTRUCTION - 2));
//
//        assertEquals(1, ida.rowToLocation(3, CallFlow.CURRENT_INSTRUCTION - 1));
//        assertEquals(0, ida.rowToLocation(3, CallFlow.CURRENT_INSTRUCTION - 2));
//
//        assertEquals(3, ida.rowToLocation(5, CallFlow.CURRENT_INSTRUCTION - 1));
//        assertEquals(1, ida.rowToLocation(5, CallFlow.CURRENT_INSTRUCTION - 2));
//        assertEquals(0, ida.rowToLocation(5, CallFlow.CURRENT_INSTRUCTION - 3));
//
//        assertEquals(5, ida.rowToLocation(6, CallFlow.CURRENT_INSTRUCTION - 1));
//        assertEquals(3, ida.rowToLocation(6, CallFlow.CURRENT_INSTRUCTION - 2));
//        assertEquals(1, ida.rowToLocation(6, CallFlow.CURRENT_INSTRUCTION - 3));
//        assertEquals(0, ida.rowToLocation(6, CallFlow.CURRENT_INSTRUCTION - 4));
//
//        assertEquals(6, ida.rowToLocation(9, CallFlow.CURRENT_INSTRUCTION - 1));
//        assertEquals(5, ida.rowToLocation(9, CallFlow.CURRENT_INSTRUCTION - 2));
//        assertEquals(3, ida.rowToLocation(9, CallFlow.CURRENT_INSTRUCTION - 3));
//        assertEquals(1, ida.rowToLocation(9, CallFlow.CURRENT_INSTRUCTION - 4));
//    }
//
//    @Test
//    public void testCurrentPageAfterJumpThenStepThenJumpBackInstructionsBelowAreDecodedCorrectly() throws Exception {
//        CallFlow ida = new CallFlow(makeDisassembler(SHORT_PROGRAM), MEMORY_SIZE);
//
//        // advance for updating cache
//        ida.rowToLocation(0, 0);
//        ida.rowToLocation(9, 0); // jump
//        ida.rowToLocation(0, 0); // jump back
//
//        assertEquals(3, ida.rowToLocation(1, CallFlow.CURRENT_INSTRUCTION + 1));
//        assertEquals(5, ida.rowToLocation(1, CallFlow.CURRENT_INSTRUCTION + 2));
//        assertEquals(6, ida.rowToLocation(1, CallFlow.CURRENT_INSTRUCTION + 3));
//        assertEquals(9, ida.rowToLocation(1, CallFlow.CURRENT_INSTRUCTION + 4));
//
//        assertEquals(5, ida.rowToLocation(3, CallFlow.CURRENT_INSTRUCTION + 1));
//        assertEquals(6, ida.rowToLocation(3, CallFlow.CURRENT_INSTRUCTION + 2));
//        assertEquals(9, ida.rowToLocation(3, CallFlow.CURRENT_INSTRUCTION + 3));
//
//        assertEquals(6, ida.rowToLocation(5, CallFlow.CURRENT_INSTRUCTION + 1));
//        assertEquals(9, ida.rowToLocation(5, CallFlow.CURRENT_INSTRUCTION + 2));
//
//        assertEquals(9, ida.rowToLocation(6, CallFlow.CURRENT_INSTRUCTION + 1));
//    }
//
//    @Test
//    public void testCurrentPageAfterJumpThenStepThenJumpBackInstructionsAboveAreDecodedCorrectly() throws Exception {
//        CallFlow ida = new CallFlow(makeDisassembler(SHORT_PROGRAM), MEMORY_SIZE);
//
//        // advance for updating cache
//        ida.rowToLocation(0, 0);
//        ida.rowToLocation(1, 0);
//        ida.rowToLocation(9, 0); // jump
//        ida.rowToLocation(1, 0); // jump back
//
//        assertEquals(0, ida.rowToLocation(1, CallFlow.CURRENT_INSTRUCTION - 1));
//        assertEquals(-1, ida.rowToLocation(1, CallFlow.CURRENT_INSTRUCTION - 2));
//    }
//
//    @Test
//    public void testNextPageThenJumpToTheFirstInstructionUpdatesPage() throws Exception {
//        CallFlow ida = new CallFlow(makeDisassembler(LONG_PROGRAM), MEMORY_SIZE);
//
//        // advance for updating cache
//        ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION);
//        ida.pageNext();
//        ida.rowToLocation(ida.bytesPerPage(), CallFlow.CURRENT_INSTRUCTION);
//        ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION); // jump back
//
//        assertEquals(ida.bytesPerPage(), ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION));
//    }
//
//    @Test
//    public void testPreviousPageThenJumpToTheFirstInstructionUpdatesPage() throws Exception {
//        CallFlow ida = new CallFlow(makeDisassembler(LONG_PROGRAM), MEMORY_SIZE);
//
//        // advance for updating cache
//        ida.rowToLocation(24, CallFlow.CURRENT_INSTRUCTION);
//        ida.pagePrevious();
//        int location = ida.rowToLocation(25, CallFlow.CURRENT_INSTRUCTION); // jump back
//
//        assertEquals(location, ida.rowToLocation(25, CallFlow.CURRENT_INSTRUCTION));
//    }
//
//    @Test
//    public void testCurrentPageStepThenChangeFirstInstructionInMemoryAreDecodedCorrectly() throws Exception {
//        DisassemblerStub dis = makeDisassembler(SHORT_PROGRAM);
//        CallFlow ida = new CallFlow(dis, MEMORY_SIZE);
//
//        ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION);
//        ida.rowToLocation(1, CallFlow.CURRENT_INSTRUCTION + 1);
//
//        dis.set(0, 4);
//        dis.set(1, 4);
//        dis.set(2, 4);
//
//        ida.flushCache(0,3);
//
//        assertEquals(3, ida.rowToLocation(3, CallFlow.CURRENT_INSTRUCTION));
//        assertEquals(-1, ida.rowToLocation(3, CallFlow.CURRENT_INSTRUCTION - 1));
//        assertEquals(-1, ida.rowToLocation(3, CallFlow.CURRENT_INSTRUCTION - 2));
//    }
//
//    @Test
//    public void testCurrentPageStepThenChangeFirstInstructionInMemoryDoesNotChangeCurrentLocation() throws Exception {
//        DisassemblerStub dis = makeDisassembler(SHORT_PROGRAM);
//        CallFlow ida = new CallFlow(dis, MEMORY_SIZE);
//
//        ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION);
//        ida.rowToLocation(1, CallFlow.CURRENT_INSTRUCTION + 1);
//
//        dis.set(0, 4);
//        dis.set(1, 4);
//        dis.set(2, 4);
//
//        ida.flushCache(0,3);
//        ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION); // jump to updated instruction
//
//        assertEquals(3, ida.rowToLocation(3, CallFlow.CURRENT_INSTRUCTION));
//        assertEquals(0, ida.rowToLocation(3, CallFlow.CURRENT_INSTRUCTION - 1));
//        assertEquals(-1, ida.rowToLocation(3, CallFlow.CURRENT_INSTRUCTION - 2));
//    }
//
//    @Test
//    public void testFirstPageReturnsZeroAsTheFirstRowLocation() throws Exception {
//        CallFlow ida = new CallFlow(makeDisassembler(SHORT_PROGRAM), MEMORY_SIZE);
//
//        ida.pageFirst();
//        assertEquals(0, ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION));
//    }
//
//    @Test
//    public void testFirstPageThenStepThenJumpThenJumpBackReturnsZeroAsTheFirstRowLocation() throws Exception {
//        CallFlow ida = new CallFlow(makeDisassembler(LONG_PROGRAM), MEMORY_SIZE);
//
//        ida.rowToLocation(0, 0); // step
//        ida.pageFirst();
//
//        ida.rowToLocation(32, 0); // jump
//        ida.rowToLocation(0, 0); // jump back
//
//        assertEquals(0, ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION));
//    }
//
//    @Test
//    public void testLastPageReturnsMemSizeMinusOneAsTheFirstRowLocationAndItIsAlsoTheLastInstruction() throws Exception {
//        CallFlow ida = new CallFlow(makeDisassembler(LONG_PROGRAM), MEMORY_SIZE);
//
//        ida.pageLast();
//
//        assertEquals(MEMORY_SIZE - 1, ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION));
//    }
//
//    @Test
//    public void testLastPageThenJumpThenJumpBackReturnsMemSizeMinusOneAsTheFirstRowLocationAndItIsAlsoTheLastInstruction() throws Exception {
//        CallFlow ida = new CallFlow(makeDisassembler(LONG_PROGRAM), MEMORY_SIZE);
//
//        ida.pageLast();
//
//        ida.rowToLocation(33, CallFlow.CURRENT_INSTRUCTION); // jump somewhere
//        ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION); // jump back
//
//        assertEquals(MEMORY_SIZE - 1, ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION));
//    }
//
//    @Test
//    public void testNextPageBeforeCallsToRowToLocationWorks() throws Exception {
//        CallFlow ida = new CallFlow(makeDisassembler(LONG_PROGRAM), MEMORY_SIZE);
//
//        ida.pageNext();
//
//        assertEquals(ida.bytesPerPage(), ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION));
//    }
//
//    @Test
//    public void testAfterFewStepsFirstPagePointsAtTheSameThingsAsCurrentPage() throws Exception {
//        CallFlow ida = new CallFlow(makeDisassembler(LONG_PROGRAM), MEMORY_SIZE);
//
//        ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION);
//        ida.rowToLocation(1, CallFlow.CURRENT_INSTRUCTION);
//        ida.rowToLocation(3, CallFlow.CURRENT_INSTRUCTION);
//        ida.rowToLocation(5, CallFlow.CURRENT_INSTRUCTION);
//        ida.rowToLocation(6, CallFlow.CURRENT_INSTRUCTION);
//        int location = ida.rowToLocation(9, CallFlow.CURRENT_INSTRUCTION);
//
//        ida.pageFirst();
//
//        assertEquals(location, ida.rowToLocation(9, CallFlow.CURRENT_INSTRUCTION));
//    }
//
//    @Test
//    public void testCurrentInstructionIsStillShownWhenCurrentLocationIsBelowBytesPerPageAndPreviousPageIsInvoked() throws Exception {
//        CallFlow ida = new CallFlow(makeDisassembler(SHORT_PROGRAM), MEMORY_SIZE);
//
//        ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION);
//        ida.pagePrevious();
//
//        assertTrue(ida.isRowAtCurrentInstruction(CallFlow.CURRENT_INSTRUCTION, 0));
//    }
//
//    @Test
//    public void testCurrentInstructionIsNotShownWhenCurrentLocationIsBelowBytesPerPageAndPreviousPageIsInvoked() throws Exception {
//        CallFlow ida = new CallFlow(makeDisassembler(LONG_PROGRAM), MEMORY_SIZE);
//
//        ida.rowToLocation(ida.bytesPerPage() + 1, CallFlow.CURRENT_INSTRUCTION);
//        ida.pagePrevious();
//
//        assertFalse(ida.isRowAtCurrentInstruction(CallFlow.CURRENT_INSTRUCTION, ida.bytesPerPage() + 1));
//    }
//
//    @Test
//    public void testCurrentInstructionIsShownWhenMemorySizeIsLessThanPageAheadAndNextPageIsInvoked() throws Exception {
//        CallFlow ida = new CallFlow(makeDisassembler(SHORT_PROGRAM), SHORT_PROGRAM.length);
//
//        ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION);
//        ida.pageNext();
//
//        assertTrue(ida.isRowAtCurrentInstruction(CallFlow.CURRENT_INSTRUCTION, 0));
//    }
//
//    @Test
//    public void testCurrentInstructionIsNotShownWhenMemorySizeIsMoreThanPageAheadAndNextPageIsInvoked() throws Exception {
//        CallFlow ida = new CallFlow(makeDisassembler(LONG_PROGRAM), MEMORY_SIZE);
//
//        ida.rowToLocation(0, CallFlow.CURRENT_INSTRUCTION);
//        ida.pageNext();
//
//        assertFalse(ida.isRowAtCurrentInstruction(CallFlow.CURRENT_INSTRUCTION, 0));
//    }
}
