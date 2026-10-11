/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.ast.instr.*;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoSet;

public class NodeVisitor extends BaseNodeVisitor {
    public void visit(InstrExpr node) {
        visitChildren(node);
    }

    public void visit(InstrNoArgs node) {
        visitChildren(node);
    }

    public void visit(InstrReg node) {
        visitChildren(node);
    }

    public void visit(InstrRegExpr node) {
        visitChildren(node);
    }

    public void visit(InstrRegPair node) {
        visitChildren(node);
    }

    public void visit(InstrRegPairExpr node) {
        visitChildren(node);
    }

    public void visit(InstrRegReg node) {
        visitChildren(node);
    }

    public void visit(PseudoSet node) {
        visitChildren(node);
    }
}
