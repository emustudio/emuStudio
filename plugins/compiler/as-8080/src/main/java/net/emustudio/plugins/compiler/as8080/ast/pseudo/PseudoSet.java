/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.ast.pseudo;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

public class PseudoSet extends Node {
    public final String id;

    public PseudoSet(SourceCodePosition position, String id) {
        super(position);
        this.id = Objects.requireNonNull(id);
        // expr is the only child
    }

    public PseudoSet(String fileName, Token id) {
        this(new SourceCodePosition(id.getLine(), id.getCharPositionInLine(), fileName), id.getText());
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "PseudoSet(" + id + ")";
    }

    @Override
    protected Node mkCopy() {
        return new PseudoSet(position, id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PseudoSet pseudoSet = (PseudoSet) o;
        return Objects.equals(id, pseudoSet.id);
    }
}
