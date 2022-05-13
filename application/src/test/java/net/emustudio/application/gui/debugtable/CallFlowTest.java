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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.emustudio.application.gui.debugtable.MockHelper.*;
import static org.junit.Assert.assertEquals;

public class CallFlowTest {

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceNullDisassemblerThrows() {
        new CallFlow(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTraverseWithFromHigherThanUntilToThrows() {
        CallFlow callFlow = makeCallFlow(0, 6);
        callFlow.traverseUpTo(6, 0, i -> {
        });
    }

    @Test
    public void testTraverseToHigherLocationThanMemorySizeDoesNotThrow() {
        List<Integer> locations = new ArrayList<>();

        CallFlow callFlow = makeCallFlow(0);
        callFlow.traverseUpTo(0, 1000, locations::add);

        for (int i = 0; i < 10; i++) {
            assertEquals(i, locations.get(i).intValue());
        }
    }

    @Test
    public void testFlushCache() {
        CallFlow callFlow = makeCallFlow(0, 1, 2, 3, 4, 5);

        List<Integer> locations = callFlow.getLocations(0, 5);
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), locations);

        callFlow.flushCache(0, 5);

        locations = callFlow.getLocations(0, 5);
        assertEquals(Collections.singletonList(5), locations);
    }

    @Test
    public void getDefaultLongestInstructionSizeIsOne() {
        assertEquals(1, makeCallFlow().getLongestInstructionSize());
    }

    @Test
    public void testKnownFromButEmptyUntilTo() {
        CallFlow callFlow = makeCallFlow(0);
        List<Integer> locations = callFlow.getLocations(0, 9);

        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), locations);
    }

    @Test
    public void testEmptyFromButKnownTo() {
        CallFlow callFlow = makeCallFlow(9);
        List<Integer> locations = callFlow.getLocations(0, 9);

        assertEquals(Collections.singletonList(9), locations);
    }

    @Test
    public void testPreviousFromEmptyUntilTo() {
        CallFlow callFlow = makeCallFlow(0);
        List<Integer> locations = callFlow.getLocations(5, 9);

        assertEquals(Arrays.asList(5, 6, 7, 8, 9), locations);
    }

    @Test
    public void testHigherFromEmptyUntilTo() {
        CallFlow callFlow = makeCallFlow(3);
        List<Integer> locations = callFlow.getLocations(0, 5);

        assertEquals(Arrays.asList(3, 4, 5), locations);
    }

    @Test
    public void testKnownFromPreviousUntilTo() {
        CallFlow callFlow = makeCallFlow(3, 5);
        List<Integer> locations = callFlow.getLocations(3, 9);

        assertEquals(Arrays.asList(3, 4, 5, 6, 7, 8, 9), locations);
    }

    @Test
    public void testEmptyFromPreviousUntilTo() {
        CallFlow callFlow = makeCallFlow(5);
        List<Integer> locations = callFlow.getLocations(3, 9);

        assertEquals(Arrays.asList(5, 6, 7, 8, 9), locations);
    }

    @Test
    public void testKnownFromUnfitTo() {
        CallFlow callFlow = makeCallFlowStep(0, 2);
        List<Integer> locations = callFlow.getLocations(0, 3);

        assertEquals(Arrays.asList(0, 2), locations);
    }

    @Test
    public void testUnfitBeforeKnownFromKnownTo() {
        CallFlow callFlow = makeCallFlowStep(2, 6);
        List<Integer> locations = callFlow.getLocations(1, 6);

        assertEquals(Arrays.asList(2, 4, 6), locations);
    }

    @Test
    public void testUnfitAfterKnownFromKnownTo() {
        CallFlow callFlow = makeCallFlowStep(2, 6);
        List<Integer> locations = callFlow.getLocations(3, 6);

        assertEquals(Arrays.asList(2, 4, 6), locations);
    }

    @Test
    public void testUnfitPreviousKnownFromUnfitPreviousKnownTo() {
        CallFlow callFlow = makeCallFlowStep(2, 6);
        List<Integer> locations = callFlow.getLocations(1, 5);

        assertEquals(Arrays.asList(2, 4), locations);
    }

    @Test
    public void testUnfitAfterKnownFromUnfitPreviousKnownTo() {
        CallFlow callFlow = makeCallFlowStep(0, 6);
        List<Integer> locations = callFlow.getLocations(1, 5);

        assertEquals(Arrays.asList(0, 2, 4), locations);
    }

    @Test
    public void testUnfitPreviousKnownFromUnfitAfterKnownTo() {
        CallFlow callFlow = makeCallFlowStep(2, 6);
        List<Integer> locations = callFlow.getLocations(1, 7);

        assertEquals(Arrays.asList(2, 4, 6), locations);
    }

    @Test
    public void testUnfitAfterKnownFromUnfitAfterKnownTo() {
        CallFlow callFlow = makeCallFlowStep(0, 6);
        List<Integer> locations = callFlow.getLocations(1, 7);

        assertEquals(Arrays.asList(0, 2, 4, 6), locations);
    }

    @Test
    public void testUnfitEmptyKnownUnfitEmptyTo() {
        CallFlow callFlow = makeCallFlowStep();
        List<Integer> locations = callFlow.getLocations(0, 5);

        assertEquals(Arrays.asList(0, 2, 4), locations);
    }

    @Test
    public void testSelfModificationKnownFromKnownUntilTo() {
        DisassemblerStub disasm = makeDisassembler();
        CallFlow callFlow = makeCallFlow(disasm, 2, 4);
        modify(callFlow, disasm, 2, 6);

        List<Integer> locations = callFlow.getLocations(2, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testSelfModificationPreviousKnownFromPreviousKnownUntilTo() {
        DisassemblerStub disasm = makeDisassembler();
        CallFlow callFlow = makeCallFlow(disasm, 2, 4);
        modify(callFlow, disasm, 2, 6);

        List<Integer> locations = callFlow.getLocations(3, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testSelfModificationAfterKnownFromPreviousKnownUntilTo() {
        DisassemblerStub disasm = makeDisassembler();
        CallFlow callFlow = makeCallFlow(disasm, 2, 4);
        modify(callFlow, disasm, 2, 6);

        List<Integer> locations = callFlow.getLocations(1, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testSelfModificationKnownFromEmptyUntilTo() {
        DisassemblerStub disasm = makeDisassembler();
        CallFlow callFlow = makeCallFlow(disasm, 2);
        modify(callFlow, disasm, 2, 6);

        List<Integer> locations = callFlow.getLocations(2, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testSelfModificationKnownFromKnownUntilToLonger() {
        DisassemblerStub disasm = makeDisassembler();
        CallFlow callFlow = makeCallFlow(disasm, 2, 4, 5, 8);
        modify(callFlow, disasm, 2, 6);

        List<Integer> locations = callFlow.getLocations(2, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testInvalidLocationBetweenFromAndTo() {
        DisassemblerStub disasm = makeDisassembler();
        disasm.set(1, 10);
        CallFlow callFlow = makeCallFlow(disasm, 0, 6);

        List<Integer> locations = callFlow.getLocations(0, 8);

        assertEquals(Arrays.asList(0, 1), locations);
    }

    @Test
    public void testNegativeFrom() {
        CallFlow callFlow = makeCallFlow(0);

        List<Integer> locations = callFlow.getLocations(-10, 0);

        assertEquals(Collections.singletonList(0), locations);
    }

    @Test
    public void testNegativeFromAndNegativeTo() {
        CallFlow callFlow = makeCallFlow(0);

        List<Integer> locations = callFlow.getLocations(-10, -5);

        assertEquals(Collections.emptyList(), locations);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromIsHigherThanToBothNegative() {
        CallFlow callFlow = makeCallFlow(0);

        callFlow.getLocations(-5, -10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromIsHigherThanTo() {
        CallFlow callFlow = makeCallFlow(0);

        callFlow.getLocations(10, 5);
    }
}
