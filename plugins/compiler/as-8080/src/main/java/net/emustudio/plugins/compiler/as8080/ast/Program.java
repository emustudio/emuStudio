package net.emustudio.plugins.compiler.as8080.ast;

import java.util.Objects;
import java.util.Optional;

public class Program extends Node {
    private final NameSpace env;
    private String filename;

    public Program(int line, int column, NameSpace env) {
        super(line, column);
        this.env = Objects.requireNonNull(env);
    }

    public Program(NameSpace env) {
        this(0, 0, env);
    }

    public Program() {
        this(new NameSpace());
    }

    public void setFileName(String filename) {
        this.filename = filename;
    }

    public Optional<String> getFileName() {
        return Optional.ofNullable(filename);
    }


    public NameSpace env() {
        return env;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        Program program = new Program(line, column, env);
        program.setFileName(filename);
        return program;
    }
}
