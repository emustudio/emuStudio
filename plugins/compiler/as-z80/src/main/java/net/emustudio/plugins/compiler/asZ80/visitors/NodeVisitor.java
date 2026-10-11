/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.as8080.visitors.BaseNodeVisitor;
import net.emustudio.plugins.compiler.asZ80.ast.instr.*;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoVar;

public class NodeVisitor extends BaseNodeVisitor {
    public void visit(Instr node) {
        visitChildren(node);
    }

    public void visit(InstrCB node) {
        visitChildren(node);
    }

    public void visit(InstrED node) {
        visitChildren(node);
    }

    public void visit(InstrXD node) {
        visitChildren(node);
    }

    public void visit(InstrXDCB node) {
        visitChildren(node);
    }

    public void visit(PseudoVar node) {
        visitChildren(node);
    }
}
