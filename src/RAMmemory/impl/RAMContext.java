/***
 * RAMContext.java
 * 
 *  KISS, YAGNI
 *
 * Copyright (C) 2009-2011 Peter Jakubčo <pjakubco at gmail.com>
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
package RAMmemory.impl;

import interfaces.C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E;
import interfaces.C8E258161A30C508D5E8ED07CE943EEF7408CA508;

import java.util.HashMap;
import java.util.ArrayList;

import emuLib8.plugins.memory.SimpleMemoryContext;
import java.util.Iterator;

public class RAMContext extends SimpleMemoryContext implements C8E258161A30C508D5E8ED07CE943EEF7408CA508 {

    private ArrayList<C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E> memory;
    private HashMap<Integer, String> labels;
    private ArrayList<String> inputs; // not for memory, but for CPU. Memory holds program so...

    public RAMContext() {
        super();
        memory = new ArrayList<C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E>();
        labels = new HashMap<Integer, String>();
        inputs = new ArrayList<String>();
    }

    @Override
    public String getID() {
        return "ram-memory-context";
    }

    @Override
    public void clearMemory() {
        memory.clear();
        labels.clear();
        inputs.clear();
        fireChange(-1);
    }

    public void clearInputs() {
        inputs.clear();
    }

    @Override
    public Class<?> getDataType() {
        return C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.class;
    }

    public int getSize() {
        return memory.size();
    }

    @Override
    public Object read(int pos) {
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
    public void write(int pos, Object instr) {
        if (pos >= memory.size()) {
            memory.add(pos, (C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E) instr);
        } else {
            memory.set(pos, (C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E) instr);
        }
        fireChange(pos);
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

    public HashMap<String, Integer> getSwitchedLabels() {
        HashMap<String, Integer> h = new HashMap<String, Integer>();
        Iterator<Integer> k = labels.keySet().iterator();
        while (k.hasNext()) {
            int pos = k.next();
            h.put(labels.get(pos), pos);
        }
        return h;
    }

    @Override
    public void addInputs(ArrayList<String> inputs) {
        if (inputs == null) {
            return;
        }
        this.inputs.addAll(inputs);
    }

    @Override
    public ArrayList<String> getInputs() {
        return inputs;
    }

    public void destroy() {
        memory.clear();
        memory = null;
    }

}
