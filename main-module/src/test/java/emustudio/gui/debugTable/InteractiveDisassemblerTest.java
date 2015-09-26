package emustudio.gui.debugTable;

import emulib.plugins.cpu.Disassembler;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertEquals;

public class InteractiveDisassemblerTest {
    private final static int MEMORY_SIZE = 0x10000;

    private Disassembler makeDisassembler(int... instructions) {
        return new DisassemblerStub(MEMORY_SIZE, instructions);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceNullDisasseblerThrows() throws Exception {
        new InteractiveDisassembler(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateInstanceNegativeMemorySizeThrows() throws Exception {
        new InteractiveDisassembler(createMock(Disassembler.class), -1);
    }

    @Test
    public void testCurrentPageInstructionsAboveCurrentLocationAreNotDecoded() throws Exception {
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(
                1,
                3,3,
                5,5,
                6,
                9,9,9,
                10
        ), MEMORY_SIZE);

        for (int i = 0; i < InteractiveDisassembler.INSTRUCTIONS_IN_GAP; i++) {
            assertEquals(-1, ida.rowToLocation(0, i));
        }
    }

    @Test
    public void testCurrentPageForCurrentLocationRowToLocationReturnsCurrentLocation() throws Exception {
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(
                1,
                3,3,
                5,5,
                6,
                9,9,9,
                10
        ), MEMORY_SIZE);

        assertEquals(0, ida.rowToLocation(0, InteractiveDisassembler.INSTRUCTIONS_IN_GAP));
    }

    @Test
    public void testCurrentPageInstructionsBelowAreDecodedCorrectly() throws Exception {
        int[] program = new int[] {
                1,
                3,3,
                5,5,
                6,
                9,9,9,
                10
        };
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(program), MEMORY_SIZE);

        assertEquals(1, ida.rowToLocation(0, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 1));
        assertEquals(3, ida.rowToLocation(0, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 2));
        assertEquals(5, ida.rowToLocation(0, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 3));
        assertEquals(6, ida.rowToLocation(0, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 4));
        assertEquals(9, ida.rowToLocation(0, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 5));
        assertEquals(10, ida.rowToLocation(0, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 6));
    }

    @Test
    public void testCurrentPageInstructionsBelowAreDecodedCorrectlyAfterAdvance() throws Exception {
        int[] program = new int[] {
                1,
                3,3,
                5,5,
                6,
                9,9,9,
                10
        };
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(program), MEMORY_SIZE);

        // advance for updating cache
        ida.rowToLocation(0, 0);
        ida.rowToLocation(1, 0);
        ida.rowToLocation(3, 0);
        ida.rowToLocation(5, 0);
        ida.rowToLocation(6, 0);
        ida.rowToLocation(9, 0);

        assertEquals(3, ida.rowToLocation(1, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 1));
        assertEquals(5, ida.rowToLocation(1, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 2));
        assertEquals(6, ida.rowToLocation(1, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 3));
        assertEquals(9, ida.rowToLocation(1, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 4));

        assertEquals(5, ida.rowToLocation(3, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 1));
        assertEquals(6, ida.rowToLocation(3, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 2));
        assertEquals(9, ida.rowToLocation(3, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 3));

        assertEquals(6, ida.rowToLocation(5, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 1));
        assertEquals(9, ida.rowToLocation(5, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 2));

        assertEquals(9, ida.rowToLocation(6, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 1));
    }


    @Test
    public void testCurrentPageInstructionsAboveAreDecodedCorrectlyAfterAdvance() throws Exception {
        int[] program = new int[] {
                1,
                3,3,
                5,5,
                6,
                9,9,9,
                10
        };
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(program), MEMORY_SIZE);

        // advance for updating cache
        ida.rowToLocation(0, 0);
        ida.rowToLocation(1, 0);
        ida.rowToLocation(3, 0);
        ida.rowToLocation(5, 0);
        ida.rowToLocation(6, 0);
        ida.rowToLocation(9, 0);

        assertEquals(0, ida.rowToLocation(1, InteractiveDisassembler.INSTRUCTIONS_IN_GAP - 1));
        assertEquals(-1, ida.rowToLocation(1, InteractiveDisassembler.INSTRUCTIONS_IN_GAP - 2));

        assertEquals(1, ida.rowToLocation(3, InteractiveDisassembler.INSTRUCTIONS_IN_GAP - 1));
        assertEquals(0, ida.rowToLocation(3, InteractiveDisassembler.INSTRUCTIONS_IN_GAP - 2));

        assertEquals(3, ida.rowToLocation(5, InteractiveDisassembler.INSTRUCTIONS_IN_GAP - 1));
        assertEquals(1, ida.rowToLocation(5, InteractiveDisassembler.INSTRUCTIONS_IN_GAP - 2));
        assertEquals(0, ida.rowToLocation(5, InteractiveDisassembler.INSTRUCTIONS_IN_GAP - 3));

        assertEquals(5, ida.rowToLocation(6, InteractiveDisassembler.INSTRUCTIONS_IN_GAP - 1));
        assertEquals(3, ida.rowToLocation(6, InteractiveDisassembler.INSTRUCTIONS_IN_GAP - 2));
        assertEquals(1, ida.rowToLocation(6, InteractiveDisassembler.INSTRUCTIONS_IN_GAP - 3));
        assertEquals(0, ida.rowToLocation(6, InteractiveDisassembler.INSTRUCTIONS_IN_GAP - 4));

        assertEquals(6, ida.rowToLocation(9, InteractiveDisassembler.INSTRUCTIONS_IN_GAP - 1));
        assertEquals(5, ida.rowToLocation(9, InteractiveDisassembler.INSTRUCTIONS_IN_GAP - 2));
        assertEquals(3, ida.rowToLocation(9, InteractiveDisassembler.INSTRUCTIONS_IN_GAP - 3));
        assertEquals(1, ida.rowToLocation(9, InteractiveDisassembler.INSTRUCTIONS_IN_GAP - 4));
        assertEquals(0, ida.rowToLocation(9, InteractiveDisassembler.INSTRUCTIONS_IN_GAP - 5));
    }

    @Test
    public void testCurrentPageAfterJumpThenStepThenJumpBackInstructionsBelowAreDecodedCorrectly() throws Exception {
        int[] program = new int[] {
                1,
                3,3,
                5,5,
                6,
                9,9,9,
                10
        };
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(program), MEMORY_SIZE);

        // advance for updating cache
        ida.rowToLocation(0, 0);
        ida.rowToLocation(9, 0); // jump
        ida.rowToLocation(0, 0); // jump back

        assertEquals(3, ida.rowToLocation(1, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 1));
        assertEquals(5, ida.rowToLocation(1, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 2));
        assertEquals(6, ida.rowToLocation(1, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 3));
        assertEquals(9, ida.rowToLocation(1, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 4));

        assertEquals(5, ida.rowToLocation(3, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 1));
        assertEquals(6, ida.rowToLocation(3, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 2));
        assertEquals(9, ida.rowToLocation(3, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 3));

        assertEquals(6, ida.rowToLocation(5, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 1));
        assertEquals(9, ida.rowToLocation(5, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 2));

        assertEquals(9, ida.rowToLocation(6, InteractiveDisassembler.INSTRUCTIONS_IN_GAP + 1));
    }

    @Test
    public void testCurrentPageAfterJumpThenStepThenJumpBackInstructionsAboveAreDecodedCorrectly() throws Exception {
        int[] program = new int[] {
                1,
                3,3,
                5,5,
                6,
                9,9,9,
                10
        };
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(program), MEMORY_SIZE);

        // advance for updating cache
        ida.rowToLocation(0, 0);
        ida.rowToLocation(1, 0);
        ida.rowToLocation(9, 0); // jump
        ida.rowToLocation(1, 0); // jump back

        assertEquals(0, ida.rowToLocation(1, InteractiveDisassembler.INSTRUCTIONS_IN_GAP - 1));
        assertEquals(-1, ida.rowToLocation(1, InteractiveDisassembler.INSTRUCTIONS_IN_GAP - 2));
    }

    @Test
    public void testNextPageThenJumpToTheFirstInstructionUpdatesPage() throws Exception {
        int[] program = new int[] {
                1,
                3,3,
                5,5,
                6,
                9,9,9,
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
        InteractiveDisassembler ida = new InteractiveDisassembler(makeDisassembler(program), MEMORY_SIZE);

        // advance for updating cache
        ida.rowToLocation(0, InteractiveDisassembler.INSTRUCTIONS_IN_GAP);
        ida.pageNext();
        ida.rowToLocation(0, InteractiveDisassembler.INSTRUCTIONS_IN_GAP); // jump back

        assertEquals(InteractiveDisassembler.BYTES_PER_PAGE, ida.rowToLocation(0, InteractiveDisassembler.INSTRUCTIONS_IN_GAP));
    }

    @Test
    public void testPreviousPageThenJumpToTheFirstInstructionUpdatesPage() throws Exception {


    }

    @Test
    public void testCurrentPageStepThenChangeFirstInstructionInMemoryAreDecodedCorrectly() throws Exception {


    }

    @Test
    public void testFirstPageReturnsZeroAsTheFirstRowLocation() throws Exception {


    }

    @Test
    public void testFirstPageThenJumpThenStepThenJumpBackReturnsZeroAsTheFirstRowLocation() throws Exception {


    }

    @Test
    public void testLastPageReturnsMemSizeMinusOneAsTheFirstRowLocationAndItIsAlsoTheLastInstruction() throws Exception {


    }

    @Test
    public void testLastPageThenJumpThenJumpBackReturnsMemSizeMinusOneAsTheFirstRowLocationAndItIsAlsoTheLastInstruction() throws Exception {


    }

    @Test
    public void testNextPageBeforeCallsToRowToLocationWorks() throws Exception {


    }

    @Test
    public void testLastPageBeforeCallsToRowToLocationWorks() throws Exception {


    }




}