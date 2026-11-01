/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.*;

public class MemoryAnalysisTest {
    @Test
    public void capturesBanksWithoutChangingSelectionAndKeepsCommonRegionShared() {
        DebugMemory m = new DebugMemory(16, 2, 8);
        m.selected = 1;
        m.values[0][2] = 1;
        m.values[1][2] = 2;
        m.values[0][10] = 3;
        MemoryAnalysis image = MemoryAnalysis.capture(m);
        m.write(2, (byte) 7);
        assertEquals(1, m.selected);
        assertEquals(1, image.value(2, 0));
        assertEquals(2, image.value(2, 1));
        assertEquals(3, image.value(10, 1));
        assertEquals(List.of(2), image.differences(MemoryAnalysis.capture(m), 1));
        assertTrue(image.differences(MemoryAnalysis.capture(m), 0).isEmpty());
    }

    @Test
    public void maskedNibbleAndStringSearch() {
        MemoryAnalysis image =
                new MemoryAnalysis(
                        new byte[][] {{(byte) 0xab, 0x12, (byte) 0xaf, 0x13, 65, 66}}, 0);
        byte[][] p = MemoryAnalysis.pattern("A? 1?");
        assertEquals(List.of(0, 2), image.search(0, p[0], p[1]));
        p = MemoryAnalysis.pattern("str:AB");
        assertEquals(List.of(4), image.search(0, p[0], p[1]));
        assertEquals("AB", image.strings(0, 2).get(4));
    }

    @Test
    public void endianIntegersAndUnsignedRescans() {
        MemoryAnalysis old = new MemoryAnalysis(new byte[][] {{1, 2, 3, 4}}, 0),
                now = new MemoryAnalysis(new byte[][] {{2, 2, 1, 4}}, 0);
        assertEquals(0x0201, old.number(0, 0, 2, true));
        assertEquals(0x0102, old.number(0, 0, 2, false));
        assertArrayEquals(new byte[] {0x34, 0x12}, MemoryAnalysis.integer(0x1234, 2, true));
        assertArrayEquals(new byte[] {0x12, 0x34}, MemoryAnalysis.integer(0x1234, 2, false));
        assertEquals(List.of(0, 2), old.rescan(now, 0, List.of(0, 1, 2), 1, true, "Changed"));
        assertEquals(List.of(1), old.rescan(now, 0, List.of(0, 1, 2), 1, true, "Equal"));
        assertEquals(List.of(0), old.rescan(now, 0, List.of(0, 1, 2), 1, true, "Increased"));
        assertEquals(List.of(2), old.rescan(now, 0, List.of(0, 1, 2), 1, true, "Decreased"));
    }

    @Test
    public void rejectsLayoutMismatchAndInvalidPatterns() {
        MemoryAnalysis a = new MemoryAnalysis(new byte[][] {{1, 2}}, 0),
                b = new MemoryAnalysis(new byte[][] {{1, 2}}, 1);
        assertThrows(IllegalArgumentException.class, () -> a.differences(b, 0));
        assertThrows(IllegalArgumentException.class, () -> MemoryAnalysis.pattern("GG"));
        assertThrows(IllegalArgumentException.class, () -> MemoryAnalysis.integer(256, 1, true));
        assertThrows(IllegalArgumentException.class, () -> a.number(0, 1, 2, true));
    }
}
