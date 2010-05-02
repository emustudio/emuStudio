/**
 * Program.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 * KISS, YAGNI
 * 
 */
package ramc_ram.tree;

//import ram.impl.HEXFileHandler;
import java.util.Vector;

import ramc_ram.compiled.CompiledFileHandler;

public class Program {
    private Vector<Row> list; // zoznam všetkých inštrukcií

    public Program() { 
        list = new Vector<Row>();
    }
    
    public void addRow(Row node) {
    	if (node != null) list.addElement(node);
    }

    public int pass1(int addr_start) throws Exception {
        int curr_addr = addr_start;
        
        for (int i = 0; i < list.size(); i++)
      		curr_addr = list.get(i).pass1(curr_addr);
        return curr_addr;
    }
 
    public void pass2(CompiledFileHandler hex) throws Exception {
        for (int i = 0; i < list.size(); i++)
            list.get(i).pass2(hex);
    }

}
