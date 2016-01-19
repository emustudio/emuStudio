package sk.tuke.emustudio.rasp.cpu.gui;

import emulib.plugins.cpu.AbstractDebugColumn;
import java.util.Objects;
import sk.tuke.emustudio.rasp.memory.RASPMemoryContext;

/**
 * Debug colomn with labels.
 *
 * @author miso
 */
public class LabelDebugColumn extends AbstractDebugColumn {

    private final RASPMemoryContext memory;

    /**
     * Constructor.
     * @param memory memory to read labels from
     */
    public LabelDebugColumn(RASPMemoryContext memory) {
        super("LABEL", String.class, false);
        this.memory = Objects.requireNonNull(memory);
    }

    @Override
    public void setDebugValue(int i, Object o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Get label at given address from the memory.
     * @param position the position in the memory
     * @return label at given address from the memory
     */
    @Override
    public Object getDebugValue(int position) {
        return memory.getLabel(position);
    }

}
