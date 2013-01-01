/*
 * RAMMemoryContextImpl.java
 * 
 * Copyright (C) 2009-2012 Peter Jakubčo
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.emustudio.ram.memory.RAMInstruction;
import net.sf.emustudio.ram.memory.RAMMemoryContext;

public class RAMMemoryContextImpl extends AbstractMemoryContext<RAMInstruction> implements RAMMemoryContext {
    private List<RAMInstruction> memory;
    private Map<Integer, String> labels;
    private List<String> inputs; // not for memory, but for CPU. Memory holds program so...

    public RAMMemoryContextImpl() {
        super();
        memory = new ArrayList<RAMInstruction>();
        labels = new HashMap<Integer, String>();
        inputs = new ArrayList<String>();
    }

    @Override
    public void clear() {
        memory.clear();
        labels.clear();
        inputs.clear();
        notifyMemoryChanged(-1);
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
        if (pos >= memory.size()) {
            return null;
        }
        return memory.get(pos);
    }

    @Override
    public Object readWord(int pos) {
        if (pos >= memory.size()) {
            return null;
        }
        return memory.get(pos);
    }

    // This method is not and won't be implemented.
    @Override
    public void write(int pos, RAMInstruction instr) {
        if (pos >= memory.size()) {
            memory.add(pos, instr);
        } else {
            memory.set(pos, instr);
        }
        notifyMemoryChanged(pos);
    }

    // This method is not and won't be implemented.
    @Override
    public void writeWord(int pos, Object instr) {
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
        Map<String, Integer> h = new HashMap<String, Integer>();
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

    public void destroy() {
        memory.clear();
        memory = null;
    }

}
