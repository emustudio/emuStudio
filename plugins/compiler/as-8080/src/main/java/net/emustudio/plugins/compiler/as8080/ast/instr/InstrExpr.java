package net.emustudio.plugins.compiler.as8080.ast.instr;

import net.emustudio.plugins.compiler.as8080.ast.expr.Expr;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

public class InstrExpr extends Instr {
    private final Token opcode;
    private final Expr expr;

    public InstrExpr(Token opcode, Expr expr) {
        this.opcode = Objects.requireNonNull(opcode);
        this.expr = Objects.requireNonNull(expr);
    }
}
