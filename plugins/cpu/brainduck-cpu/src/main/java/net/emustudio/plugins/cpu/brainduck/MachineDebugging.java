/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck;

import net.emustudio.emulib.plugin.cpu.Disassembler;
import net.emustudio.emulib.plugin.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugin.cpu.InvalidInstructionException;
import net.emustudio.emulib.plugin.debugger.*;
import net.emustudio.emulib.plugin.memory.MemoryContext;

import javax.swing.*;
import java.awt.FlowLayout;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.function.*;

/** Machine-local expression, editing and capture adapter. Never exposes machine data to the host. */
public final class MachineDebugging implements Debugging {
    private final String machine;
    private final MemoryContext<?> memory;
    private final Disassembler disassembler;
    private final Map<String, Binding> bindings = new LinkedHashMap<>();
    private final List<Access> accesses = new ArrayList<>();
    private final DebuggingSupport support;
    private Map<String, Object> before;
    private Runnable boundary = () -> {};
    private String mnemonic = "", opcodes = "";
    private int address;
    private boolean inStep;

    MachineDebugging(String machine, MemoryContext<?> memory, Disassembler disassembler) {
        this.machine = machine;
        this.memory = memory;
        this.disassembler = disassembler;
        support = new DebuggingSupport(this::captureBefore, this::captureAfter, active -> {});
    }

    public void register(String name, int bits, LongSupplier read, LongConsumer write) {
        bindings.put(name, new Binding(() -> read.getAsLong(), text -> {
            long value = Long.decode(text.trim());
            long mask = -1L >>> (64 - bits);
            if (bits < 64 && (value < 0 || value > mask)) throw new IllegalArgumentException("Value exceeds " + bits + " bits");
            write.accept(value);
        }));
    }

    void inspect(String name, Supplier<?> read) { bindings.put(name, new Binding(read, null)); }

    public void onBoundary(Runnable callback) { boundary = callback; }
    public void boundary() { boundary.run(); }
    public boolean isActive() { return support.isActive(); }

    @Override public AutoCloseable observe(Observer observer, boolean capture) {
        return support.observe(observer, capture);
    }

    public boolean begin(int pc) {
        address = pc;
        accesses.clear();
        inStep = support.begin();
        return inStep;
    }

    public boolean end(boolean completed) {
        inStep = false;
        return support.end(completed);
    }

    public void access(int at, int value, boolean write) {
        if (inStep) accesses.add(new Access(at, value, write));
    }

    @Override public Expression compile(String source) {
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
            return accesses.stream().anyMatch(a -> a.address == at && a.write == fn.equals("write"));
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
            for (int i = 0; i < width; i++) value |= ((Byte) memory.peek(at + i) & 255L) << (8 * (fn.endsWith("be") ? width - i - 1 : i));
            return value;
        };
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

    private void captureBefore() {
        before = new LinkedHashMap<>();
        bindings.forEach((name, binding) -> before.put(name, binding.get()));
        try {
            DisassembledInstruction instruction = disassembler.disassemble(address);
            mnemonic = instruction.mnemo;
            opcodes = instruction.opCode;
        } catch (InvalidInstructionException | RuntimeException failure) {
            mnemonic = "<invalid instruction>";
            opcodes = "<unavailable>";
        }
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
            rows.add(new ViewRow("Memory", (access.write ? "Write @" : "Read @") + String.format(Locale.ROOT, "%04X", access.address), "", String.valueOf(access.value),
                    access.write, access.write ? List.of(tag) : List.of(), access.write ? List.of() : List.of(new NavLink("Find earlier write", tag))));
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
    private static final class Access {
        final int address, value;
        final boolean write;
        Access(int address, int value, boolean write) { this.address = address; this.value = value; this.write = write; }
    }
}
