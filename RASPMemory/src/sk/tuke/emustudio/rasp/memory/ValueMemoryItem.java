/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.memory;

/**
 * Value as a memory item, either value of register, or instruction operand
 *
 * @author miso
 */
public class ValueMemoryItem implements MemoryItem {

    private final String value;

    /**
     * Constructor.
     *
     * @param value the value
     */
    public ValueMemoryItem(String value) {
        this.value = value;
    }

    /**
     * Get the string representation ot the value.
     * @return string representation ot the value
     */
    public String getValue() {
        return value;
    }

}
