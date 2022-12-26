/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoMacroCall;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoMacroDef;

import java.util.*;

import static net.emustudio.plugins.compiler.as8080.CompileError.infiniteLoopDetected;
import static net.emustudio.plugins.compiler.as8080.CompileError.notDefined;
import static net.emustudio.plugins.compiler.as8080.ParsingUtils.normalizeId;

/**
 * Expands macros. It means - find macro definitions, remove them from the parent node and put them as a child under
 * each macro call. It supports forward references too.
 * <p>
 * It doesn't mean the macro expansion will be used in code yet.
 */
public class ExpandMacrosVisitor extends NodeVisitor {
    private final Map<String, Node> macros = new HashMap<>();
    private final Map<String, List<PseudoMacroCall>> forwardMacroCalls = new HashMap<>();

    @Override
    public void visit(Program node) {
        super.visit(node);
        for (Map.Entry<String, List<PseudoMacroCall>> entry : forwardMacroCalls.entrySet()) {
            error(notDefined(entry.getValue().get(0), "Macro '" + entry.getKey() + "'"));
        }
    }

    @Override
    public void visit(PseudoMacroDef node) {
        String id = normalizeId(node.id);

        // save macro
        macros.put(id, node);
        node.remove();

        // expand macro if we had earlier calls (as a forward reference)
        if (forwardMacroCalls.containsKey(id)) {
            for (PseudoMacroCall macroCall : forwardMacroCalls.remove(id)) {
                Node def = node.copy();
                macroCall.addChild(def);
                visitChildren(def); // b/c original node does not have a parent
            }
        } else {
            visitChildren(node);
        }
    }

    @Override
    public void visit(PseudoMacroCall node) {
        String id = normalizeId(node.id);
        if (macros.containsKey(id)) {
            node.addChild(macros.get(id).copy());
            checkInfiniteLoop(node);
        } else {
            // maybe the macro is defined later
            forwardMacroCalls.putIfAbsent(id, new ArrayList<>());
            List<PseudoMacroCall> macroCalls = forwardMacroCalls.get(id);
            macroCalls.add(node);
        }
    }

    private void checkInfiniteLoop(PseudoMacroCall node) {
        String id = normalizeId(node.id);
        Optional<Node> parent = node.getParent();
        while (parent.isPresent()) {
            // search all parents to the top for PseudoMacroDef
            Node parentValue = parent.get();
            if (parentValue instanceof PseudoMacroDef) {
                String parentId = normalizeId(((PseudoMacroDef) parentValue).id);
                if (parentId.equals(id)) {
                    fatalError(infiniteLoopDetected(node, "macro call '" + id + "'"));
                }
            }
            parent = parentValue.getParent();
        }
    }
}
