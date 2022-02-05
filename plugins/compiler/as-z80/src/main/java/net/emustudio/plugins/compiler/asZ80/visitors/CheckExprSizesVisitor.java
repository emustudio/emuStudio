package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDB;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDS;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDW;
import net.emustudio.plugins.compiler.asZ80.ast.instr.InstrExpr;
import net.emustudio.plugins.compiler.asZ80.ast.instr.InstrRegExpr;
import net.emustudio.plugins.compiler.asZ80.ast.instr.InstrRegPairExpr;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoMacroArgument;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoOrg;

import static net.emustudio.plugins.compiler.asZ80.CompileError.expressionIsBiggerThanExpected;

/**
 * Checks proper sizes of evaluated nodes
 */
public class CheckExprSizesVisitor extends NodeVisitor {
    private int expectedBytes;

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
        expectedBytes = 2;
        visitChildren(node);
    }

    @Override
    public void visit(InstrExpr node) {
        expectedBytes = node.getExprSizeBytes();
        if (expectedBytes == 1 && node.getChildrenCount() > 1) {
            error(expressionIsBiggerThanExpected(node, expectedBytes, node.getChildrenCount()));
        }
        visitChildren(node);
    }

    @Override
    public void visit(InstrRegExpr node) {
        expectedBytes = 1;
        if (node.getChildrenCount() > 1) {
            error(expressionIsBiggerThanExpected(node, expectedBytes, node.getChildrenCount()));
        }
        visitChildren(node);
    }

    @Override
    public void visit(InstrRegPairExpr node) {
        expectedBytes = 2;
        visitChildren(node);
    }

    @Override
    public void visit(PseudoOrg node) {
        expectedBytes = 2;
        visitChildren(node);
    }

    @Override
    public void visit(PseudoMacroArgument node) {
        node.remove();
    }

    @Override
    public void visit(Evaluated node) {
        if (expectedBytes > 0) {
            int value = node.value < 0 ? ((~node.value) * 2) : node.value;

            int wasBits = (int) Math.floor(Math.log10(Math.abs(value)) / Math.log10(2)) + 1;
            int wasBytes = (int) Math.ceil(wasBits / 8.0);

            if (wasBytes > expectedBytes) {
                error(expressionIsBiggerThanExpected(node, expectedBytes, wasBytes));
            }
        }
    }
}
