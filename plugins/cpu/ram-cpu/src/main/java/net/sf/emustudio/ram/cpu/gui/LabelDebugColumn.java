package net.sf.emustudio.ram.cpu.gui;

import emulib.plugins.cpu.AbstractDebugColumn;
import net.sf.emustudio.ram.memory.RAMMemoryContext;

import java.util.Objects;

public class LabelDebugColumn extends AbstractDebugColumn {
    private final RAMMemoryContext memory;

    public LabelDebugColumn(RAMMemoryContext memory) {
        super("label", String.class, false);
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public void setDebugValue(int location, Object value) {

    }

    @Override
    public Object getDebugValue(int location) {
        return memory.getLabel(location);
    }
}
