/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.pseudo;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ParsingUtils;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

public class PseudoInclude extends Node {
    public final String filename;

    public PseudoInclude(SourceCodePosition position, String fileName) {
        super(position);
        this.filename = Objects.requireNonNull(fileName);
    }

    public PseudoInclude(String srcFileName, Token fileName) {
        this(positionFromToken(srcFileName, fileName), ParsingUtils.parseLitString(fileName));
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "PseudoInclude('" + filename + "')";
    }

    @Override
    protected Node mkCopy() {
        return new PseudoInclude(position, filename);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PseudoInclude that = (PseudoInclude) o;
        return Objects.equals(filename, that.filename);
    }
}
