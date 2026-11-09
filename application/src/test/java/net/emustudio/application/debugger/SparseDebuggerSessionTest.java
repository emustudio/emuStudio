/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

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

public class SparseDebuggerSessionTest {
    @Test public void blankWordMemoryCanLoadAndClearWithoutReplacingItsSparseAddressDomain() throws Exception {
        CPU cpu = mock(CPU.class, withSettings().extraInterfaces(net.emustudio.emulib.plugins.debugger.Debugging.class));
        when(((net.emustudio.emulib.plugins.debugger.Debugging) cpu).observe(any(),
                any(net.emustudio.emulib.plugins.debugger.Debugging.Capture.class)))
                .thenReturn(() -> {});
        @SuppressWarnings("unchecked") MemoryContext<Integer> memory = mock(MemoryContext.class);
        AtomicInteger occupancy = new AtomicInteger();
        AtomicReference<MemoryContext.MemoryListener> listener = new AtomicReference<>();
        when(cpu.getAddressSpaceSize()).thenReturn(Integer.MAX_VALUE);
        when(memory.getCellTypeClass()).thenReturn(Integer.class);
        when(memory.getSize()).thenAnswer(call -> occupancy.get());
        doAnswer(call -> { listener.set(call.getArgument(0)); return null; }).when(memory).addMemoryListener(any());

        try (DebuggerSession session = new DebuggerSession(cpu, memory, null)) {
            assertEquals(0, memory.getSize());
            assertEquals(Integer.MAX_VALUE, session.addressSpaceSize());
            int address = Integer.MAX_VALUE - 1;
            session.annotationEngine.put(new net.emustudio.emulib.runtime.annotations.MemoryAnnotations.Entry(
                    99, 1, address, Integer.MAX_VALUE, "label", "last cell", null));
            session.setBreakpoint(address, true).join();
            session.annotations().add(new AnnotationIndex.Entry(address, "metadata", "CODE"));
            assertEquals("metadata", session.annotations().get(address).label);
            assertThrows(IllegalArgumentException.class, session::liveMemory);

            occupancy.set(1);
            listener.get().memorySizeChanged();
            assertEquals(Integer.MAX_VALUE, session.addressSpaceSize());
            assertTrue(session.isBreakpointSet(address));
            occupancy.set(0);
            listener.get().memorySizeChanged();
            assertEquals(Integer.MAX_VALUE, session.addressSpaceSize());
            assertEquals("last cell", session.annotationEngine.get(99, 1).text);
            assertEquals(Integer.MAX_VALUE, session.addressSpaceSize());
            verify(memory, never()).read(anyInt());
            verify(memory, never()).read(anyInt());
        }
    }
}
