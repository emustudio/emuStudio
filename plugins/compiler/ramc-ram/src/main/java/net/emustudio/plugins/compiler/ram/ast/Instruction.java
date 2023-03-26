/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
