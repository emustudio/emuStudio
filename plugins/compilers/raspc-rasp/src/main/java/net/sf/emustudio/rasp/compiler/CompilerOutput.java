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

package net.sf.emustudio.rasp.compiler;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.emustudio.rasp.compiler.tree.Input;
import net.sf.emustudio.rasp.compiler.tree.Label;
import net.sf.emustudio.rasp.memory.memoryitems.MemoryItem;
import net.sf.emustudio.rasp.memory.RASPMemoryContext;

/**
 *
 * @author miso
 */
public class CompilerOutput {

    private int programStart;
    private List<Integer> inputs = new ArrayList<>();

    private HashMap<Integer, String> labels = new HashMap<>();
    private HashMap<String, Integer> reversedLabels = new HashMap<>();
    private List<MemoryItem> memoryItems = new ArrayList<>();
    private static CompilerOutput instance = null;

    private CompilerOutput() {

    }

    public static CompilerOutput getInstance() {
        if (instance == null) {
            instance = new CompilerOutput();
        }
        return instance;
    }

    public void addLabel(Label label) {
        labels.put(label.getAddress(), label.getValue());
        reversedLabels.put(label.getValue(), label.getAddress());
    }

    public int getAddressForLabel(String labelValue) throws Exception {
        if (reversedLabels.containsKey(labelValue+":")) {
            return reversedLabels.get(labelValue+":");
        } else {
            throw new Exception("There is no label " + "\"" + labelValue+ "\"");
        }
    }

    public void addMemoryItem(MemoryItem item) {
        memoryItems.add(item);
    }

    public int getProgramStart() {
        return programStart;
    }

    public void setProgramStart(int programStart) {
        this.programStart = programStart;
    }

    public void clear() {
        labels.clear();
        reversedLabels.clear();
        inputs.clear();
        memoryItems.clear();
    }

    public void saveToFile(String outputFile) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream)) {
                objectOutputStream.writeObject(labels);
                objectOutputStream.writeObject(programStart);
                objectOutputStream.writeObject(inputs);
                objectOutputStream.writeObject(memoryItems);
            }
        } catch (FileNotFoundException ex) {
            System.out.println("Error saving to file.");
        } catch (IOException ex) {
            System.out.println("Error saving to file.");
        }
    }

    public void loadIntoMemory(RASPMemoryContext memory) {
        memory.clear();
        
        for (Map.Entry<Integer, String> entry : labels.entrySet()) {
            memory.addLabel(entry.getKey(), entry.getValue());
        }
        int position = programStart;
        memory.setProgramStart(programStart);
        for (MemoryItem item : memoryItems) {
            memory.write(position++, item);
        }

        memory.addInputs(inputs);
    }
    
    public HashMap<String, Integer> getReversedLabels() {
        return reversedLabels;
    }

    public List<Integer> getInputs() {
        return inputs;
    }

    public void addInputs(List<Integer> inputs) {
        if (inputs == null) {
            return;
        }
        this.inputs.addAll(inputs);
    }
}
