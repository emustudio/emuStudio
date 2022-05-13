/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.compiler.ssem.ast;

import net.emustudio.plugins.compiler.ssem.Position;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static net.emustudio.plugins.compiler.ssem.CompilerChecks.*;

public class Program {
    private int startLine;
    private boolean startLineDefined;

    private final Map<Integer, Instruction> instructions = new HashMap<>();

    public void setStartLine(int startLine, Position pos) {
        checkStartLineDefined(startLineDefined, pos, this.startLine);
        checkLineOutOfBounds(pos, startLine);
        this.startLine = startLine;
        startLineDefined = true;
    }

    public int getStartLine() {
        return startLine;
    }

    public void add(int line, Instruction instruction, Position pos) {
        checkDuplicateLineDefinition(instructions.containsKey(line), pos, line);
        checkLineOutOfBounds(pos, line);
        instructions.put(line, instruction);
    }

    public void forEach(BiConsumer<Integer, Instruction> processor) {
        instructions.forEach(processor);
    }

    public Map<Integer,Instruction> getInstructions() {
        return Collections.unmodifiableMap(instructions);
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(startLine).append(" start\n");
        forEach((line, instr) -> buffer.append(String.format("%02d %s\n", line, instr)));
        return buffer.toString();
    }
}
