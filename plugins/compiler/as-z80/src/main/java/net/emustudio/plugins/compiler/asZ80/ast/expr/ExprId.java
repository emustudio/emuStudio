/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.expr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.NameSpace;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

import static net.emustudio.emulib.plugins.compiler.antlr.ParsingUtils.normalizeId;

public class ExprId extends Node {
    public final String id;

    public ExprId(SourceCodePosition position, String id) {
        super(position);
        this.id = Objects.requireNonNull(id);
    }

    public ExprId(String fileName, Token id) {
        this(positionFromToken(fileName, id), id.getText());
    }

    @Override
    public Evaluated eval(Integer currentAddress, NameSpace env) {
        return env.get(normalizeId(id));
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "ExprId(" + id + ")";
    }

    @Override
    protected Node mkCopy() {
        return new ExprId(position, id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExprId exprId = (ExprId) o;
        return Objects.equals(id, exprId.id);
    }
}
