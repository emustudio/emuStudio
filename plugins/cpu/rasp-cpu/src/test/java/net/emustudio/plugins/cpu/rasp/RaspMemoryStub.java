package net.emustudio.plugins.cpu.rasp;

import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.plugins.memory.rasp.api.MemoryItem;
import net.emustudio.plugins.memory.rasp.api.RASPMemoryContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RaspMemoryStub extends AbstractMemoryContext<MemoryItem> implements RASPMemoryContext {
    private final List<MemoryItem> memory = new ArrayList<>();
    private final List<Integer> inputs = new ArrayList<>();
    private final Map<Integer, String> labels = new HashMap<>();
    private int programLocation = 0;

    public RaspMemoryStub(List<MemoryItem> memory, List<Integer> inputs, Map<Integer, String> labels) {
        this.memory.addAll(memory);
        this.inputs.addAll(inputs);
        this.labels.putAll(labels);
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
        this.programLocation = programLocation;
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

    @Override
    public MemoryItem read(int memoryPosition) {
        return memory.get(memoryPosition);
    }

    @Override
    public MemoryItem[] readWord(int memoryPosition) {
        return new MemoryItem[] {
            memory.get(memoryPosition),
            memory.get(memoryPosition + 1),
        };
    }

    @Override
    public void write(int memoryPosition, MemoryItem value) {
        memory.set(memoryPosition, value);
    }

    @Override
    public void writeWord(int memoryPosition, MemoryItem[] value) {
        memory.set(memoryPosition, value[0]);
        memory.set(memoryPosition + 1, value[1]);
    }

    @Override
    public Class<MemoryItem> getDataType() {
        return MemoryItem.class;
    }

    @Override
    public void clear() {
        memory.clear();
    }

    @Override
    public int getSize() {
        return memory.size();
    }
}
