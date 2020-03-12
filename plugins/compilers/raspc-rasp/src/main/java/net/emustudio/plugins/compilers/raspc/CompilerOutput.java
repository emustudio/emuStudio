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
package net.emustudio.plugins.compilers.raspc;

import net.emustudio.plugins.compilers.raspc.tree.Label;
import net.emustudio.plugins.memory.rasp.api.RASPMemoryContext;
import net.emustudio.plugins.memory.rasp.api.MemoryItem;

import java.io.*;
import java.util.*;

public class CompilerOutput {

    private int programStart;
    private final List<Integer> inputs = new ArrayList<>();

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
        if (reversedLabels.containsKey(labelValue + ":")) {
            return reversedLabels.get(labelValue + ":");
        } else {
            throw new Exception("There is no label " + "\"" + labelValue + "\"");
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
        memory.setProgramLocation(programStart);
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
        Objects.requireNonNull(inputs, "inputs cannot be null");
        this.inputs.addAll(inputs);
    }
}
