/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp;

import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.plugins.memory.rasp.api.RaspLabel;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;

import java.util.*;

public class MemoryStub extends AbstractMemoryContext<Integer> implements RaspMemoryContext {
    private final Integer[] memory = new Integer[1000];
    private final Map<Integer, RaspLabel> labels = new HashMap<>();
    private final List<Integer> inputs = new ArrayList<>();
    private final MemoryContextAnnotations annotations;

    protected MemoryStub(MemoryContextAnnotations annotations) {
        this.annotations = Objects.requireNonNull(annotations);
    }

    @Override
    public Integer read(int address) {
        return memory[address];
    }

    @Override
    public Integer[] read(int address, int count) {
        return Arrays.copyOfRange(memory, address, count);
    }

    @Override
    public void write(int address, Integer value) {
        memory[address] = value;
    }

    @Override
    public void write(int address, Integer[] instructions, int count) {
        System.arraycopy(instructions, 0, this.memory, address, count);
    }

    @Override
    public Class<Integer> getCellTypeClass() {
        return Integer.class;
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
    public void setLabels(List<RaspLabel> labels) {
        this.labels.clear();
        for (RaspLabel label : labels) {
            this.labels.put(label.getAddress(), label);
        }
    }

    @Override
    public Optional<RaspLabel> getLabel(int address) {
        return Optional.ofNullable(labels.get(address));
    }

    @Override
    public void setInputs(List<Integer> inputs) {
        this.inputs.addAll(inputs);
    }

    @Override
    public RaspMemory getSnapshot() {
        Map<Integer, Integer> programMemory = new HashMap<>();
        for (int i = 0; i < memory.length; i++) {
            if (memory[i] != null) {
                programMemory.put(i, memory[i]);
            }
        }
        return new RaspMemory(labels.values(), programMemory, inputs);
    }
}
