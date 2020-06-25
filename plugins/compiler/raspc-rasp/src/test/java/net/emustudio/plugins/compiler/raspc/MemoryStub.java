package net.emustudio.plugins.compiler.raspc;

import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.plugins.memory.rasp.api.MemoryItem;
import net.emustudio.plugins.memory.rasp.api.RASPMemoryContext;

import java.util.*;

public class MemoryStub extends AbstractMemoryContext<MemoryItem> implements RASPMemoryContext {
    private final Map<Integer, MemoryItem> memory = new HashMap<>();
    private final Map<Integer, String> labels = new HashMap<>();
    private final List<Integer> inputs = new ArrayList<>();

    @Override
    public MemoryItem read(int memoryPosition) {
        return memory.get(memoryPosition);
    }

    @Override
    public MemoryItem[] readWord(int memoryPosition) {
        return new MemoryItem[] {
            memory.get(memoryPosition),
            memory.get(memoryPosition + 1)
        };
    }

    @Override
    public void write(int memoryPosition, MemoryItem value) {
        memory.put(memoryPosition, value);
    }

    @Override
    public void writeWord(int memoryPosition, MemoryItem[] value) {
        memory.put(memoryPosition, value[0]);
        memory.put(memoryPosition + 1, value[1]);
    }

    @Override
    public Class<MemoryItem> getDataType() {
        return MemoryItem.class;
    }

    @Override
    public void clear() {
        memory.clear();
        labels.clear();
        inputs.clear();
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
