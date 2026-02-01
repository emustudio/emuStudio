/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.pseudo;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

public class PseudoMacroDef extends Node {
    public final String id;

    public PseudoMacroDef(SourceCodePosition position, String id) {
        super(position);
        this.id = Objects.requireNonNull(id);
        // parameters are the first children
        // statements are followed
    }

    public PseudoMacroDef(String fileName, Token id) {
        this(positionFromToken(fileName, id), id.getText());
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "PseudoMacroDef(" + id + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PseudoMacroDef that = (PseudoMacroDef) o;
        return Objects.equals(id, that.id);
    }

    public PseudoMacroDef mkCopy() {
        return new PseudoMacroDef(position, id);
    }
}
