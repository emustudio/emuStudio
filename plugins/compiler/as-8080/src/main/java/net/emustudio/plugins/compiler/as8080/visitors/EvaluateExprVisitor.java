package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.Either;
import net.emustudio.plugins.compiler.as8080.ast.*;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDS;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDW;
import net.emustudio.plugins.compiler.as8080.ast.data.DataPlainString;
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
 *
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

    private Either<NeedMorePass, Evaluated> latestEval;
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
        expectedBytes = 1;
        visitChildren(node);
    }

    @Override
    public void visit(DataDW node) {
        expectedBytes = 2;
        visitChildren(node);
    }

    @Override
    public void visit(DataDS node) {
        expectedBytes = 0;
        visitChildren(node);
        if (latestEval.isRight()) {
            // TODO: check 2 bytes
            currentAddress += latestEval.right.getValue();
        } else {
            // we don't know now how the address changes since we can't evaluate the expr yet
            doNotEvaluateCurrentAddress = true;
        }
    }

    @Override
    public void visit(PseudoEqu node) {
        expectedBytes = 0; // expected number of bytes will be known on usage (DB, DW, DS, instruction with expr, ORG)
        visitChildren(node);
        env.put(normalizeId(node.id), latestEval);
        if (latestEval.isRight()) {
            node.remove(); // we don't need to re-evaluate the constant
        }
    }

    @Override
    public void visit(PseudoSet node) {
        expectedBytes = 0; // expected number of bytes will be known on usage (DB, DW, DS, instruction with expr, ORG)
        visitChildren(node);
        env.put(normalizeId(node.id), latestEval);
    }

    @Override
    public void visit(PseudoLabel node) {
        Either<NeedMorePass, Evaluated> eval = node.eval(currentAddress, 2, env, doNotEvaluateCurrentAddress);
        env.put(normalizeId(node.label), eval);
        if (eval.isRight()) {
            node.remove(); // we don't need to re-evaluate label
        } else {
            needMorePassThings.add(node);
        }
    }

    @Override
    public void visit(PseudoOrg node) {
        expectedBytes = 0;
        visitChildren(node);
        if (latestEval.isRight()) {
            currentAddress = latestEval.right.getValue();
        } else {
            // if we can't evaluate current address now, we cannot evaluate it below too
            doNotEvaluateCurrentAddress = true;
        }
    }

    @Override
    public void visit(PseudoIf node) {
        expectedBytes = 0;
        visit(node.getChild(0));

        Optional<Evaluated> expr = node.collectChild(Evaluated.class);
        boolean includeBlock = expr.filter(p -> p.getValue() != 0).isPresent();
        boolean excludeBlock = expr.filter(p -> p.getValue() == 0).isPresent();

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
        expectedBytes = node.getExprSizeBytes();
        visitChildren(node);
        currentAddress++; // opcode
    }

    @Override
    public void visit(InstrRegExpr node) {
        expectedBytes = 1;
        visitChildren(node);
        currentAddress++; // opcode
    }

    @Override
    public void visit(InstrRegPairExpr node) {
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

        node.collectChild(ExprId.class).ifPresent(exprId -> {
            String macroParameter = normalizeId(exprId.id);
            macroArguments.get(currentMacro).add(macroParameter);
            env.put(macroParameter, latestEval);
        });
    }

    @Override
    public void visit(DataPlainString node) {
        Either<NeedMorePass, Evaluated> eval = node.eval(currentAddress, -1, env);
        node.remove().ifPresent(p -> p.addChild(eval.right));
        currentAddress += eval.right.sizeBytes;
    }

    @Override
    public void visit(Evaluated node) {
        latestEval = Either.ofRight(node);
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
    }

    private void evalExpr(Node node) {
        latestEval = node.eval(currentAddress, expectedBytes, env);
        if (latestEval.isRight()) {
            node.remove().ifPresent(p -> p.addChild(latestEval.right));
        } else {
            needMorePassThings.add(node);
        }
        currentAddress += expectedBytes;
    }
}
