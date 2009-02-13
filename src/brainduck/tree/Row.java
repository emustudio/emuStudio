/**
 * Row.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 * KISS, YAGNI
 */
package brainduck.tree;

import brainduck.impl.HEXFileHandler;

public class Row {
    private Statement stat;
    
    public Row(Statement stat) {      
        this.stat = stat;
    }

    public int pass1(int addr_start) throws Exception {
        if (stat != null) 
            addr_start = stat.pass1(addr_start);
        return addr_start;
    }
    
    public void pass2(HEXFileHandler hex) throws Exception {
        if (stat != null) 
            stat.pass2(hex);
    }
    
}
