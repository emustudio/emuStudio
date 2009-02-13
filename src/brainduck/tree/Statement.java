/**
 * Statement.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 * KISS, YAGNI
 */
package brainduck.tree;

import brainduck.impl.HEXFileHandler;

public class Statement {
	public final static int INC = 1;
	public final static int DEC = 2;
	public final static int INCV = 3;
	public final static int DECV = 4;
	public final static int PRINT = 5;
	public final static int LOAD = 6;
	public final static int LOOP = 7;
	public final static int ENDL = 8;
	
	private int instr;
	
	public Statement(int instr) {
		this.instr = instr;
	}

	// prvá fáza vracia nasledujúcu adresu
	// od adresy addr_start
	public int pass1(int addr_start) throws Exception {
		return addr_start +1;
	}
	
	public void pass2(HEXFileHandler hex) {
        hex.putCode(String.format("%1$02X",instr));
	}
}
