package net.emustudio.plugins.compiler.as8080.ast.pseudo;

import java.util.Objects;

public class PseudoMacroDef extends Pseudo {
    private final String id;

    public PseudoMacroDef(String id) {
        this.id = Objects.requireNonNull(id);
        // parameters are the first children
        // statements are followed
    }
}
