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
import net.emustudio.plugins.memory.ram.api.RAMInstruction;
import net.emustudio.plugins.memory.ram.api.RAMLabel;
import net.emustudio.plugins.memory.ram.api.RAMMemoryContext;
import net.emustudio.plugins.memory.ram.api.RAMValue;

import java.util.*;

public class MemoryStub extends AbstractMemoryContext<RAMInstruction> implements RAMMemoryContext {
    private final RAMInstruction[] memory = new RAMInstruction[1000];
    private final Map<Integer, RAMLabel> labels = new HashMap<>();
    private final List<RAMValue> inputs = new ArrayList<>();

    @Override
    public RAMInstruction read(int address) {
        return memory[address];
    }

    @Override
    public RAMInstruction[] read(int address, int count) {
        return Arrays.copyOfRange(memory, address, count);
    }

    @Override
    public void write(int address, RAMInstruction value) {
        memory[address] = value;
    }

    @Override
    public void write(int address, RAMInstruction[] instructions, int count) {
        System.arraycopy(instructions, 0, this.memory, address, count);
    }

    @Override
    public Class<RAMInstruction> getDataType() {
        return RAMInstruction.class;
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
    public void setLabels(List<RAMLabel> labels) {
        this.labels.clear();
        for (RAMLabel label : labels) {
            this.labels.put(label.getAddress(), label);
        }
    }

    @Override
    public Optional<RAMLabel> getLabel(int address) {
        return Optional.ofNullable(labels.get(address));
    }

    @Override
    public List<RAMValue> getInputs() {
        return inputs;
    }

    @Override
    public void setInputs(List<RAMValue> inputs) {
        this.inputs.addAll(inputs);
    }
}
