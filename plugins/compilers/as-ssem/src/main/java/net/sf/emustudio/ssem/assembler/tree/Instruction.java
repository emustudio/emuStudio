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
package net.sf.emustudio.ssem.assembler.tree;

import java.util.Optional;

public class Instruction implements ASTnode {
    public final static byte JMP = 0; // 000
    public final static byte JRP = 4; // 100
    public final static byte LDN = 2; // 010
    public final static byte STO = 6; // 110
    public final static byte SUB = 1; // 001
    public final static byte CMP = 3; // 011
    public final static byte STP = 7; // 111
    private final static String[] INSTRUCTION_STRING = new String[] {
        "JMP", "SUB", "LDN", "CMP", "JRP", null, "STO", "STP"
    };

    private final int opcode;
    private final Optional<Byte> operand;

    private Instruction(int opcode, byte operand) {
        this.operand = Optional.of(operand);
        this.opcode = opcode;
    }

    private Instruction(int opcode) {
        this.operand = Optional.empty();
        this.opcode = opcode;
    }

    public int getOpcode() {
        return opcode;
    }

    public Optional<Byte> getOperand() {
        return operand;
    }

    public static Instruction jmp(byte address) {
        return new Instruction(JMP, address);
    }

    public static Instruction jrp(byte address) {
        return new Instruction(JRP, address);
    }

    public static Instruction ldn(byte address) {
        return new Instruction(LDN, address);
    }

    public static Instruction sto(byte address) {
        return new Instruction(STO, address);
    }

    public static Instruction sub(byte address) {
        return new Instruction(SUB, address);
    }

    public static Instruction cmp() {
        return new Instruction(CMP);
    }

    public static Instruction stp() {
        return new Instruction(STP);
    }

    @Override
    public void accept(ASTvisitor visitor) throws Exception {
         visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Instruction that = (Instruction) o;
        return opcode == that.opcode && operand.equals(that.operand);
    }

    @Override
    public int hashCode() {
        int result = opcode;
        result = 31 * result + operand.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return INSTRUCTION_STRING[opcode] + " " + operand;
    }
}
