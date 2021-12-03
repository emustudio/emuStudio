package net.emustudio.plugins.compiler.as8080.ast.expr;

import java.util.Objects;

public class ExprId extends Expr {
    private final String id;

    public ExprId(String id) {
        this.id = Objects.requireNonNull(id);
    }
}
