/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
 * Copyright (C) 2020  Peter Jakubčo
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
import net.emustudio.plugins.memory.rasp.api.MemoryItem;
import net.emustudio.plugins.memory.rasp.api.RASPInstruction;
import net.emustudio.plugins.memory.rasp.api.RASPMemoryContext;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

public class MemoryContextImpl extends AbstractMemoryContext<MemoryItem> implements RASPMemoryContext {

    private final List<MemoryItem> memory = new ArrayList<>();
    private Integer programLocation;
    private final List<Integer> inputs = new ArrayList<>();

    private final Map<Integer, String> labels = new HashMap<>();

    /**
     * Reads memory item from given address.
     *
     * @param position the memory address
     * @return the item at the given address
     */
    @Override
    public MemoryItem read(int position) {
        if (position >= 0 && position < getSize()) {
            return memory.get(position);
        } else {
            return null;
        }
    }

    /**
     * Write a memory item to the given address.
     *
     * @param position the memory address
     * @param item     the item to write
     */
    @Override
    public void write(int position, MemoryItem item) {
        if (position >= memory.size()) {
            //padd with nulls to prevent IndexOutOfBoundsException
            for (int i = memory.size(); i < position; i++) {
                memory.add(i, new NumberMemoryItem(0));
            }
            memory.add(position, item);
            notifyMemoryChanged(memory.size());
            notifyMemorySizeChanged();
        } else {
            //if there is an instruction at "position", modify its opcode
            MemoryItem currentValue = read(position);
            if (currentValue instanceof RASPInstruction) {
                int number = ((NumberMemoryItem) item).getValue();
                memory.set(position, new InstructionImpl(number));
            } //if there is not an instruction, i.e. its a NumberMemoryItem or null
            else {
                memory.set(position, item);
            }
        }
        notifyMemoryChanged(position);
    }

    @Override
    public MemoryItem[] readWord(int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeWord(int i, MemoryItem[] cts) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Class<MemoryItem> getDataType() {
        return MemoryItem.class;
    }

    /**
     * Clears memory as well as labels.
     */
    @Override
    public void clear() {
        inputs.clear();
        memory.clear();
        labels.clear();
        notifyMemoryChanged(-1);
        notifyMemorySizeChanged();
    }

    @Override
    public int getSize() {
        return memory.size();
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
    public String addressToLabelString(int address) {
        String label = getLabel(address);
        if (label != null) {
            int index = label.lastIndexOf(':');
            label = label.substring(0, index);
            return label.toLowerCase();
        } else {
            //if no label at the address, simply return the number
            return String.valueOf(address);
        }
    }

    @Override
    public void addInputs(List<Integer> inputs) {
        Objects.requireNonNull(inputs, "inputs cannot be null");
        this.inputs.addAll(inputs);
    }

    @Override
    public List<Integer> getInputs() {
        return inputs;
    }

    /**
     * Destroys the memory content.
     */
    public void destroy() {
        memory.clear();
    }

    public int getProgramLocation() {
        return programLocation;
    }

    @Override
    public void setProgramLocation(Integer programLocation) {
        this.programLocation = programLocation;
    }

    public void loadFromFile(String filename) throws IOException, ClassNotFoundException {
        try {
            FileInputStream fileInputStream = new FileInputStream(filename);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            try (ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream)) {
                //clear labels and memory before loading
                labels.clear();
                memory.clear();
                inputs.clear();

                labels.putAll((Map<Integer, String>) objectInputStream.readObject());
                programLocation = (Integer) objectInputStream.readObject();
                inputs.addAll((List<Integer>) objectInputStream.readObject());
                //load program from file
                List<MemoryItem> program = (List<MemoryItem>) objectInputStream.readObject();
                int position = programLocation;
                //write all the program, beginning at programStart
                for (MemoryItem item : program) {
                    write(position++, item);
                }
            }
        } finally {
            /*any number can be put in this method, handling of the notification
             is just updating the whole table
             */
            notifyMemoryChanged(0);
            notifyMemorySizeChanged();
        }
    }
}
