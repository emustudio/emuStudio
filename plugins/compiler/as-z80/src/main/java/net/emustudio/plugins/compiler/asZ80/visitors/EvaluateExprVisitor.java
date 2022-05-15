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

import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.ast.Program;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDB;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDS;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDW;
import net.emustudio.plugins.compiler.asZ80.ast.expr.*;
import net.emustudio.plugins.compiler.asZ80.ast.instr.*;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.*;

import java.util.*;
import java.util.stream.Collectors;

import static net.emustudio.plugins.compiler.asZ80.CompileError.*;
import static net.emustudio.plugins.compiler.asZ80.ParsingUtils.normalizeId;

/**
 * The goal is to replace all Expr* with Evaluated
 * <p>
 * - replace ExprId (referencing PseudoEqu, PseudoSet, PseudoLabel or PseudoMacroArgument) with value
 * - replace ExprCurrentAddress with the current address
 * - address changes after processing Expr*, DataDS, PseudoOrg
 * - eliminate or include code block of PseudoIf if expr evaluates to 1
 * <p>
 * After finishing this visitor, there should be:
 * - no PseudoEqu
 * - no ExprId
 * - no PseudoIf
 * - no PseudoLabel
 */
public class EvaluateExprVisitor extends NodeVisitor {
    private int currentAddress = 0;
    private int sizeBytes = 0;
    private boolean doNotEvaluateCurrentAddress = false;
    private final Set<String> doNotEvaluateVariables = new HashSet<>();
    private final Set<String> forwardReferences = new HashSet<>();

    private Optional<Evaluated> latestEval;
    private Set<Node> needMorePassThings = new HashSet<>();

    private final Map<String, List<String>> macroArguments = new HashMap<>();
    private String currentMacroId;

    @Override
    public void visit(Program node) {
        if (env == null) {
            this.env = node.env();
        }

        currentAddress = 0;
        visitChildren(node);

        Set<Node> oldNeedMorePass;
        while (!needMorePassThings.isEmpty()) {
            oldNeedMorePass = needMorePassThings;
            needMorePassThings = new HashSet<>();

            currentAddress = 0;
            doNotEvaluateCurrentAddress = false;
            doNotEvaluateVariables.clear();
            forwardReferences.clear();

            visitChildren(node);

            // at least one thing must disappear from "oldNeedMorePass";
            // doesn't matter if something is added - since the source code is final, also additions must be final.
            boolean atLeastOneResolved = false;
            for (Node oldNode : oldNeedMorePass) {
                if (!needMorePassThings.contains(oldNode)) {
                    atLeastOneResolved = true;
                    break;
                }
            }
            if (!atLeastOneResolved) {
                // there is a cycle
                for (Node unresolved : needMorePassThings) {
                    error(ambiguousExpression(unresolved));
                }
                break;
            }
        }
    }

    @Override
    public void visit(DataDB node) {
        node.setAddress(currentAddress);
        sizeBytes = 1;
        visitChildren(node);
    }

    @Override
    public void visit(DataDW node) {
        node.setAddress(currentAddress);
        sizeBytes = 2;
        visitChildren(node);
    }

    @Override
    public void visit(DataDS node) {
        node.setAddress(currentAddress);
        sizeBytes = 0;
        visitChildren(node);
        latestEval.ifPresentOrElse(
            e -> currentAddress += e.value,
            () -> doNotEvaluateCurrentAddress = true
        );
    }

    @Override
    public void visit(PseudoEqu node) {
        sizeBytes = 0; // expected number of bytes will be known on usage (DB, DW, DS, instruction with expr, ORG)
        visitChildren(node);
        env.put(normalizeId(node.id), latestEval);

        // we don't need to re-evaluate the constant
        latestEval.ifPresent(e -> node.remove());
    }

    @Override
    public void visit(PseudoVar node) {
        sizeBytes = 0; // expected number of bytes will be known on usage (DB, DW, DS, instruction with expr, ORG)
        String normalizedId = normalizeId(node.id);
        if (!doNotEvaluateVariables.contains(normalizedId)) {
            visitChildren(node);
            env.put(normalizedId, latestEval);

            if (forwardReferences.contains(normalizedId)) {
                // if there are forward references for this ID and the variable is redefined (defined more than once),
                // the second pass would evaluate the reference with the latest variable redefinition instead of the
                // first definition which appears after the reference. The reason is that references are evaluated by
                // looking at env - and env contains previously defined values which are "inherited" in upcoming passes.
                // So in the second pass env would contain the latest definition from the previous pass.
                doNotEvaluateVariables.add(normalizedId);
            }
        }
    }

    @Override
    public void visit(PseudoLabel node) {
        Optional<Evaluated> eval = node.eval(getCurrentAddress(), env);
        env.put(normalizeId(node.label), eval);
        eval.ifPresentOrElse(
            e -> {
                // we don't need to re-evaluate label
                node.exclude();
            },
            () -> needMorePassThings.add(node)
        );
        visitChildren(node);
    }

    @Override
    public void visit(PseudoOrg node) {
        node.setAddress(currentAddress);
        sizeBytes = 0;
        visitChildren(node);
        latestEval.ifPresentOrElse(
            e -> currentAddress = e.value,
            () -> doNotEvaluateCurrentAddress = true
        );
    }

    @Override
    public void visit(PseudoIf node) {
        sizeBytes = 0;
        Optional<Evaluated> expr = node
            .collectChild(PseudoIfExpression.class)
            .flatMap(p -> {
                visitChildren(p);
                return p.collectChild(Evaluated.class);
            });

        boolean includeBlock = expr.filter(p -> p.value != 0).isPresent();
        boolean excludeBlock = expr.filter(p -> p.value == 0).isPresent();

        if (includeBlock) {
            List<Node> codeChildren = node.getChildren().stream().skip(1).collect(Collectors.toList());
            node.remove().ifPresent(p -> p.addChildren(codeChildren));
            visitChildren(node, 1);
        } else if (excludeBlock) {
            node.remove();
        } else {
            // expr needs more pass - it means all exprCurrentAddress and pseudoLabel evaluations below must be ignored
            // until this expr is evaluated
            doNotEvaluateCurrentAddress = true;
        }
    }

    @Override
    public void visit(Instr node) {
        node.setAddress(currentAddress);
        sizeBytes = 0;
        visitChildren(node);
        node.getSizeBytes().ifPresent(s -> currentAddress += s);
    }

    @Override
    public void visit(InstrCB node) {
        node.setAddress(currentAddress);
        sizeBytes = 0;
        visitChildren(node);
        node.getSizeBytes().ifPresent(s -> currentAddress += s);
    }

    @Override
    public void visit(InstrED node) {
        node.setAddress(currentAddress);
        sizeBytes = 0;
        visitChildren(node);
        node.getSizeBytes().ifPresent(s -> currentAddress += s);
    }

    @Override
    public void visit(InstrXD node) {
        node.setAddress(currentAddress);
        sizeBytes = 0;
        visitChildren(node);
        node.getSizeBytes().ifPresent(s -> currentAddress += s);
    }

    @Override
    public void visit(InstrXDCB node) {
        node.setAddress(currentAddress);
        sizeBytes = 0;
        visitChildren(node);
        node.getSizeBytes().ifPresent(s -> currentAddress += s);
    }

    @Override
    public void visit(PseudoMacroCall node) {
        // save old current macro, including its params
        String oldCurrentMacroId = currentMacroId;
        Map<String, Optional<Evaluated>> oldMacroParams = new HashMap<>();
        if (oldCurrentMacroId != null) {
            for (String macroParameter : macroArguments.get(oldCurrentMacroId)) {
                oldMacroParams.put(macroParameter, env.get(macroParameter));
            }
        }

        currentMacroId = normalizeId(node.id);
        macroArguments.put(currentMacroId, new ArrayList<>());
        visitChildren(node);

        // on macro exit, remove current macro arguments from env
        for (String macroParameter : macroArguments.get(currentMacroId)) {
            env.remove(macroParameter);
        }
        // and put back old current macro arguments to env
        oldMacroParams.forEach((macroParam, expr) -> env.put(macroParam, expr));

        macroArguments.remove(currentMacroId);
        currentMacroId = oldCurrentMacroId;
    }

    @Override
    public void visit(PseudoMacroArgument node) {
        sizeBytes = 0; // expected number of bytes will be known on usage (DB, DW, DS, instruction with expr, ORG)
        visitChildren(node, 1); // expected two children: ExprId and Expr*

        node.collectChild(ExprId.class)
            .ifPresent(exprId -> {
                String macroParameter = normalizeId(exprId.id);
                macroArguments.get(currentMacroId).add(macroParameter);
                env.put(macroParameter, latestEval);
            });
    }

    @Override
    public void visit(Evaluated node) {
        latestEval = Optional.of(node);
        currentAddress += sizeBytes;
    }

    @Override
    public void visit(ExprInfix node) {
        evalExpr(node);
    }

    @Override
    public void visit(ExprNumber node) {
        evalExpr(node);
    }

    @Override
    public void visit(ExprUnary node) {
        evalExpr(node);
    }

    @Override
    public void visit(ExprCurrentAddress node) {
        evalExpr(node);
    }

    @Override
    public void visit(ExprId node) {
        evalExpr(node);
        if (latestEval.isEmpty()) {
            forwardReferences.add(normalizeId(node.id));
        }
    }

    @Override
    public void visit(ExprString node) {
        // 2-byte sized strings are merged, for simplicity. 1-bytes will be added byte per byte.
        // hopefully this covers all cases.

        int strLen = node.string.length();
        if (sizeBytes == 2) {
            if (strLen > sizeBytes) {
                error(expressionIsBiggerThanExpected(node, sizeBytes, strLen));
            }
            int result = node.string.charAt(0);
            if (strLen > 1) {
                result |= (node.string.charAt(1) << 8);
            }
            node.addChild(new Evaluated(node.line, node.column, result).setSizeBytes(2));
            currentAddress += sizeBytes;
        } else {
            int maxValue = node.getMaxValue().map(v -> Math.min(v, 0xFF)).orElse(0xFF);
            for (int i = 0; i < strLen; i++) {
                node.addChild(new Evaluated(node.line, node.column, node.string.charAt(i)).setMaxValue(maxValue));
            }
            currentAddress += strLen;
        }
        node.exclude();
    }

    private Optional<Integer> getCurrentAddress() {
        return doNotEvaluateCurrentAddress ? Optional.empty() : Optional.of(currentAddress);
    }

    private void evalExpr(Node node) {
        latestEval = node.eval(getCurrentAddress(), env);
        latestEval.ifPresent(e -> node.getMaxValue().ifPresent(e::setMaxValue));
        latestEval.ifPresentOrElse(
            e -> node.remove().ifPresent(p -> p.addChild(e)),
            () -> needMorePassThings.add(node)
        );
        currentAddress += sizeBytes;
    }
}
