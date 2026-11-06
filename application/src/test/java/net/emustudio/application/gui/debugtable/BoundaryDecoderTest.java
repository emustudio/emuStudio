/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.application.gui.debugtable.PaginatingDisassembler.BoundaryDecoder;

import org.junit.Test;
import net.emustudio.emulib.plugins.cpu.Disassembler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static net.emustudio.application.gui.debugtable.MockHelper.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class BoundaryDecoderTest {

    @Test
    public void missingPredecessorTraversalRetainsValidatedBoundariesForNextQuery() {
        AtomicInteger calls = new AtomicInteger();
        Disassembler disassembler = mock(Disassembler.class);
        when(disassembler.getNextInstructionPosition(anyInt())).thenAnswer(call -> {
            calls.incrementAndGet();
            return (Integer) call.getArgument(0) + 1;
        });
        BoundaryDecoder flow = new BoundaryDecoder(disassembler);
        flow.updateCache(0);
        List<Integer> first = flow.getLocations(32760, 32780);
        int firstCalls = calls.get();
        assertTrue(firstCalls >= 32780);
        assertEquals(first, flow.getLocations(32760, 32780));
        assertEquals(firstCalls, calls.get());
    }

    @Test(expected = NullPointerException.class)
    public void testCreateInstanceNullDisassemblerThrows() {
        new BoundaryDecoder(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTraverseWithFromHigherThanUntilToThrows() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(0, 6);
        boundaryDecoder.traverseUpTo(6, 0, i -> {
        });
    }

    @Test
    public void testTraverseToHigherLocationThanMemorySizeDoesNotThrow() {
        List<Integer> locations = new ArrayList<>();

        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(0);
        boundaryDecoder.traverseUpTo(0, 1000, locations::add);

        for (int i = 0; i < 10; i++) {
            assertEquals(i, locations.get(i).intValue());
        }
    }

    @Test
    public void testFlushCache() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(0, 1, 2, 3, 4, 5);

        List<Integer> locations = boundaryDecoder.getLocations(0, 5);
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), locations);

        boundaryDecoder.flushCache(0, 5);

        locations = boundaryDecoder.getLocations(0, 5);
        assertEquals(Collections.singletonList(5), locations);
    }

    @Test
    public void getDefaultLongestInstructionSizeIsOne() {
        assertEquals(1, makeBoundaryDecoder().getLongestInstructionSize());
    }

    @Test
    public void testKnownFromButEmptyUntilTo() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(0);
        List<Integer> locations = boundaryDecoder.getLocations(0, 9);

        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), locations);
    }

    @Test
    public void testEmptyFromButKnownTo() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(9);
        List<Integer> locations = boundaryDecoder.getLocations(0, 9);

        assertEquals(Collections.singletonList(9), locations);
    }

    @Test
    public void testPreviousFromEmptyUntilTo() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(0);
        List<Integer> locations = boundaryDecoder.getLocations(5, 9);

        assertEquals(Arrays.asList(5, 6, 7, 8, 9), locations);
    }

    @Test
    public void testHigherFromEmptyUntilTo() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(3);
        List<Integer> locations = boundaryDecoder.getLocations(0, 5);

        assertEquals(Arrays.asList(3, 4, 5), locations);
    }

    @Test
    public void testKnownFromPreviousUntilTo() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(3, 5);
        List<Integer> locations = boundaryDecoder.getLocations(3, 9);

        assertEquals(Arrays.asList(3, 4, 5, 6, 7, 8, 9), locations);
    }

    @Test
    public void testEmptyFromPreviousUntilTo() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(5);
        List<Integer> locations = boundaryDecoder.getLocations(3, 9);

        assertEquals(Arrays.asList(5, 6, 7, 8, 9), locations);
    }

    @Test
    public void testKnownFromUnfitTo() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoderStep(0, 2);
        List<Integer> locations = boundaryDecoder.getLocations(0, 3);

        assertEquals(Arrays.asList(0, 2), locations);
    }

    @Test
    public void testUnfitBeforeKnownFromKnownTo() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoderStep(2, 6);
        List<Integer> locations = boundaryDecoder.getLocations(1, 6);

        assertEquals(Arrays.asList(2, 4, 6), locations);
    }

    @Test
    public void testUnfitAfterKnownFromKnownTo() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoderStep(2, 6);
        List<Integer> locations = boundaryDecoder.getLocations(3, 6);

        assertEquals(Arrays.asList(2, 4, 6), locations);
    }

    @Test
    public void testUnfitPreviousKnownFromUnfitPreviousKnownTo() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoderStep(2, 6);
        List<Integer> locations = boundaryDecoder.getLocations(1, 5);

        assertEquals(Arrays.asList(2, 4), locations);
    }

    @Test
    public void testUnfitAfterKnownFromUnfitPreviousKnownTo() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoderStep(0, 6);
        List<Integer> locations = boundaryDecoder.getLocations(1, 5);

        assertEquals(Arrays.asList(0, 2, 4), locations);
    }

    @Test
    public void testUnfitPreviousKnownFromUnfitAfterKnownTo() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoderStep(2, 6);
        List<Integer> locations = boundaryDecoder.getLocations(1, 7);

        assertEquals(Arrays.asList(2, 4, 6), locations);
    }

    @Test
    public void testUnfitAfterKnownFromUnfitAfterKnownTo() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoderStep(0, 6);
        List<Integer> locations = boundaryDecoder.getLocations(1, 7);

        assertEquals(Arrays.asList(0, 2, 4, 6), locations);
    }

    @Test
    public void testUnfitEmptyKnownUnfitEmptyTo() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoderStep();
        List<Integer> locations = boundaryDecoder.getLocations(0, 5);

        assertEquals(Arrays.asList(0, 2, 4), locations);
    }

    @Test
    public void testSelfModificationKnownFromKnownUntilTo() {
        int[] disasm = defaultDisassembly();
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(makeDisassembler(disasm), 2, 4);
        modify(boundaryDecoder, disasm, 2, 6);

        List<Integer> locations = boundaryDecoder.getLocations(2, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testSelfModificationPreviousKnownFromPreviousKnownUntilTo() {
        int[] disasm = defaultDisassembly();
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(makeDisassembler(disasm), 2, 4);
        modify(boundaryDecoder, disasm, 2, 6);

        List<Integer> locations = boundaryDecoder.getLocations(3, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testSelfModificationAfterKnownFromPreviousKnownUntilTo() {
        int[] disasm = defaultDisassembly();
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(makeDisassembler(disasm), 2, 4);
        modify(boundaryDecoder, disasm, 2, 6);

        List<Integer> locations = boundaryDecoder.getLocations(1, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testSelfModificationKnownFromEmptyUntilTo() {
        int[] disasm = defaultDisassembly();
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(makeDisassembler(disasm), 2);
        modify(boundaryDecoder, disasm, 2, 6);

        List<Integer> locations = boundaryDecoder.getLocations(2, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testSelfModificationKnownFromKnownUntilToLonger() {
        int[] disasm = defaultDisassembly();
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(makeDisassembler(disasm), 2, 4, 5, 8);
        modify(boundaryDecoder, disasm, 2, 6);

        List<Integer> locations = boundaryDecoder.getLocations(2, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testInvalidLocationBetweenFromAndTo() {
        int[] disasm = defaultDisassembly();
        disasm[1] = 10;
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(makeDisassembler(disasm), 0, 6);

        List<Integer> locations = boundaryDecoder.getLocations(0, 8);

        assertEquals(Arrays.asList(0, 1), locations);
    }

    @Test
    public void testNegativeFrom() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(0);

        List<Integer> locations = boundaryDecoder.getLocations(-10, 0);

        assertEquals(Collections.singletonList(0), locations);
    }

    @Test
    public void testNegativeFromAndNegativeTo() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(0);

        List<Integer> locations = boundaryDecoder.getLocations(-10, -5);

        assertEquals(Collections.emptyList(), locations);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromIsHigherThanToBothNegative() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(0);

        boundaryDecoder.getLocations(-5, -10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromIsHigherThanTo() {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(0);

        boundaryDecoder.getLocations(10, 5);
    }
}
