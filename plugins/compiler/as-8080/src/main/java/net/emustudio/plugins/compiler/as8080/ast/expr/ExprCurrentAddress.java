package net.emustudio.plugins.compiler.as8080.ast.expr;

import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.NameSpace;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;

import java.util.Optional;

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
    public Optional<Evaluated> eval(int currentAddress, NameSpace env) {
        Evaluated evaluated = new Evaluated(line, column);
        evaluated.addChild(new ExprNumber(line, column, currentAddress));
        return Optional.of(evaluated);
    }

    public Optional<Evaluated> eval(int currentAddress, NameSpace env, boolean evalLeft) {
        return evalLeft ? Optional.empty() : eval(currentAddress, env);
    }
}
