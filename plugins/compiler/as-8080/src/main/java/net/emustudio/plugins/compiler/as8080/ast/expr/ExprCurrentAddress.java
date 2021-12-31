package net.emustudio.plugins.compiler.as8080.ast.expr;

import net.emustudio.plugins.compiler.as8080.Either;
import net.emustudio.plugins.compiler.as8080.ast.*;

public class ExprCurrentAddress extends Node {

    public ExprCurrentAddress(int line, int column) {
        super(line, column);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new ExprCurrentAddress(line, column);
    }

    @Override
    public Either<Node, Evaluated> eval(int currentAddress, NameSpace env) {
        Evaluated evaluated = new Evaluated(line, column);
        evaluated.addChild(new ExprNumber(line, column, currentAddress));
        return Either.ofRight(evaluated);
    }

    public Either<Node, Evaluated> eval(int currentAddress, NameSpace env, boolean evalLeft) {
        return evalLeft ? Either.ofLeft(this) : eval(currentAddress, env);
    }
}
