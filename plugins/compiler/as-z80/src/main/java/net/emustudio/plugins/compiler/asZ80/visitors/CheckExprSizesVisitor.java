package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDB;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDS;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDW;
import net.emustudio.plugins.compiler.asZ80.ast.instr.*;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoMacroArgument;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoOrg;

import static net.emustudio.plugins.compiler.asZ80.CompileError.expressionIsBiggerThanExpected;
import static net.emustudio.plugins.compiler.asZ80.CompileError.valueOutOfBounds;

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
    public void visit(Instr node) {
        expectedBytes = node.hasRelativeAddress() ? 1 : 0;
        visitChildren(node);
    }

    @Override
    public void visit(InstrCB node) {
        expectedBytes = 0;
        visitChildren(node);
    }

    @Override
    public void visit(InstrED node) {
        expectedBytes = 0;
        visitChildren(node);
    }

    @Override
    public void visit(InstrXD node) {
        expectedBytes = 0;
        visitChildren(node);
    }

    @Override
    public void visit(InstrXDCB node) {
        expectedBytes = 0;
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
        int value = node.value < 0 ? ((~node.value) * 2) : node.value;
        if (expectedBytes > 0) {
            int wasBits = (int) Math.floor(Math.log10(Math.abs(value)) / Math.log10(2)) + 1;
            int wasBytes = (int) Math.ceil(wasBits / 8.0);

            if (wasBytes > expectedBytes) {
                error(expressionIsBiggerThanExpected(node, expectedBytes, wasBytes));
            }
        } else {
            node.getMaxValue().ifPresent(maxValue -> {
                if (value > maxValue) {
                    error(valueOutOfBounds(node, 0, maxValue));
                }
            });
        }
    }
}
