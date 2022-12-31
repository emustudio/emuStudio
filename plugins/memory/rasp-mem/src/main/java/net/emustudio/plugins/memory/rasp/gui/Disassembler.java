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
package net.emustudio.plugins.memory.rasp.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Disassembler {
    public final static int JMP = 15;
    public final static int JZ = 16;
    public final static int JGTZ = 17;
    private final static Map<Integer, String> instructions = new HashMap<>();

    static {
        instructions.put(1, "READ");
        instructions.put(2, "WRITE =");
        instructions.put(3, "WRITE");
        instructions.put(4, "LOAD =");
        instructions.put(5, "LOAD");
        instructions.put(6, "STORE");
        instructions.put(7, "ADD =");
        instructions.put(8, "ADD");
        instructions.put(9, "SUB =");
        instructions.put(10, "SUB");
        instructions.put(11, "MUL =");
        instructions.put(12, "MUL");
        instructions.put(13, "DIV =");
        instructions.put(14, "DIV");
        instructions.put(JMP, "JMP");
        instructions.put(JZ, "JZ");
        instructions.put(JGTZ, "JGTZ");
        instructions.put(18, "HALT");
    }

    public static Optional<String> disassemble(int opcode) {
        return Optional.ofNullable(instructions.get(opcode));
    }
}
