/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.brainduck.ast;

import net.emustudio.emulib.runtime.io.IntelHEX;

import java.util.ArrayList;
import java.util.List;

public class Program {
    private final List<Instruction> instructions = new ArrayList<>();

    public void add(Instruction instruction) {
        if (instruction != null) {
            instructions.add(instruction);
        }
    }

    public void generateCode(IntelHEX hex) {
        for (Instruction instruction : instructions) {
            instruction.generateCode(hex);
        }
    }

    @Override
    public String toString() {
        return "Program{" +
                "instructions=" + instructions +
                '}';
    }
}
