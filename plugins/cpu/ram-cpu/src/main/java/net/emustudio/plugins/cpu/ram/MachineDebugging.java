/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram;

import net.emustudio.emulib.plugin.cpu.Decoder;
import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.plugin.cpu.InvalidInstructionException;
import net.emustudio.emulib.plugin.debugger.*;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;

import java.util.*;
import java.util.function.*;

/**
 * Machine-local expression and capture adapter. Never exposes machine data to the host.
 */
public final class MachineDebugging implements Debugging {
    private final String machine;
    private final Decoder disassembler;
    private final PackedCapture packed;
    private final DebuggingSupport support;
    private BiFunction<String, List<Object>, Object> machineFunction;
    private Set<String> machineFunctions = Set.of();
    private DecodedInstruction instruction;
    private int address;
    private boolean inStep;

    MachineDebugging(String machine, Decoder disassembler) {
        this.machine = machine;
        this.disassembler = disassembler;
        packed = new PackedCapture(machine, List.of("input", "output", "storage"));
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

    void functions(Set<String> names, BiFunction<String, List<Object>, Object> functions) {
        machineFunctions = Set.copyOf(names);
        machineFunction = functions;
    }

    public boolean isActive() {
        return support.isActive();
    }

    public boolean requiresAccesses() {
        return support.requiresAccesses();
    }

    boolean isCapturing() {
        return support.isCapturing();
    }

    @Override
    public AutoCloseable observe(Observer observer, Capture capture) {
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

    void tapeAccess(String tape, int at, TapeSymbol before, TapeSymbol value, boolean write) {
        if (inStep && support.requiresAccesses()) {
            packed.tapeAccess(tape, at, symbolValue(before), symbolValue(value), write, write && !Objects.equals(before, value));
        }
    }

    private static Object symbolValue(TapeSymbol symbol) {
        return symbol.type == TapeSymbol.Type.NUMBER ? symbol.number : symbol.string;
    }

    boolean accessed(String tape, int at, boolean write) {
        return packed.accessed(tape, at, write);
    }

    @Override
    public Expression compile(String source) {
        return ExpressionParser.parse(source, this::binding, this::function);
    }

    private Function<List<Object>, Object> function(String name) {
        String fn = name.toLowerCase(Locale.ROOT);
        if (machineFunctions.contains(fn)) return args -> machineFunction.apply(fn, args);
        if (fn.equals("reg")) return args -> {
            if (args.size() != 1) throw new IllegalArgumentException("reg expects one value");
            Object value = args.get(0);
            return value instanceof String ? binding((String) value).get() : value;
        };
        throw new IllegalArgumentException("Unknown " + machine + " function: " + name);
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
