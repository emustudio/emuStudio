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
package net.emustudio.plugins.compiler.rasp;

import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.plugins.memory.rasp.api.RaspLabel;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryCell;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;

import java.util.*;

public class MemoryStub extends AbstractMemoryContext<RaspMemoryCell> implements RaspMemoryContext {
    private final RaspMemoryCell[] memory = new RaspMemoryCell[1000];
    private final Map<Integer, RaspLabel> labels = new HashMap<>();
    private final List<Integer> inputs = new ArrayList<>();

    @Override
    public RaspMemoryCell read(int address) {
        return memory[address];
    }

    @Override
    public RaspMemoryCell[] read(int address, int count) {
        return Arrays.copyOfRange(memory, address, count);
    }

    @Override
    public void write(int address, RaspMemoryCell value) {
        memory[address] = value;
    }

    @Override
    public void write(int address, RaspMemoryCell[] instructions, int count) {
        System.arraycopy(instructions, 0, this.memory, address, count);
    }

    @Override
    public Class<RaspMemoryCell> getDataType() {
        return RaspMemoryCell.class;
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
    public List<Integer> getInputs() {
        return inputs;
    }

    @Override
    public void setInputs(List<Integer> inputs) {
        this.inputs.addAll(inputs);
    }
}
