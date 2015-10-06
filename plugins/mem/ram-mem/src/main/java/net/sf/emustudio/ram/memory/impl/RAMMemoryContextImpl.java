/*
 * RAMMemoryContextImpl.java
 *
 * Copyright (C) 2009-2013 Peter Jakubčo
 * KISS, YAGNI, DRY
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
package net.sf.emustudio.ram.memory.impl;

import emulib.plugins.memory.AbstractMemoryContext;
import net.sf.emustudio.ram.memory.RAMInstruction;
import net.sf.emustudio.ram.memory.RAMMemoryContext;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RAMMemoryContextImpl extends AbstractMemoryContext<RAMInstruction> implements RAMMemoryContext {
    private final List<RAMInstruction> memory = new ArrayList<>();
    private final Map<Integer, String> labels = new HashMap<>();
    private final List<String> inputs = new ArrayList<>(); // not for memory, but for CPU. Memory holds program so...

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
    public Class<?> getDataType() {
        return RAMInstruction.class;
    }

    @Override
    public int getSize() {
        return memory.size();
    }

    @Override
    public RAMInstruction read(int pos) {
        return memory.get(pos);
    }

    @Override
    public RAMInstruction[] readWord(int pos) {
        return new RAMInstruction[] { memory.get(pos), null } ;
    }

    @Override
    public void write(int pos, RAMInstruction instr) {
        if (pos >= memory.size()) {
            memory.add(pos, instr);
            notifyMemoryChanged(memory.size());
            notifyMemorySizeChanged();
        } else {
            memory.set(pos, instr);
        }
        notifyMemoryChanged(pos);
    }

    // This method is not and won't be implemented.
    @Override
    public void writeWord(int pos, RAMInstruction[] instr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addLabel(int pos, String label) {
        labels.put(pos, label);
    }

    @Override
    public String getLabel(int pos) {
        return labels.get(pos);
    }

    public Map<String, Integer> getSwitchedLabels() {
        Map<String, Integer> h = new HashMap<>();
        for (Map.Entry<Integer, String> entry : labels.entrySet()) {
            h.put(entry.getValue(), entry.getKey());
        }
        return h;
    }

    @Override
    public void addInputs(List<String> inputs) {
        if (inputs == null) {
            return;
        }
        this.inputs.addAll(inputs);
    }

    @Override
    public List<String> getInputs() {
        return inputs;
    }

    public boolean deserialize(String filename) {
        try {
            InputStream file = new FileInputStream(filename);
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);

            labels.clear();
            inputs.clear();
            memory.clear();

            labels.putAll((Map<Integer, String>)input.readObject());
            inputs.addAll((List<String>)input.readObject());
            memory.addAll((List<RAMInstruction>)input.readObject());

            input.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    public void destroy() {
        memory.clear();
    }

}
