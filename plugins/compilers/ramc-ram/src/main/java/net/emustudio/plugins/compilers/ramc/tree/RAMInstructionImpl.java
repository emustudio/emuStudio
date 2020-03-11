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
package net.emustudio.plugins.compilers.ramc.tree;

import net.emustudio.plugins.compilers.ramc.Namespace;
import net.emustudio.plugins.memory.ram.api.RAMInstruction;

import java.io.Serializable;

public class RAMInstructionImpl implements RAMInstruction, Serializable {
    private final int instructionCode;
    private final Direction direction;
    private final String label;

    private Object operand;
    private boolean eval = false;

    public RAMInstructionImpl(int in, Direction direction, Object operand) {
        instructionCode = in;
        this.direction = direction;
        this.operand = operand;
        this.label = null;
    }

    public RAMInstructionImpl(int in, String label) {
        this.instructionCode = in;
        this.direction = Direction.REGISTER;
        this.operand = -1;
        this.label = label;
    }

    @Override
    public int getCode() {
        return instructionCode;
    }

    @Override
    public String getCodeStr() {
        switch (instructionCode) {
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
        if (instructionCode == HALT) {
            return "";
        }

        String s = direction.value();
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
    public Direction getDirection() {
        return direction;
    }

    public boolean pass2() {
        if (!eval && label != null) {
            int a = Namespace.getLabelAddr(label);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RAMInstructionImpl that = (RAMInstructionImpl) o;

        if (instructionCode != that.instructionCode) return false;
        if (direction != that.direction) return false;
        if (label == null && that.label == null) {
            if (operand != null) return operand.equals(that.operand);
            else if (that.operand != null) return false;
        }

        return label != null ? label.equals(that.label) : that.label == null;
    }

    @Override
    public int hashCode() {
        int result = instructionCode;
        result = 31 * result + direction.hashCode();
        result = 31 * result + (label != null ? label.hashCode() : (operand != null ? operand.hashCode() : 0));
        return result;
    }

    @Override
    public String toString() {
        return "RAMInstruction{" + getCodeStr() + " " + getOperandStr() +
            ", label='" + label + '\'' +
            ", operand=" + operand +
            ", eval=" + eval +
            '}';
    }
}
