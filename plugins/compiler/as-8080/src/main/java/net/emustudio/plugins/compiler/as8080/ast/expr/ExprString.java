/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.ast.expr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.NameSpace;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

import static net.emustudio.plugins.compiler.as8080.ParsingUtils.parseLitString;

public class ExprString extends Node {
    public final String string;

    public ExprString(SourceCodePosition position, String string) {
        super(position);
        this.string = Objects.requireNonNull(string);
    }

    public ExprString(String fileName, Token str) {
        this(new SourceCodePosition(str.getLine(), str.getCharPositionInLine(), fileName), parseLitString(str));
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new ExprString(position, string);
    }

    @Override
    public Evaluated eval(Integer currentAddress, NameSpace env) {
        if (string.length() == 1) {
            return new Evaluated(position, string.charAt(0) & 0xFF);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExprString that = (ExprString) o;
        return Objects.equals(string, that.string);
    }

    @Override
    protected String toStringShallow() {
        return "ExprString(" + string + ")";
    }
}
