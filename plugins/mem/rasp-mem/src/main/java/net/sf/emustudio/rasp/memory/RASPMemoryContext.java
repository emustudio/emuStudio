package net.sf.emustudio.rasp.memory;

import net.sf.emustudio.rasp.memory.memoryitems.MemoryItem;
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

    void setProgramStart(Integer programStart);

    /**
     * Returns string representation of the label at given address, but if there
     * is no label for given address, just returns string representation of the
     * address.
     *
     * @param address the address
     * @return string representation of the label at given address, if there is
     * any
     */
    public String addressToLabelString(int address);

}
