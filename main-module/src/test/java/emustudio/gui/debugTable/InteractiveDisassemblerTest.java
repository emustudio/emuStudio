/*
 * (c) Copyright 2006-2015, Peter Jakubčo
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

import emulib.plugins.cpu.Disassembler;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertEquals;

public class InteractiveDisassemblerTest {
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
            34
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
        new InteractiveDisassembler(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateInstanceNegativeMemorySizeThrows() throws Exception {
        new InteractiveDisassembler(createMock(Disassembler.class), -1);
    }

    @Test
    public void testCurrentPageInstructionsAboveCurrentLocationAreNotDecoded() throws Exception {
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(SHORT_PROGRAM), MEMORY_SIZE);

        for (int i = 0; i < InteractiveDisassembler.CURRENT_INSTRUCTION; i++) {
            assertEquals(-1, ida.rowToLocation(0, i));
        }
    }

    @Test
    public void testCurrentPageForCurrentLocationRowToLocationReturnsCurrentLocation() throws Exception {
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(SHORT_PROGRAM), MEMORY_SIZE);

        assertEquals(0, ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION));
    }

    @Test
    public void testCurrentPageInstructionsBelowAreDecodedCorrectly() throws Exception {
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(SHORT_PROGRAM), MEMORY_SIZE);

        assertEquals(1, ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION + 1));
        assertEquals(3, ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION + 2));
        assertEquals(5, ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION + 3));
        assertEquals(6, ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION + 4));
        assertEquals(9, ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION + 5));
        assertEquals(10, ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION + 6));
    }

    @Test
    public void testCurrentPageInstructionsBelowAreDecodedCorrectlyAfterAdvance() throws Exception {
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(SHORT_PROGRAM), MEMORY_SIZE);

        // advance for updating cache
        ida.rowToLocation(0, 0);
        ida.rowToLocation(1, 0);
        ida.rowToLocation(3, 0);
        ida.rowToLocation(5, 0);
        ida.rowToLocation(6, 0);
        ida.rowToLocation(9, 0);

        assertEquals(3, ida.rowToLocation(1, InteractiveDisassembler.CURRENT_INSTRUCTION + 1));
        assertEquals(5, ida.rowToLocation(1, InteractiveDisassembler.CURRENT_INSTRUCTION + 2));
        assertEquals(6, ida.rowToLocation(1, InteractiveDisassembler.CURRENT_INSTRUCTION + 3));
        assertEquals(9, ida.rowToLocation(1, InteractiveDisassembler.CURRENT_INSTRUCTION + 4));

        assertEquals(5, ida.rowToLocation(3, InteractiveDisassembler.CURRENT_INSTRUCTION + 1));
        assertEquals(6, ida.rowToLocation(3, InteractiveDisassembler.CURRENT_INSTRUCTION + 2));
        assertEquals(9, ida.rowToLocation(3, InteractiveDisassembler.CURRENT_INSTRUCTION + 3));

        assertEquals(6, ida.rowToLocation(5, InteractiveDisassembler.CURRENT_INSTRUCTION + 1));
        assertEquals(9, ida.rowToLocation(5, InteractiveDisassembler.CURRENT_INSTRUCTION + 2));

        assertEquals(9, ida.rowToLocation(6, InteractiveDisassembler.CURRENT_INSTRUCTION + 1));
    }


    @Test
    public void testCurrentPageInstructionsAboveAreDecodedCorrectlyAfterAdvance() throws Exception {
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(SHORT_PROGRAM), MEMORY_SIZE);

        // advance for updating cache
        ida.rowToLocation(0, 0);
        ida.rowToLocation(1, 0);
        ida.rowToLocation(3, 0);
        ida.rowToLocation(5, 0);
        ida.rowToLocation(6, 0);
        ida.rowToLocation(9, 0);

        assertEquals(0, ida.rowToLocation(1, InteractiveDisassembler.CURRENT_INSTRUCTION - 1));
        assertEquals(-1, ida.rowToLocation(1, InteractiveDisassembler.CURRENT_INSTRUCTION - 2));

        assertEquals(1, ida.rowToLocation(3, InteractiveDisassembler.CURRENT_INSTRUCTION - 1));
        assertEquals(0, ida.rowToLocation(3, InteractiveDisassembler.CURRENT_INSTRUCTION - 2));

        assertEquals(3, ida.rowToLocation(5, InteractiveDisassembler.CURRENT_INSTRUCTION - 1));
        assertEquals(1, ida.rowToLocation(5, InteractiveDisassembler.CURRENT_INSTRUCTION - 2));
        assertEquals(0, ida.rowToLocation(5, InteractiveDisassembler.CURRENT_INSTRUCTION - 3));

        assertEquals(5, ida.rowToLocation(6, InteractiveDisassembler.CURRENT_INSTRUCTION - 1));
        assertEquals(3, ida.rowToLocation(6, InteractiveDisassembler.CURRENT_INSTRUCTION - 2));
        assertEquals(1, ida.rowToLocation(6, InteractiveDisassembler.CURRENT_INSTRUCTION - 3));
        assertEquals(0, ida.rowToLocation(6, InteractiveDisassembler.CURRENT_INSTRUCTION - 4));

        assertEquals(6, ida.rowToLocation(9, InteractiveDisassembler.CURRENT_INSTRUCTION - 1));
        assertEquals(5, ida.rowToLocation(9, InteractiveDisassembler.CURRENT_INSTRUCTION - 2));
        assertEquals(3, ida.rowToLocation(9, InteractiveDisassembler.CURRENT_INSTRUCTION - 3));
        assertEquals(1, ida.rowToLocation(9, InteractiveDisassembler.CURRENT_INSTRUCTION - 4));
    }

    @Test
    public void testCurrentPageAfterJumpThenStepThenJumpBackInstructionsBelowAreDecodedCorrectly() throws Exception {
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(SHORT_PROGRAM), MEMORY_SIZE);

        // advance for updating cache
        ida.rowToLocation(0, 0);
        ida.rowToLocation(9, 0); // jump
        ida.rowToLocation(0, 0); // jump back

        assertEquals(3, ida.rowToLocation(1, InteractiveDisassembler.CURRENT_INSTRUCTION + 1));
        assertEquals(5, ida.rowToLocation(1, InteractiveDisassembler.CURRENT_INSTRUCTION + 2));
        assertEquals(6, ida.rowToLocation(1, InteractiveDisassembler.CURRENT_INSTRUCTION + 3));
        assertEquals(9, ida.rowToLocation(1, InteractiveDisassembler.CURRENT_INSTRUCTION + 4));

        assertEquals(5, ida.rowToLocation(3, InteractiveDisassembler.CURRENT_INSTRUCTION + 1));
        assertEquals(6, ida.rowToLocation(3, InteractiveDisassembler.CURRENT_INSTRUCTION + 2));
        assertEquals(9, ida.rowToLocation(3, InteractiveDisassembler.CURRENT_INSTRUCTION + 3));

        assertEquals(6, ida.rowToLocation(5, InteractiveDisassembler.CURRENT_INSTRUCTION + 1));
        assertEquals(9, ida.rowToLocation(5, InteractiveDisassembler.CURRENT_INSTRUCTION + 2));

        assertEquals(9, ida.rowToLocation(6, InteractiveDisassembler.CURRENT_INSTRUCTION + 1));
    }

    @Test
    public void testCurrentPageAfterJumpThenStepThenJumpBackInstructionsAboveAreDecodedCorrectly() throws Exception {
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(SHORT_PROGRAM), MEMORY_SIZE);

        // advance for updating cache
        ida.rowToLocation(0, 0);
        ida.rowToLocation(1, 0);
        ida.rowToLocation(9, 0); // jump
        ida.rowToLocation(1, 0); // jump back

        assertEquals(0, ida.rowToLocation(1, InteractiveDisassembler.CURRENT_INSTRUCTION - 1));
        assertEquals(-1, ida.rowToLocation(1, InteractiveDisassembler.CURRENT_INSTRUCTION - 2));
    }

    @Test
    public void testNextPageThenJumpToTheFirstInstructionUpdatesPage() throws Exception {
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(LONG_PROGRAM), MEMORY_SIZE);

        // advance for updating cache
        ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION);
        ida.pageNext();
        ida.rowToLocation(InteractiveDisassembler.BYTES_PER_PAGE, InteractiveDisassembler.CURRENT_INSTRUCTION);
        ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION); // jump back

        assertEquals(InteractiveDisassembler.BYTES_PER_PAGE, ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION));
    }

    @Test
    public void testPreviousPageThenJumpToTheFirstInstructionUpdatesPage() throws Exception {
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(LONG_PROGRAM), MEMORY_SIZE);

        // advance for updating cache
        ida.rowToLocation(24, InteractiveDisassembler.CURRENT_INSTRUCTION);
        ida.pagePrevious();
        int location = ida.rowToLocation(25, InteractiveDisassembler.CURRENT_INSTRUCTION); // jump back

        assertEquals(location, ida.rowToLocation(25, InteractiveDisassembler.CURRENT_INSTRUCTION));
    }

    @Test
    public void testCurrentPageStepThenChangeFirstInstructionInMemoryAreDecodedCorrectly() throws Exception {
        DisassemblerStub dis = makeDisassembler(SHORT_PROGRAM);
        InteractiveDisassembler ida = new InteractiveDisassembler(dis, MEMORY_SIZE);

        ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION);
        ida.rowToLocation(1, InteractiveDisassembler.CURRENT_INSTRUCTION + 1);

        dis.set(0, 4);
        dis.set(1, 4);
        dis.set(2, 4);

        ida.flushCache(0,3);

        assertEquals(3, ida.rowToLocation(3, InteractiveDisassembler.CURRENT_INSTRUCTION));
        assertEquals(-1, ida.rowToLocation(3, InteractiveDisassembler.CURRENT_INSTRUCTION - 1));
        assertEquals(-1, ida.rowToLocation(3, InteractiveDisassembler.CURRENT_INSTRUCTION - 2));
    }

    @Test
    public void testCurrentPageStepThenChangeFirstInstructionInMemoryDoesNotChangeCurrentLocation() throws Exception {
        DisassemblerStub dis = makeDisassembler(SHORT_PROGRAM);
        InteractiveDisassembler ida = new InteractiveDisassembler(dis, MEMORY_SIZE);

        ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION);
        ida.rowToLocation(1, InteractiveDisassembler.CURRENT_INSTRUCTION + 1);

        dis.set(0, 4);
        dis.set(1, 4);
        dis.set(2, 4);

        ida.flushCache(0,3);
        ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION); // jump to updated instruction

        assertEquals(3, ida.rowToLocation(3, InteractiveDisassembler.CURRENT_INSTRUCTION));
        assertEquals(0, ida.rowToLocation(3, InteractiveDisassembler.CURRENT_INSTRUCTION - 1));
        assertEquals(-1, ida.rowToLocation(3, InteractiveDisassembler.CURRENT_INSTRUCTION - 2));
    }

    @Test
    public void testFirstPageReturnsZeroAsTheFirstRowLocation() throws Exception {
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(SHORT_PROGRAM), MEMORY_SIZE);

        ida.pageFirst();
        assertEquals(0, ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION));
    }

    @Test
    public void testFirstPageThenStepThenJumpThenJumpBackReturnsZeroAsTheFirstRowLocation() throws Exception {
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(LONG_PROGRAM), MEMORY_SIZE);

        ida.rowToLocation(0, 0); // step
        ida.pageFirst();

        ida.rowToLocation(32, 0); // jump
        ida.rowToLocation(0, 0); // jump back

        assertEquals(0, ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION));
    }

    @Test
    public void testLastPageReturnsMemSizeMinusOneAsTheFirstRowLocationAndItIsAlsoTheLastInstruction() throws Exception {
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(LONG_PROGRAM), MEMORY_SIZE);

        ida.pageLast();

        assertEquals(MEMORY_SIZE - 1, ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION));
    }

    @Test
    public void testLastPageThenJumpThenJumpBackReturnsMemSizeMinusOneAsTheFirstRowLocationAndItIsAlsoTheLastInstruction() throws Exception {
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(LONG_PROGRAM), MEMORY_SIZE);

        ida.pageLast();

        ida.rowToLocation(33, InteractiveDisassembler.CURRENT_INSTRUCTION); // jump somewhere
        ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION); // jump back

        assertEquals(MEMORY_SIZE - 1, ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION));
    }

    @Test
    public void testNextPageBeforeCallsToRowToLocationWorks() throws Exception {
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(LONG_PROGRAM), MEMORY_SIZE);

        ida.pageNext();

        assertEquals(InteractiveDisassembler.BYTES_PER_PAGE, ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION));
    }

    @Test
    public void testAfterFewStepsFirstPagePointsAtTheSameThingsAsCurrentPage() throws Exception {
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(LONG_PROGRAM), MEMORY_SIZE);

        ida.rowToLocation(0, InteractiveDisassembler.CURRENT_INSTRUCTION);
        ida.rowToLocation(1, InteractiveDisassembler.CURRENT_INSTRUCTION);
        ida.rowToLocation(3, InteractiveDisassembler.CURRENT_INSTRUCTION);
        ida.rowToLocation(5, InteractiveDisassembler.CURRENT_INSTRUCTION);
        ida.rowToLocation(6, InteractiveDisassembler.CURRENT_INSTRUCTION);
        int location = ida.rowToLocation(9, InteractiveDisassembler.CURRENT_INSTRUCTION);

        ida.pageFirst();

        assertEquals(location, ida.rowToLocation(9, InteractiveDisassembler.CURRENT_INSTRUCTION));
    }
}