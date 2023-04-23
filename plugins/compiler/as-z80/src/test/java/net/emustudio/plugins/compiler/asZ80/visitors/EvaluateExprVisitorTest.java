/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.Program;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDB;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDS;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDW;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprCurrentAddress;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprId;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprInfix;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.asZ80.ast.instr.Instr;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.*;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static net.emustudio.plugins.compiler.asZ80.CompileError.ERROR_AMBIGUOUS_EXPRESSION;
import static net.emustudio.plugins.compiler.asZ80.Utils.assertTrees;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EvaluateExprVisitorTest {
    private final static SourceCodePosition POSITION = new SourceCodePosition(0, 0, "");


    @Test
    public void testEvaluateDB() {
        Program program = new Program("");
        program
                .addChild(new DataDB(POSITION)
                        .addChild(new ExprInfix(POSITION, OP_ADD)
                                .addChild(new ExprNumber(POSITION, 1))
                                .addChild(new ExprNumber(POSITION, 2)))
                        .addChild(new ExprNumber(POSITION, 'h'))
                        .addChild(new ExprNumber(POSITION, 'e'))
                        .addChild(new ExprNumber(POSITION, 'l'))
                        .addChild(new ExprNumber(POSITION, 'l'))
                        .addChild(new ExprNumber(POSITION, 'o'))
                );

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrees(
                new Program("")
                        .addChild(new DataDB(POSITION)
                                .addChild(new Evaluated(POSITION, 3))
                                .addChild(new Evaluated(POSITION, 'h'))
                                .addChild(new Evaluated(POSITION, 'e'))
                                .addChild(new Evaluated(POSITION, 'l'))
                                .addChild(new Evaluated(POSITION, 'l'))
                                .addChild(new Evaluated(POSITION, 'o'))),
                program
        );
    }

    @Test
    public void testEvaluateDW() {
        Program program = new Program("");
        program
                .addChild(new DataDW(POSITION)
                        .addChild(new ExprInfix(POSITION, OP_ADD)
                                .addChild(new ExprNumber(POSITION, 1))
                                .addChild(new ExprNumber(POSITION, 2))))
                .addChild(new DataDW(POSITION)
                        .addChild(new ExprNumber(POSITION, 0)));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrees(
                new Program("")
                        .addChild(new DataDW(POSITION)
                                .addChild(new Evaluated(POSITION, 3)))
                        .addChild(new DataDW(POSITION)
                                .addChild(new Evaluated(POSITION, 0))),
                program
        );
        assertEquals(0, program.getChild(0).getAddress());
        assertEquals(2, program.getChild(1).getAddress());
    }

    @Test
    public void testEvaluateDS() {
        Program program = new Program("");
        program
                .addChild(new DataDS(POSITION)
                        .addChild(new ExprInfix(POSITION, OP_ADD)
                                .addChild(new ExprNumber(POSITION, 1))
                                .addChild(new ExprNumber(POSITION, 2))))
                .addChild(new DataDB(POSITION)
                        .addChild(new ExprNumber(POSITION, 0)));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrees(
                new Program("")
                        .addChild(new DataDS(POSITION)
                                .addChild(new Evaluated(POSITION, 3)))
                        .addChild(new DataDB(POSITION)
                                .addChild(new Evaluated(POSITION, 0))),
                program
        );
        assertEquals(0, program.getChild(0).getAddress());
        assertEquals(3, program.getChild(1).getAddress());
    }

    @Test
    public void testEvaluateDSambiguous() {
        Program program = new Program("");
        program
                .addChild(new DataDS(POSITION)
                        .addChild(new ExprId(POSITION, "label")))
                .addChild(new PseudoLabel(POSITION, "label"));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_AMBIGUOUS_EXPRESSION));
    }

    @Test
    public void testEvaluateDSconstReference() {
        Program program = new Program("");
        program
                .addChild(new DataDS(POSITION)
                        .addChild(new ExprId(POSITION, "label")))
                .addChild(new PseudoEqu(POSITION, "label")
                        .addChild(new ExprNumber(POSITION, 5)));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());

        Optional<Evaluated> label = program.env().get("label");
        assertTrue(label.isPresent());
        assertEquals(0, label.get().getAddress());

        assertTrees(
                new Program("")
                        .addChild(new DataDS(POSITION)
                                .addChild(new Evaluated(POSITION, 5))),
                program
        );
    }

    @Test
    public void testEvaluateEQUfivePasses() {
        Program program = new Program("");
        program
                .addChild(new PseudoEqu(POSITION, "one")
                        .addChild(new ExprId(POSITION, "two")))
                .addChild(new PseudoEqu(POSITION, "two")
                        .addChild(new ExprId(POSITION, "three")))
                .addChild(new PseudoEqu(POSITION, "three")
                        .addChild(new ExprId(POSITION, "four")))
                .addChild(new PseudoEqu(POSITION, "four")
                        .addChild(new ExprId(POSITION, "five")))
                .addChild(new PseudoEqu(POSITION, "five")
                        .addChild(new ExprCurrentAddress(POSITION)));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
        assertTrees(new Program(""), program);

        List<String> constants = List.of("one", "two", "three", "four", "five");
        for (String c : constants) {
            Optional<Evaluated> constant = program.env().get(c);
            assertTrue(constant.isPresent());
            assertEquals(0, constant.get().getAddress());
            assertEquals(0, constant.get().value);
        }
    }

    @Test
    public void testEvaluateIFwithForwardConst() {
        Program program = new Program("");
        program
                .addChild(new Instr(POSITION, OPCODE_LD, 0, 7, 6)
                        .setSizeBytes(2)
                        .addChild(new ExprId(POSITION, "const")))
                .addChild(new PseudoIf(POSITION)
                        .addChild(new PseudoIfExpression(POSITION)
                                .addChild(new ExprInfix(POSITION, OP_EQUAL)
                                        .addChild(new ExprCurrentAddress(POSITION))
                                        .addChild(new ExprNumber(POSITION, 2))))
                        .addChild(new Instr(POSITION, OPCODE_RST, 3, 0, 7)
                                .setSizeBytes(1)
                                .addChild(new ExprNumber(POSITION, 0))))
                .addChild(new PseudoEqu(POSITION, "const")
                        .addChild(new ExprInfix(POSITION, OP_ADD)
                                .addChild(new ExprCurrentAddress(POSITION))
                                .addChild(new ExprCurrentAddress(POSITION))));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrees(
                new Program("")
                        .addChild(new Instr(POSITION, OPCODE_LD, 0, 7, 6)
                                .addChild(new Evaluated(POSITION, 6)))
                        .addChild(new Instr(POSITION, OPCODE_RST, 3, 0, 7)
                                .addChild(new Evaluated(POSITION, 0))),
                program
        );
        assertEquals(0, program.getChild(0).getAddress());
        assertEquals(2, program.getChild(1).getAddress());
    }

    @Test
    public void testEvaluateIFwithForwardAddressReference() {
        Program program = new Program("");
        program
                .addChild(new PseudoIf(POSITION)
                        .addChild(new PseudoIfExpression(POSITION)
                                .addChild(new ExprInfix(POSITION, OP_EQUAL)
                                        .addChild(new ExprCurrentAddress(POSITION))
                                        .addChild(new ExprId(POSITION, "const"))))
                        .addChild(new Instr(POSITION, OPCODE_RST, 3, 0, 7)
                                .addChild(new ExprNumber(POSITION, 0))))
                .addChild(new PseudoEqu(POSITION, "const")
                        .addChild(new ExprInfix(POSITION, OP_ADD)
                                .addChild(new ExprCurrentAddress(POSITION))
                                .addChild(new ExprCurrentAddress(POSITION))));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_AMBIGUOUS_EXPRESSION));
    }

    @Test
    public void testEvaluateIFexcludeBlock() {
        Program program = new Program("");
        program
                .addChild(new PseudoIf(POSITION)
                        .addChild(new PseudoIfExpression(POSITION)
                                .addChild(new ExprNumber(POSITION, 0)))
                        .addChild(new Instr(POSITION, OPCODE_RST, 3, 0, 7)
                                .addChild(new ExprNumber(POSITION, 0))));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrees(new Program(""), program);
    }

    @Test
    public void testEvaluateSETforwardTwoTimes() {
        Program program = new Program("");
        program
                .addChild(new Instr(POSITION, OPCODE_LD, 0, 7, 6)
                        .addChild(new ExprId(POSITION, "const")))
                .addChild(new PseudoVar(POSITION, "const")
                        .addChild(new ExprNumber(POSITION, 1)))
                .addChild(new Instr(POSITION, OPCODE_LD, 0, 0, 6)
                        .addChild(new ExprId(POSITION, "const")))
                .addChild(new PseudoVar(POSITION, "const")
                        .addChild(new ExprNumber(POSITION, 2)));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrees(
                new Program("")
                        .addChild(new Instr(POSITION, OPCODE_LD, 0, 7, 6)
                                .addChild(new Evaluated(POSITION, 1)))
                        .addChild(new PseudoVar(POSITION, "const")
                                .addChild(new Evaluated(POSITION, 1)))
                        .addChild(new Instr(POSITION, OPCODE_LD, 0, 0, 6)
                                .addChild(new Evaluated(POSITION, 1)))
                        .addChild(new PseudoVar(POSITION, "const")
                                .addChild(new Evaluated(POSITION, 2))),
                program
        );
    }

    @Test
    public void testEvaluateSETforwardMoreTimes() {
        Program program = new Program("");
        program
                .addChild(new DataDB(POSITION)
                        .addChild(new ExprId(POSITION, "id")))
                .addChild(new PseudoVar(POSITION, "id")
                        .addChild(new ExprId(POSITION, "const")))
                .addChild(new PseudoVar(POSITION, "id")
                        .addChild(new ExprNumber(POSITION, 2)))
                .addChild(new PseudoEqu(POSITION, "const")
                        .addChild(new ExprNumber(POSITION, 1)));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrees(
                new Program("")
                        .addChild(new DataDB(POSITION)
                                .addChild(new Evaluated(POSITION, 1)))
                        .addChild(new PseudoVar(POSITION, "id")
                                .addChild(new Evaluated(POSITION, 1)))
                        .addChild(new PseudoVar(POSITION, "id")
                                .addChild(new Evaluated(POSITION, 2))),
                program
        );
    }

    @Test
    public void testTwoSETthenReference() {
        Program program = new Program("");
        program
                .addChild(new PseudoVar(POSITION, "id")
                        .addChild(new ExprId(POSITION, "const")))
                .addChild(new PseudoVar(POSITION, "id")
                        .addChild(new ExprNumber(POSITION, 2)))
                .addChild(new PseudoEqu(POSITION, "const")
                        .addChild(new ExprNumber(POSITION, 1)))
                .addChild(new DataDB(POSITION)
                        .addChild(new ExprId(POSITION, "id")));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrees(
                new Program("")
                        .addChild(new PseudoVar(POSITION, "id")
                                .addChild(new Evaluated(POSITION, 1)))
                        .addChild(new PseudoVar(POSITION, "id")
                                .addChild(new Evaluated(POSITION, 2)))
                        .addChild(new DataDB(POSITION)
                                .addChild(new Evaluated(POSITION, 2))),
                program
        );
    }

    @Test
    public void testEvaluateLABEL() {
        Program program = new Program("");
        program
                .addChild(new DataDB(POSITION)
                        .addChild(new ExprInfix(POSITION, OP_ADD)
                                .addChild(new ExprCurrentAddress(POSITION))
                                .addChild(new ExprId(POSITION, "label")))
                        .addChild(new ExprInfix(POSITION, OP_ADD)
                                .addChild(new ExprCurrentAddress(POSITION))
                                .addChild(new ExprId(POSITION, "label")))
                        .addChild(new ExprInfix(POSITION, OP_ADD)
                                .addChild(new ExprCurrentAddress(POSITION))
                                .addChild(new ExprId(POSITION, "label"))))
                .addChild(new PseudoLabel(POSITION, "label"));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrees(
                new Program("")
                        .addChild(new DataDB(POSITION)
                                .addChild(new Evaluated(POSITION, 3))
                                .addChild(new Evaluated(POSITION, 4))
                                .addChild(new Evaluated(POSITION, 5))),
                program
        );
    }

    @Test
    public void testEvaluateMacroCalls() {
        Program program = new Program("");
        program
                .addChild(new PseudoLabel(POSITION, "label"))
                .addChild(new PseudoMacroCall(POSITION, "x")
                        .addChild(new PseudoMacroArgument(POSITION)
                                .addChild(new ExprId(POSITION, "addr"))
                                .addChild(new ExprId(POSITION, "label")))
                        .addChild(new Instr(POSITION, OPCODE_LD, 0, 0, 1)
                                .addChild(new ExprId(POSITION, "addr"))));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrees(
                new Program("")
                        .addChild(new PseudoMacroCall(POSITION, "x")
                                .addChild(new PseudoMacroArgument(POSITION)
                                        .addChild(new ExprId(POSITION, "addr"))
                                        .addChild(new Evaluated(POSITION, 0)))
                                .addChild(new Instr(POSITION, OPCODE_LD, 0, 0, 1)
                                        .addChild(new Evaluated(POSITION, 0)))),
                program
        );
    }

    @Test
    public void testEvaluateMacroCallAmbiguous() {
        Program program = new Program("");
        program
                .addChild(new PseudoMacroCall(POSITION, "x")
                        .addChild(new PseudoMacroArgument(POSITION)
                                .addChild(new ExprId(POSITION, "label"))
                                .addChild(new ExprId(POSITION, "addr")))
                        .addChild(new Instr(POSITION, OPCODE_LD, 0, 0, 1)
                                .addChild(new ExprId(POSITION, "addr"))))
                .addChild(new PseudoLabel(POSITION, "label"));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_AMBIGUOUS_EXPRESSION));
    }

    @Test
    public void testEvaluateMacroScopedArguments() {
        Program program = new Program("");
        program
                .addChild(new PseudoMacroCall(POSITION, "x")
                        .addChild(new PseudoMacroArgument(POSITION)
                                .addChild(new ExprId(POSITION, "arg"))
                                .addChild(new ExprNumber(POSITION, 0)))
                        .addChild(new Instr(POSITION, OPCODE_LD, 0, 0, 1)
                                .addChild(new ExprId(POSITION, "arg")))
                        .addChild(new PseudoMacroCall(POSITION, "y")
                                .addChild(new PseudoMacroArgument(POSITION)
                                        .addChild(new ExprId(POSITION, "arg"))
                                        .addChild(new ExprNumber(POSITION, 1)))
                                .addChild(new Instr(POSITION, OPCODE_LD, 0, 0, 1)
                                        .addChild(new ExprId(POSITION, "arg"))))
                        .addChild(new Instr(POSITION, OPCODE_LD, 0, 0, 1)
                                .addChild(new ExprId(POSITION, "arg"))));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrees(
                new Program("")
                        .addChild(new PseudoMacroCall(POSITION, "x")
                                .addChild(new PseudoMacroArgument(POSITION)
                                        .addChild(new ExprId(POSITION, "arg"))
                                        .addChild(new Evaluated(POSITION, 0)))
                                .addChild(new Instr(POSITION, OPCODE_LD, 0, 0, 1)
                                        .addChild(new Evaluated(POSITION, 0)))
                                .addChild(new PseudoMacroCall(POSITION, "y")
                                        .addChild(new PseudoMacroArgument(POSITION)
                                                .addChild(new ExprId(POSITION, "arg"))
                                                .addChild(new Evaluated(POSITION, 1)))
                                        .addChild(new Instr(POSITION, OPCODE_LD, 0, 0, 1)
                                                .addChild(new Evaluated(POSITION, 1))))
                                .addChild(new Instr(POSITION, OPCODE_LD, 0, 0, 1)
                                        .addChild(new Evaluated(POSITION, 0)))),
                program
        );
    }

    @Test
    public void testLabelKeepsChildren() {
        Program program = new Program("");
        program
                .addChild(new PseudoLabel(POSITION, "label")
                        .addChild(new Instr(POSITION, OPCODE_RET, 3, 1, 1)));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrees(
                new Program("").addChild(new Instr(POSITION, OPCODE_RET, 3, 1, 1)),
                program
        );
    }
}
