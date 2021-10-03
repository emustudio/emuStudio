package net.emustudio.plugins.compiler.as8080.ast.expr;

import org.antlr.v4.runtime.Token;

import java.util.Objects;

public class ExprId extends Expr {
    private final Token id;

    public ExprId(Token id) {
        this.id = Objects.requireNonNull(id);
    }
}
