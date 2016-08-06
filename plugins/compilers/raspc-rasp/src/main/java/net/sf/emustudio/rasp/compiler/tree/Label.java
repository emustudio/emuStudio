/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.emustudio.rasp.compiler.tree;

import net.sf.emustudio.rasp.compiler.CompilerOutput;

/**
 *
 * @author miso
 */
public class Label extends AbstractTreeNode{

    private final String value;
    private int address;

    public int getAddress() {
        return address;
    }

    public String getValue() {
        return value;
    }

    public Label(String value) {
        this.value = value.toUpperCase();
    }

    public void setAddress(int address) {
        this.address = address;
    }
    
    @Override
    public void pass(){
        CompilerOutput.getInstance().addLabel(this);
    }
}
