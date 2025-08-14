/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.pseudo;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

public class PseudoEqu extends Node {
    public final String id;

    public PseudoEqu(SourceCodePosition position, String id) {
        super(position);
        this.id = Objects.requireNonNull(id);
        // expr is the only child
    }

    public PseudoEqu(String fileName, Token id) {
        this(positionFromToken(fileName, id), id.getText());
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "PseudoEqu(" + id + ")";
    }

    @Override
    protected Node mkCopy() {
        return new PseudoEqu(position, id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PseudoEqu pseudoEqu = (PseudoEqu) o;
        return Objects.equals(id, pseudoEqu.id);
    }
}
