/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.memory;


public class NumberMemoryItemImpl implements NumberMemoryItem {

    private final int value;

    public NumberMemoryItemImpl(int value) {
        this.value = value;
    }
    
    @Override
    public int getValue() {
        return value;
    }
    
}
