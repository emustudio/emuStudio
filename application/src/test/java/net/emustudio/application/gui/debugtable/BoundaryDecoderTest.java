/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.application.gui.debugtable.PaginatingDisassembler.BoundaryDecoder;

import org.junit.Test;
import net.emustudio.emulib.plugins.cpu.Decoder;

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
    public void wrappedExtentAtLargestAddressSpaceClipsListingWithoutOverflow() {
        int address = Integer.MAX_VALUE - 1;
        Decoder decoder = at -> net.emustudio.emulib.plugins.cpu.DecodedInstruction.of(at, 2, "NOP", "00 00");
        BoundaryDecoder boundaries = new BoundaryDecoder(decoder);
        boundaries.updateCache(address);
        List<Integer> next = new ArrayList<>();

        boundaries.traverseForInstructionCount(address, 1, next::add);

        assertEquals(List.of(Integer.MAX_VALUE), next);
        assertEquals(1, boundaries.getLongestInstructionSize());
    }

    @Test
    public void missingPredecessorTraversalRetainsValidatedBoundariesForNextQuery() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Decoder disassembler = mock(Decoder.class);
        when(disassembler.decode(anyInt())).thenAnswer(call -> {
            calls.incrementAndGet();
            return instruction(call.getArgument(0), 1);
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
    public void testCreateInstanceNullDisassemblerThrows() throws Exception {
        new BoundaryDecoder(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTraverseWithFromHigherThanUntilToThrows() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(0, 6);
        boundaryDecoder.traverseUpTo(6, 0, i -> {
        });
    }

    @Test
    public void testTraverseToHigherLocationThanMemorySizeDoesNotThrow() throws Exception {
        List<Integer> locations = new ArrayList<>();

        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(0);
        boundaryDecoder.traverseUpTo(0, 1000, locations::add);

        for (int i = 0; i < 10; i++) {
            assertEquals(i, locations.get(i).intValue());
        }
    }

    @Test
    public void testFlushCache() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(0, 1, 2, 3, 4, 5);

        List<Integer> locations = boundaryDecoder.getLocations(0, 5);
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), locations);

        boundaryDecoder.flushCache(0, 5);

        locations = boundaryDecoder.getLocations(0, 5);
        assertEquals(Collections.singletonList(5), locations);
    }

    @Test
    public void getDefaultLongestInstructionSizeIsOne() throws Exception {
        assertEquals(1, makeBoundaryDecoder().getLongestInstructionSize());
    }

    @Test
    public void testKnownFromButEmptyUntilTo() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(0);
        List<Integer> locations = boundaryDecoder.getLocations(0, 9);

        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), locations);
    }

    @Test
    public void testEmptyFromButKnownTo() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(9);
        List<Integer> locations = boundaryDecoder.getLocations(0, 9);

        assertEquals(Collections.singletonList(9), locations);
    }

    @Test
    public void testPreviousFromEmptyUntilTo() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(0);
        List<Integer> locations = boundaryDecoder.getLocations(5, 9);

        assertEquals(Arrays.asList(5, 6, 7, 8, 9), locations);
    }

    @Test
    public void testHigherFromEmptyUntilTo() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(3);
        List<Integer> locations = boundaryDecoder.getLocations(0, 5);

        assertEquals(Arrays.asList(3, 4, 5), locations);
    }

    @Test
    public void testKnownFromPreviousUntilTo() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(3, 5);
        List<Integer> locations = boundaryDecoder.getLocations(3, 9);

        assertEquals(Arrays.asList(3, 4, 5, 6, 7, 8, 9), locations);
    }

    @Test
    public void testEmptyFromPreviousUntilTo() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(5);
        List<Integer> locations = boundaryDecoder.getLocations(3, 9);

        assertEquals(Arrays.asList(5, 6, 7, 8, 9), locations);
    }

    @Test
    public void testKnownFromUnfitTo() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoderStep(0, 2);
        List<Integer> locations = boundaryDecoder.getLocations(0, 3);

        assertEquals(Arrays.asList(0, 2), locations);
    }

    @Test
    public void testUnfitBeforeKnownFromKnownTo() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoderStep(2, 6);
        List<Integer> locations = boundaryDecoder.getLocations(1, 6);

        assertEquals(Arrays.asList(2, 4, 6), locations);
    }

    @Test
    public void testUnfitAfterKnownFromKnownTo() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoderStep(2, 6);
        List<Integer> locations = boundaryDecoder.getLocations(3, 6);

        assertEquals(Arrays.asList(2, 4, 6), locations);
    }

    @Test
    public void testUnfitPreviousKnownFromUnfitPreviousKnownTo() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoderStep(2, 6);
        List<Integer> locations = boundaryDecoder.getLocations(1, 5);

        assertEquals(Arrays.asList(2, 4), locations);
    }

    @Test
    public void testUnfitAfterKnownFromUnfitPreviousKnownTo() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoderStep(0, 6);
        List<Integer> locations = boundaryDecoder.getLocations(1, 5);

        assertEquals(Arrays.asList(0, 2, 4), locations);
    }

    @Test
    public void testUnfitPreviousKnownFromUnfitAfterKnownTo() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoderStep(2, 6);
        List<Integer> locations = boundaryDecoder.getLocations(1, 7);

        assertEquals(Arrays.asList(2, 4, 6), locations);
    }

    @Test
    public void testUnfitAfterKnownFromUnfitAfterKnownTo() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoderStep(0, 6);
        List<Integer> locations = boundaryDecoder.getLocations(1, 7);

        assertEquals(Arrays.asList(0, 2, 4, 6), locations);
    }

    @Test
    public void testUnfitEmptyKnownUnfitEmptyTo() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoderStep();
        List<Integer> locations = boundaryDecoder.getLocations(0, 5);

        assertEquals(Arrays.asList(0, 2, 4), locations);
    }

    @Test
    public void testSelfModificationKnownFromKnownUntilTo() throws Exception {
        int[] disasm = defaultDisassembly();
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(makeDisassembler(disasm), 2, 4);
        modify(boundaryDecoder, disasm, 2, 6);

        List<Integer> locations = boundaryDecoder.getLocations(2, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testSelfModificationPreviousKnownFromPreviousKnownUntilTo() throws Exception {
        int[] disasm = defaultDisassembly();
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(makeDisassembler(disasm), 2, 4);
        modify(boundaryDecoder, disasm, 2, 6);

        List<Integer> locations = boundaryDecoder.getLocations(3, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testSelfModificationAfterKnownFromPreviousKnownUntilTo() throws Exception {
        int[] disasm = defaultDisassembly();
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(makeDisassembler(disasm), 2, 4);
        modify(boundaryDecoder, disasm, 2, 6);

        List<Integer> locations = boundaryDecoder.getLocations(1, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testSelfModificationKnownFromEmptyUntilTo() throws Exception {
        int[] disasm = defaultDisassembly();
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(makeDisassembler(disasm), 2);
        modify(boundaryDecoder, disasm, 2, 6);

        List<Integer> locations = boundaryDecoder.getLocations(2, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testSelfModificationKnownFromKnownUntilToLonger() throws Exception {
        int[] disasm = defaultDisassembly();
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(makeDisassembler(disasm), 2, 4, 5, 8);
        modify(boundaryDecoder, disasm, 2, 6);

        List<Integer> locations = boundaryDecoder.getLocations(2, 8);

        assertEquals(Arrays.asList(2, 6, 7, 8), locations);
    }

    @Test
    public void testInvalidLocationBetweenFromAndTo() throws Exception {
        int[] disasm = defaultDisassembly();
        disasm[1] = 10;
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(makeDisassembler(disasm), 0, 6);

        List<Integer> locations = boundaryDecoder.getLocations(0, 8);

        assertEquals(Arrays.asList(0, 1), locations);
    }

    @Test
    public void testNegativeFrom() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(0);

        List<Integer> locations = boundaryDecoder.getLocations(-10, 0);

        assertEquals(Collections.singletonList(0), locations);
    }

    @Test
    public void testNegativeFromAndNegativeTo() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(0);

        List<Integer> locations = boundaryDecoder.getLocations(-10, -5);

        assertEquals(Collections.emptyList(), locations);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromIsHigherThanToBothNegative() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(0);

        boundaryDecoder.getLocations(-5, -10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromIsHigherThanTo() throws Exception {
        BoundaryDecoder boundaryDecoder = makeBoundaryDecoder(0);

        boundaryDecoder.getLocations(10, 5);
    }
}
