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
package net.emustudio.plugins.memory.ram;

import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.plugins.memory.ram.api.RAMInstruction;
import net.emustudio.plugins.memory.ram.api.RAMLabel;
import net.emustudio.plugins.memory.ram.api.RAMMemoryContext;
import net.emustudio.plugins.memory.ram.api.RAMValue;

import java.io.*;
import java.util.*;

public class MemoryContextImpl extends AbstractMemoryContext<RAMInstruction> implements RAMMemoryContext {
    private final Map<Integer, RAMInstruction> memory = new HashMap<>();
    private final Map<Integer, RAMLabel> labels = new HashMap<>();
    private final List<RAMValue> inputs = new ArrayList<>();

    @Override
    public void clear() {
        memory.clear();
        labels.clear();
        inputs.clear();
        notifyMemoryChanged(-1);
        notifyMemorySizeChanged();
    }

    public void clearInputs() {
        inputs.clear();
    }

    @Override
    public int getSize() {
        return memory.size();
    }

    @Override
    public RAMInstruction read(int address) {
        return memory.get(address);
    }

    @Override
    public RAMInstruction[] read(int address, int count) {
        List<RAMInstruction> copy = new ArrayList<>();
        for (int i = address; i < address + count; i++) {
            copy.add(memory.get(i));
        }
        return copy.toArray(new RAMInstruction[0]);
    }

    @Override
    public void write(int address, RAMInstruction value) {
        memory.put(address, value);
        notifyMemoryChanged(address);
    }

    @Override
    public void write(int address, RAMInstruction[] values, int count) {
        for (int i = 0; i < count; i++) {
            memory.put(address + i, values[i]);
            notifyMemoryChanged(address + i);
        }
    }

    @Override
    public Class<RAMInstruction> getDataType() {
        return RAMInstruction.class;
    }

    @Override
    public synchronized void setLabels(List<RAMLabel> labels) {
        this.labels.clear();
        for (RAMLabel label: labels) {
            this.labels.put(label.getAddress(), label);
        }
    }

    @Override
    public Optional<RAMLabel> getLabel(int address) {
        return Optional.ofNullable(labels.get(address));
    }

    @Override
    public synchronized void setInputs(List<RAMValue> inputs) {
        clearInputs();
        this.inputs.addAll(inputs);
    }

    @Override
    public List<RAMValue> getInputs() {
        return Collections.unmodifiableList(inputs);
    }

    @SuppressWarnings("unchecked")
    public void deserialize(String filename) throws IOException, ClassNotFoundException {
        try {
            InputStream file = new FileInputStream(filename);
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);

            labels.clear();
            inputs.clear();
            memory.clear();

            labels.putAll((Map<Integer, RAMLabel>) input.readObject());
            inputs.addAll((List<RAMValue>) input.readObject());
            memory.putAll((Map<Integer, RAMInstruction>) input.readObject());

            input.close();
        } finally {
            notifyMemoryChanged(-1);
            notifyMemorySizeChanged();
        }
    }

    public void destroy() {
        clear();
    }
}
