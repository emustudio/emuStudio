package net.emustudio.plugins.compiler.as8080.ast.expr;

import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.NameSpace;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static net.emustudio.plugins.compiler.as8080.As8080Parser.*;

public class ExprUnary extends Node {
    private final static Map<Integer, Function<Integer, Integer>> unaryOps = Map.of(
        OP_ADD, x -> x,
        OP_SUBTRACT, x -> -x,
        OP_NOT, x -> ~x,
        OP_NOT_2, x -> ~x
    );
    private final Function<Integer, Integer> operation;
    public final int operationCode;

    public ExprUnary(int line, int column, int op) {
        super(line, column);
        this.operationCode = op;
        this.operation = Objects.requireNonNull(unaryOps.get(op), "Unknown unary operation");
        // child is expr
    }

    public ExprUnary(Token op) {
        this(op.getLine(), op.getCharPositionInLine(), op.getType());
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Optional<Evaluated> eval(Optional<Integer> currentAddress, NameSpace env) {
        return getChild(0)
            .eval(currentAddress, env)
            .map(childEval -> new Evaluated(line, column, operation.apply(childEval.value)));
    }

    @Override
    protected String toStringShallow() {
        return "ExprUnary(" + operationCode + ")";
    }

    @Override
    protected Node mkCopy() {
        return new ExprUnary(line, column, operationCode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExprUnary exprUnary = (ExprUnary) o;
        return operationCode == exprUnary.operationCode;
    }
}
