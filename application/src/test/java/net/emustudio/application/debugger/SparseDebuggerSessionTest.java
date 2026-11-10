/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import static net.emustudio.application.TestCpu.mockCpu;

import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class SparseDebuggerSessionTest {
    @Test public void blankWordMemoryCanLoadAndClearWithoutReplacingItsSparseAddressDomain() throws Exception {
        CPU cpu = net.emustudio.application.TestCpu.mockCpu(withSettings().extraInterfaces(net.emustudio.emulib.plugin.debugger.Debugging.class));
        when(((net.emustudio.emulib.plugin.debugger.Debugging) cpu).observe(any(),
                any(net.emustudio.emulib.plugin.debugger.Debugging.Capture.class)))
                .thenReturn(() -> {});
        @SuppressWarnings("unchecked") MemoryContext<Integer> memory = mock(MemoryContext.class);
        AtomicInteger occupancy = new AtomicInteger();
        AtomicReference<MemoryContext.MemoryListener> listener = new AtomicReference<>();
        when(cpu.getAddressSpaceSize()).thenReturn(Integer.MAX_VALUE);
        when(memory.getCellTypeClass()).thenReturn(Integer.class);
        when(memory.getSize()).thenAnswer(call -> occupancy.get());
        doAnswer(call -> { listener.set(call.getArgument(0)); return null; }).when(memory).addMemoryListener(any());

        try (DebuggerSession session = new DebuggerSession(cpu, net.emustudio.application.TestCpu.context(cpu), memory, null)) {
            assertEquals(0, memory.getSize());
            assertEquals(Integer.MAX_VALUE, session.addressSpaceSize());
            int address = Integer.MAX_VALUE - 1;
            session.annotationEngine.put(new net.emustudio.emulib.application.annotations.MemoryAnnotations.Entry(
                    99, 1, address, Integer.MAX_VALUE, "label", "last cell", null));
            session.setBreakpoint(address, true).join();
            session.annotations().add(new UserAnnotations.Entry(address, "metadata", "CODE"));
            assertEquals("metadata", session.annotations().get(address).label);

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
