package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.ast.Program;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprId;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprInfix;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.asZ80.ast.instr.InstrRegExpr;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.*;
import org.junit.Test;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static net.emustudio.plugins.compiler.asZ80.CompileError.ERROR_MACRO_ARGUMENTS_DO_NOT_MATCH;
import static net.emustudio.plugins.compiler.asZ80.Utils.assertTrees;
import static org.junit.Assert.assertTrue;

public class SortMacroArgumentsVisitorTest {

    @Test
    public void testMacroArgumentsAreConnectedWithIds() {
        Node program = new Program()
            .addChild(new PseudoMacroCall(0, 0, "x")
                .addChild(new PseudoMacroArgument(0, 0)
                    .addChild(new ExprNumber(0, 0, 1)))
                .addChild(new PseudoMacroArgument(0, 0)
                    .addChild(new ExprNumber(0, 0, 2)))
                .addChild(new PseudoMacroArgument(0, 0)
                    .addChild(new ExprNumber(0, 0, 3)))
                .addChild(new PseudoMacroDef(0, 0, "x")
                    .addChild(new PseudoMacroParameter(0, 0)
                        .addChild(new ExprId(0, 0, "q")))
                    .addChild(new PseudoMacroParameter(0, 0)
                        .addChild(new ExprId(0, 0, "r")))
                    .addChild(new PseudoMacroParameter(0, 0)
                        .addChild(new ExprId(0, 0, "t")))
                    .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, REG_A)
                        .addChild(new ExprId(0, 0, "q")))
                    .addChild(new PseudoEqu(0, 0, "uu")
                        .addChild(new ExprInfix(0, 0, OP_ADD)
                            .addChild(new ExprId(0, 0, "r"))
                            .addChild(new ExprId(0, 0, "t"))))));

        SortMacroArgumentsVisitor visitor = new SortMacroArgumentsVisitor();
        visitor.visit(program);

        assertTrees(new Program()
                .addChild(new PseudoMacroCall(0, 0, "x")
                    .addChild(new PseudoMacroArgument(0, 0)
                        .addChild(new ExprId(0, 0, "q"))
                        .addChild(new ExprNumber(0, 0, 1)))
                    .addChild(new PseudoMacroArgument(0, 0)
                        .addChild(new ExprId(0, 0, "r"))
                        .addChild(new ExprNumber(0, 0, 2)))
                    .addChild(new PseudoMacroArgument(0, 0)
                        .addChild(new ExprId(0, 0, "t"))
                        .addChild(new ExprNumber(0, 0, 3)))
                    .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, REG_A)
                        .addChild(new ExprId(0, 0, "q")))
                    .addChild(new PseudoEqu(0, 0, "uu")
                        .addChild(new ExprInfix(0, 0, OP_ADD)
                            .addChild(new ExprId(0, 0, "r"))
                            .addChild(new ExprId(0, 0, "t"))))),
            program
        );
    }

    @Test
    public void testMultipleMacroCalls() {
        Node program = new Program()
            .addChild(new PseudoMacroCall(0, 0, "x")
                .addChild(new PseudoMacroArgument(0, 0)
                    .addChild(new ExprNumber(0, 0, 1)))
                .addChild(new PseudoMacroDef(0, 0, "x")
                    .addChild(new PseudoMacroParameter(0, 0)
                        .addChild(new ExprId(0, 0, "q")))))
            .addChild(new PseudoMacroCall(0, 0, "x")
                .addChild(new PseudoMacroArgument(0, 0)
                    .addChild(new ExprNumber(0, 0, 2)))
                .addChild(new PseudoMacroDef(0, 0, "x")
                    .addChild(new PseudoMacroParameter(0, 0)
                        .addChild(new ExprId(0, 0, "q")))));

        SortMacroArgumentsVisitor visitor = new SortMacroArgumentsVisitor();
        visitor.visit(program);

        assertTrees(new Program()
                .addChild(new PseudoMacroCall(0, 0, "x")
                    .addChild(new PseudoMacroArgument(0, 0)
                        .addChild(new ExprId(0, 0, "q"))
                        .addChild(new ExprNumber(0, 0, 1))))
                .addChild(new PseudoMacroCall(0, 0, "x")
                    .addChild(new PseudoMacroArgument(0, 0)
                        .addChild(new ExprId(0, 0, "q"))
                        .addChild(new ExprNumber(0, 0, 2)))),
            program
        );
    }

    @Test
    public void testNestedMacroCallWithSameNamedArgs() {
        Node program = new Program()
            .addChild(new PseudoMacroCall(0, 0, "x")
                .addChild(new PseudoMacroArgument(0, 0)
                    .addChild(new ExprNumber(0, 0, 1)))
                .addChild(new PseudoMacroDef(0, 0, "x")
                    .addChild(new PseudoMacroParameter(0, 0)
                        .addChild(new ExprId(0, 0, "q")))
                    .addChild(new PseudoMacroCall(0, 0, "y")
                        .addChild(new PseudoMacroArgument(0, 0)
                            .addChild(new ExprNumber(0, 0, 3)))
                        .addChild(new PseudoMacroDef(0, 0, "y")
                            .addChild(new PseudoMacroParameter(0, 0)
                                .addChild(new ExprId(0, 0, "q")))))));

        SortMacroArgumentsVisitor visitor = new SortMacroArgumentsVisitor();
        visitor.visit(program);

        assertTrees(
            new Program()
                .addChild(new PseudoMacroCall(0, 0, "x")
                    .addChild(new PseudoMacroArgument(0, 0)
                        .addChild(new ExprId(0, 0, "q"))
                        .addChild(new ExprNumber(0, 0, 1)))
                    .addChild(new PseudoMacroCall(0, 0, "y")
                        .addChild(new PseudoMacroArgument(0, 0)
                            .addChild(new ExprId(0, 0, "q"))
                            .addChild(new ExprNumber(0, 0, 3))))),
            program
        );
    }

    @Test
    public void testMoreMacroArgumentsThanParameters() {
        Program program = new Program();
        program
            .addChild(new PseudoMacroCall(0, 0, "x")
                .addChild(new PseudoMacroArgument(0, 0)
                    .addChild(new ExprNumber(0, 0, 1)))
                .addChild(new PseudoMacroArgument(0, 0)
                    .addChild(new ExprNumber(0, 0, 2)))
                .addChild(new PseudoMacroDef(0, 0, "x")
                    .addChild(new PseudoMacroParameter(0, 0)
                        .addChild(new ExprId(0, 0, "q")))));

        SortMacroArgumentsVisitor visitor = new SortMacroArgumentsVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_MACRO_ARGUMENTS_DO_NOT_MATCH));
    }

    @Test
    public void testMoreMacroParametersThanArguments() {
        Program program = new Program();
        program
            .addChild(new PseudoMacroCall(0, 0, "x")
                .addChild(new PseudoMacroArgument(0, 0)
                    .addChild(new ExprNumber(0, 0, 1)))
                .addChild(new PseudoMacroDef(0, 0, "x")
                    .addChild(new PseudoMacroParameter(0, 0)
                        .addChild(new ExprId(0, 0, "q")))
                    .addChild(new PseudoMacroParameter(0, 0)
                        .addChild(new ExprId(0, 0, "r")))));

        SortMacroArgumentsVisitor visitor = new SortMacroArgumentsVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_MACRO_ARGUMENTS_DO_NOT_MATCH));
    }
}
