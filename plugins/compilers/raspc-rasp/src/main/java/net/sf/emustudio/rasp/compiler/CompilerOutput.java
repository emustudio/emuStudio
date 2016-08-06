/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import sk.tuke.emustudio.rasp.compiler.tree.Label;
import sk.tuke.emustudio.rasp.memory.memoryitems.MemoryItem;
import sk.tuke.emustudio.rasp.memory.RASPMemoryContext;

/**
 *
 * @author miso
 */
public class CompilerOutput {

    private int programStart;

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
        memoryItems.clear();
    }

    public void saveToFile(String outputFile) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream)) {
                objectOutputStream.writeObject(labels);
                objectOutputStream.writeObject(programStart);
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
    }
    
    public HashMap<String, Integer> getReversedLabels() {
        return reversedLabels;
    }

}
