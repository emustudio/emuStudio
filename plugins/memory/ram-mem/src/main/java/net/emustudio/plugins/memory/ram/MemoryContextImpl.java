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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class MemoryContextImpl extends AbstractMemoryContext<RamInstruction> implements RamMemoryContext {
    private final Map<Integer, RamInstruction> memory = new HashMap<>();
    private final Map<Integer, RamLabel> labels = new HashMap<>();
    private final List<RamValue> inputs = new ArrayList<>();
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
        return readLock(memory::size);
    }

    @Override
    public RamInstruction read(int address) {
        return readLock(() -> memory.get(address));
    }

    @Override
    public RamInstruction[] read(int address, int count) {
        List<RamInstruction> copy = new ArrayList<>();
        readLock(() -> {
            for (int i = address; i < address + count; i++) {
                copy.add(memory.get(i));
            }
        });
        return copy.toArray(new RamInstruction[0]);
    }

    @Override
    public void write(int address, RamInstruction value) {
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
    public void write(int address, RamInstruction[] values, int count) {
        AtomicBoolean sizeChanged = new AtomicBoolean();
        writeLock(() -> {
            for (int i = 0; i < count; i++) {
                sizeChanged.set(sizeChanged.get() || !memory.containsKey(address));
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
    public void setLabels(List<RamLabel> labels) {
        writeLock(() -> {
            this.labels.clear();
            for (RamLabel label : labels) {
                this.labels.put(label.getAddress(), label);
            }
        });
    }

    @Override
    public Optional<RamLabel> getLabel(int address) {
        return Optional.ofNullable(labels.get(address));
    }

    @Override
    public void setInputs(List<RamValue> inputs) {
        writeLock(() -> {
            this.inputs.clear();
            this.inputs.addAll(inputs);
        });
    }

    @SuppressWarnings("unchecked")
    public void deserialize(String filename) throws IOException, ClassNotFoundException {
        rwl.writeLock().lock();
        try {
            InputStream file = new FileInputStream(filename);
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);

            labels.clear();
            inputs.clear();
            memory.clear();

            Map<Integer, String> rawLabels = (Map<Integer, String>) input.readObject();
            for (Map.Entry<Integer, String> rawLabel : rawLabels.entrySet()) {
                this.labels.put(rawLabel.getKey(), new RamLabel() {
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

            inputs.addAll((List<RamValue>) input.readObject());
            memory.putAll((Map<Integer, RamInstruction>) input.readObject());

            input.close();
        } finally {
            rwl.writeLock().unlock();
            notifyMemoryChanged(-1);
            notifyMemorySizeChanged();
        }
    }

    public void destroy() {
        clear();
    }

    @Override
    public RamMemory getSnapshot() {
        return readLock(() -> new RamMemory(labels.values(), memory, inputs));
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
