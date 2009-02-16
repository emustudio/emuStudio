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
	public final static int HALT = 0;
	public final static int INC = 1;
	public final static int DEC = 2;
	public final static int INCV = 3;
	public final static int DECV = 4;
	public final static int PRINT = 5;
	public final static int LOAD = 6;
	public final static int LOOP = 7;
	public final static int ENDL = 8;
	
	private int instr;
	private int param;
	
	public Statement(int instr, int param) {
		this.instr = instr;
		this.param = param;
	}

	// prvá fáza vracia nasledujúcu adresu
	// od adresy addr_start
	public int pass1(int addr_start) throws Exception {
		if (instr == LOOP || instr == ENDL)
			return addr_start +1;
		else
			return addr_start+2;
	}
	
	public void pass2(HEXFileHandler hex) {
		if (instr == LOOP || instr == ENDL)
			hex.putCode(String.format("%1$02X",instr));
		else
			hex.putCode(String.format("%1$02X%2$02X",instr,param));
	}
}
