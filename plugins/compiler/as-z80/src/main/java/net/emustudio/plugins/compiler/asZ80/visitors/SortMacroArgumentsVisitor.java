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
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoMacroArgument;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoMacroCall;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoMacroDef;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoMacroParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.emustudio.plugins.compiler.asZ80.CompileError.macroArgumentsDoNotMatch;
import static net.emustudio.plugins.compiler.asZ80.ParsingUtils.normalizeId;

/**
 * Connects macro parameters with arguments.
 * Example input:
 * <pre>
 * PseudoMacroCall
 *   PseudoMacroArgument
 *     ExprNumber(1)
 *   PseudoMacroArgument
 *     ExprNumber(2)
 *   PseudoMacroDef
 *     PseudoMacroParameter
 *       ExprId(q)
 *     PseudoMacroParameter
 *       ExprId(r)
 *     ...
 * </pre>
 * <p>
 * Example output:
 * <pre>
 * PseudoMacroCall
 *   PseudoMacroArgument
 *     ExprId(q)
 *     ExprNumber(1)
 *   PseudoMacroArgument
 *     ExprId(r)
 *     ExprNumber(2)
 *   ...
 * </pre>
 */
public class SortMacroArgumentsVisitor extends NodeVisitor {

    private final Map<String, List<Node>> macroArguments = new HashMap<>();
    private final Map<String, List<Node>> macroParameters = new HashMap<>();
    private String currentMacroCall;
    private String currentMacroDef;

    @Override
    public void visit(PseudoMacroCall node) {
        currentMacroCall = normalizeId(node.id);
        macroArguments.put(currentMacroCall, new ArrayList<>());
        visitChildren(node);
    }

    @Override
    public void visit(PseudoMacroArgument node) {
        macroArguments.get(currentMacroCall).add(node);
    }

    @Override
    public void visit(PseudoMacroDef node) {
        currentMacroDef = normalizeId(node.id);
        macroParameters.put(currentMacroDef, new ArrayList<>());

        String origMacroDef = currentMacroDef; // we can have macro calls inside this macro
        visitChildren(node);

        // there should not be recursive macro calls (assured by ExpandMacrosVisitor)
        List<Node> origMacroParams = macroParameters.get(origMacroDef);
        List<Node> origMacroArguments = macroArguments.get(origMacroDef);

        if (origMacroArguments.size() != origMacroParams.size()) {
            error(macroArgumentsDoNotMatch(node)); // better would be to have macro call instead of macro def
        } else {
            for (int i = 0; i < origMacroArguments.size(); i++) {
                Node macroParam = origMacroParams.get(i);
                macroParam.remove();

                Node macroArgument = origMacroArguments.get(i);
                macroParam
                    .collectChild(ExprId.class)
                    .ifPresentOrElse(macroArgument::addChildFirst, () -> error(macroArgumentsDoNotMatch(node)));
            }
            node.exclude();
        }
    }

    @Override
    public void visit(PseudoMacroParameter node) {
        macroParameters.get(currentMacroDef).add(node);
    }
}
