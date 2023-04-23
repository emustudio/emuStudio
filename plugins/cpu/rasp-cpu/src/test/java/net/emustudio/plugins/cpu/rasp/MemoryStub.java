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

    protected MemoryStub(MemoryContextAnnotations annotations) {
        super(annotations);
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
