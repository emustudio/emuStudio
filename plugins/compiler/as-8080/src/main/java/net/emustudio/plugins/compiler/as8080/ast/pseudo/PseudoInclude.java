/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.compiler.as8080.ast.pseudo;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.ParsingUtils;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

public class PseudoInclude extends Node {
    public final String filename;

    public PseudoInclude(SourceCodePosition position, String fileName) {
        super(position);
        this.filename = Objects.requireNonNull(fileName);
    }

    public PseudoInclude(String srcFileName, Token fileName) {
        this(new SourceCodePosition(fileName.getLine(), fileName.getCharPositionInLine(), srcFileName), ParsingUtils.parseLitString(fileName));
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
