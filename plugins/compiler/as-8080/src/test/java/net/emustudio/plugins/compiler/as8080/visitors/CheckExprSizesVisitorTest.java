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
package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDS;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDW;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprId;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrExpr;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrRegExpr;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrRegPairExpr;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoMacroArgument;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoMacroCall;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoOrg;
import org.junit.Test;

import static net.emustudio.plugins.compiler.as8080.As8080Parser.*;
import static net.emustudio.plugins.compiler.as8080.CompileError.ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED;
import static net.emustudio.plugins.compiler.as8080.Utils.assertTrees;
import static org.junit.Assert.assertTrue;

public class CheckExprSizesVisitorTest {
    private final static SourceCodePosition POSITION = new SourceCodePosition(0, 0, "");

    @Test
    public void testDBoneByte() {
        Program program = new Program("");
        program
                .addChild(new DataDB(POSITION)
                        .addChild(new Evaluated(POSITION, 0xFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testDBtwoBytes() {
        Program program = new Program("");
        program
                .addChild(new DataDB(POSITION)
                        .addChild(new Evaluated(POSITION, 0xFF))
                        .addChild(new Evaluated(POSITION, 0x100))); // bad size

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testDWtwoBytes() {
        Program program = new Program("");
        program
                .addChild(new DataDW(POSITION)
                        .addChild(new Evaluated(POSITION, 0xFFFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testDWthreeBytes() {
        Program program = new Program("");
        program
                .addChild(new DataDW(POSITION)
                        .addChild(new Evaluated(POSITION, 0xFFFF))
                        .addChild(new Evaluated(POSITION, 0x10000)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testDStwoBytes() {
        Program program = new Program("");
        program
                .addChild(new DataDS(POSITION)
                        .addChild(new Evaluated(POSITION, 0xFFFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testDSthreeBytes() {
        Program program = new Program("");
        program
                .addChild(new DataDS(POSITION)
                        .addChild(new Evaluated(POSITION, 0x10000)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testInstrExprTwoBytes() {
        Program program = new Program("");
        program
                .addChild(new InstrExpr(POSITION, OPCODE_ADI)
                        .addChild(new Evaluated(POSITION, 0xFF00)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testInstrExprThreeBytes() {
        Program program = new Program("");
        program
                .addChild(new InstrExpr(POSITION, OPCODE_JMP)
                        .addChild(new Evaluated(POSITION, 0xFF000)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testInstrRegExprOneByte() {
        Program program = new Program("");
        program
                .addChild(new InstrRegExpr(POSITION, OPCODE_MVI, REG_A)
                        .addChild(new Evaluated(POSITION, 0xFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testInstrRegExprTwoBytes() {
        Program program = new Program("");
        program
                .addChild(new InstrRegExpr(POSITION, OPCODE_MVI, REG_A)
                        .addChild(new Evaluated(POSITION, 0x100))); // bad size

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testInstrRegPairExprTwoBytes() {
        Program program = new Program("");
        program
                .addChild(new InstrRegPairExpr(POSITION, OPCODE_LXI, REG_B)
                        .addChild(new Evaluated(POSITION, 0xFFFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testInstrRegPairExprThreeBytes() {
        Program program = new Program("");
        program
                .addChild(new InstrRegPairExpr(POSITION, OPCODE_LXI, REG_B)
                        .addChild(new Evaluated(POSITION, 0x10000))); // bad size

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testPseudoOrgTwoBytes() {
        Program program = new Program("");
        program
                .addChild(new PseudoOrg(POSITION)
                        .addChild(new Evaluated(POSITION, 0xFFFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testPseudoOrgThreeBytes() {
        Program program = new Program("");
        program
                .addChild(new PseudoOrg(POSITION)
                        .addChild(new Evaluated(POSITION, 0x10000))); // bad size

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testMacroArgumentsAreRemoved() {
        Program program = new Program("");
        program
                .addChild(new PseudoMacroCall(POSITION, "x")
                        .addChild(new PseudoMacroArgument(POSITION)
                                .addChild(new ExprId(POSITION, "arg"))
                                .addChild(new Evaluated(POSITION, 0)))
                        .addChild(new InstrRegPairExpr(POSITION, OPCODE_LXI, REG_B)
                                .addChild(new Evaluated(POSITION, 0)))
                        .addChild(new PseudoMacroCall(POSITION, "y")
                                .addChild(new PseudoMacroArgument(POSITION)
                                        .addChild(new ExprId(POSITION, "arg"))
                                        .addChild(new Evaluated(POSITION, 1)))
                                .addChild(new InstrRegPairExpr(POSITION, OPCODE_LXI, REG_B)
                                        .addChild(new Evaluated(POSITION, 1))))
                        .addChild(new InstrRegPairExpr(POSITION, OPCODE_LXI, REG_B)
                                .addChild(new Evaluated(POSITION, 0))));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrees(
                new Program("")
                        .addChild(new PseudoMacroCall(POSITION, "x")
                                .addChild(new InstrRegPairExpr(POSITION, OPCODE_LXI, REG_B)
                                        .addChild(new Evaluated(POSITION, 0)))
                                .addChild(new PseudoMacroCall(POSITION, "y")
                                        .addChild(new InstrRegPairExpr(POSITION, OPCODE_LXI, REG_B)
                                                .addChild(new Evaluated(POSITION, 1))))
                                .addChild(new InstrRegPairExpr(POSITION, OPCODE_LXI, REG_B)
                                        .addChild(new Evaluated(POSITION, 0)))),
                program
        );
    }
}
