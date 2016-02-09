package sk.tuke.emustudio.rasp.memory;

import emulib.annotations.ContextType;
import emulib.plugins.memory.MemoryContext;

/**
 * Context of the RASP memory.
 *
 * @author miso
 */
@ContextType
public interface RASPMemoryContext extends MemoryContext<MemoryItem> {

    /**
     * Adds label to memory's set of labels.
     *
     * @param pos adress which label refers to
     * @param label the string reprezentation of the label
     */
    public void addLabel(int pos, String label);

    /**
     * Returns string reprezentation of the label at given address.
     *
     * @param pos the memory address
     * @return string reprezentation of the label at given address
     */
    public String getLabel(int pos);

}
