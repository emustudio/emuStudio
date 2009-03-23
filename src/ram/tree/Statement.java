/**
 * Statement.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 * KISS, YAGNI
 */
package ram.tree;

import RAMmemory.impl.RAMInstruction;
import ram.compiled.CompiledFileHandler;
import ram.compiled.CompiledInstruction;
import ram.compiled.CompilerEnvironment;

public class Statement {
	public final static int LOAD = 1;
	public final static int STORE = 2;
	public final static int READ = 3;
	public final static int WRITE = 4;
	public final static int ADD = 5;
	public final static int SUB = 6;
	public final static int MUL = 7;
	public final static int DIV = 8;
	public final static int JMP = 9;
	public final static int JZ = 10;
	public final static int HALT = 11;
	
	private CompiledInstruction instr;
	private int direction;
	private int operand;
	private String label;
	
	public Statement(int instr, int direction, int operand) {
		String s = "";
		
		if (direction == 1) s = "=";
		else if (direction == 2) s = "*";
		
		s += String.valueOf(operand);
		
		this.instr = new CompiledInstruction(new RAMInstruction(
				instr,s));
		this.instr.addCode(instr);
		this.direction = direction;
		this.operand = operand;
	}
	
	/**
	 * This constructor is only for JMP and JZ instructions.
	 * @param instr   JMP or JZ
	 * @param label   label where to jump (addresses aren't allowed)
	 */
	public Statement(int instr, String label) {
		direction = 0;
		this.instr = new CompiledInstruction(new RAMInstruction(
				instr,label));
		this.instr.addCode(instr);
		this.label = label;
	}

	// prvá fáza vracia nasledujúcu adresu
	// od adresy addr_start
	public int pass1(int addr_start) throws Exception {
		int instr = this.instr.getInstr().getInstr();
		if (instr == JMP || instr == JZ)
			return addr_start+2;
		else if (instr == HALT)
			return addr_start+1;
		else 
			return addr_start+3;
	}
	
	public void pass2(CompiledFileHandler hex) {
		int instr = this.instr.getInstr().getInstr();
		switch(instr) {
		case JMP: case JZ:
			this.instr.addCode(CompilerEnvironment.getLabelAddr(label));
			break;
		case HALT: break;
		default:
			this.instr.addCode(direction);
		    this.instr.addCode(operand);
		}
		hex.addInstruction(this.instr);
	}
}
