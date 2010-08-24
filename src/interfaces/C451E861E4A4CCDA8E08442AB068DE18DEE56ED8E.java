/**
 * IRAMInstruction.java
 * 
 *  KISS, YAGNI
 *
 * Copyright (C) 2009-2010 Peter Jakubčo <pjakubco at gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package interfaces;

import plugins.compiler.ICompilerContext;

public interface C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E extends ICompilerContext {
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
	public final static int JGTZ = 11;
	public final static int HALT = 12;
	
	/**
	 * Method returns binary code of RAM instruction. 
	 * @return code of instruction
	 */
	public int getCode();
	
	/**
	 * Method returns direction of instruction
	 * 0   - register
	 * '=' - direct
	 * '*' - indirect
	 * @return direction
	 */
	public char getDirection();
	
	/**
	 * Method returns operand of the instruction. Also
	 * for JMP/JZ instructions. If the operand is direct,
	 * it returns String. Otherwise Integer.
	 * @return operand (number or address, or string)
	 */
	public Object getOperand();
	
	/**
	 * Method returns string representation of label
	 * operand (meaningful only for JMP/JZ instructions)
	 * @return label operand
	 */
	public String getOperandLabel();
	
	/**
	 * Return string representation of RAM instruction.
	 * @return RAM instruction
	 */
	public String getCodeStr();
	
	/**
	 * Method returns string representation of operand.
	 * It includes labels, direction and integer operands.
	 * @return String representation of operand
	 */
	public String getOperandStr();
}
