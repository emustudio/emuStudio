package net.emustudio.plugins.compiler.as8080.ast.expr;

import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.NameSpace;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    public Optional<Evaluated> eval(int currentAddress, NameSpace env) {
        Node leftChild = getChild(0);
        Node rightChild = getChild(1);

        Optional<Evaluated> left = leftChild.eval(currentAddress, env);
        Optional<Evaluated> right = rightChild.eval(currentAddress, env);

        if (left.isPresent() && right.isPresent()) {
            int l = left.get().getValue();
            int r = right.get().getValue();
            int result = operation.apply(l, r);

            Evaluated evaluated = new Evaluated(line, column);
            evaluated.addChild(new ExprNumber(line, column, result));
            return Optional.of(evaluated);
        }

        return Optional.empty();
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
