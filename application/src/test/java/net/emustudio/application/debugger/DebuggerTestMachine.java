/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.DecodedInstruction;
import net.emustudio.emulib.plugins.debugger.Debugging;
import net.emustudio.emulib.plugins.debugger.ExpressionParser;
import net.emustudio.emulib.plugins.memory.MemoryContext;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Plugin-owned expression meanings exercised through the real host execution worker. */
final class DebuggerTestMachine {
    final CPU cpu;
    final Debugging debug;
    final MemoryContext<Byte> memory;
    final List<MemoryContext.MemoryListener> memoryListeners = new ArrayList<>();
    final Map<String, Object> values = new ConcurrentHashMap<>(Map.of("A", 0L));
    final AtomicInteger detached = new AtomicInteger();
    final AtomicReference<Debugging.Capture> captureMode = new AtomicReference<>();
    private final AtomicReference<Debugging.Observer> observer = new AtomicReference<>();
    private DebuggerSession session;
    private final AtomicReference<CPU.RunState> result = new AtomicReference<>(CPU.RunState.STATE_RUNNING);
    private CountDownLatch entered;
    private int accessed = -1;
    private boolean wrote;

    DebuggerTestMachine(byte[][] banks, int commonBoundary) throws Exception {
        this(banks, commonBoundary, true);
    }

    @SuppressWarnings("unchecked")
    DebuggerTestMachine(byte[][] banks, int commonBoundary, boolean debugging) throws Exception {
        cpu = debugging ? mock(CPU.class, withSettings().extraInterfaces(Debugging.class)) : mock(CPU.class);
        debug = debugging ? (Debugging) cpu : null;
        memory = mock(MemoryContext.class);
        when(cpu.getTitle()).thenReturn("Test CPU");
        when(cpu.getAddressSpaceSize()).thenReturn(banks[0].length);
        when(cpu.execute()).thenAnswer(call -> {
            entered.countDown();
            while (!Thread.currentThread().isInterrupted()) {
                session.execution().processUpdates();
                Thread.onSpinWait();
            }
            return result.get();
        });
        when(cpu.step()).thenReturn(CPU.RunState.STATE_STOPPED_BREAK);
        if (debug != null) {
            when(debug.compile(anyString())).thenAnswer(call -> ExpressionParser.parse(
                    call.getArgument(0), this::variable, this::function));
            when(debug.observe(any(), any(Debugging.Capture.class))).thenAnswer(call -> {
                Debugging.Observer installed = call.getArgument(0);
                if (!observer.compareAndSet(null, installed)) throw new IllegalStateException("Observer already installed");
                captureMode.set(call.getArgument(1));
                return (AutoCloseable) () -> {
                    if (observer.compareAndSet(installed, null)) {
                        captureMode.set(null);
                        detached.incrementAndGet();
                    }
                };
            });
        }
        when(memory.getCellTypeClass()).thenReturn(Byte.class);
        when(memory.getSize()).thenReturn(banks[0].length);
        when(memory.read(anyInt())).thenAnswer(call -> banks[0][(int) call.getArgument(0)]);
        when(memory.peek(anyInt())).thenAnswer(call -> memory.read(call.getArgument(0)));
        doAnswer(call -> { memoryListeners.add(call.getArgument(0)); return null; })
                .when(memory).addMemoryListener(any());
        doAnswer(call -> { memoryListeners.remove(call.getArgument(0)); return null; })
                .when(memory).removeMemoryListener(any());
    }

    private Supplier<Object> variable(String name) {
        String key = name.toUpperCase(Locale.ROOT);
        if (key.equals("PC") || key.equals("IP")) return () -> (long) cpu.getInstructionLocation();
        if (!values.containsKey(key)) throw new IllegalArgumentException("Unknown name: " + name);
        return () -> values.get(key);
    }

    private Function<List<Object>, Object> function(String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "reg": return args -> args.get(0) instanceof String
                    ? variable((String) args.get(0)).get() : args.get(0);
            case "read": return args -> accessed == ((Number) args.get(0)).intValue() && !wrote;
            case "write": return args -> accessed == ((Number) args.get(0)).intValue() && wrote;
            case "mem": return args -> bytes(args, 1, false);
            case "mem16": return args -> bytes(args, 2, false);
            case "mem16be": return args -> bytes(args, 2, true);
            case "mem32": return args -> bytes(args, 4, false);
            case "mem32be": return args -> bytes(args, 4, true);
            default: throw new IllegalArgumentException("Unknown function: " + name);
        }
    }

    private long bytes(List<Object> arguments, int width, boolean bigEndian) {
        int at = Math.toIntExact(((Number) arguments.get(0)).longValue());
        if (at < 0 || at > memory.getSize() - width) throw new IllegalArgumentException("Outside byte memory");
        long value = 0;
        for (int i = 0; i < width; i++) value |= (memory.peek(at + i) & 255L) << (8 * (bigEndian ? width - i - 1 : i));
        return value;
    }

    void accessed(int address, boolean write) { accessed = address; wrote = write; }

    void observedStep(DecodedInstruction instruction, OptionalInt nextAddress, boolean completed) {
        Debugging.Observer installed = observer.get();
        if (installed == null) throw new AssertionError("No runtime observer installed");
        if (instruction != null) when(cpu.getInstructionLocation()).thenReturn(instruction.address());
        if (installed.beforeStep()) throw new AssertionError("Unexpected pre-execution stop");
        installed.afterStep(completed, Optional.ofNullable(instruction), nextAddress, Optional.empty());
    }

    boolean beforeStep(int address) {
        when(cpu.getInstructionLocation()).thenReturn(address);
        return session.beforeStep();
    }

    void state(CPU.RunState state) {
        if (state == CPU.RunState.STATE_RUNNING) {
            result.set(CPU.RunState.STATE_RUNNING);
            entered = new CountDownLatch(1);
            session.execution().start().join();
            try { if (!entered.await(2, TimeUnit.SECONDS)) throw new AssertionError("CPU did not enter execution"); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
        } else if (session.execution().snapshot().runState() == CPU.RunState.STATE_RUNNING) {
            result.set(state);
            session.execution().pause().join();
        } else if (state == CPU.RunState.STATE_STOPPED_BREAK) {
            session.execution().step().join();
        }
    }

    DebuggerSession session(Path settings) throws IOException { return session(settings, true); }

    DebuggerSession session(Path settings, boolean withMemory) throws IOException {
        session = new DebuggerSession(cpu, withMemory ? memory : null, settings);
        return session;
    }

    void memoryChanged(int address) {
        List.copyOf(memoryListeners).forEach(listener -> listener.memoryContentChanged(address, address));
    }
}
