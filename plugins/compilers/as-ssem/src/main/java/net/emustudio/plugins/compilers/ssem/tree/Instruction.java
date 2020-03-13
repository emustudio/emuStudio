/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.compilers.ssem.tree;

import net.emustudio.plugins.compilers.ssem.CompileException;

import java.util.Optional;

public class Instruction implements ASTnode {
    public final static byte JMP = 0; // 000
    public final static byte JRP = 4; // 100
    public final static byte LDN = 2; // 010
    public final static byte STO = 6; // 110
    public final static byte SUB = 1; // 001
    public final static byte CMP = 3; // 011
    public final static byte STP = 7; // 111
    private final static String[] INSTRUCTION_STRING = new String[]{
        "JMP", "SUB", "LDN", "CMP", "JRP", null, "STO", "STP"
    };

    private final int opcode;
    private final Byte operand;

    private Instruction(int opcode, int operand) throws CompileException {
        if (operand > 31 || operand < 0) {
            throw new CompileException("Instruction operand must be in range <0,31>!");
        }
        this.operand = (byte) (operand & 0xFF);
        this.opcode = opcode;
    }

    private Instruction(int opcode) {
        this.operand = null;
        this.opcode = opcode;
    }

    public int getOpcode() {
        return opcode;
    }

    public Optional<Byte> getOperand() {
        return Optional.ofNullable(operand);
    }

    public static Instruction jmp(int address) throws CompileException {
        return new Instruction(JMP, address);
    }

    public static Instruction jrp(int address) throws CompileException {
        return new Instruction(JRP, address);
    }

    public static Instruction ldn(int address) throws CompileException {
        return new Instruction(LDN, address);
    }

    public static Instruction sto(int address) throws CompileException {
        return new Instruction(STO, address);
    }

    public static Instruction sub(int address) throws CompileException {
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
        return opcode == that.opcode && Optional.ofNullable(operand).equals(Optional.ofNullable(that.operand));
    }

    @Override
    public int hashCode() {
        int result = opcode;
        result = 31 * result + Optional.ofNullable(operand).hashCode();
        return result;
    }

    @Override
    public String toString() {
        return INSTRUCTION_STRING[opcode] + " " + Optional.ofNullable(operand);
    }
}
