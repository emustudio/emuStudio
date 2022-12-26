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
package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprId;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.*;

import java.util.*;

import static net.emustudio.plugins.compiler.asZ80.CompileError.*;
import static net.emustudio.plugins.compiler.asZ80.ParsingUtils.normalizeId;

/**
 * Checks if all declarations are valid:
 * - ID of constant,variable and macro can be used just once in the whole program.
 * - ID of constant and macro cannot reference itself in the declaration
 * - ID of variable cannot reference itself in the declaration only if it wasn't already declared
 * - macro parameters should not conflict with:
 * - declarations in and out of the macro scope
 * - previously declared parameters in current or parent macros if the current one is nested
 * - if expressions should not reference declarations inside that if (including all nested ifs)
 * <p>
 * - cyclic references will be checked in evaluator since it requires > 1 passes
 */
public class CheckDeclarationsVisitor extends NodeVisitor {
    private final Set<String> allDeclarations = new HashSet<>();
    private final Set<String> macros = new HashSet<>();
    private final Set<String> variables = new HashSet<>();

    // for checking collisions with other declarations (e.g. forward referenced labels)
    private final Set<String> allMacroParams = new HashSet<>();

    // for checking marco param names in nested macros
    private final List<Set<String>> macroParamsInScope = new ArrayList<>();
    // if expr references
    private final Set<String> currentIfReferences = new HashSet<>();
    // for easier removal of current macro params from macroParamsInScope when the macro definition ends
    private Set<String> currentMacroParams;
    private boolean insideMacroParameter = false;
    private int insideIfLevel = 0; // if nesting level, to know how long to keep currentIfReferences
    private boolean insideIfExpr = false;

    private String currentDeclarationId;

    @Override
    public void visit(PseudoEqu node) {
        addDeclaration(node.id, node);
        currentDeclarationId = normalizeId(node.id);
        visitChildren(node);
        currentDeclarationId = null;
    }

    @Override
    public void visit(PseudoMacroDef node) {
        addMacro(node.id, node);
        currentMacroParams = new HashSet<>();
        macroParamsInScope.add(currentMacroParams);
        visitChildren(node);
        macroParamsInScope.remove(macroParamsInScope.size() - 1);
    }

    @Override
    public void visit(PseudoVar node) {
        currentDeclarationId = normalizeId(node.id);
        visitChildren(node);
        addVariable(node.id, node);
        currentDeclarationId = null;
    }

    @Override
    public void visit(PseudoLabel node) {
        addDeclaration(node.label, node);
        visitChildren(node);
    }

    @Override
    public void visit(PseudoMacroParameter node) {
        insideMacroParameter = true;
        visitChildren(node);
        insideMacroParameter = false;
    }

    @Override
    public void visit(ExprId node) {
        if (insideMacroParameter) {
            addMacroParameter(node.id, node);
        }
        if (insideIfExpr) {
            addIfReference(node);
        }
        String idLower = normalizeId(node.id);
        if (!variables.contains(idLower) && idLower.equals(currentDeclarationId)) {
            error(declarationReferencesItself(node));
        }
    }

    @Override
    public void visit(PseudoIf node) {
        insideIfLevel++;
        insideIfExpr = true;
        visit(node.getChild(0)); // visiting only expr
        insideIfExpr = false;
        visitChildren(node, 1);
        insideIfLevel--;

        if (insideIfLevel == 0) {
            currentIfReferences.clear();
        }
    }

    private void addVariable(String id, Node node) {
        addDeclaration(id, true, node);
    }

    private void addDeclaration(String id, Node node) {
        addDeclaration(id, false, node);
    }

    private void addDeclaration(String id, boolean isVariable, Node node) {
        String normId = normalizeId(id);

        if (currentIfReferences.contains(normId)) {
            error(ifExpressionReferencesOwnBlock(node));
        }

        // if we're in macro (arbitrary nesting level) and it has param named the same way -> error
        // if there exist any declaration with that name, but either this declaration is not a variable or the name was taken by not a variable
        if (allMacroParams.contains(normId) || buildDeclarations().contains(normId) && (!isVariable || !variables.contains(id))) {
            error(alreadyDeclared(node, id));
        }
        allDeclarations.add(normId);
        if (isVariable) {
            variables.add(normId);
        }
    }

    private void addMacro(String id, Node node) {
        String idLower = id.toLowerCase(Locale.ENGLISH);
        if (macros.contains(idLower)) {
            error(alreadyDeclared(node, id));
        }
        macros.add(idLower);
    }

    private void addMacroParameter(String id, Node node) {
        String idLower = id.toLowerCase(Locale.ENGLISH);
        if (buildDeclarations().contains(idLower) || macros.contains(idLower)) {
            error(alreadyDeclared(node, id));
        }
        currentMacroParams.add(idLower);
        allMacroParams.add(idLower);
    }

    private void addIfReference(ExprId node) {
        String idLower = node.id.toLowerCase(Locale.ENGLISH);
        if (currentIfReferences.contains(idLower)) {
            error(ifExpressionReferencesOwnBlock(node));
        } else {
            currentIfReferences.add(idLower);
        }
    }

    private Set<String> buildDeclarations() {
        Set<String> allDeclarations = new HashSet<>();
        for (Set<String> parameters : macroParamsInScope) {
            allDeclarations.addAll(parameters);
        }
        allDeclarations.addAll(this.allDeclarations);
        return allDeclarations;
    }
}
