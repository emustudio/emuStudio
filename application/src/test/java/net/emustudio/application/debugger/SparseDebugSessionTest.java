/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class SparseDebugSessionTest {
    @Test public void blankWordMemoryCanLoadAndClearWithoutReplacingItsSparseAddressDomain() throws Exception {
        CPU cpu = mock(CPU.class);
        @SuppressWarnings("unchecked") MemoryContext<Integer> memory = mock(MemoryContext.class);
        AtomicInteger occupancy = new AtomicInteger();
        AtomicReference<MemoryContext.MemoryListener> listener = new AtomicReference<>();
        when(cpu.getAddressSpaceSize()).thenReturn(Integer.MAX_VALUE);
        when(memory.getCellTypeClass()).thenReturn(Integer.class);
        when(memory.getSize()).thenAnswer(call -> occupancy.get());
        doAnswer(call -> { listener.set(call.getArgument(0)); return null; }).when(memory).addMemoryListener(any());

        try (DebugSession session = new DebugSession(cpu, memory, null)) {
            ProgramIndex index = session.programIndex();
            assertEquals(0, memory.getSize());
            assertEquals(Integer.MAX_VALUE, index.snapshot().layout().addressSpaceSize());
            int address = Integer.MAX_VALUE - 1;
            session.annotationEngine.put(new net.emustudio.emulib.runtime.annotations.MemoryAnnotations.Entry(
                    99, 1, address, Integer.MAX_VALUE, ProgramIndex.Kinds.LABEL, "last cell", null));
            try (ProgramIndex.BreakpointUpdate update = index.updateBreakpoints(index.snapshot())) {
                update.put(1, address, true);
                assertEquals(ProgramIndex.CommitResult.PUBLISHED, update.commit());
            }
            session.annotations().put(new AnnotationIndex.Entry(address, "metadata", "CODE"));
            assertEquals("metadata", session.annotations().get(address).label);
            assertThrows(IllegalArgumentException.class, session::liveMemory);

            occupancy.set(1);
            listener.get().memorySizeChanged();
            assertSame(index, session.programIndex());
            assertTrue(index.snapshot().hasExecutionBreakpoint(address));
            occupancy.set(0);
            listener.get().memorySizeChanged();
            assertSame(index, session.programIndex());
            assertEquals("last cell", session.annotationEngine.get(99, 1).text);
            assertEquals(Integer.MAX_VALUE, index.snapshot().layout().addressSpaceSize());
            verify(memory, never()).read(anyInt());
            verify(memory, never()).read(anyInt());
        }
    }
}
