/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
import net.emustudio.plugins.memory.rasp.api.RASPLabel;
import net.emustudio.plugins.memory.rasp.api.RASPMemoryCell;
import net.emustudio.plugins.memory.rasp.api.RASPMemoryContext;

import java.util.*;

public class MemoryStub extends AbstractMemoryContext<RASPMemoryCell> implements RASPMemoryContext {
    private final RASPMemoryCell[] memory = new RASPMemoryCell[1000];
    private final Map<Integer, RASPLabel> labels = new HashMap<>();
    private final List<Integer> inputs = new ArrayList<>();

    @Override
    public RASPMemoryCell read(int address) {
        return memory[address];
    }

    @Override
    public RASPMemoryCell[] read(int address, int count) {
        return Arrays.copyOfRange(memory, address, count);
    }

    @Override
    public void write(int address, RASPMemoryCell value) {
        memory[address] = value;
    }

    @Override
    public void write(int address, RASPMemoryCell[] instructions, int count) {
        System.arraycopy(instructions, 0, this.memory, address, count);
    }

    @Override
    public Class<RASPMemoryCell> getDataType() {
        return RASPMemoryCell.class;
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
    public void setLabels(List<RASPLabel> labels) {
        this.labels.clear();
        for (RASPLabel label : labels) {
            this.labels.put(label.getAddress(), label);
        }
    }

    @Override
    public Optional<RASPLabel> getLabel(int address) {
        return Optional.ofNullable(labels.get(address));
    }

    @Override
    public void setInputs(List<Integer> inputs) {
        this.inputs.addAll(inputs);
    }

    @Override
    public List<Integer> getInputs() {
        return inputs;
    }
}
