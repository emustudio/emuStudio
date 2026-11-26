/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080;

import net.emustudio.emulib.plugin.cpu.Decoder;
import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.plugin.cpu.InvalidInstructionException;
import net.emustudio.emulib.plugin.debugger.*;
import net.emustudio.emulib.plugin.memory.AbstractMemoryContext;
import net.emustudio.emulib.plugin.memory.MemoryContext;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.*;

/**
 * Machine-local expression and capture adapter. Never exposes machine data to the host.
 */
public final class MachineDebugging implements Debugging {
    private final String machine;
    private final MemoryContext<?> memory;
    private final Decoder disassembler;
    private final PackedCapture packed;
    private final DebuggingSupport support;
    private String interruptSummary, interruptEncoding;
    private DecodedInstruction instruction;
    private int address;
    private boolean inStep;

    MachineDebugging(String machine, MemoryContext<?> memory, Decoder disassembler) {
        this.machine = machine;
        this.memory = memory;
        this.disassembler = disassembler;
        packed = new PackedCapture(machine, PackedStep.AccessFormat.BYTE);
        support = new DebuggingSupport(this::captureInstruction, packed::before, this::captureAfter, active -> activityChanged.accept(active));
    }

    public void register(String name, LongSupplier read) {
        register(name, 8, read);
    }

    void register(String name, int width, LongSupplier read) {
        packed.register(name, width, read);
    }

    void registerPair(String name, String high, String low) {
        packed.registerPair(name, high, low);
    }

    public void onActivityChanged(Consumer<Boolean> callback) {
        activityChanged = callback;
    }

    private Consumer<Boolean> activityChanged = active -> {
    };

    public boolean isActive() {
        return support.isActive();
    }

    public boolean requiresAccesses() {
        return support.requiresAccesses();
    }

    @Override
    public AutoCloseable observe(Observer observer, Capture capture) {
        return support.observe(observer, capture);
    }

    public boolean begin(int pc) {
        address = pc;
        packed.clearAccesses();
        interruptSummary = null;
        inStep = support.begin();
        return inStep;
    }

    public boolean end(boolean completed, int nextAddress) {
        inStep = false;
        return support.end(completed, Optional.ofNullable(instruction), OptionalInt.of(nextAddress));
    }

    public void access(int at, int value, boolean write) {
        if (inStep && support.requiresAccesses()) packed.access(at, value, write);
    }

    public void describeInterrupt(String text, String bytes) {
        instruction = null;
        interruptSummary = text;
        interruptEncoding = bytes;
    }

    @Override
    public Expression compile(String source) {
        return ExpressionParser.parse(source, this::binding, this::function);
    }

    @Override
    public String getExpressionsHelp() {
        return String.join("\n",
                "Intel 8080",
                "",
                "Registers: A, B, C, D, E, H, L, F, BC, DE, HL, PC, SP, INTE.",
                "Example: mem[HL]",
                "",
                "reg(\"name\") returns a named value. Names are case-insensitive.",
                "",
                "mem[address]           Read one byte.",
                "mem16[address]         Read two bytes, little-endian.",
                "mem16be[address]       Read two bytes, big-endian.",
                "mem32[address]         Read four bytes, little-endian.",
                "mem32be[address]       Read four bytes, big-endian.",
                "read[address]          Was this address read by the tracked instruction?",
                "write[address]         Was this address written by the tracked instruction?",
                "",
                "",
                ExpressionParser.syntaxHelp());
    }

    private Function<List<Object>, Object> function(String name) {
        String fn = name.toLowerCase(Locale.ROOT);
        if (fn.equals("reg")) return args -> {
            if (args.size() != 1) throw new IllegalArgumentException("reg expects one value");
            Object value = args.get(0);
            return value instanceof String ? binding((String) value).get() : value;
        };
        if (fn.equals("read") || fn.equals("write")) return args -> {
            int at = location(args);
            return packed.accessed(at, fn.equals("write"));
        };
        if (!Set.of("mem", "mem16", "mem16be", "mem32", "mem32be").contains(fn))
            throw new IllegalArgumentException("Unknown " + machine + " function: " + name);
        if (memory.getCellTypeClass() != Byte.class)
            throw new IllegalArgumentException("Byte memory unavailable for " + machine);
        int width = fn.startsWith("mem16") ? 2 : fn.startsWith("mem32") ? 4 : 1;
        return args -> {
            int at = location(args);
            if (memory.getCellTypeClass() != Byte.class || at < 0 || at > memory.getSize() - width)
                throw new IllegalArgumentException("Memory address outside byte memory");
            long value = 0;
            for (int i = 0; i < width; i++)
                value |= ((Byte) memory.peek(at + i) & 255L) << (8 * (fn.endsWith("be") ? width - i - 1 : i));
            return value;
        };
    }

    static int location(List<Object> args) {
        if (args.size() != 1 || !(args.get(0) instanceof Number))
            throw new IllegalArgumentException("Expected one numeric location");
        long value = ((Number) args.get(0)).longValue();
        if (value < 0 || value > Integer.MAX_VALUE) throw new IllegalArgumentException("Location outside machine");
        return (int) value;
    }

    private Supplier<Object> binding(String name) {
        Supplier<Object> binding = packed.binding(name.toUpperCase(Locale.ROOT));
        if (binding == null) throw new IllegalArgumentException("Unknown " + machine + " value: " + name);
        return binding;
    }

    private void captureInstruction() {
        try {
            instruction = disassembler.decode(address);
        } catch (InvalidInstructionException | RuntimeException failure) {
            instruction = null;
        }
    }

    private StepView captureAfter() {
        return interruptSummary == null ? packed.after(instruction, address)
                : packed.after(address, false, interruptSummary, interruptEncoding);
    }

    public static <T> MemoryContext<T> inspectionMemory(MemoryContext<T> memory) {
        return new AbstractMemoryContext<T>() {
            @Override
            public T read(int at) {
                return memory.peek(at & 0xFFFF);
            }

            @Override
            @SuppressWarnings("unchecked")
            public T[] read(int at, int count) {
                if (count < 0) throw new IndexOutOfBoundsException("Negative count");
                int size = count;
                T[] cells = (T[]) Array.newInstance(memory.getCellTypeClass(), size);
                for (int i = 0; i < size; i++) cells[i] = read(at + i);
                return cells;
            }

            @Override
            public int getSize() {
                return memory.getSize();
            }

            @Override
            public Class<T> getCellTypeClass() {
                return memory.getCellTypeClass();
            }

            @Override
            public void write(int at, T value) {
                throw new UnsupportedOperationException("Read-only inspection");
            }

            @Override
            public void write(int at, T[] value, int count) {
                throw new UnsupportedOperationException("Read-only inspection");
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("Read-only inspection");
            }
        };
    }

    @SuppressWarnings("unchecked")
    public MemoryContext<Byte> instrumentedMemory() {
        MemoryContext<Byte> source = (MemoryContext<Byte>) memory;
        return new MemoryContext<Byte>() {
            @Override
            public Byte read(int at) {
                Byte value = source.read(at);
                access(at, value & 255, false);
                return value;
            }

            @Override
            public Byte peek(int at) {
                return source.peek(at);
            }

            @Override
            public Byte[] read(int at, int count) {
                Byte[] values = source.read(at, count);
                for (int i = 0; i < values.length; i++) access(at + i, values[i] & 255, false);
                return values;
            }

            @Override
            public void write(int at, Byte value) {
                source.write(at, value);
                access(at, value & 255, true);
            }

            @Override
            public void write(int at, Byte[] values, int count) {
                source.write(at, values, count);
                for (int i = 0; i < count; i++) access(at + i, values[i] & 255, true);
            }

            @Override
            public Class<Byte> getCellTypeClass() {
                return source.getCellTypeClass();
            }

            @Override
            public int getSize() {
                return source.getSize();
            }

            @Override
            public void clear() {
                source.clear();
            }

            @Override
            public void addMemoryListener(MemoryListener listener) {
                source.addMemoryListener(listener);
            }

            @Override
            public void removeMemoryListener(MemoryListener listener) {
                source.removeMemoryListener(listener);
            }

            @Override
            public void setMemoryNotificationsEnabled(boolean enabled) {
                source.setMemoryNotificationsEnabled(enabled);
            }

            @Override
            public boolean areMemoryNotificationsEnabled() {
                return source.areMemoryNotificationsEnabled();
            }
        };
    }

}
