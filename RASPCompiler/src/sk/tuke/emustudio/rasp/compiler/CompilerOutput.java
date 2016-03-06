/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.compiler;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import sk.tuke.emustudio.rasp.compiler.tree.Label;
import sk.tuke.emustudio.rasp.memory.MemoryItem;

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

    public int getAddressForLabel(String labelValue) {
        if(reversedLabels.containsKey(labelValue)){
            return reversedLabels.get(labelValue);
        }else {
            throw new RuntimeException("NO MAPPING for "+labelValue);
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

    private void clear() {
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
            clear();
        } catch (FileNotFoundException ex) {
            System.out.println("Error saving to file.");
        } catch (IOException ex) {
            System.out.println("Error saving to file.");
        }
    }

    public HashMap<String, Integer> getReversedLabels() {
        return reversedLabels;
    }
    
   
}
