package sk.tuke.emustudio.rasp.memory.impl;

import emulib.plugins.memory.AbstractMemoryContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sk.tuke.emustudio.rasp.memory.RASPInstruction;
import sk.tuke.emustudio.rasp.memory.RASPMemoryContext;

public class RASPMemoryContextImpl extends AbstractMemoryContext<RASPInstruction> implements RASPMemoryContext {

    private final List<RASPInstruction> memory = new ArrayList();
    private final Map<Integer, String> labels = new HashMap();
    private final List<String> inputs = new ArrayList<>();

    @Override
    public RASPInstruction read(int position) {
        return memory.get(position);
    }

    @Override
    public void write(int position, RASPInstruction instruction) {
        if (position >= memory.size()) {
            memory.add(position, instruction);
            notifyMemoryChanged(memory.size());
            notifyMemorySizeChanged();
        } else {
            memory.set(position, instruction);
        }
        notifyMemoryChanged(position);
    }

    @Override
    public RASPInstruction[] readWord(int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeWord(int i, RASPInstruction[] cts) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Class<?> getDataType() {
        return RASPInstruction.class;
    }

    @Override
    public void clear() {
        memory.clear();
        inputs.clear();
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
    public void destroy() {
        memory.clear();
    }

}
