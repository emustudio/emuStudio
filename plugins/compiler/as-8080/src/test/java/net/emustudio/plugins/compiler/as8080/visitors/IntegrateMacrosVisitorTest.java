package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprId;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprInfix;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrRegExpr;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.*;
import org.junit.Test;

import static net.emustudio.plugins.compiler.as8080.As8080Parser.*;
import static net.emustudio.plugins.compiler.as8080.Utils.assertTrees;

public class IntegrateMacrosVisitorTest {

    @Test
    public void testMacroArgumentsAreExpanded() {
        Node program = new Program()
            .addChild(new PseudoMacroCall(0, 0, "x")
                .addChild(new PseudoMacroArgument()
                    .addChild(new ExprNumber(0, 0, 1)))
                .addChild(new PseudoMacroArgument()
                    .addChild(new ExprNumber(0, 0, 2)))
                .addChild(new PseudoMacroArgument()
                    .addChild(new ExprNumber(0, 0, 3)))
                .addChild(new PseudoMacroDef(0, 0, "x")
                    .addChild(new PseudoMacroParameter()
                        .addChild(new ExprId(0, 0, "q")))
                    .addChild(new PseudoMacroParameter()
                        .addChild(new ExprId(0, 0, "r")))
                    .addChild(new PseudoMacroParameter()
                        .addChild(new ExprId(0, 0, "t")))
                    .addChild(new InstrRegExpr(0,0,OPCODE_MVI, REG_A)
                        .addChild(new ExprId(0,0,"q")))
                    .addChild(new PseudoEqu(0,0,"uu")
                        .addChild(new ExprInfix(0,0, OP_ADD)
                            .addChild(new ExprId(0,0,"r"))
                            .addChild(new ExprId(0,0,"t"))))));

        IntegrateMacrosVisitor visitor = new IntegrateMacrosVisitor();
        visitor.visit(program);

        assertTrees(new Program()
            .addChild(new InstrRegExpr(0,0,OPCODE_MVI, REG_A)
                .addChild(new ExprNumber(0,0,1)))
            .addChild(new PseudoEqu(0,0, "uu")
                .addChild(new ExprInfix(0,0,OP_ADD)
                    .addChild(new ExprNumber(0,0,2))
                    .addChild(new ExprNumber(0,0,3)))),
            program
        );
    }
}
