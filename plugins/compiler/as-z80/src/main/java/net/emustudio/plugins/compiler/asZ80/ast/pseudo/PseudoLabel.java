/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.pseudo;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.NameSpace;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Objects;
import java.util.Optional;

import static net.emustudio.plugins.compiler.asZ80.ParsingUtils.parseLabel;

public class PseudoLabel extends Node {
    public final String label;

    public PseudoLabel(SourceCodePosition position, String label) {
        super(position);
        this.label = Objects.requireNonNull(label);
    }

    public PseudoLabel(String fileName, Token label) {
        this(positionFromToken(fileName, label), parseLabel(label));
    }

    @Override
    public Optional<Evaluated> eval(Optional<Integer> currentAddress, NameSpace env) {
        return currentAddress.map(addr -> new Evaluated(position, addr, true));
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "Label(" + label + ")";
    }

    @Override
    protected Node mkCopy() {
        return new PseudoLabel(position, label);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PseudoLabel pseudoLabel1 = (PseudoLabel) o;
        return Objects.equals(label, pseudoLabel1.label);
    }
}
