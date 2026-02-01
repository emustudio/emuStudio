/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ram;

import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.api.RamLabel;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import net.emustudio.plugins.memory.ram.api.RamValue;

import java.util.*;

public class MemoryStub extends AbstractMemoryContext<RamInstruction> implements RamMemoryContext {
    private final RamInstruction[] memory = new RamInstruction[1000];
    private final Map<Integer, RamLabel> labels = new HashMap<>();
    private final List<RamValue> inputs = new ArrayList<>();
    private final MemoryContextAnnotations annotations;

    protected MemoryStub(MemoryContextAnnotations annotations) {
        this.annotations = Objects.requireNonNull(annotations);
    }

    @Override
    public RamInstruction read(int address) {
        return memory[address];
    }

    @Override
    public RamInstruction[] read(int address, int count) {
        return Arrays.copyOfRange(memory, address, count);
    }

    @Override
    public void write(int address, RamInstruction value) {
        memory[address] = value;
    }

    @Override
    public void write(int address, RamInstruction[] instructions, int count) {
        System.arraycopy(instructions, 0, this.memory, address, count);
    }

    @Override
    public Class<RamInstruction> getCellTypeClass() {
        return RamInstruction.class;
    }

    @Override
    public void clear() {
        Arrays.fill(memory, null);
        labels.clear();
        inputs.clear();
    }

    @Override
    public int getSize() {
        return memory.length;
    }

    @Override
    public MemoryContextAnnotations annotations() {
        return annotations;
    }

    @Override
    public void setLabels(List<RamLabel> labels) {
        this.labels.clear();
        for (RamLabel label : labels) {
            this.labels.put(label.getAddress(), label);
        }
    }

    @Override
    public Optional<RamLabel> getLabel(int address) {
        return Optional.ofNullable(labels.get(address));
    }

    @Override
    public void setInputs(List<RamValue> inputs) {
        this.inputs.addAll(inputs);
    }

    @Override
    public RamMemory getSnapshot() {
        return null;
    }
}
