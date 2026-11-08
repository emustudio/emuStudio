/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.debug.DebugCPU;
import net.emustudio.emulib.plugins.memory.MemoryContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** In-memory CPU ports exercised through the real host execution module. */
final class DebuggerTestMachine {
    final CPU cpu = mock(CPU.class);
    final DebugCPU debug = mock(DebugCPU.class);
    final MemoryContext<Byte> memory;
    final List<MemoryContext.MemoryListener> memoryListeners = new ArrayList<>();
    private DebugSession session;
    private final AtomicReference<CPU.RunState> result = new AtomicReference<>(CPU.RunState.STATE_RUNNING);
    private CountDownLatch entered;

    @SuppressWarnings("unchecked")
    DebuggerTestMachine(byte[][] banks, int commonBoundary) throws Exception {
        memory = mock(MemoryContext.class);
        when(cpu.getTitle()).thenReturn("Test CPU");
        when(cpu.getDebugger()).thenReturn(Optional.of(debug));
        when(cpu.getAddressSpaceSize()).thenReturn(banks[0].length);
        when(cpu.execute()).thenAnswer(call -> {
            CountDownLatch wait = new CountDownLatch(1);
            entered.countDown();
            try { wait.await(); }
            catch (InterruptedException stopped) { Thread.currentThread().interrupt(); }
            return result.get();
        });
        when(cpu.step()).thenReturn(CPU.RunState.STATE_STOPPED_BREAK);
        doReturn(memory).when(debug).memory();
        when(debug.processor()).thenReturn("test-processor");
        when(debug.registers()).thenReturn(List.of(new DebugCPU.Register("A", 8)));
        when(memory.getCellTypeClass()).thenReturn(Byte.class);
        when(memory.getSize()).thenReturn(banks[0].length);
        when(memory.read(anyInt())).thenAnswer(call -> {
            int address = call.getArgument(0);
            return banks[0][address];
        });
        when(memory.peek(anyInt())).thenAnswer(call -> memory.read(call.getArgument(0)));
        doAnswer(call -> { memoryListeners.add(call.getArgument(0)); return null; })
                .when(memory).addMemoryListener(any());
        doAnswer(call -> { memoryListeners.remove(call.getArgument(0)); return null; })
                .when(memory).removeMemoryListener(any());
    }

    void state(CPU.RunState state) {
        if (state == CPU.RunState.STATE_RUNNING) {
            result.set(CPU.RunState.STATE_RUNNING);
            entered = new CountDownLatch(1);
            session.execution().start().join();
            try { if (!entered.await(2, TimeUnit.SECONDS)) throw new AssertionError("CPU did not enter execution"); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
        } else if (session.execution().runState() == CPU.RunState.STATE_RUNNING) {
            result.set(state);
            session.execution().pause().join();
        } else if (state == CPU.RunState.STATE_STOPPED_BREAK) {
            session.execution().step().join();
        }
    }

    DebugSession session(Path settings) throws IOException {
        session = new DebugSession(cpu, memory, settings);
        return session;
    }

    void memoryChanged(int address) {
        List.copyOf(memoryListeners).forEach(listener -> listener.memoryContentChanged(address, address));
    }
}
