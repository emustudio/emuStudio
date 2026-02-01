/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ram.ast;

import net.emustudio.plugins.compiler.ram.SerializableOptional;
import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.api.RamLabel;
import net.emustudio.plugins.memory.ram.api.RamValue;

import java.util.Objects;
import java.util.Optional;

public class Instruction implements RamInstruction {
    public final int line;
    public final int column;

    private final int address;
    private final Opcode opcode;
    private final Direction direction;
    private final SerializableOptional<RamValue> operand;
    private SerializableOptional<RamLabel> label;

    public Instruction(int line, int column, Opcode opcode, Direction direction,
                       int address, Optional<RamValue> operand) {
        this(line, column, opcode, direction, address, operand, null);
    }

    public Instruction(int line, int column, Opcode opcode, Direction direction,
                       int address, Optional<RamValue> operand, RamLabel label) {
        this.opcode = opcode;
        this.direction = direction;
        this.address = address;
        this.operand = SerializableOptional.fromOpt(Objects.requireNonNull(operand));
        this.line = line;
        this.column = column;
        this.label = SerializableOptional.ofNullable(label);
    }

    public Instruction(Opcode opcode, Direction direction,
                       int address, Optional<RamValue> operand, RamLabel label) {
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
    public Optional<RamValue> getOperand() {
        return operand.opt();
    }

    @Override
    public int getAddress() {
        return address;
    }

    @Override
    public Optional<RamLabel> getLabel() {
        return label.opt();
    }

    public void setLabel(RamLabel label) {
        this.label = SerializableOptional.ofNullable(label);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Instruction that = (Instruction) o;

        if (address != that.address) return false;
        if (opcode != that.opcode) return false;
        if (direction != that.direction) return false;
        if (!operand.equals(that.operand)) return false;
        return label.equals(that.label);
    }

    @Override
    public int hashCode() {
        int result = address;
        result = 31 * result + opcode.hashCode();
        result = 31 * result + direction.hashCode();
        result = 31 * result + operand.hashCode();
        result = 31 * result + label.hashCode();
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
