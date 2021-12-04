package net.emustudio.plugins.compiler.as8080.ast;

public class Program extends Node {
    private final NameSpace nameSpace = new NameSpace();


    public NameSpace env() {
        return nameSpace;
    }


}
