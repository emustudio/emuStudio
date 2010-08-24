/**
 * RAMInstruction.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 *  KISS, YAGNI
 */
package ramc_ram.tree;

import ramc_ram.compiled.CompilerEnvironment;
import interfaces.C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E;

public class RAMInstruction implements C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E {

    private int instr;      // instruction code
    private char direction; // 0 - register, '=' - direct, '*' - indirect
    private Object operand; // operand
    private String label;   // label as operand for jmp/jz
    private boolean eval = false;

    // constructor not for jmp/jz
    public RAMInstruction(int in, char direction, Object operand) {
        instr = in;
        this.direction = direction;
        this.operand = operand;
    }

    public RAMInstruction(int in, String label) {
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

    @Override
    public String getID() {
        return "ramc-instruction";
    }
}
