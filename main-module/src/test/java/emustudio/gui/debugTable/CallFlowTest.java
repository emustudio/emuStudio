/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static emustudio.gui.debugTable.MockHelper.*;
import static org.junit.Assert.assertEquals;

public class CallFlowTest {

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceNullDisassemblerThrows() throws Exception {
        new CallFlow(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTraverseWithFromHigherThanUntilToThrows() throws Exception {
        CallFlow callFlow = makeCallFlow(0, 6);
        callFlow.traverseUpTo(6, 0, i -> {
        });
    }

    @Test
    public void testTraverseToHigherLocationThanMemorySizeDoesNotThrow() throws Exception {
        List<Integer> locations = new ArrayList<>();

        CallFlow callFlow = makeCallFlow(0);
        callFlow.traverseUpTo(0, 1000, locations::add);

        for (int i = 0; i < 10; i++) {
            assertEquals(i, locations.get(i).intValue());
        }
    }

    @Test
    public void testFlushCache() throws Exception {
        CallFlow callFlow = makeCallFlow(0, 1, 2, 3, 4, 5);

        List<Integer> locations = callFlow.getLocations(0, 5);
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), locations);

        callFlow.flushCache(0, 5);

        locations = callFlow.getLocations(0, 5);
        assertEquals(Arrays.asList(5), locations);
    }

    @Test
    public void getDefaultLongestInstructionSizeIsOne() throws Exception {
        assertEquals(1, makeCallFlow().getLongestInstructionSize());
    }

    @Test
    public void testKnownFromButEmptyUntilTo() throws Exception {
        CallFlow callFlow = makeCallFlow(0);
        List<Integer> locations = callFlow.getLocations(0, 9);

        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), locations);
    }

    @Test
    public void testEmptyFromButKnownTo() throws Exception {
        CallFlow callFlow = makeCallFlow(9);
        List<Integer> locations = callFlow.getLocations(0, 9);

        assertEquals(Arrays.asList(9), locations);
    }

    @Test
    public void testPreviousFromEmptyUntilTo() throws Exception {
        CallFlow callFlow = makeCallFlow(0);
        List<Integer> locations = callFlow.getLocations(5, 9);

        assertEquals(Arrays.asList(5, 6, 7, 8, 9), locations);
    }

    @Test
    public void testHigherFromEmptyUntilTo() throws Exception {
        CallFlow callFlow = makeCallFlow(3);
        List<Integer> locations = callFlow.getLocations(0, 5);

        assertEquals(Arrays.asList(3, 4, 5), locations);
    }

    @Test
    public void testKnownFromPreviousUntilTo() throws Exception {
        CallFlow callFlow = makeCallFlow(3, 5);
        List<Integer> locations = callFlow.getLocations(3, 9);

        assertEquals(Arrays.asList(3, 4, 5, 6, 7, 8, 9), locations);
    }

    @Test
    public void testEmptyFromPreviousUntilTo() throws Exception {
        CallFlow callFlow = makeCallFlow(5);
        List<Integer> locations = callFlow.getLocations(3, 9);

        assertEquals(Arrays.asList(5, 6, 7, 8, 9), locations);
    }

    @Test
    public void testKnownFromUnfitTo() throws Exception {
        CallFlow callFlow = makeCallFlowStep(0, 2);
        List<Integer> locations = callFlow.getLocations(0, 3);

        assertEquals(Arrays.asList(0, 2), locations);
    }

    @Test
    public void testUnfitBeforeKnownFromKnownTo() throws Exception {
        CallFlow callFlow = makeCallFlowStep(2, 6);
        List<Integer> locations = callFlow.getLocations(1, 6);

        assertEquals(Arrays.asList(2, 4, 6), locations);
    }

    @Test
    public void testUnfitAfterKnownFromKnownTo() throws Exception {
        CallFlow callFlow = makeCallFlowStep(2, 6);
        List<Integer> locations = callFlow.getLocations(3, 6);

        assertEquals(Arrays.asList(2, 4, 6), locations);
    }

    @Test
    public void testUnfitPreviousKnownFromUnfitPreviousKnownTo() throws Exception {
        CallFlow callFlow = makeCallFlowStep(2, 6);
        List<Integer> locations = callFlow.getLocations(1, 5);

        assertEquals(Arrays.asList(2, 4), locations);
    }

    @Test
    public void testUnfitAfterKnownFromUnfitPreviousKnownTo() throws Exception {
        CallFlow callFlow = makeCallFlowStep(0, 6);
        List<Integer> locations = callFlow.getLocations(1, 5);

        assertEquals(Arrays.asList(0, 2, 4), locations);
    }

    @Test
    public void testUnfitPreviousKnownFromUnfitAfterKnownTo() throws Exception {
        CallFlow callFlow = makeCallFlowStep(2, 6);
        List<Integer> locations = callFlow.getLocations(1, 7);

        assertEquals(Arrays.asList(2, 4, 6), locations);
    }

    @Test
    public void testUnfitAfterKnownFromUnfitAfterKnownTo() throws Exception {
        CallFlow callFlow = makeCallFlowStep(0, 6);
        List<Integer> locations = callFlow.getLocations(1, 7);

        assertEquals(Arrays.asList(0, 2, 4, 6), locations);
    }

    @Test
    public void testUnfitEmptyKnownUnfitEmptyTo() throws Exception {
        CallFlow callFlow = makeCallFlowStep();
        List<Integer> locations = callFlow.getLocations(0, 5);

        assertEquals(Arrays.asList(0, 2, 4), locations);
    }

    @Test
    public void testSelfModificationKnownFromKnownUntilTo() throws Exception {
        DisassemblerStub disasm = makeDisassembler();
        CallFlow callFlow = makeCallFlow(disasm, 2, 4);
        modify(callFlow, disasm, 2, 6);

        List<Integer> locations = callFlow.getLocations(2, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testSelfModificationPreviousKnownFromPreviousKnownUntilTo() throws Exception {
        DisassemblerStub disasm = makeDisassembler();
        CallFlow callFlow = makeCallFlow(disasm, 2, 4);
        modify(callFlow, disasm, 2, 6);

        List<Integer> locations = callFlow.getLocations(3, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testSelfModificationAfterKnownFromPreviousKnownUntilTo() throws Exception {
        DisassemblerStub disasm = makeDisassembler();
        CallFlow callFlow = makeCallFlow(disasm, 2, 4);
        modify(callFlow, disasm, 2, 6);

        List<Integer> locations = callFlow.getLocations(1, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testSelfModificationKnownFromEmptyUntilTo() throws Exception {
        DisassemblerStub disasm = makeDisassembler();
        CallFlow callFlow = makeCallFlow(disasm, 2);
        modify(callFlow, disasm, 2, 6);

        List<Integer> locations = callFlow.getLocations(2, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testSelfModificationKnownFromKnownUntilToLonger() throws Exception {
        DisassemblerStub disasm = makeDisassembler();
        CallFlow callFlow = makeCallFlow(disasm, 2, 4, 5, 8);
        modify(callFlow, disasm, 2, 6);

        List<Integer> locations = callFlow.getLocations(2, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testInvalidLocationBetweenFromAndTo() throws Exception {
        DisassemblerStub disasm = makeDisassembler();
        disasm.set(1, 10);
        CallFlow callFlow = makeCallFlow(disasm, 0, 6);

        List<Integer> locations = callFlow.getLocations(0, 8);

        assertEquals(Arrays.asList(0, 1), locations);
    }

    @Test
    public void testNegativeFrom() throws Exception {
        CallFlow callFlow = makeCallFlow(0);

        List<Integer> locations = callFlow.getLocations(-10, 0);

        assertEquals(Arrays.asList(0), locations);
    }

    @Test
    public void testNegativeFromAndNegativeTo() throws Exception {
        CallFlow callFlow = makeCallFlow(0);

        List<Integer> locations = callFlow.getLocations(-10, -5);

        assertEquals(Arrays.asList(), locations);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromIsHigherThanToBothNegative() throws Exception {
        CallFlow callFlow = makeCallFlow(0);

        callFlow.getLocations(-5, -10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromIsHigherThanTo() throws Exception {
        CallFlow callFlow = makeCallFlow(0);

        callFlow.getLocations(10, 5);
    }

}
