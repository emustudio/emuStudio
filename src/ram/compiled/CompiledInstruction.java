/**
 * CompiledInstruction.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 *  KISS, YAGNI
 */
package ram.compiled;

import java.util.ArrayList;

import RAMmemory.impl.RAMInstruction;

public class CompiledInstruction {
    private RAMInstruction instr;
    private ArrayList<Integer> code;
    
    public CompiledInstruction(RAMInstruction instr) {
    	this.instr = instr;
    	code = new ArrayList<Integer>();
    }
    
    public void addCode(int code) {
        this.code.add(code);
    }
    
    public ArrayList<Integer> getCode() { return code; }
    
    public RAMInstruction getInstr() { return instr; }
}
