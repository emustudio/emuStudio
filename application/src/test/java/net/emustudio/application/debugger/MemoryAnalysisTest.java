/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MemoryAnalysisTest {
    @Test
    public void captureReadsEveryLogicalBankIncludingSharedMemory() {
        MemoryContext<Byte> memory = byteMemory();
        when(memory.getBanksCount()).thenReturn(2);
        when(memory.getSize()).thenReturn(3);
        when(memory.getCommonBoundary()).thenReturn(2);
        when(memory.readBank(0, 0)).thenReturn((byte) 1);
        when(memory.readBank(1, 0)).thenReturn((byte) 2);
        when(memory.readBank(2, 0)).thenReturn((byte) 9);
        when(memory.readBank(0, 1)).thenReturn((byte) 3);
        when(memory.readBank(1, 1)).thenReturn((byte) 4);
        when(memory.readBank(2, 1)).thenReturn((byte) 9);

        MemoryAnalysis image = MemoryAnalysis.capture(memory);

        assertEquals(2, image.bankCount());
        assertEquals(3, image.size());
        assertArrayEquals(new byte[] {1, 2, 9}, image.bytes(0));
        assertArrayEquals(new byte[] {3, 4, 9}, image.bytes(1));
        assertEquals(1, image.physicalBank(1, 1));
        assertEquals(0, image.physicalBank(2, 1));
    }

    @Test
    public void imageOwnsInputAndReturnedByteArrays() {
        byte[][] banks = {{1, (byte) 255}};
        MemoryAnalysis image = new MemoryAnalysis(banks, 2);

        banks[0][0] = 99;
        byte[] exported = image.bytes(0);
        exported[1] = 0;

        assertEquals(1, image.value(0, 0));
        assertEquals(255, image.value(1, 0));
    }

    @Test
    public void captureRejectsNonByteMemory() {
        MemoryContext<Integer> memory = integerMemory();
        when(memory.getCellTypeClass()).thenReturn(Integer.class);

        assertThrows(IllegalArgumentException.class, () -> MemoryAnalysis.capture(memory));
        verify(memory, never()).getBanksCount();
    }

    @Test
    public void invalidBankLayoutsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MemoryAnalysis(new byte[0][], 0));
        assertThrows(IllegalArgumentException.class, () -> new MemoryAnalysis(new byte[][] {{1}, {1, 2}}, 1));
        assertThrows(IllegalArgumentException.class, () -> new MemoryAnalysis(new byte[][] {{1}}, -1));
        assertThrows(IllegalArgumentException.class, () -> new MemoryAnalysis(new byte[][] {{1}}, 2));
    }

    @Test
    public void addressesAndBanksMustBelongToImage() {
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {{1, 2}}, 2);

        assertThrows(IllegalArgumentException.class, () -> image.value(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> image.value(2, 0));
        assertThrows(IllegalArgumentException.class, () -> image.value(0, 1));
        assertThrows(IllegalArgumentException.class, () -> image.physicalBank(0, -1));
    }

    @Test
    public void identityDependsOnContentsAndLayout() {
        MemoryAnalysis original = new MemoryAnalysis(new byte[][] {{1, 2}}, 2);
        MemoryAnalysis identical = new MemoryAnalysis(new byte[][] {{1, 2}}, 2);
        MemoryAnalysis changedByte = new MemoryAnalysis(new byte[][] {{1, 3}}, 2);
        MemoryAnalysis changedBoundary = new MemoryAnalysis(new byte[][] {{1, 2}}, 1);
        MemoryAnalysis changedBanks = new MemoryAnalysis(new byte[][] {{1, 2}, {1, 2}}, 2);

        assertEquals(original.identity(), identical.identity());
        assertNotEquals(original.identity(), changedByte.identity());
        assertNotEquals(original.identity(), changedBoundary.identity());
        assertNotEquals(original.identity(), changedBanks.identity());
    }

    @Test
    public void differencesReportOnlyChangedAddressesInRequestedBank() {
        MemoryAnalysis before = new MemoryAnalysis(new byte[][] {{0, 0, 9}, {1, 2, 9}}, 2);
        MemoryAnalysis after = new MemoryAnalysis(new byte[][] {{7, 0, 9}, {1, 8, 9}}, 2);

        assertEquals(List.of(1), before.differences(after, 1));
        assertEquals(List.of(0), before.differences(after, 0));
        assertEquals(List.of(1), before.differences(new byte[] {1, 8, 9}, 1));
    }

    @Test
    public void comparisonsRejectDifferentLayoutsAndBinaryLengths() {
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {{1, 2}}, 2);

        assertThrows(IllegalArgumentException.class,
                () -> image.differences(new MemoryAnalysis(new byte[][] {{1, 2}}, 1), 0));
        assertThrows(IllegalArgumentException.class,
                () -> image.requireSameLayout(new MemoryAnalysis(new byte[][] {{1}}, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> image.requireSameLayout(new MemoryAnalysis(new byte[][] {{1, 2}, {1, 2}}, 2)));
        assertThrows(IllegalArgumentException.class, () -> image.differences(new byte[] {1}, 0));
    }

    @Test
    public void hexPatternsKeepIndependentNibbleWildcards() {
        byte[][] pattern = MemoryAnalysis.pattern(" a? ?F ?? 12 ");

        assertArrayEquals(new byte[] {(byte) 0xa0, 0x0f, 0, 0x12}, pattern[0]);
        assertArrayEquals(new byte[] {(byte) 0xf0, 0x0f, 0, (byte) 0xff}, pattern[1]);
    }

    @Test
    public void stringPatternsUseUtf8BytesAndExactMasks() {
        byte[][] pattern = MemoryAnalysis.pattern("str:é");

        assertArrayEquals(new byte[] {(byte) 0xc3, (byte) 0xa9}, pattern[0]);
        assertArrayEquals(new byte[] {(byte) 0xff, (byte) 0xff}, pattern[1]);
    }

    @Test
    public void malformedAndEmptyPatternsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> MemoryAnalysis.pattern(""));
        assertThrows(IllegalArgumentException.class, () -> MemoryAnalysis.pattern("str:"));
        assertThrows(IllegalArgumentException.class, () -> MemoryAnalysis.pattern("1"));
        assertThrows(IllegalArgumentException.class, () -> MemoryAnalysis.pattern("GG"));
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {{1, 2}}, 2);
        assertThrows(IllegalArgumentException.class, () -> image.search(0, new byte[0], new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> image.search(0, new byte[] {1}, new byte[0]));
    }

    @Test
    public void searchFindsOverlappingMatchesIncludingLastPossibleAddress() {
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {{1, 1, 1}}, 3);

        List<Integer> matches = image.search(0, new byte[] {1, 1}, new byte[] {-1, -1});

        assertEquals(List.of(0, 1), matches);
        assertTrue(image.search(0, new byte[] {1, 1, 1, 1}, new byte[] {-1, -1, -1, -1}).isEmpty());
    }

    @Test
    public void searchAppliesMasksWithinSelectedBank() {
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {{0, 0, 0}, {(byte) 0xa1, 0x2f, (byte) 0xa9}}, 3);

        List<Integer> matches = image.search(1, new byte[] {(byte) 0xa0}, new byte[] {(byte) 0xf0});

        assertEquals(List.of(0, 2), matches);
    }

    @Test
    public void integersEncodeRequestedByteOrderWithoutSignExtension() {
        assertArrayEquals(new byte[] {0x34, 0x12}, MemoryAnalysis.integer(0x1234, 2, true));
        assertArrayEquals(new byte[] {0x12, 0x34}, MemoryAnalysis.integer(0x1234, 2, false));
        assertArrayEquals(new byte[] {(byte) 255}, MemoryAnalysis.integer(255, 1, true));
        assertArrayEquals(new byte[] {-1, -1, -1, -1, -1, -1, -1, -1}, MemoryAnalysis.integer(-1L, 8, false));
    }

    @Test
    public void integerEncodingRejectsUnsupportedWidthsAndOverflow() {
        assertThrows(IllegalArgumentException.class, () -> MemoryAnalysis.integer(1, 0, true));
        assertThrows(IllegalArgumentException.class, () -> MemoryAnalysis.integer(1, 9, true));
        assertThrows(IllegalArgumentException.class, () -> MemoryAnalysis.integer(256, 1, true));
        assertThrows(IllegalArgumentException.class, () -> MemoryAnalysis.integer(-1, 2, true));
    }

    @Test
    public void numbersDecodeBothByteOrdersAtEndOfMemory() {
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {{0, (byte) 0x80, (byte) 0xff}}, 3);

        assertEquals(0xff80L, image.number(0, 1, 2, true));
        assertEquals(0x80ffL, image.number(0, 1, 2, false));
        assertThrows(IllegalArgumentException.class, () -> image.number(0, 2, 2, true));
        assertThrows(IllegalArgumentException.class, () -> image.number(0, 0, 0, true));
    }

    @Test
    public void rescanFiltersCandidateAddressesByChangeDirection() {
        MemoryAnalysis before = new MemoryAnalysis(new byte[][] {{10, 20, 30}}, 3);
        MemoryAnalysis after = new MemoryAnalysis(new byte[][] {{11, 19, 30}}, 3);
        List<Integer> candidates = List.of(0, 1, 2);

        assertEquals(List.of(0, 1), before.rescan(after, 0, candidates, 1, true, "Changed"));
        assertEquals(List.of(2), before.rescan(after, 0, candidates, 1, true, "Equal"));
        assertEquals(List.of(0), before.rescan(after, 0, candidates, 1, true, "Increased"));
        assertEquals(List.of(1), before.rescan(after, 0, candidates, 1, true, "Decreased"));
        assertThrows(IllegalArgumentException.class,
                () -> before.rescan(after, 0, candidates, 1, true, "Unknown"));
    }

    @Test
    public void rescanComparesEightByteValuesAsUnsigned() {
        MemoryAnalysis before = new MemoryAnalysis(new byte[][] {{0x7f, -1, -1, -1, -1, -1, -1, -1}}, 8);
        MemoryAnalysis after = new MemoryAnalysis(new byte[][] {{(byte) 0x80, 0, 0, 0, 0, 0, 0, 0}}, 8);

        assertEquals(List.of(0), before.rescan(after, 0, List.of(0), 8, false, "Increased"));
        assertTrue(before.rescan(after, 0, List.of(0), 8, false, "Decreased").isEmpty());
    }

    @Test
    public void stringsExcludeShortRunsAndIncludeUnterminatedFinalRun() {
        byte[] bytes = "AB\0HELLO\nEND".getBytes(StandardCharsets.US_ASCII);
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {bytes}, bytes.length);

        assertEquals(Map.of(3, "HELLO", 9, "END"), image.strings(0, 3));
        assertThrows(IllegalArgumentException.class, () -> image.strings(0, 0));
    }

    @Test
    public void binaryReadRequiresExactSize() throws IOException {
        assertArrayEquals(new byte[] {1, 2, 3},
                MemoryAnalysis.readBinary(new ByteArrayInputStream(new byte[] {1, 2, 3}), 3));
        assertThrows(IOException.class,
                () -> MemoryAnalysis.readBinary(new ByteArrayInputStream(new byte[] {1, 2}), 3));
        assertThrows(IOException.class,
                () -> MemoryAnalysis.readBinary(new ByteArrayInputStream(new byte[] {1, 2, 3, 4}), 3));
    }

    @Test
    public void interruptedSearchStopsWithoutClearingInterruptFlag() {
        MemoryAnalysis image = new MemoryAnalysis(new byte[][] {{1, 2}}, 2);
        try {
            Thread.currentThread().interrupt();

            assertThrows(CancellationException.class,
                    () -> image.search(0, new byte[] {1}, new byte[] {-1}));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @SuppressWarnings("unchecked")
    private static MemoryContext<Byte> byteMemory() {
        MemoryContext<Byte> memory = mock(MemoryContext.class);
        when(memory.getCellTypeClass()).thenReturn(Byte.class);
        return memory;
    }

    @SuppressWarnings("unchecked")
    private static MemoryContext<Integer> integerMemory() {
        return mock(MemoryContext.class);
    }
}
