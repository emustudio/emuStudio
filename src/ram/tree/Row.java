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
    private Statement stat;
	private Label label;
    
    public Row(Statement stat, Label label) {      
        this.stat = stat;
        this.label = label;
        CompilerEnvironment.addLabel(label);
    }

    public Row(Label label) {      
        this.stat = null;
        this.label = label;
        CompilerEnvironment.addLabel(label);
    }
    
    public int pass1(int addr_start) throws Exception {
		if  (label != null) label.pass1(addr_start);
		if (stat != null) 
            addr_start = stat.pass1(addr_start);
        return addr_start;
    }
    
    public void pass2(CompiledFileHandler hex) throws Exception {
        if (stat != null) 
            stat.pass2(hex);
    }
    
}
