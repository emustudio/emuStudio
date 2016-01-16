/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.memory;

/**
 * Value as a memory item, either value of register, or instruction operand.
 *
 * @author miso
 */
public class ValueMemoryItem implements MemoryItem {

    /**
     * This value will be Integer if it serves as register operand, e.g. ADD 3,
     * value will be an Integer with value 3. Otherwise, this value will be
     * String.
     */
    private final Object value;

    /**
     * Constructor.
     *
     * @param value the value
     */
    public ValueMemoryItem(Object value) {
        this.value = value;
    }

    /**
     * Get the string representation ot the value.
     *
     * @return string representation ot the value
     */
    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

}
