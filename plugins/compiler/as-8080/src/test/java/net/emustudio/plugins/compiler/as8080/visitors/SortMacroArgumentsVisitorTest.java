/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprId;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprInfix;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrRegExpr;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.*;
import org.junit.Test;

import static net.emustudio.plugins.compiler.as8080.As8080Parser.*;
import static net.emustudio.plugins.compiler.as8080.CompileError.ERROR_MACRO_ARGUMENTS_DO_NOT_MATCH;
import static net.emustudio.plugins.compiler.as8080.Utils.assertTrees;
import static org.junit.Assert.assertTrue;

public class SortMacroArgumentsVisitorTest {
    private final static SourceCodePosition POSITION = new SourceCodePosition(0, 0, "");

    @Test
    public void testMacroArgumentsAreConnectedWithIds() {
        Node program = new Program("")
                .addChild(new PseudoMacroCall(POSITION, "x")
                        .addChild(new PseudoMacroArgument(POSITION)
                                .addChild(new ExprNumber(POSITION, 1)))
                        .addChild(new PseudoMacroArgument(POSITION)
                                .addChild(new ExprNumber(POSITION, 2)))
                        .addChild(new PseudoMacroArgument(POSITION)
                                .addChild(new ExprNumber(POSITION, 3)))
                        .addChild(new PseudoMacroDef(POSITION, "x")
                                .addChild(new PseudoMacroParameter(POSITION)
                                        .addChild(new ExprId(POSITION, "q")))
                                .addChild(new PseudoMacroParameter(POSITION)
                                        .addChild(new ExprId(POSITION, "r")))
                                .addChild(new PseudoMacroParameter(POSITION)
                                        .addChild(new ExprId(POSITION, "t")))
                                .addChild(new InstrRegExpr(POSITION, OPCODE_MVI, REG_A)
                                        .addChild(new ExprId(POSITION, "q")))
                                .addChild(new PseudoEqu(POSITION, "uu")
                                        .addChild(new ExprInfix(POSITION, OP_ADD)
                                                .addChild(new ExprId(POSITION, "r"))
                                                .addChild(new ExprId(POSITION, "t"))))));

        SortMacroArgumentsVisitor visitor = new SortMacroArgumentsVisitor();
        visitor.visit(program);

        assertTrees(new Program("")
                        .addChild(new PseudoMacroCall(POSITION, "x")
                                .addChild(new PseudoMacroArgument(POSITION)
                                        .addChild(new ExprId(POSITION, "q"))
                                        .addChild(new ExprNumber(POSITION, 1)))
                                .addChild(new PseudoMacroArgument(POSITION)
                                        .addChild(new ExprId(POSITION, "r"))
                                        .addChild(new ExprNumber(POSITION, 2)))
                                .addChild(new PseudoMacroArgument(POSITION)
                                        .addChild(new ExprId(POSITION, "t"))
                                        .addChild(new ExprNumber(POSITION, 3)))
                                .addChild(new InstrRegExpr(POSITION, OPCODE_MVI, REG_A)
                                        .addChild(new ExprId(POSITION, "q")))
                                .addChild(new PseudoEqu(POSITION, "uu")
                                        .addChild(new ExprInfix(POSITION, OP_ADD)
                                                .addChild(new ExprId(POSITION, "r"))
                                                .addChild(new ExprId(POSITION, "t"))))),
                program
        );
    }

    @Test
    public void testMultipleMacroCalls() {
        Node program = new Program("")
                .addChild(new PseudoMacroCall(POSITION, "x")
                        .addChild(new PseudoMacroArgument(POSITION)
                                .addChild(new ExprNumber(POSITION, 1)))
                        .addChild(new PseudoMacroDef(POSITION, "x")
                                .addChild(new PseudoMacroParameter(POSITION)
                                        .addChild(new ExprId(POSITION, "q")))))
                .addChild(new PseudoMacroCall(POSITION, "x")
                        .addChild(new PseudoMacroArgument(POSITION)
                                .addChild(new ExprNumber(POSITION, 2)))
                        .addChild(new PseudoMacroDef(POSITION, "x")
                                .addChild(new PseudoMacroParameter(POSITION)
                                        .addChild(new ExprId(POSITION, "q")))));

        SortMacroArgumentsVisitor visitor = new SortMacroArgumentsVisitor();
        visitor.visit(program);

        assertTrees(new Program("")
                        .addChild(new PseudoMacroCall(POSITION, "x")
                                .addChild(new PseudoMacroArgument(POSITION)
                                        .addChild(new ExprId(POSITION, "q"))
                                        .addChild(new ExprNumber(POSITION, 1))))
                        .addChild(new PseudoMacroCall(POSITION, "x")
                                .addChild(new PseudoMacroArgument(POSITION)
                                        .addChild(new ExprId(POSITION, "q"))
                                        .addChild(new ExprNumber(POSITION, 2)))),
                program
        );
    }

    @Test
    public void testNestedMacroCallWithSameNamedArgs() {
        Node program = new Program("")
                .addChild(new PseudoMacroCall(POSITION, "x")
                        .addChild(new PseudoMacroArgument(POSITION)
                                .addChild(new ExprNumber(POSITION, 1)))
                        .addChild(new PseudoMacroDef(POSITION, "x")
                                .addChild(new PseudoMacroParameter(POSITION)
                                        .addChild(new ExprId(POSITION, "q")))
                                .addChild(new PseudoMacroCall(POSITION, "y")
                                        .addChild(new PseudoMacroArgument(POSITION)
                                                .addChild(new ExprNumber(POSITION, 3)))
                                        .addChild(new PseudoMacroDef(POSITION, "y")
                                                .addChild(new PseudoMacroParameter(POSITION)
                                                        .addChild(new ExprId(POSITION, "q")))))));

        SortMacroArgumentsVisitor visitor = new SortMacroArgumentsVisitor();
        visitor.visit(program);

        assertTrees(
                new Program("")
                        .addChild(new PseudoMacroCall(POSITION, "x")
                                .addChild(new PseudoMacroArgument(POSITION)
                                        .addChild(new ExprId(POSITION, "q"))
                                        .addChild(new ExprNumber(POSITION, 1)))
                                .addChild(new PseudoMacroCall(POSITION, "y")
                                        .addChild(new PseudoMacroArgument(POSITION)
                                                .addChild(new ExprId(POSITION, "q"))
                                                .addChild(new ExprNumber(POSITION, 3))))),
                program
        );
    }

    @Test
    public void testMoreMacroArgumentsThanParameters() {
        Program program = new Program("");
        program
                .addChild(new PseudoMacroCall(POSITION, "x")
                        .addChild(new PseudoMacroArgument(POSITION)
                                .addChild(new ExprNumber(POSITION, 1)))
                        .addChild(new PseudoMacroArgument(POSITION)
                                .addChild(new ExprNumber(POSITION, 2)))
                        .addChild(new PseudoMacroDef(POSITION, "x")
                                .addChild(new PseudoMacroParameter(POSITION)
                                        .addChild(new ExprId(POSITION, "q")))));

        SortMacroArgumentsVisitor visitor = new SortMacroArgumentsVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_MACRO_ARGUMENTS_DO_NOT_MATCH));
    }

    @Test
    public void testMoreMacroParametersThanArguments() {
        Program program = new Program("");
        program
                .addChild(new PseudoMacroCall(POSITION, "x")
                        .addChild(new PseudoMacroArgument(POSITION)
                                .addChild(new ExprNumber(POSITION, 1)))
                        .addChild(new PseudoMacroDef(POSITION, "x")
                                .addChild(new PseudoMacroParameter(POSITION)
                                        .addChild(new ExprId(POSITION, "q")))
                                .addChild(new PseudoMacroParameter(POSITION)
                                        .addChild(new ExprId(POSITION, "r")))));

        SortMacroArgumentsVisitor visitor = new SortMacroArgumentsVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_MACRO_ARGUMENTS_DO_NOT_MATCH));
    }
}
