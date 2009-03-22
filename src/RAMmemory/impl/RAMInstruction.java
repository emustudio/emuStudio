/**
 * RAMInstruction.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 * KISS, YAGNI
 */
package RAMmemory.impl;

public class RAMInstruction {
	public final static int READ = 1;
	public final static int WRITE = 2;
	public final static int LOAD = 3;
	public final static int STORE = 4;
	public final static int ADD = 5;
	public final static int SUB = 6;
	public final static int MUL = 7;
	public final static int DIV = 8;
	public final static int JMP = 9;
	public final static int JZ = 10;
	public final static int HALT = 11;
	
	private int instr;
	private String operand;
	
	public RAMInstruction(int in, String op) {
		instr = in;
		operand = op;
	}
	
	public int getInstr() { return instr; }
	public String getInstrStr() {
		switch(instr) {
			case LOAD:  return "LOAD";
			case STORE: return "STORE";
			case READ:  return "READ";
			case WRITE: return "WRITE";
			case ADD:   return "ADD";
			case SUB:   return "SUB";
			case MUL:   return "MUL";
			case DIV:   return "DIV";
			case JMP:   return "JMP";
			case JZ:    return "JZ";
			case HALT:  return "HALT";
		}
		return "unknown";
	}
	public String getOperand() { return operand; }
}
