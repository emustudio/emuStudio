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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MemoryContextImpl extends AbstractMemoryContext<Integer> implements RaspMemoryContext {
    private final Map<Integer, Integer> memory = new HashMap<>();
    private final Map<Integer, RaspLabel> labels = new HashMap<>();
    private final List<Integer> inputs = new ArrayList<>();
    private final ReadWriteLock rwl = new ReentrantReadWriteLock();

    @Override
    public void clear() {
        writeLock(() -> {
            memory.clear();
            labels.clear();
            inputs.clear();
        });
        notifyMemoryChanged(-1);
        notifyMemorySizeChanged();
    }

    @Override
    public int getSize() {
        return readLock(() -> memory.keySet().stream().max(Comparator.naturalOrder()).orElse(0));
    }

    @Override
    public Integer read(int address) {
        return readLock(() -> memory.getOrDefault(address, 0));
    }

    @Override
    public Integer[] read(int address, int count) {
        List<Integer> copy = new ArrayList<>();
        readLock(() -> {
            for (int i = address; i < address + count; i++) {
                copy.add(memory.getOrDefault(i, 0));
            }
        });
        return copy.toArray(new Integer[0]);
    }

    @Override
    public void write(int address, Integer value) {
        AtomicBoolean sizeChanged = new AtomicBoolean();
        writeLock(() -> {
            sizeChanged.set(!memory.containsKey(address));
            memory.put(address, value);
        });
        if (sizeChanged.get()) {
            notifyMemorySizeChanged();
        }
        notifyMemoryChanged(address);
    }

    @Override
    public void write(int address, Integer[] values, int count) {
        AtomicBoolean sizeChanged = new AtomicBoolean();
        writeLock(() -> {
            for (int i = 0; i < count; i++) {
                sizeChanged.set(sizeChanged.get() || !memory.containsKey(address + i));
                memory.put(address + i, values[i]);
            }
        });
        if (sizeChanged.get()) {
            notifyMemorySizeChanged();
        }
        for (int i = 0; i < count; i++) {
            notifyMemoryChanged(address + i);
        }
    }

    @Override
    public void setLabels(List<RaspLabel> labels) {
        writeLock(() -> {
            this.labels.clear();
            for (RaspLabel label : labels) {
                this.labels.put(label.getAddress(), label);
            }
        });
    }

    @Override
    public Optional<RaspLabel> getLabel(int address) {
        return readLock(() -> Optional.ofNullable(labels.get(address)));
    }

    @Override
    public void setInputs(List<Integer> inputs) {
        rwl.writeLock().lock();
        try {
            this.inputs.clear();
            this.inputs.addAll(inputs);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    @Override
    public RaspMemory getSnapshot() {
        return readLock(() -> new RaspMemory(labels.values(), memory, inputs));
    }

    @SuppressWarnings("unchecked")
    public void deserialize(String filename, Consumer<Integer> setProgramLocation) throws IOException, ClassNotFoundException {
        rwl.writeLock().lock();
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
            rwl.writeLock().unlock();
            notifyMemorySizeChanged();
            notifyMemoryChanged(-1);
        }
    }

    public void destroy() {
        clear();
    }

    private void writeLock(Runnable r) {
        rwl.writeLock().lock();
        try {
            r.run();
        } finally {
            rwl.writeLock().unlock();
        }
    }

    private <T> T readLock(Supplier<T> r) {
        rwl.readLock().lock();
        try {
            return r.get();
        } finally {
            rwl.readLock().unlock();
        }
    }

    private void readLock(Runnable r) {
        rwl.readLock().lock();
        try {
            r.run();
        } finally {
            rwl.readLock().unlock();
        }
    }
}
