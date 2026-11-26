/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck;

import net.emustudio.emulib.plugin.cpu.Decoder;
import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.plugin.cpu.InvalidInstructionException;
import net.emustudio.emulib.plugin.debugger.*;
import net.emustudio.emulib.plugin.memory.MemoryContext;

import java.util.*;
import java.util.function.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Machine-local expression and capture adapter. Never exposes machine data to the host.
 */
public final class MachineDebugging implements Debugging {
    private final String machine;
    private final MemoryContext<?> memory;
    private final Decoder disassembler;
    private final PackedCapture packed;
    private final DebuggingSupport support;
    private final AtomicLong observationVersion = new AtomicLong();
    private DecodedInstruction instruction;
    private int address;
    private boolean inStep;

    MachineDebugging(String machine, MemoryContext<?> memory, Decoder disassembler) {
        this.machine = machine;
        this.memory = memory;
        this.disassembler = disassembler;
        packed = new PackedCapture(machine, PackedStep.AccessFormat.WIDE_BYTE);
        support = new DebuggingSupport(this::captureInstruction, packed::before, this::captureAfter, active -> {
        });
    }

    public void register(String name, LongSupplier read) {
        register(name, 8, read);
    }

    void register(String name, int width, LongSupplier read) {
        packed.register(name, width, read);
    }

    void inspect(String name, Supplier<?> read) {
        packed.inspect(name, read);
    }

    public boolean isActive() {
        return support.isActive();
    }

    public boolean requiresAccesses() {
        return support.requiresAccesses();
    }

    public boolean canBatchInstructions() { return support.canBatchInstructions(); }

    long observationVersion() { return observationVersion.get(); }

    public void endBlock(ExecutedBlock block) {
        inStep = false;
        support.endBlock(block);
    }

    @Override
    public AutoCloseable observe(Observer observer, Capture capture) {
        observationVersion.incrementAndGet();
        return support.observe(observer, capture);
    }

    public boolean begin(int pc) {
        address = pc;
        packed.clearAccesses();
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

    @Override
    public Expression compile(String source) {
        return ExpressionParser.parse(source, this::binding, this::function);
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
        return packed.after(instruction, address);
    }
}
