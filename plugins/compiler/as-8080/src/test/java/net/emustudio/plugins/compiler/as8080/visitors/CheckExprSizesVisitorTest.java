/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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

    @Test
    public void testDBoneByte() {
        Program program = new Program();
        program
            .addChild(new DataDB(0, 0)
                .addChild(new Evaluated(0,0, 0xFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testDBtwoBytes() {
        Program program = new Program();
        program
            .addChild(new DataDB(0, 0)
                .addChild(new Evaluated(0,0, 0xFF))
                .addChild(new Evaluated(0,0, 0x100))); // bad size

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testDWtwoBytes() {
        Program program = new Program();
        program
            .addChild(new DataDW(0, 0)
                .addChild(new Evaluated(0,0, 0xFFFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testDWthreeBytes() {
        Program program = new Program();
        program
            .addChild(new DataDW(0, 0)
                .addChild(new Evaluated(0,0, 0xFFFF))
                .addChild(new Evaluated(0,0, 0x10000)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testDStwoBytes() {
        Program program = new Program();
        program
            .addChild(new DataDS(0, 0)
                .addChild(new Evaluated(0,0, 0xFFFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testDSthreeBytes() {
        Program program = new Program();
        program
            .addChild(new DataDS(0, 0)
                .addChild(new Evaluated(0,0, 0x10000)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testInstrExprTwoBytes() {
        Program program = new Program();
        program
            .addChild(new InstrExpr(0, 0, OPCODE_ADI)
                .addChild(new Evaluated(0,0, 0xFF00)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testInstrExprThreeBytes() {
        Program program = new Program();
        program
            .addChild(new InstrExpr(0, 0, OPCODE_JMP)
                .addChild(new Evaluated(0,0, 0xFF000)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testInstrRegExprOneByte() {
        Program program = new Program();
        program
            .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, REG_A)
                .addChild(new Evaluated(0,0, 0xFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testInstrRegExprTwoBytes() {
        Program program = new Program();
        program
            .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, REG_A)
                .addChild(new Evaluated(0,0, 0x100))); // bad size

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testInstrRegPairExprTwoBytes() {
        Program program = new Program();
        program
            .addChild(new InstrRegPairExpr(0, 0, OPCODE_LXI, REG_B)
                .addChild(new Evaluated(0,0, 0xFFFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testInstrRegPairExprThreeBytes() {
        Program program = new Program();
        program
            .addChild(new InstrRegPairExpr(0, 0, OPCODE_LXI, REG_B)
                .addChild(new Evaluated(0,0, 0x10000))); // bad size

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testPseudoOrgTwoBytes() {
        Program program = new Program();
        program
            .addChild(new PseudoOrg(0, 0)
                .addChild(new Evaluated(0,0, 0xFFFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testPseudoOrgThreeBytes() {
        Program program = new Program();
        program
            .addChild(new PseudoOrg(0, 0)
                .addChild(new Evaluated(0,0, 0x10000))); // bad size

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testMacroArgumentsAreRemoved() {
        Program program = new Program();
        program
            .addChild(new PseudoMacroCall(0, 0, "x")
                .addChild(new PseudoMacroArgument(0, 0)
                    .addChild(new ExprId(0, 0, "arg"))
                    .addChild(new Evaluated(0, 0, 0)))
                .addChild(new InstrRegPairExpr(0, 0, OPCODE_LXI, REG_B)
                    .addChild(new Evaluated(0, 0, 0)))
                .addChild(new PseudoMacroCall(0, 0, "y")
                    .addChild(new PseudoMacroArgument(0, 0)
                        .addChild(new ExprId(0, 0, "arg"))
                        .addChild(new Evaluated(0, 0, 1)))
                    .addChild(new InstrRegPairExpr(0, 0, OPCODE_LXI, REG_B)
                        .addChild(new Evaluated(0, 0, 1))))
                .addChild(new InstrRegPairExpr(0, 0, OPCODE_LXI, REG_B)
                    .addChild(new Evaluated(0, 0, 0))));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrees(
            new Program()
                .addChild(new PseudoMacroCall(0, 0, "x")
                    .addChild(new InstrRegPairExpr(0, 0, OPCODE_LXI, REG_B)
                        .addChild(new Evaluated(0, 0, 0)))
                    .addChild(new PseudoMacroCall(0, 0, "y")
                        .addChild(new InstrRegPairExpr(0, 0, OPCODE_LXI, REG_B)
                            .addChild(new Evaluated(0, 0, 1))))
                    .addChild(new InstrRegPairExpr(0, 0, OPCODE_LXI, REG_B)
                        .addChild(new Evaluated(0, 0, 0)))),
            program
        );
    }
}
