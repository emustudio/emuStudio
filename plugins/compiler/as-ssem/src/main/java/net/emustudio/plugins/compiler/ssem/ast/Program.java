/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ssem.ast;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static net.emustudio.plugins.compiler.ssem.CompilerChecks.*;

public class Program {
    private final Map<Integer, Instruction> instructions = new HashMap<>();
    private int startLine;
    private boolean startLineDefined;

    public void setStartLine(int startLine, SourceCodePosition position) {
        checkStartLineDefined(startLineDefined, position, this.startLine);
        checkLineOutOfBounds(position, startLine);
        this.startLine = startLine;
        startLineDefined = true;
    }

    public int getStartLine() {
        return startLine;
    }

    public void add(int line, Instruction instruction, SourceCodePosition position) {
        checkDuplicateLineDefinition(instructions.containsKey(line), position, line);
        checkLineOutOfBounds(position, line);
        instructions.put(line, instruction);
    }

    public void forEach(BiConsumer<Integer, Instruction> processor) {
        instructions.forEach(processor);
    }

    public Map<Integer, Instruction> getInstructions() {
        return Collections.unmodifiableMap(instructions);
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(startLine).append(" start\n");
        forEach((line, instr) -> buffer.append(String.format("%02d %s\n", line, instr)));
        return buffer.toString();
    }
}
