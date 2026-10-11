/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.ast.expr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.NameSpace;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.exceptions.CompileException;
import net.emustudio.plugins.compiler.as8080.visitors.BaseNodeVisitor;

import java.util.Objects;

public class ExprInfix extends Node {
    public final InfixOperator operation;

    public ExprInfix(SourceCodePosition position, InfixOperator operation) {
        super(position);
        this.operation = Objects.requireNonNull(operation, "Unknown infix operation");
        // children are: left, right
    }

    @Override
    public void accept(BaseNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Evaluated eval(Integer currentAddress, NameSpace env) {
        Node leftChild = getChild(0);
        Node rightChild = getChild(1);

        Evaluated left = leftChild.eval(currentAddress, env);
        Evaluated right = rightChild.eval(currentAddress, env);

        if (left != null && right != null) {
            try {
                return new Evaluated(position, operation.apply(left.value, right.value));
            } catch (ArithmeticException e) {
                throw new CompileException(position, "Arithmetic error: " + e.getMessage());
            }
        }

        return null;
    }

    @Override
    protected String toStringShallow() {
        return "ExprInfix(" + operation + ")";
    }

    @Override
    protected Node mkCopy() {
        return new ExprInfix(position, operation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExprInfix exprInfix = (ExprInfix) o;
        return operation == exprInfix.operation;
    }
}
