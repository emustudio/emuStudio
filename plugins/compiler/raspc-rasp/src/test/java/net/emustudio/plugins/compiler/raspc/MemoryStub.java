package net.emustudio.plugins.compiler.raspc;

import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.plugins.memory.rasp.api.MemoryItem;
import net.emustudio.plugins.memory.rasp.api.RASPMemoryContext;

import java.util.*;

public class MemoryStub extends AbstractMemoryContext<MemoryItem> implements RASPMemoryContext {
    private final MemoryItem[] memory = new MemoryItem[1000];
    private final Map<Integer, String> labels = new HashMap<>();
    private final List<Integer> inputs = new ArrayList<>();

    @Override
    public MemoryItem read(int memoryPosition) {
        return memory[memoryPosition];
    }

    @Override
    public MemoryItem[] readWord(int memoryPosition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(int memoryPosition, MemoryItem value) {
        memory[memoryPosition] = value;
    }

    @Override
    public void writeWord(int memoryPosition, MemoryItem[] value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<MemoryItem> getDataType() {
        return MemoryItem.class;
    }

    @Override
    public void clear() {
        Arrays.fill(memory, null);
        labels.clear();
        inputs.clear();
    }

    @Override
    public int getSize() {
        return memory.length;
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
    public void setProgramLocation(Integer programLocation) {

    }

    @Override
    public String addressToLabelString(int address) {
        return null;
    }

    @Override
    public void addInputs(List<Integer> input) {
        this.inputs.addAll(input);
    }

    @Override
    public List<Integer> getInputs() {
        return inputs;
    }
}
