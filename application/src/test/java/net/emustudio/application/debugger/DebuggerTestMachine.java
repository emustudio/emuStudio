/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.debug.DebugCPU;
import net.emustudio.emulib.plugins.memory.MemoryContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** In-memory ports with synchronous CPU notifications; no emulator thread is started. */
final class DebuggerTestMachine {
    final CPU cpu = mock(CPU.class);
    final DebugCPU debug = mock(DebugCPU.class);
    final MemoryContext<Byte> memory;
    final List<CPU.CPUListener> cpuListeners = new ArrayList<>();
    final List<MemoryContext.MemoryListener> memoryListeners = new ArrayList<>();
    final Set<Integer> breakpoints = new TreeSet<>();

    @SuppressWarnings("unchecked")
    DebuggerTestMachine(byte[][] banks, int commonBoundary, boolean nativeBreakpoints) {
        memory = mock(MemoryContext.class);
        when(cpu.getTitle()).thenReturn("Test CPU");
        when(cpu.getDebugger()).thenReturn(Optional.of(debug));
        when(cpu.getAddressSpaceSize()).thenReturn(banks[0].length);
        when(cpu.isBreakpointSupported()).thenReturn(nativeBreakpoints);
        when(cpu.getBreakpoints()).thenAnswer(call -> new TreeSet<>(breakpoints));
        when(cpu.isBreakpointSet(anyInt())).thenAnswer(call -> breakpoints.contains(call.getArgument(0)));
        doAnswer(call -> { breakpoints.add(call.getArgument(0)); return null; }).when(cpu).setBreakpoint(anyInt());
        doAnswer(call -> { breakpoints.remove(call.getArgument(0)); return null; }).when(cpu).unsetBreakpoint(anyInt());
        doAnswer(call -> { cpuListeners.add(call.getArgument(0)); return null; }).when(cpu).addCPUListener(any());
        doAnswer(call -> { cpuListeners.remove(call.getArgument(0)); return null; }).when(cpu).removeCPUListener(any());
        doAnswer(call -> { state(CPU.RunState.STATE_STOPPED_BREAK); return null; }).when(cpu).pause();
        doReturn(memory).when(debug).memory();
        when(debug.processor()).thenReturn("test-processor");
        when(debug.registers()).thenReturn(List.of(new DebugCPU.Register("A", 8)));
        when(memory.getCellTypeClass()).thenReturn(Byte.class);
        when(memory.getSize()).thenReturn(banks[0].length);
        when(memory.getBanksCount()).thenReturn(banks.length);
        when(memory.getCommonBoundary()).thenReturn(commonBoundary);
        when(memory.readBank(anyInt(), anyInt())).thenAnswer(call -> {
            int address = call.getArgument(0);
            int bank = call.getArgument(1);
            return banks[address < commonBoundary ? bank : 0][address];
        });
        when(memory.getBank(anyInt())).thenAnswer(call -> 0);
        doAnswer(call -> { memoryListeners.add(call.getArgument(0)); return null; })
                .when(memory).addMemoryListener(any());
        doAnswer(call -> { memoryListeners.remove(call.getArgument(0)); return null; })
                .when(memory).removeMemoryListener(any());
    }

    void state(CPU.RunState state) {
        List.copyOf(cpuListeners).forEach(listener -> listener.runStateChanged(state));
    }

    void memoryChanged(int address) {
        List.copyOf(memoryListeners).forEach(listener -> listener.memoryContentChanged(address, address));
    }
}
