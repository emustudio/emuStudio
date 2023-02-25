/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
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

package net.emustudio.plugins.memory.rasp;

import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.plugins.memory.rasp.api.RaspLabel;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;

import java.io.*;
import java.util.*;

public class MemoryContextImpl extends AbstractMemoryContext<Integer> implements RaspMemoryContext {
    private final Map<Integer, Integer> memory = new HashMap<>();
    private final Map<Integer, RaspLabel> labels = new HashMap<>();
    private final List<Integer> inputs = new ArrayList<>();
    private int programLocation;

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
        return memory.keySet().stream().max(Comparator.naturalOrder()).orElse(0);
    }

    @Override
    public Integer read(int address) {
        Integer cell = memory.get(address);
        return (cell == null) ? 0 : cell;
    }

    @Override
    public Integer[] read(int address, int count) {
        List<Integer> copy = new ArrayList<>();
        for (int i = address; i < address + count; i++) {
            Integer cell = memory.get(i);
            copy.add((cell == null) ? 0 : cell);
        }
        return copy.toArray(new Integer[0]);
    }

    @Override
    public void write(int address, Integer value) {
        boolean sizeChanged = !memory.containsKey(address);
        memory.put(address, value);
        if (sizeChanged) {
            notifyMemorySizeChanged();
        }
        notifyMemoryChanged(address);
    }

    @Override
    public void write(int address, Integer[] values, int count) {
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
    public synchronized void setLabels(List<RaspLabel> labels) {
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
    public List<Integer> getInputs() {
        return Collections.unmodifiableList(inputs);
    }

    @Override
    public synchronized void setInputs(List<Integer> inputs) {
        clearInputs();
        this.inputs.addAll(inputs);
    }

    public int getProgramLocation() {
        return programLocation;
    }

    public void setProgramLocation(int location) {
        this.programLocation = location;
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

            programLocation = (Integer) input.readObject();
            labels.putAll((Map<Integer, RaspLabel>) input.readObject());
            inputs.addAll((List<Integer>) input.readObject());
            memory.putAll((Map<Integer, Integer>) input.readObject());

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
