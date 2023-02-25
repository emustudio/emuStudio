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
package net.emustudio.plugins.memory.ram;

import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.api.RamLabel;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import net.emustudio.plugins.memory.ram.api.RamValue;

import java.io.*;
import java.util.*;

public class MemoryContextImpl extends AbstractMemoryContext<RamInstruction> implements RamMemoryContext {
    private final Map<Integer, RamInstruction> memory = new HashMap<>();
    private final Map<Integer, RamLabel> labels = new HashMap<>();
    private final List<RamValue> inputs = new ArrayList<>();

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
    public RamInstruction read(int address) {
        return memory.get(address);
    }

    @Override
    public RamInstruction[] read(int address, int count) {
        List<RamInstruction> copy = new ArrayList<>();
        for (int i = address; i < address + count; i++) {
            copy.add(memory.get(i));
        }
        return copy.toArray(new RamInstruction[0]);
    }

    @Override
    public void write(int address, RamInstruction value) {
        boolean sizeChanged = !memory.containsKey(address);
        memory.put(address, value);
        if (sizeChanged) {
            notifyMemorySizeChanged();
        }
        notifyMemoryChanged(address);
    }

    @Override
    public void write(int address, RamInstruction[] values, int count) {
        for (int i = 0; i < count; i++) {
            boolean sizeChanged = !memory.containsKey(address);
            memory.put(address + i, values[i]);
            if (sizeChanged) {
                notifyMemorySizeChanged();
            }
            notifyMemoryChanged(address + i);
        }
    }

    @Override
    public Class<RamInstruction> getDataType() {
        return RamInstruction.class;
    }

    @Override
    public synchronized void setLabels(List<RamLabel> labels) {
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
    public List<RamValue> getInputs() {
        return Collections.unmodifiableList(inputs);
    }

    @Override
    public synchronized void setInputs(List<RamValue> inputs) {
        clearInputs();
        this.inputs.addAll(inputs);
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

            labels.putAll((Map<Integer, RamLabel>) input.readObject());
            inputs.addAll((List<RamValue>) input.readObject());
            memory.putAll((Map<Integer, RamInstruction>) input.readObject());

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
