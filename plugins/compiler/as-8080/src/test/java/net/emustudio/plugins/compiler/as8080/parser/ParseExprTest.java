/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080.parser;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprInfix;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprUnary;
import net.emustudio.plugins.compiler.as8080.ast.expr.InfixOperator;
import net.emustudio.plugins.compiler.as8080.ast.expr.UnaryOperator;
import org.junit.Test;

import static net.emustudio.plugins.compiler.as8080.As8080Parser.*;
import static net.emustudio.plugins.compiler.as8080.Utils.assertTrees;
import static net.emustudio.plugins.compiler.as8080.Utils.parseProgram;

public class ParseExprTest {
    private final static SourceCodePosition POSITION = new SourceCodePosition(0, 0, "");

    @Test
    public void testPrioritiesAddMul() {
        Program program = parseProgram("db 2+3*4");
        assertTrees(
                new Program("")
                        .addChild(new DataDB(POSITION)
                                .addChild(new ExprInfix(POSITION, InfixOperator.ADD)
                                        .addChild(new ExprNumber(POSITION, 2))
                                        .addChild(new ExprInfix(POSITION, InfixOperator.MULTIPLY)
                                                .addChild(new ExprNumber(POSITION, 3))
                                                .addChild(new ExprNumber(POSITION, 4))))),
                program
        );
    }

    @Test
    public void testPrioritiesMulAdd() {
        Program program = parseProgram("db 2*3+4");
        assertTrees(
                new Program("")
                        .addChild(new DataDB(POSITION)
                                .addChild(new ExprInfix(POSITION, InfixOperator.ADD)
                                        .addChild(new ExprInfix(POSITION, InfixOperator.MULTIPLY)
                                                .addChild(new ExprNumber(POSITION, 2))
                                                .addChild(new ExprNumber(POSITION, 3)))
                                        .addChild(new ExprNumber(POSITION, 4)))),
                program
        );
    }

    @Test
    public void testAssociativityPlusMinus() {
        Program program = parseProgram("db 2-3+4-9");
        assertTrees(
                new Program("")
                        .addChild(new DataDB(POSITION)
                                .addChild(new ExprInfix(POSITION, InfixOperator.SUBTRACT)
                                        .addChild(new ExprInfix(POSITION, InfixOperator.ADD)
                                                .addChild(new ExprInfix(POSITION, InfixOperator.SUBTRACT)
                                                        .addChild(new ExprNumber(POSITION, 2))
                                                        .addChild(new ExprNumber(POSITION, 3)))
                                                .addChild(new ExprNumber(POSITION, 4)))
                                        .addChild(new ExprNumber(POSITION, 9)))),
                program
        );
    }

    @Test
    public void testAssociativitMulDiv() {
        Program program = parseProgram("db 2/3*4/9");
        assertTrees(
                new Program("")
                        .addChild(new DataDB(POSITION)
                                .addChild(new ExprInfix(POSITION, InfixOperator.DIVIDE)
                                        .addChild(new ExprInfix(POSITION, InfixOperator.MULTIPLY)
                                                .addChild(new ExprInfix(POSITION, InfixOperator.DIVIDE)
                                                        .addChild(new ExprNumber(POSITION, 2))
                                                        .addChild(new ExprNumber(POSITION, 3)))
                                                .addChild(new ExprNumber(POSITION, 4)))
                                        .addChild(new ExprNumber(POSITION, 9)))),
                program
        );
    }

    @Test
    public void testPrecedencePlusMinusMulDivMod() {
        Program program = parseProgram("db 2+3*4-9/2 mod 3");
        assertTrees(
                new Program("")
                        .addChild(new DataDB(POSITION)
                                .addChild(new ExprInfix(POSITION, InfixOperator.SUBTRACT)
                                        .addChild(new ExprInfix(POSITION, InfixOperator.ADD)
                                                .addChild(new ExprNumber(POSITION, 2))
                                                .addChild(new ExprInfix(POSITION, InfixOperator.MULTIPLY)
                                                        .addChild(new ExprNumber(POSITION, 3))
                                                        .addChild(new ExprNumber(POSITION, 4))))
                                        .addChild(new ExprInfix(POSITION, InfixOperator.MOD)
                                                .addChild(new ExprInfix(POSITION, InfixOperator.DIVIDE)
                                                        .addChild(new ExprNumber(POSITION, 9))
                                                        .addChild(new ExprNumber(POSITION, 2)))
                                                .addChild(new ExprNumber(POSITION, 3))))),
                program
        );
    }

    @Test
    public void testAssociativityEqual() {
        Program program = parseProgram("db 1 + 2 + 2 = 5 = 5 = 6 - 1");
        assertTrees(
                new Program("")
                        .addChild(new DataDB(POSITION)
                                .addChild(new ExprInfix(POSITION, InfixOperator.EQUAL)
                                        .addChild(new ExprInfix(POSITION, InfixOperator.ADD) // 1 + 2 + 2 associates to left
                                                .addChild(new ExprInfix(POSITION, InfixOperator.ADD)
                                                        .addChild(new ExprNumber(POSITION, 1))
                                                        .addChild(new ExprNumber(POSITION, 2)))
                                                .addChild(new ExprNumber(POSITION, 2)))
                                        .addChild(new ExprInfix(POSITION, InfixOperator.EQUAL)
                                                .addChild(new ExprNumber(POSITION, 5)) // ... = 5 associates to right
                                                .addChild(new ExprInfix(POSITION, InfixOperator.EQUAL)
                                                        .addChild(new ExprNumber(POSITION, 5))
                                                        .addChild(new ExprInfix(POSITION, InfixOperator.SUBTRACT) // minus has > precedence than =
                                                                .addChild(new ExprNumber(POSITION, 6))
                                                                .addChild(new ExprNumber(POSITION, 1))))))),
                program
        );
    }

    @Test
    public void testAndMulXorDivNotPlusMinus() {
        Program program = parseProgram("db not 1 and 2 or 2 xor 5 = - 5 * 6 shl 4 - 1 shr 2");
        assertTrees(
                new Program("")
                        .addChild(new DataDB(POSITION)
                                .addChild(new ExprInfix(POSITION, InfixOperator.OR)
                                        .addChild(new ExprInfix(POSITION, InfixOperator.AND)
                                                .addChild(new ExprUnary(POSITION, UnaryOperator.NOT)
                                                        .addChild(new ExprNumber(POSITION, 1)))
                                                .addChild(new ExprNumber(POSITION, 2)))
                                        .addChild(new ExprInfix(POSITION, InfixOperator.XOR)
                                                .addChild(new ExprNumber(POSITION, 2))
                                                .addChild(new ExprInfix(POSITION, InfixOperator.EQUAL)
                                                        .addChild(new ExprNumber(POSITION, 5))
                                                        .addChild(new ExprInfix(POSITION, InfixOperator.SHR)
                                                                .addChild(new ExprInfix(POSITION, InfixOperator.SHL)
                                                                        .addChild(new ExprInfix(POSITION, InfixOperator.MULTIPLY)
                                                                                .addChild(new ExprUnary(POSITION, UnaryOperator.SUBTRACT)
                                                                                        .addChild(new ExprNumber(POSITION, 5)))
                                                                                .addChild(new ExprNumber(POSITION, 6)))
                                                                        .addChild(new ExprInfix(POSITION, InfixOperator.SUBTRACT)
                                                                                .addChild(new ExprNumber(POSITION, 4))
                                                                                .addChild(new ExprNumber(POSITION, 1))))
                                                                .addChild(new ExprNumber(POSITION, 2))))))),
                program
        );
    }

    @Test
    public void testAndMulXorDivNotPlusMinusWithOperators() {
        Program program = parseProgram("db ~1 & 2 | 2 ^ 5 = -5 * 6 << 4 - 1 >> 2");
        assertTrees(
                new Program("")
                        .addChild(new DataDB(POSITION)
                                .addChild(new ExprInfix(POSITION, InfixOperator.OR)
                                        .addChild(new ExprInfix(POSITION, InfixOperator.AND)
                                                .addChild(new ExprUnary(POSITION, UnaryOperator.NOT)
                                                        .addChild(new ExprNumber(POSITION, 1)))
                                                .addChild(new ExprNumber(POSITION, 2)))
                                        .addChild(new ExprInfix(POSITION, InfixOperator.XOR)
                                                .addChild(new ExprNumber(POSITION, 2))
                                                .addChild(new ExprInfix(POSITION, InfixOperator.EQUAL)
                                                        .addChild(new ExprNumber(POSITION, 5))
                                                        .addChild(new ExprInfix(POSITION, InfixOperator.SHR)
                                                                .addChild(new ExprInfix(POSITION, InfixOperator.SHL)
                                                                        .addChild(new ExprInfix(POSITION, InfixOperator.MULTIPLY)
                                                                                .addChild(new ExprUnary(POSITION, UnaryOperator.SUBTRACT)
                                                                                        .addChild(new ExprNumber(POSITION, 5)))
                                                                                .addChild(new ExprNumber(POSITION, 6)))
                                                                        .addChild(new ExprInfix(POSITION, InfixOperator.SUBTRACT)
                                                                                .addChild(new ExprNumber(POSITION, 4))
                                                                                .addChild(new ExprNumber(POSITION, 1))))
                                                                .addChild(new ExprNumber(POSITION, 2))))))),
                program
        );
    }

    @Test
    public void testParenthesis() {
        Program program = parseProgram("db (2 + 3) * (4 - 2)");
        assertTrees(
                new Program("")
                        .addChild(new DataDB(POSITION)
                                .addChild(new ExprInfix(POSITION, InfixOperator.MULTIPLY)
                                        .addChild(new ExprInfix(POSITION, InfixOperator.ADD)
                                                .addChild(new ExprNumber(POSITION, 2))
                                                .addChild(new ExprNumber(POSITION, 3)))
                                        .addChild(new ExprInfix(POSITION, InfixOperator.SUBTRACT)
                                                .addChild(new ExprNumber(POSITION, 4))
                                                .addChild(new ExprNumber(POSITION, 2))))),
                program
        );
    }
}
