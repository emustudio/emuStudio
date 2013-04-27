/*
 * RAMInstructionImpl.java
 * 
 * Copyright (C) 2009-2012 Peter Jakubčo
 * KISS, YAGNI, DRY
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
package net.sf.emustudio.ram.compiler.tree;

import java.io.Serializable;
import net.sf.emustudio.ram.compiler.impl.CompilerEnvironment;
import net.sf.emustudio.ram.memory.RAMInstruction;

/**
 * Implementation of interface which is defined in RAM Memory.
 * @author Peter Jakubčo
 */
public class RAMInstructionImpl implements RAMInstruction, Serializable {
    private int instr;      // instruction code
    private char direction; // 0 - register, '=' - direct, '*' - indirect
    private Object operand; // operand
    private String label;   // label as operand for jmp/jz
    private boolean eval = false;

    // constructor not for jmp/jz
    public RAMInstructionImpl(int in, char direction, Object operand) {
        instr = in;
        this.direction = direction;
        this.operand = operand;
    }

    public RAMInstructionImpl(int in, String label) {
        this.instr = in;
        this.direction = 0;
        this.operand = -1;
        this.label = label;
    }

    @Override
    public int getCode() {
        return instr;
    }

    @Override
    public String getCodeStr() {
        switch (instr) {
            case LOAD:
                return "LOAD";
            case STORE:
                return "STORE";
            case READ:
                return "READ";
            case WRITE:
                return "WRITE";
            case ADD:
                return "ADD";
            case SUB:
                return "SUB";
            case MUL:
                return "MUL";
            case DIV:
                return "DIV";
            case JMP:
                return "JMP";
            case JZ:
                return "JZ";
            case JGTZ:
                return "JGTZ";
            case HALT:
                return "HALT";
        }
        return "unknown";
    }

    @Override
    public String getOperandStr() {
        if (instr == HALT) {
            return "";
        }

        String s = "";
        if (direction != 0) {
            s += direction;
        }
        if (label != null) {
            s += label;
            return s;
        }
        s += operand;
        return s;
    }

    @Override
    public String getOperandLabel() {
        return label;
    }

    @Override
    public Object getOperand() {
        return operand;
    }

    @Override
    public char getDirection() {
        return direction;
    }

    public boolean pass2() {
        if (!eval && label != null) {
            int a = CompilerEnvironment.getLabelAddr(label);
            if (a == -1) {
                return false;
            }
            operand = a;
            eval = true;
            return true;
        }
        eval = true;
        return true;
    }

}
