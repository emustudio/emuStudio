package net.emustudio.plugins.compiler.as8080.ast.expr;

import net.emustudio.plugins.compiler.as8080.Either;
import net.emustudio.plugins.compiler.as8080.ast.*;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import static net.emustudio.plugins.compiler.as8080.As8080Parser.*;

public class ExprInfix extends Node {
    private final static Map<Integer, BiFunction<Integer, Integer, Integer>> infixOps = new HashMap<>();
    private final BiFunction<Integer, Integer, Integer> operation;
    public final int operationCode;

    static {
        infixOps.putAll(Map.of(
            OP_ADD, Integer::sum,
            OP_SUBTRACT, (x, y) -> x - y,
            OP_DIVIDE, (x, y) -> x / y, // can throw!
            OP_MULTIPLY, (x, y) -> x * y,
            OP_MOD, (x, y) -> x % y,
            OP_AND, (x, y) -> x & y,
            OP_OR, (x, y) -> x | y,
            OP_XOR, (x, y) -> x ^ y,
            OP_SHL, (x, y) -> x << y,
            OP_SHR, (x, y) -> x >>> y
        ));
        infixOps.put(OP_EQUAL, (x, y) -> ((x.equals(y)) ? 1 : 0));
    }

    public ExprInfix(int line, int column, int op) {
        super(line, column);
        this.operationCode = op;
        this.operation = Objects.requireNonNull(infixOps.get(op), "Unknown infix operation");
        // children are: left, right
    }

    public ExprInfix(Token op) {
        this(op.getLine(), op.getCharPositionInLine(), op.getType());
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Either<Node, Evaluated> eval(int currentAddress, int expectedSizeBytes, NameSpace env) {
        Node leftChild = getChild(0);
        Node rightChild = getChild(1);

        Either<Node, Evaluated> left = leftChild.eval(currentAddress, expectedSizeBytes, env);
        Either<Node, Evaluated> right = rightChild.eval(currentAddress, expectedSizeBytes, env);

        if (left.isRight() && right.isRight()) {
            int l = left.right.getValue();
            int r = right.right.getValue();
            int result = operation.apply(l, r);

            Evaluated evaluated = new Evaluated(line, column, currentAddress, expectedSizeBytes);
            evaluated.addChild(new ExprNumber(line, column, result));
            return Either.ofRight(evaluated);
        }

        return Either.ofLeft(this);
    }

    @Override
    protected String toStringShallow() {
        return "ExprInfix(" + operationCode + ")";
    }

    @Override
    protected Node mkCopy() {
        return new ExprInfix(line, column, operationCode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExprInfix exprInfix = (ExprInfix) o;
        return operationCode == exprInfix.operationCode;
    }
}
