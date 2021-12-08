package net.emustudio.plugins.compiler.as8080.ast;

public class Program extends Node {
    private final NameSpace nameSpace = new NameSpace();

    public Program(int line, int column) {
        super(line, column);
    }

    public Program() {
        this(0, 0);
    }


    public NameSpace env() {
        return nameSpace;
    }


}
