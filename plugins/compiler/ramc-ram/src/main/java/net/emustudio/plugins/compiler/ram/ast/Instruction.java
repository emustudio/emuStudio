/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ram.ast;

import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.api.RamLabel;
import net.emustudio.plugins.memory.ram.api.RamValue;

import java.util.Objects;

public class Instruction implements RamInstruction {
    public final int line;
    public final int column;

    private final int address;
    private final Opcode opcode;
    private final Direction direction;
    private final RamValue operand;
    private RamLabel label;

    public Instruction(int line, int column, Opcode opcode, Direction direction,
                       int address, RamValue operand) {
        this(line, column, opcode, direction, address, operand, null);
    }

    public Instruction(int line, int column, Opcode opcode, Direction direction,
                       int address, RamValue operand, RamLabel label) {
        this.opcode = opcode;
        this.direction = direction;
        this.address = address;
        this.operand = operand;
        this.line = line;
        this.column = column;
        this.label = label;
    }

    public Instruction(Opcode opcode, Direction direction,
                       int address, RamValue operand, RamLabel label) {
        this(0, 0, opcode, direction, address, operand, label);
    }

    @Override
    public Opcode getOpcode() {
        return opcode;
    }

    @Override
    public Direction getDirection() {
        return direction;
    }

    @Override
    public RamValue getOperand() {
        return operand;
    }

    @Override
    public int getAddress() {
        return address;
    }

    @Override
    public RamLabel getLabel() {
        return label;
    }

    public void setLabel(RamLabel label) {
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Instruction that = (Instruction) o;

        if (address != that.address) return false;
        if (opcode != that.opcode) return false;
        if (direction != that.direction) return false;
        if (!Objects.equals(operand, that.operand)) return false;
        return Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        int result = address;
        result = 31 * result + opcode.hashCode();
        result = 31 * result + direction.hashCode();
        result = 31 * result + (operand != null ? operand.hashCode() : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Instruction{" +
                "address=" + address +
                ", opcode=" + opcode +
                ", direction=" + direction +
                ", operand=" + operand +
                ", label=" + label +
                '}';
    }
}
