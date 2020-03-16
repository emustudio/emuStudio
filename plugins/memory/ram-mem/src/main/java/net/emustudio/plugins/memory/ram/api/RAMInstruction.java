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

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.compiler.CompilerContext;

import java.util.Objects;

/**
 * This context will be registered by RAM compiler.
 */
@PluginContext
public interface RAMInstruction extends CompilerContext {
    int READ = 1;
    int WRITE = 2;
    int LOAD = 3;
    int STORE = 4;
    int ADD = 5;
    int SUB = 6;
    int MUL = 7;
    int DIV = 8;
    int JMP = 9;
    int JZ = 10;
    int JGTZ = 11;
    int HALT = 12;

    enum Direction {
        REGISTER(""), DIRECT("="), INDIRECT("*");

        private final String value;

        Direction(String value) {
            this.value = Objects.requireNonNull(value);
        }

        public String value() {
            return value;
        }
    }

    /**
     * Get machine code of the RAM instruction.
     *
     * @return code of the instruction
     */
    int getCode();

    /**
     * Get direction of the RAM instruction:
     *
     * @return direction of the instruction
     */
    Direction getDirection();

    /**
     * Get operand of the RAM instruction.
     *
     * @return operand (number or address, or string). If the operand is direct,
     * it returns a String. Otherwise Integer.
     */
    Object getOperand();

    /**
     * Get a string representation of label operand (meaningful only for
     * JMP/JZ instructions)
     *
     * @return label operand
     */
    String getOperandLabel();

    /**
     * Get string representation of the RAM instruction (mnemonic code).
     *
     * @return string representation of the instruction
     */
    String getCodeStr();

    /**
     * Get string representation of the operand.
     * <p>
     * It includes labels, direction and integer operands.
     *
     * @return String representation of operand
     */
    String getOperandStr();
}
