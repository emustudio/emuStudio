package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDS;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDW;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrRegExpr;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrRegPairExpr;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoOrg;

import static net.emustudio.plugins.compiler.as8080.CompileError.expressionIsBiggerThanExpected;

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
    public void visit(InstrRegExpr node) {
        expectedBytes = 1;
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
    public void visit(Evaluated node) {
        long maxNumber = Long.MAX_VALUE >>> (63 - expectedBytes * 8);
        if (node.value != (node.value & maxNumber)) {
            int wasBits = (int)Math.ceil(Math.log10(node.value) / Math.log10(2)) + 1;
            int wasBytes = wasBits / 8 + wasBits % 8;
            error(expressionIsBiggerThanExpected(node, expectedBytes, wasBytes));
        }
    }
}
