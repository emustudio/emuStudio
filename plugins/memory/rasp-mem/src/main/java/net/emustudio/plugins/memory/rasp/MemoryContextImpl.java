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
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.emulib.runtime.helpers.ReadWriteLockSupport;
import net.emustudio.plugins.memory.rasp.api.RaspLabel;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MemoryContextImpl extends AbstractMemoryContext<Integer> implements RaspMemoryContext {
    private final Map<Integer, Integer> memory = new HashMap<>();
    private final Map<Integer, RaspLabel> labels = new HashMap<>();
    private final List<Integer> inputs = new ArrayList<>();
    private final ReadWriteLockSupport rwl = new ReadWriteLockSupport();

    protected MemoryContextImpl(MemoryContextAnnotations annotations) {
        super(annotations);
    }

    @Override
    public void clear() {
        rwl.lockWrite(() -> {
            memory.clear();
            labels.clear();
            inputs.clear();
        });
        notifyMemoryContentChanged(-1);
        notifyMemorySizeChanged();
    }

    @Override
    public int getSize() {
        return rwl.lockRead(() -> memory.keySet().stream().max(Comparator.naturalOrder()).orElse(0));
    }

    @Override
    public Integer read(int address) {
        return rwl.lockRead(() -> memory.getOrDefault(address, 0));
    }

    @Override
    public Integer[] read(int address, int count) {
        List<Integer> copy = new ArrayList<>();
        rwl.lockRead(() -> {
            for (int i = address; i < address + count; i++) {
                copy.add(memory.getOrDefault(i, 0));
            }
        });
        return copy.toArray(new Integer[0]);
    }

    @Override
    public void write(int address, Integer value) {
        AtomicBoolean sizeChanged = new AtomicBoolean();
        rwl.lockWrite(() -> {
            sizeChanged.set(!memory.containsKey(address));
            memory.put(address, value);
        });
        if (sizeChanged.get()) {
            notifyMemorySizeChanged();
        }
        notifyMemoryContentChanged(address);
    }

    @Override
    public void write(int address, Integer[] values, int count) {
        AtomicBoolean sizeChanged = new AtomicBoolean();
        rwl.lockWrite(() -> {
            for (int i = 0; i < count; i++) {
                sizeChanged.set(sizeChanged.get() || !memory.containsKey(address + i));
                memory.put(address + i, values[i]);
            }
        });
        if (sizeChanged.get()) {
            notifyMemorySizeChanged();
        }
        for (int i = 0; i < count; i++) {
            notifyMemoryContentChanged(address + i);
        }
    }

    @Override
    public void setLabels(List<RaspLabel> labels) {
        rwl.lockWrite(() -> {
            this.labels.clear();
            for (RaspLabel label : labels) {
                this.labels.put(label.getAddress(), label);
            }
        });
    }

    @Override
    public Optional<RaspLabel> getLabel(int address) {
        return rwl.lockRead(() -> Optional.ofNullable(labels.get(address)));
    }

    @Override
    public void setInputs(List<Integer> inputs) {
        rwl.lockWrite(() -> {
            this.inputs.clear();
            this.inputs.addAll(inputs);
        });
    }

    @Override
    public RaspMemory getSnapshot() {
        return rwl.lockRead(() -> new RaspMemory(labels.values(), memory, inputs));
    }

    // Keep throws IOException, ClassNotFoundException due to lock sneaky throw
    @SuppressWarnings("unchecked")
    public void deserialize(String filename, Consumer<Integer> setProgramLocation) throws IOException, ClassNotFoundException {
        rwl.lockWrite(() -> {
            try {
                InputStream file = new FileInputStream(filename);
                InputStream buffer = new BufferedInputStream(file);
                ObjectInput input = new ObjectInputStream(buffer);

                labels.clear();
                inputs.clear();
                memory.clear();

                int programLocation = (Integer) input.readObject();
                setProgramLocation.accept(programLocation);

                Map<Integer, String> rawLabels = (Map<Integer, String>) input.readObject();
                for (Map.Entry<Integer, String> rawLabel : rawLabels.entrySet()) {
                    this.labels.put(rawLabel.getKey(), new RaspLabel() {
                        @Override
                        public int getAddress() {
                            return rawLabel.getKey();
                        }

                        @Override
                        public String getLabel() {
                            return rawLabel.getValue();
                        }
                    });
                }

                inputs.addAll((List<Integer>) input.readObject());
                memory.putAll((Map<Integer, Integer>) input.readObject());

                input.close();
            } finally {
                notifyMemorySizeChanged();
                notifyMemoryContentChanged(-1);
            }
        });
    }

    public void destroy() {
        clear();
    }
}
