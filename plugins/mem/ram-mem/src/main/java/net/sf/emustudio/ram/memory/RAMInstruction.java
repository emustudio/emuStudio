/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
package net.sf.emustudio.ram.memory;

import emulib.annotations.ContextType;
import emulib.plugins.compiler.CompilerContext;

/**
 * This context will be registered by RAM compiler.
 */
@ContextType
public interface RAMInstruction extends CompilerContext {
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
	 * Get machine code of the RAM instruction. 
	 * @return code of the instruction
	 */
	public int getCode();
	
	/**
	 * Get direction of the RAM instruction:
         * 
	 * 0   - register
	 * '=' - direct
	 * '*' - indirect
	 * @return direction of the instruction
	 */
	public char getDirection();
	
	/**
	 * Get operand of the RAM instruction.
         * 
	 * @return operand (number or address, or string). If the operand is direct,
         * it returns a String. Otherwise Integer.
	 */
	public Object getOperand();
	
	/**
	 * Get a string representation of label operand (meaningful only for
         * JMP/JZ instructions)
         * @return label operand
	 */
	public String getOperandLabel();
	
	/**
	 * Get string representation of the RAM instruction (mnemonic code).
         * 
	 * @return string representation of the instruction
	 */
	public String getCodeStr();
	
	/**
	 * Get string representation of the operand.
         * 
	 * It includes labels, direction and integer operands.
	 * @return String representation of operand
	 */
	public String getOperandStr();
}
