package net.emustudio.plugins.compiler.as8080.ast;

import net.emustudio.plugins.compiler.as8080.ast.instr.Instr;

import java.util.LinkedList;
import java.util.List;

public class Program extends AbstractNode {
    private final NameSpace nameSpace = new NameSpace();
    private final List<Instr> instructions = new LinkedList<>();

    public NameSpace env() {
        return nameSpace;
    }

    public Program addIntruction(Instr instr) {
        instructions.add(instr);
        return this;
    }
}
