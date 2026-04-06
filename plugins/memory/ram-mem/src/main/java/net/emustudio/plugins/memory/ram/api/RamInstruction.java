/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ram.api;

import java.io.Serializable;
import java.util.Objects;

/**
 * RAM instruction.
 * It is the type of the "memory cell".
 */
public interface RamInstruction extends Serializable {

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
     * @return instruction operand if the instruction has any. (nullable)
     */
    RamValue getOperand();

    /**
     * Get the label if the operand is a label.
     *
     * @return label operand (nullable)
     */
    RamLabel getLabel();

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
}
