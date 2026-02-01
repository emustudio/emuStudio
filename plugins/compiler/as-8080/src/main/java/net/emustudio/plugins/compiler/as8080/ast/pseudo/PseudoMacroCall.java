/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.ast.pseudo;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

public class PseudoMacroCall extends Node {
    public final String id;

    public PseudoMacroCall(SourceCodePosition position, String id) {
        super(position);
        this.id = Objects.requireNonNull(id);
        // children are exprs (arguments)
    }

    public PseudoMacroCall(String fileName, Token id) {
        this(new SourceCodePosition(id.getLine(), id.getCharPositionInLine(), fileName), id.getText());
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "PseudoMacroCall(" + id + ")";
    }

    @Override
    protected Node mkCopy() {
        return new PseudoMacroCall(position, id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PseudoMacroCall that = (PseudoMacroCall) o;
        return Objects.equals(id, that.id);
    }
}
