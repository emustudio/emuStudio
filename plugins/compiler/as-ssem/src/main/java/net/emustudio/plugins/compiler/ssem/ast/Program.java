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
