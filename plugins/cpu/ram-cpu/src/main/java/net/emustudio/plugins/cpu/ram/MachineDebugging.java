/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram;

import net.emustudio.emulib.plugins.cpu.Decoder;
import net.emustudio.emulib.plugins.cpu.DecodedInstruction;
import net.emustudio.emulib.plugins.cpu.InvalidInstructionException;
import net.emustudio.emulib.plugins.debugger.*;

import javax.swing.*;
import java.awt.FlowLayout;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.function.*;

/** Machine-local expression, editing and capture adapter. Never exposes machine data to the host. */
public final class MachineDebugging implements Debugging {
    private final String machine;
    private final Decoder disassembler;
    private final Map<String, Binding> bindings = new LinkedHashMap<>();
    private final List<TapeAccess> accesses = new ArrayList<>();
    private final DebuggingSupport support;
    private BiFunction<String, List<Object>, Object> machineFunction;
    private Set<String> machineFunctions = Set.of();
    private Map<String, Object> before;
    private Runnable boundary = () -> {};
    private String mnemonic = "", opcodes = "";
    private DecodedInstruction instruction;
    private int address;
    private boolean inStep;

    MachineDebugging(String machine, Decoder disassembler) {
        this.machine = machine;
        this.disassembler = disassembler;
        support = new DebuggingSupport(this::captureInstruction, this::captureBefore, this::captureAfter, active -> {});
    }

    public void register(String name, int bits, LongSupplier read, LongConsumer write) {
        bindings.put(name, new Binding(() -> read.getAsLong(), text -> {
            long value = Long.decode(text.trim());
            long mask = -1L >>> (64 - bits);
            if (bits < 64 && (value < 0 || value > mask)) throw new IllegalArgumentException("Value exceeds " + bits + " bits");
            write.accept(value);
        }));
    }

    void symbol(String name, Supplier<?> read, Consumer<String> write) {
        bindings.put(name, new Binding(read, write));
    }

    void inspect(String name, Supplier<?> read) { bindings.put(name, new Binding(read, null)); }

    void functions(Set<String> names, BiFunction<String, List<Object>, Object> functions) { machineFunctions = Set.copyOf(names); machineFunction = functions; }
    public void onBoundary(Runnable callback) { boundary = callback; }
    public void boundary() { boundary.run(); }
    public boolean isActive() { return support.isActive(); }
    public boolean requiresAccesses() { return support.requiresAccesses(); }
    boolean isCapturing() { return support.isCapturing(); }

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

    void tapeAccess(String tape, int at, Object before, Object value, boolean write) {
        if (inStep && support.requiresAccesses()) accesses.add(new TapeAccess(tape, at, before, value, write));
    }

    boolean accessed(String tape, int at, boolean write) {
        return accesses.stream().anyMatch(access -> access.tape.equals(tape) && access.address == at && access.write == write);
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
        throw new IllegalArgumentException("Unknown " + machine + " function: " + name);
    }

    static int location(List<Object> args) {
        if (args.size() != 1 || !(args.get(0) instanceof Number)) throw new IllegalArgumentException("Expected one numeric location");
        long value = ((Number) args.get(0)).longValue();
        if (value < 0 || value > Integer.MAX_VALUE) throw new IllegalArgumentException("Location outside machine");
        return (int) value;
    }

    private Binding binding(String name) {
        Binding binding = bindings.get(name.toUpperCase(Locale.ROOT));
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
        for (TapeAccess access : accesses) {
            String tag = "RAM/" + access.tape + "/write/" + access.address;
            rows.add(new ViewRow("Tape", (access.write ? "Write " : "Read ") + access.tape + "[" + access.address + "]",
                    String.valueOf(access.before), String.valueOf(access.value), access.write && !Objects.equals(access.before, access.value),
                    access.write ? List.of(tag) : List.of(), access.write ? List.of() : List.of(new NavLink("Find earlier write", tag))));
        }
        return new StepView(mnemonic, machine + ":" + mnemonic + ":" + opcodes, OptionalInt.of(address), rows);
    }

    /** Plugin-owned editing controls; successful changes are applied at the next semantic boundary. */
    public JPanel editor(Function<Runnable, ? extends CompletionStage<Void>> update) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        JComboBox<String> values = new JComboBox<>(bindings.entrySet().stream().filter(entry -> entry.getValue().write != null)
                .map(Map.Entry::getKey).toArray(String[]::new));
        JTextField text = new JTextField(12);
        JButton apply = new JButton("Apply");
        JLabel result = new JLabel(" ");
        panel.add(values); panel.add(text); panel.add(apply); panel.add(result);
        apply.addActionListener(event -> {
            String name = (String) values.getSelectedItem();
            String input = text.getText();
            update.apply(() -> binding(name).write.accept(input)).whenComplete((ignored, failure) -> SwingUtilities.invokeLater(() -> {
                result.setText(failure == null ? "Applied" : "Invalid value");
                result.setToolTipText(failure == null ? null : failure.getMessage());
            }));
        });
        return panel;
    }

    private static final class Binding implements Supplier<Object> {
        final Supplier<?> read;
        final Consumer<String> write;
        Binding(Supplier<?> read, Consumer<String> write) { this.read = read; this.write = write; }
        @Override public Object get() { return read.get(); }
    }
    private static final class TapeAccess {
        final String tape;
        final int address;
        final Object before, value;
        final boolean write;
        TapeAccess(String tape, int address, Object before, Object value, boolean write) {
            this.tape = tape; this.address = address; this.before = before; this.value = value; this.write = write;
        }
    }
}
