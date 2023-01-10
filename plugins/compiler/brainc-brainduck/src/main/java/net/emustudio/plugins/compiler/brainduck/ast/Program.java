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
