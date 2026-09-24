/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.emulib.plugins.memory.annotations.SourceCodeAnnotation;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDS;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDW;
import net.emustudio.plugins.compiler.as8080.ast.instr.*;

import java.util.Objects;

public class SourceCodeAnnotationsVisitor extends NodeVisitor {
    private final long pluginId;
    private final MemoryContextAnnotations annotations;

    public SourceCodeAnnotationsVisitor(long pluginId, MemoryContextAnnotations annotations) {
        this.pluginId = pluginId;
        this.annotations = Objects.requireNonNull(annotations);
    }

    @Override public void visit(DataDB node) { add(node); }
    @Override public void visit(DataDW node) { add(node); }
    @Override public void visit(DataDS node) { add(node); }
    @Override public void visit(InstrExpr node) { add(node); }
    @Override public void visit(InstrNoArgs node) { add(node); }
    @Override public void visit(InstrReg node) { add(node); }
    @Override public void visit(InstrRegExpr node) { add(node); }
    @Override public void visit(InstrRegPair node) { add(node); }
    @Override public void visit(InstrRegPairExpr node) { add(node); }
    @Override public void visit(InstrRegReg node) { add(node); }

    private void add(Node node) {
        annotations.add(node.getAddress(), new SourceCodeAnnotation(pluginId, node.position));
    }
}
