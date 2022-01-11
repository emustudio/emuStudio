package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDS;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDW;
import net.emustudio.plugins.compiler.as8080.ast.expr.*;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrExpr;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrRegExpr;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrRegPairExpr;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.*;

import java.util.*;
import java.util.stream.Collectors;

import static net.emustudio.plugins.compiler.as8080.CompileError.ambiguousExpression;
import static net.emustudio.plugins.compiler.as8080.ParsingUtils.normalizeId;

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
    private int expectedBytes = 0;
    private boolean doNotEvaluateCurrentAddress = false;
    private final Set<String> doNotEvaluateVariables = new HashSet<>();
    private final Set<String> forwardReferences = new HashSet<>();

    private Optional<Evaluated> latestEval;
    private Set<Node> needMorePassThings = new HashSet<>();

    private final Map<String, List<String>> macroArguments = new HashMap<>();
    private String currentMacro;

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
        expectedBytes = 1;
        visitChildren(node);
    }

    @Override
    public void visit(DataDW node) {
        node.setAddress(currentAddress);
        expectedBytes = 2;
        visitChildren(node);
    }

    @Override
    public void visit(DataDS node) {
        node.setAddress(currentAddress);
        expectedBytes = 0;
        visitChildren(node);
        latestEval.ifPresentOrElse(
            e -> currentAddress += e.value,
            () -> doNotEvaluateCurrentAddress = true
        );
    }

    @Override
    public void visit(PseudoEqu node) {
        expectedBytes = 0; // expected number of bytes will be known on usage (DB, DW, DS, instruction with expr, ORG)
        visitChildren(node);
        env.put(normalizeId(node.id), latestEval);

        // we don't need to re-evaluate the constant
        latestEval.ifPresent(e -> node.remove());
    }

    @Override
    public void visit(PseudoSet node) {
        expectedBytes = 0; // expected number of bytes will be known on usage (DB, DW, DS, instruction with expr, ORG)
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
            e -> node.remove(), // we don't need to re-evaluate label
            () -> needMorePassThings.add(node)
        );
    }

    @Override
    public void visit(PseudoOrg node) {
        node.setAddress(currentAddress);
        expectedBytes = 0;
        visitChildren(node);
        latestEval.ifPresentOrElse(
            e -> currentAddress = e.value,
            () -> doNotEvaluateCurrentAddress = true
        );
    }

    @Override
    public void visit(PseudoIf node) {
        expectedBytes = 0;
        Optional<Evaluated> expr = node
            .collectChild(PseudoIfExpression.class)
            .flatMap(p -> {
                visit(p);
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
    public void visit(InstrExpr node) {
        node.setAddress(currentAddress);
        expectedBytes = node.getExprSizeBytes();
        visitChildren(node);
        currentAddress++; // opcode
    }

    @Override
    public void visit(InstrRegExpr node) {
        node.setAddress(currentAddress);
        expectedBytes = 1;
        visitChildren(node);
        currentAddress++; // opcode
    }

    @Override
    public void visit(InstrRegPairExpr node) {
        node.setAddress(currentAddress);
        expectedBytes = 2;
        visitChildren(node);
        currentAddress++; // opcode
    }

    @Override
    public void visit(PseudoMacroCall node) {
        currentMacro = normalizeId(node.id);
        macroArguments.put(currentMacro, new ArrayList<>());
        visitChildren(node);

        // on macro exit, remove current macro arguments from env
        for (String macroParameter : macroArguments.get(currentMacro)) {
            env.remove(macroParameter);
        }
        macroArguments.remove(currentMacro);
    }

    @Override
    public void visit(PseudoMacroArgument node) {
        expectedBytes = 0; // expected number of bytes will be known on usage (DB, DW, DS, instruction with expr, ORG)
        visitChildren(node, 1); // expected two children: ExprId and Expr*

        node.collectChild(ExprId.class)
            .ifPresent(exprId -> {
                String macroParameter = normalizeId(exprId.id);
                macroArguments.get(currentMacro).add(macroParameter);
                env.put(macroParameter, latestEval);
            });
    }

    @Override
    public void visit(Evaluated node) {
        latestEval = Optional.of(node);
        currentAddress += expectedBytes;
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

    private Optional<Integer> getCurrentAddress() {
        return doNotEvaluateCurrentAddress ? Optional.empty() : Optional.of(currentAddress);
    }

    private void evalExpr(Node node) {
        latestEval = node.eval(getCurrentAddress(), env);
        latestEval.ifPresentOrElse(
            e -> node.remove().ifPresent(p -> p.addChild(e)),
            () -> needMorePassThings.add(node)
        );
        currentAddress += expectedBytes;
    }
}
