/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

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
    private final Map<String, Supplier<Object>> bindings = new LinkedHashMap<>();
    private final List<Access> accesses = new ArrayList<>();
    private final DebuggingSupport support;
    private Map<String, Object> before;
    private String mnemonic = "", opcodes = "";
    private DecodedInstruction instruction;
    private int address, depth;
    private boolean hasProgramLocation;

    MachineDebugging(String machine, MemoryContext<?> memory, Decoder disassembler) {
        this.machine = machine;
        this.memory = memory;
        this.disassembler = disassembler;
        support = new DebuggingSupport(this::captureInstruction, this::captureBefore, this::captureAfter, active -> activityChanged.accept(active));
    }

    public void register(String name, LongSupplier read) {
        bindings.put(name, () -> read.getAsLong());
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
        if (depth > 0) {
            depth++;
            return true;
        }
        address = pc;
        accesses.clear();
        if (!support.begin()) return false;
        depth = 1;
        return true;
    }

    public boolean end(boolean completed, int nextAddress) {
        if (depth == 0) return false;
        if (--depth > 0) return false;
        return support.end(completed, Optional.ofNullable(instruction), OptionalInt.of(nextAddress));
    }

    /**
     * IM0 can execute two memory instructions inside one outer engine dispatch.
     */
    boolean nextInstruction(boolean completed, int pc) {
        if (!support.isActive()) return true;
        if (support.end(completed, Optional.ofNullable(instruction), OptionalInt.of(pc))) {
            depth = 0;
            return false;
        }
        address = pc;
        accesses.clear();
        if (!support.begin()) {
            depth = 0;
            return false;
        }
        return true;
    }

    public void access(int at, int value, boolean write) {
        if (depth > 0 && support.requiresAccesses()) accesses.add(new Access(at, value, write));
    }

    public void describeInterrupt(String text, String bytes) {
        mnemonic = text;
        opcodes = bytes;
        hasProgramLocation = false;
        instruction = null;
    }

    public void captureInstruction(int pc, String prefix) {
        if (!support.isActive()) return;
        address = pc;
        hasProgramLocation = true;
        instruction = null;
        try {
            instruction = disassembler.decode(pc);
            mnemonic = prefix + instruction.mnemonic();
            opcodes = instruction.encoding();
        } catch (InvalidInstructionException | RuntimeException failure) {
            mnemonic = prefix + "<invalid instruction>";
            opcodes = "<unavailable>";
        }
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
        Supplier<Object> binding = bindings.get(name.toUpperCase(Locale.ROOT));
        if (binding == null) throw new IllegalArgumentException("Unknown " + machine + " value: " + name);
        return binding;
    }

    private void captureInstruction() {
        captureInstruction(address, "");
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
            rows.add(new ViewRow("State", name, String.valueOf(before.get(name)), String.valueOf(value), !Objects.equals(before.get(name), value), List.of(), List.of()));
        });
        for (Access access : accesses) {
            String tag = machine + "/write/" + access.address;
            rows.add(new ViewRow("Memory", (access.write ? "Write @" : "Read @") + String.format(Locale.ROOT, "%04X", access.address), "", String.valueOf(access.value), access.write, access.write ? List.of(tag) : List.of(), access.write ? List.of() : List.of(new NavLink("Find earlier write", tag))));
        }
        return new StepView(mnemonic, machine + ":" + mnemonic + ":" + opcodes, hasProgramLocation ? OptionalInt.of(address) : OptionalInt.empty(), rows);
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

    private static final class Access {
        final int address, value;
        final boolean write;

        Access(int address, int value, boolean write) {
            this.address = address;
            this.value = value;
            this.write = write;
        }
    }
}
