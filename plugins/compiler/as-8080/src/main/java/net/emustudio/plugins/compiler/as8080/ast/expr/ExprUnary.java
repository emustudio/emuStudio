/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.ast.expr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.NameSpace;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.visitors.BaseNodeVisitor;

import java.util.Objects;

public class ExprUnary extends Node {
    public final UnaryOperator operation;

    public ExprUnary(SourceCodePosition position, UnaryOperator operation) {
        super(position);
        this.operation = Objects.requireNonNull(operation, "Unknown unary operation");
        // child is expr
    }

    @Override
    public void accept(BaseNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Evaluated eval(Integer currentAddress, NameSpace env) {
        Evaluated childEval = getChild(0).eval(currentAddress, env);
        return childEval != null ? new Evaluated(position, operation.apply(childEval.value)) : null;
    }

    @Override
    protected String toStringShallow() {
        return "ExprUnary(" + operation + ")";
    }

    @Override
    protected Node mkCopy() {
        return new ExprUnary(position, operation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExprUnary exprUnary = (ExprUnary) o;
        return operation == exprUnary.operation;
    }
}
