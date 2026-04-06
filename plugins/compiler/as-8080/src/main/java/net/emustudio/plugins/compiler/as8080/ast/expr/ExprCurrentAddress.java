/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.ast.expr;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.NameSpace;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.visitors.NodeVisitor;

public class ExprCurrentAddress extends Node {

    public ExprCurrentAddress(SourceCodePosition position) {
        super(position);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new ExprCurrentAddress(position);
    }

    @Override
    public Evaluated eval(Integer currentAddress, NameSpace env) {
        if (currentAddress != null) {
            return new Evaluated(position, currentAddress);
        }
        return null;
    }
}
