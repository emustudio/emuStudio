package sk.tuke.emustudio.rasp.memory.memoryitems;

/**
 * Value as a memory item, either value of register, or instruction operand.
 *
 * @author miso
 */
public class NumberMemoryItem implements MemoryItem {

    private final int value;

    /**
     * Constructor.
     *
     * @param value the value
     */
    public NumberMemoryItem(int value) {
        this.value = value;
    }

    /**
     * Get the value.
     *
     * @return the value
     */
    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

}
