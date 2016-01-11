package sk.tuke.emustudio.rasp.memory;

/**
 * Number as memory item, either operand of an instruction, or a data register
 * value.
 *
 * @author miso
 */
public interface NumberMemoryItem extends MemoryItem {

    /**
     * Get value of the number memory item.
     *
     * @return value of the number memory item
     */
    public int getValue();
}
