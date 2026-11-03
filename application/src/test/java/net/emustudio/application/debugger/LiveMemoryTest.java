/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LiveMemoryTest {
    @Test public void readsCurrentCellsWithoutRetainingThemAndUsesExplicitPhysicalBanks() {
        byte[][] cells = {{1, 2, 9}, {3, 4, 99}};
        DebuggerTestMachine machine = new DebuggerTestMachine(cells, 2, false);
        LiveMemory live = new LiveMemory(machine.memory, ProgramIndex.create("live", 3, 2, 2).snapshot().layout(), () -> {});
        verify(machine.memory, never()).readBank(anyInt(), anyInt());
        assertEquals(3, live.size());
        assertEquals(2, live.bankCount());
        assertEquals(3, live.value(0, 1));
        cells[1][0] = 7;
        assertEquals(7, live.value(0, 1));
        assertEquals(9, live.value(2, 1));
        verify(machine.memory).readBank(2, 0);
        assertEquals(1, live.physicalBank(0, 1));
        assertEquals(0, live.physicalBank(2, 1));
        assertThrows(IllegalArgumentException.class, () -> live.value(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> live.value(3, 0));
        assertThrows(IllegalArgumentException.class, () -> live.value(0, 2));
        assertThrows(IllegalArgumentException.class, () -> live.search(-1, new byte[] {0}, new byte[] {-1}));
    }

    @Test public void searchesAndNumbersReadLiveMemoryAndReturnOnlyDerivedResults() {
        byte[][] cells = {{0x12, 0x34, 0x12, 0x3f, 0, 'H', 'I', 0, 'O', 'K'}};
        DebuggerTestMachine machine = new DebuggerTestMachine(cells, cells[0].length, false);
        LiveMemory live = new LiveMemory(machine.memory,
                ProgramIndex.create("live", cells[0].length, 1, cells[0].length).snapshot().layout(), () -> {});
        byte[][] pattern = LiveMemory.pattern("12 3?");
        assertEquals(List.of(0, 2), live.search(0, pattern[0], pattern[1]));
        assertEquals(0x3412, live.number(0, 0, 2, true));
        assertEquals(0x1234, live.number(0, 0, 2, false));
        assertEquals(Map.of(5, "HI", 8, "OK"), live.strings(0, 2));
        cells[0][0] = 0;
        assertEquals(List.of(2), live.search(0, pattern[0], pattern[1]));
        assertArrayEquals(new byte[] {0x12, 0x34}, LiveMemory.integer(0x3412, 2, true));
        assertArrayEquals(new byte[] {(byte) 0xc3, (byte) 0xa9}, LiveMemory.pattern("str:é")[0]);
        assertThrows(IllegalArgumentException.class, () -> LiveMemory.pattern(""));
        assertThrows(IllegalArgumentException.class, () -> LiveMemory.pattern("str:"));
        assertThrows(IllegalArgumentException.class, () -> LiveMemory.integer(256, 1, true));
        assertThrows(IllegalArgumentException.class, () -> live.number(0, 9, 2, true));
        assertThrows(IllegalArgumentException.class, () -> live.strings(0, 0));
    }

    @Test public void hugeSparseDomainDoesNotReadOrAllocateAnImage() {
        @SuppressWarnings("unchecked") MemoryContext<Byte> memory = mock(MemoryContext.class);
        when(memory.getCellTypeClass()).thenReturn(Byte.class);
        when(memory.readBank(Integer.MAX_VALUE - 1, 0)).thenReturn((byte) 42);
        LiveMemory live = new LiveMemory(memory,
                ProgramIndex.create("large", Integer.MAX_VALUE, 1, 0).snapshot().layout(), () -> {});
        assertEquals(Integer.MAX_VALUE, live.size());
        verify(memory, never()).readBank(anyInt(), anyInt());
        assertEquals(42, live.value(Integer.MAX_VALUE - 1, 0));
    }

    @Test public void guardsRunAfterReadsAndCancellationAndTypedMemoryAreRejected() {
        DebuggerTestMachine machine = new DebuggerTestMachine(new byte[][] {{1}}, 1, false);
        ProgramIndex.Layout layout = ProgramIndex.create("live", 1, 1, 1).snapshot().layout();
        int[] checks = {0};
        LiveMemory live = new LiveMemory(machine.memory, layout, () -> {
            if (++checks[0] == 2) throw new IllegalStateException("Changed during read");
        });
        assertThrows(IllegalStateException.class, () -> live.value(0, 0));
        Thread.currentThread().interrupt();
        try { assertThrows(CancellationException.class, live::requireCurrent); }
        finally { Thread.interrupted(); }
        @SuppressWarnings("unchecked") MemoryContext<Integer> words = mock(MemoryContext.class);
        when(words.getCellTypeClass()).thenReturn(Integer.class);
        assertThrows(IllegalArgumentException.class, () -> new LiveMemory(words, layout, () -> {}));
    }
}
