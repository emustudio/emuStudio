package net.emustudio.plugins.compiler.as8080.ast.instr;

import net.emustudio.plugins.compiler.as8080.ast.expr.Expr;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

public class InstrExpr extends Instr {
    private final int opcode;
    private final Expr expr;

    public InstrExpr(int opcode, Expr expr) {
        this.opcode = opcode;
        this.expr = Objects.requireNonNull(expr);
    }
}
