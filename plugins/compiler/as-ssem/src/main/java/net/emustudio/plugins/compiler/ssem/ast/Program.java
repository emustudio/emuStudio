package net.emustudio.plugins.compiler.ssem.ast;

import net.emustudio.plugins.compiler.ssem.CompileException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class Program {
    private int startLine;
    private boolean startLineDefined;

    private final Map<Integer, Instruction> instructions = new HashMap<>();

    public void setStartLine(int startLine) {
        if (startLineDefined) {
            throw new CompileException("Start line is already defined!");
        }
        this.startLine = startLine;
        startLineDefined = true;
    }

    public int getStartLine() {
        return startLine;
    }

    public void add(int line, Instruction instruction) {
        if (instructions.containsKey(line)) {
            throw new CompileException("Duplicate line definition: " + line);
        }
        if (line > 31) {
            throw new CompileException("Line number is out of bounds <0;31>: " + line);
        }
        instructions.put(line, instruction);
    }

    public void forEach(BiConsumer<Integer, Instruction> processor) {
        instructions.forEach(processor);
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(startLine).append(" start\n");
        forEach((line, instr) -> {
            buffer.append(String.format("%02d %s\n", line, instr));
        });
        return buffer.toString();
    }
}
