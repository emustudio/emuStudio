/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
