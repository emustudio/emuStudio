package net.emustudio.plugins.compiler.as8080.ast.expr;

import net.emustudio.plugins.compiler.as8080.Either;
import net.emustudio.plugins.compiler.as8080.ast.*;
import org.antlr.v4.runtime.Token;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static net.emustudio.plugins.compiler.as8080.As8080Parser.*;

public class ExprUnary extends Node {
    private final static Map<Integer, Function<Integer, Integer>> unaryOps = Map.of(
        OP_ADD,  x -> x,
        OP_SUBTRACT, x -> -x,
        OP_NOT, x -> ~x
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
    public Either<Node, Evaluated> eval(int currentAddress, NameSpace env) {
        Node child = getChild(0);
        Either<Node, Evaluated> childEval = child.eval(currentAddress, env);
        if (childEval.isRight()) {
            int value = childEval.right.getValue();
            int result = operation.apply(value);

            Evaluated evaluated = new Evaluated(line, column);
            evaluated.addChild(new ExprNumber(line, column, result));
            return Either.ofRight(evaluated);
        }
        return Either.ofLeft(this);
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
