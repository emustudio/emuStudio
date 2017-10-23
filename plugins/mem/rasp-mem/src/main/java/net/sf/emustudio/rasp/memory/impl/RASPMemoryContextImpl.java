/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2016, Michal Šipoš
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

package net.sf.emustudio.rasp.memory.impl;

import net.sf.emustudio.rasp.memory.memoryitems.RASPInstructionImpl;
import emulib.plugins.memory.AbstractMemoryContext;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.emustudio.rasp.memory.memoryitems.MemoryItem;
import net.sf.emustudio.rasp.memory.memoryitems.NumberMemoryItem;
import net.sf.emustudio.rasp.memory.memoryitems.RASPInstruction;
import net.sf.emustudio.rasp.memory.RASPMemoryContext;

public class RASPMemoryContextImpl extends AbstractMemoryContext<MemoryItem> implements RASPMemoryContext {

    private final List<MemoryItem> memory = new ArrayList<>();
    private Integer programStart;
    private final List<Integer> inputs = new ArrayList<>();

    private final Map<Integer, String> labels = new HashMap<>();

    private final Map<String, Integer> switchedLabels = new HashMap<>();

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
     * @param item the item to write
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
                memory.set(position, new RASPInstructionImpl(number));
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

    /**
     * Returns the data type of memory items.
     *
     * @return the data type of memory items
     */
    @Override
    public Class<?> getDataType() {
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

    /**
     * Returns number of items in the memory.
     *
     * @return
     */
    @Override
    public int getSize() {
        return memory.size();
    }

    /**
     * Assigns label to particular address.
     *
     * @param pos the memory address
     * @param label the label
     */
    @Override
    public void addLabel(int pos, String label) {
        labels.put(pos, label);
    }

    /**
     * Get label at given address.
     *
     * @param pos the memory address
     * @return the label at given address
     */
    @Override
    public String getLabel(int pos) {
        return labels.get(pos);
    }

    /**
     * Returns string representation of the label at given address, but if there
     * is no label for given address, just returns string representation of the
     * address.
     *
     * @param address the address
     * @return string representation of the label at given address, if there is
     * any
     */
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
        if (inputs == null) {
            return;
        }
        this.inputs.addAll(inputs);
    }

    @Override
    public List<Integer> getInputs() {
        return inputs;
    }

    /**
     * Get switched map of labels; i.e. keys are the string reprezentations of
     * labels, and values are the addresses.
     *
     * @return switched map of labels
     */
    public Map<String, Integer> getSwitchedLabels() {
        if (switchedLabels.isEmpty()) {
            for (Map.Entry<Integer, String> entry : labels.entrySet()) {
                switchedLabels.put(entry.getValue(), entry.getKey());
            }
            return switchedLabels;
        } else {
            return switchedLabels;
        }
    }

    /**
     * Destroys the memory content.
     */
    public void destroy() {
        memory.clear();
    }

    /**
     * Get the address from which the program starts; this method is called by
     * RASPMemoryImpl::getProgramStart()
     *
     * @return
     */
    public int getProgramStart() {
        return programStart;
    }

    @Override
    public void setProgramStart(Integer programStart) {
        this.programStart = programStart;
    }

    /**
     * Loads compiled program to memory from file.
     *
     * @param filename the name of the file
     * @throws FileNotFoundException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public void loadFromFile(String filename) throws FileNotFoundException, IOException, ClassNotFoundException {
        try {
            FileInputStream fileInputStream = new FileInputStream(filename);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            try (ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream)) {
                //clear labels and memory before loading
                labels.clear();
                memory.clear();
                inputs.clear();

                labels.putAll((Map<Integer, String>) objectInputStream.readObject());
                programStart = (Integer) objectInputStream.readObject();
                inputs.addAll((List<Integer>) objectInputStream.readObject());
                //load program from file
                List<MemoryItem> program = (List<MemoryItem>) objectInputStream.readObject();
                int position = programStart;
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
