/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp;

import net.emustudio.emulib.plugin.cpu.Decoder;
import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.plugin.cpu.InvalidInstructionException;
import net.emustudio.emulib.plugin.debugger.*;

import java.util.*;
import java.util.function.*;

/** Machine-local expression and capture adapter. Never exposes machine data to the host. */
public final class MachineDebugging implements Debugging {
    private final String machine;
    private final Decoder disassembler;
    private final Map<String, Supplier<Object>> bindings = new LinkedHashMap<>();
    private final List<Access> accesses = new ArrayList<>();
    private final DebuggingSupport support;
    private BiFunction<String, List<Object>, Object> machineFunction;
    private Set<String> machineFunctions = Set.of();
    private Map<String, Object> before;
    private String mnemonic = "", opcodes = "";
    private DecodedInstruction instruction;
    private int address;
    private boolean inStep;

    MachineDebugging(String machine, Decoder disassembler) {
        this.machine = machine;
        this.disassembler = disassembler;
        support = new DebuggingSupport(this::captureInstruction, this::captureBefore, this::captureAfter, active -> {});
    }

    public void register(String name, LongSupplier read) {
        bindings.put(name, () -> read.getAsLong());
    }

    void inspect(String name, Supplier<?> read) { bindings.put(name, read::get); }

    void functions(Set<String> names, BiFunction<String, List<Object>, Object> functions) { machineFunctions = Set.copyOf(names); machineFunction = functions; }
    public boolean isActive() { return support.isActive(); }
    public boolean requiresAccesses() { return support.requiresAccesses(); }

    @Override public AutoCloseable observe(Observer observer, Capture capture) {
        return support.observe(observer, capture);
    }

    public boolean begin(int pc) {
        address = pc;
        accesses.clear();
        inStep = support.begin();
        return inStep;
    }

    public boolean end(boolean completed, int nextAddress) {
        inStep = false;
        return support.end(completed, Optional.ofNullable(instruction), OptionalInt.of(nextAddress));
    }

    public void access(int at, int value, boolean write) {
        if (inStep && support.requiresAccesses()) accesses.add(new Access(at, value, write));
    }

    @Override public Expression compile(String source) {
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
        if (fn.equals("read") || fn.equals("write")) return args -> {
            int at = location(args);
            return accesses.stream().anyMatch(a -> a.address == at && a.write == fn.equals("write"));
        };
        throw new IllegalArgumentException("Unknown " + machine + " function: " + name);
    }

    static int location(List<Object> args) {
        if (args.size() != 1 || !(args.get(0) instanceof Number)) throw new IllegalArgumentException("Expected one numeric location");
        long value = ((Number) args.get(0)).longValue();
        if (value < 0 || value > Integer.MAX_VALUE) throw new IllegalArgumentException("Location outside machine");
        return (int) value;
    }

    private Supplier<Object> binding(String name) {
        Supplier<Object> binding = bindings.get(name.toUpperCase(Locale.ROOT));
        if (binding == null) throw new IllegalArgumentException("Unknown " + machine + " value: " + name);
        return binding;
    }

    private void captureInstruction() {
        instruction = null;
        try {
            instruction = disassembler.decode(address);
            mnemonic = instruction.mnemonic();
            opcodes = instruction.encoding();
        } catch (InvalidInstructionException | RuntimeException failure) {
            mnemonic = "<invalid instruction>";
            opcodes = "<unavailable>";
        }
    }

    private void captureBefore() {
        before = new LinkedHashMap<>();
        bindings.forEach((name, binding) -> before.put(name, binding.get()));
    }

    private StepView captureAfter() {
        List<ViewRow> rows = new ArrayList<>();
        rows.add(new ViewRow("Instruction", "Encoding", "", opcodes, false, List.of(), List.of()));
        bindings.forEach((name, binding) -> {
            Object value = binding.get();
            rows.add(new ViewRow("State", name, String.valueOf(before.get(name)), String.valueOf(value),
                    !Objects.equals(before.get(name), value), List.of(), List.of()));
        });
        for (Access access : accesses) {
            String tag = machine + "/write/" + access.address;
            rows.add(new ViewRow("Memory", (access.write ? "Write @" : "Read @") + String.format(Locale.ROOT, "%04d", access.address), "", String.valueOf(access.value),
                    access.write, access.write ? List.of(tag) : List.of(), access.write ? List.of() : List.of(new NavLink("Find earlier write", tag))));
        }
        return new StepView(mnemonic, machine + ":" + mnemonic + ":" + opcodes, OptionalInt.of(address), rows);
    }

    private static final class Access {
        final int address, value;
        final boolean write;
        Access(int address, int value, boolean write) { this.address = address; this.value = value; this.write = write; }
    }
}
