/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
