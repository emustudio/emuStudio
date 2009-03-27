/**
 * Row.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 * KISS, YAGNI
 */
package ram.tree;

import ram.compiled.CompiledFileHandler;
import ram.compiled.CompilerEnvironment;

public class Row {
    private RAMInstruction stat;
	private Label label;
    
    public Row(RAMInstruction stat, Label label) {
        this.stat = stat;
        if (label != null) {
        	this.label = label;
        	CompilerEnvironment.addLabel(label);
        }
    }

    public Row(Label label) {      
        this.stat = null;
        if (label != null) {
        	this.label = label;
        	CompilerEnvironment.addLabel(label);
        }
    }
    
    public int pass1(int addr_start) throws Exception {
		if  (label != null) label.pass1(addr_start);
		if (stat != null)
			return addr_start +1;
		else
			return addr_start;
    }
    
    public void pass2(CompiledFileHandler hex) throws Exception {
        if (stat != null) {
        	if (stat.pass2())
        		hex.addCode(stat);
        	else
        		throw new Exception("Undefined label: '" + stat.getOperandLabel() + "'");
        }
    }
    
}
