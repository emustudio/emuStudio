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
package net.emustudio.plugins.memory.ram.api;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * RAM instruction.
 * It is the type of the "memory cell".
 */
public interface RAMInstruction extends Serializable {

    enum Opcode {
        READ, WRITE, LOAD, STORE, ADD, SUB, MUL, DIV, JMP, JZ, JGTZ, HALT
    }

    enum Direction {
        CONSTANT("="), DIRECT(""), INDIRECT("*");

        private final String value;

        Direction(String value) {
            this.value = Objects.requireNonNull(value);
        }

        public String value() {
            return value;
        }
    }

    /**
     * Get address of this instruction
     *
     * @return address
     */
    int getAddress();

    /**
     * Get opcode of the RAM instruction.
     *
     * @return opcode of the instruction
     */
    Opcode getOpcode();

    /**
     * Get direction of the RAM instruction:
     *
     * @return direction of the instruction
     */
    Direction getDirection();

    /**
     * Get operand of this RAM instruction, if it has any.
     * If the operand is a label, here will be the String representation of the label.
     *
     * @return instruction operand if the instruction has any.
     */
    Optional<RAMValue> getOperand();

    /**
     * Get the label if the operand is a label.
     *
     * @return label operand
     */
    Optional<RAMLabel> getLabel();
}
