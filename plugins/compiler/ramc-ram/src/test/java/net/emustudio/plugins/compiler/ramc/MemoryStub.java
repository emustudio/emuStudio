/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.compiler.ramc;

import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.plugins.memory.ram.api.RAMInstruction;
import net.emustudio.plugins.memory.ram.api.RAMMemoryContext;

import java.util.*;

public class MemoryStub extends AbstractMemoryContext<RAMInstruction> implements RAMMemoryContext {
    private final RAMInstruction[] memory = new RAMInstruction[1000];
    private final Map<Integer, String> labels = new HashMap<>();
    private final List<String> inputs = new ArrayList<>();

    @Override
    public RAMInstruction read(int memoryPosition) {
        return memory[memoryPosition];
    }

    @Override
    public RAMInstruction[] readWord(int memoryPosition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(int memoryPosition, RAMInstruction value) {
        memory[memoryPosition] = value;
    }

    @Override
    public void writeWord(int memoryPosition, RAMInstruction[] value) {
        throw new UnsupportedOperationException();
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
    public void addLabel(int pos, String label) {
        labels.put(pos, label);
    }

    @Override
    public String getLabel(int pos) {
        return labels.get(pos);
    }

    @Override
    public void addInputs(List<String> inputs) {
        this.inputs.addAll(inputs);
    }

    @Override
    public List<String> getInputs() {
        return inputs;
    }
}
