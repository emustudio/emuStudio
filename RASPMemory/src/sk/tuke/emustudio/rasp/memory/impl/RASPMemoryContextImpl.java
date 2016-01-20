package sk.tuke.emustudio.rasp.memory.impl;

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
import sk.tuke.emustudio.rasp.memory.MemoryItem;
import sk.tuke.emustudio.rasp.memory.RASPMemoryContext;

public class RASPMemoryContextImpl extends AbstractMemoryContext<MemoryItem> implements RASPMemoryContext {

    private final List<MemoryItem> memory = new ArrayList<>();
    private int programStart;

    private final Map<Integer, String> labels = new HashMap<>();

    private final Map<String, Integer> switchedLabels = new HashMap<>();

    /**
     * Get address of the first instruction of the program.
     *
     * @return address of the first instruction of the program
     */
    @Override
    public int getProgramStart() {
        return programStart;
    }

    /**
     * Set address of the first instruction of the program.
     *
     * @param programStart address of the first instruction of the program
     */
    @Override
    public void setProgramStart(int programStart) {
        this.programStart = programStart;
    }

    /**
     * Reads memory item from given address.
     *
     * @param position the memory address
     * @return the item at the given address
     */
    @Override
    public MemoryItem read(int position) {
        return memory.get(position);
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
            memory.add(position, item);
            notifyMemoryChanged(memory.size());
            notifyMemorySizeChanged();
        } else {
            memory.set(position, item);
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
     * Clears the memory content.
     */
    @Override
    public void destroy() {
        memory.clear();
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
            //clear labels and memory before loading
            try (ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream)) {
                //clear labels and memory before loading
                labels.clear();
                memory.clear();

                labels.putAll((HashMap<Integer, String>) objectInputStream.readObject());
                Integer startOfProgram = (Integer) objectInputStream.readObject();
                setProgramStart(startOfProgram);
                //pad the pre-program area with null-s
                for (int i = 0; i < startOfProgram; i++) {
                    memory.add(i, null);
                }
                memory.addAll(startOfProgram, (List<MemoryItem>) objectInputStream.readObject());
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
