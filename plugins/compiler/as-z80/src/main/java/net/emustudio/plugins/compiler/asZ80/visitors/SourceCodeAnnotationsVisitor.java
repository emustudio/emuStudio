/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.emulib.runtime.annotations.AnnotationEngine.Entry;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDS;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDW;
import net.emustudio.plugins.compiler.asZ80.ast.instr.*;

import java.util.ArrayList;
import java.util.List;

public class SourceCodeAnnotationsVisitor extends NodeVisitor {
    private final long pluginId;
    private final List<Entry> annotations = new ArrayList<>();

    public SourceCodeAnnotationsVisitor(long pluginId) {
        this.pluginId = pluginId;
    }

    public List<Entry> annotations() { return List.copyOf(annotations); }

    public void addLabel(int address, String label) {
        annotations.add(new Entry(pluginId, annotations.size(), address, address + 1, "label", label, null));
    }

    @Override public void visit(DataDB node) { add(node); }
    @Override public void visit(DataDW node) { add(node); }
    @Override public void visit(DataDS node) { add(node); }
    @Override public void visit(Instr node) { add(node); }
    @Override public void visit(InstrCB node) { add(node); }
    @Override public void visit(InstrED node) { add(node); }
    @Override public void visit(InstrXD node) { add(node); }
    @Override public void visit(InstrXDCB node) { add(node); }

    private void add(Node node) {
        int address = node.getAddress();
        annotations.add(new Entry(pluginId, annotations.size(), address, address + 1, "source", "", node.position));
    }
}
