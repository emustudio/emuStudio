/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.rasp.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Disassembler {
    public final static int READ = 1;
    public final static int JMP = 15;
    public final static int JZ = 16;
    public final static int JGTZ = 17;
    public final static int HALT = 18;
    private final static Map<Integer, String> instructions = new HashMap<>();

    static {
        instructions.put(READ, "READ");
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
        instructions.put(HALT, "HALT");
    }

    public static boolean isInstruction(int opcode) {
        return instructions.containsKey(opcode);
    }

    public static Optional<String> disassembleMnemo(int opcode) {
        return Optional.ofNullable(instructions.get(opcode));
    }
}
