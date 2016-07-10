/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.ram.compiler.impl;

import emulib.plugins.memory.AbstractMemoryContext;
import net.sf.emustudio.ram.memory.RAMInstruction;
import net.sf.emustudio.ram.memory.RAMMemoryContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public Class<?> getDataType() {
        return RAMInstruction.class;
    }

    @Override
    public void clear() {
        for (int i = 0; i < memory.length; i++) {
            memory[i] = null;
        }
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
