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
package net.emustudio.plugins.compiler.as8080.parser;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprInfix;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprUnary;
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
                                .addChild(new ExprInfix(POSITION, OP_ADD)
                                        .addChild(new ExprNumber(POSITION, 2))
                                        .addChild(new ExprInfix(POSITION, OP_MULTIPLY)
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
                                .addChild(new ExprInfix(POSITION, OP_ADD)
                                        .addChild(new ExprInfix(POSITION, OP_MULTIPLY)
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
                                .addChild(new ExprInfix(POSITION, OP_SUBTRACT)
                                        .addChild(new ExprInfix(POSITION, OP_ADD)
                                                .addChild(new ExprInfix(POSITION, OP_SUBTRACT)
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
                                .addChild(new ExprInfix(POSITION, OP_DIVIDE)
                                        .addChild(new ExprInfix(POSITION, OP_MULTIPLY)
                                                .addChild(new ExprInfix(POSITION, OP_DIVIDE)
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
                                .addChild(new ExprInfix(POSITION, OP_SUBTRACT)
                                        .addChild(new ExprInfix(POSITION, OP_ADD)
                                                .addChild(new ExprNumber(POSITION, 2))
                                                .addChild(new ExprInfix(POSITION, OP_MULTIPLY)
                                                        .addChild(new ExprNumber(POSITION, 3))
                                                        .addChild(new ExprNumber(POSITION, 4))))
                                        .addChild(new ExprInfix(POSITION, OP_MOD)
                                                .addChild(new ExprInfix(POSITION, OP_DIVIDE)
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
                                .addChild(new ExprInfix(POSITION, OP_EQUAL)
                                        .addChild(new ExprInfix(POSITION, OP_ADD) // 1 + 2 + 2 associates to left
                                                .addChild(new ExprInfix(POSITION, OP_ADD)
                                                        .addChild(new ExprNumber(POSITION, 1))
                                                        .addChild(new ExprNumber(POSITION, 2)))
                                                .addChild(new ExprNumber(POSITION, 2)))
                                        .addChild(new ExprInfix(POSITION, OP_EQUAL)
                                                .addChild(new ExprNumber(POSITION, 5)) // ... = 5 associates to right
                                                .addChild(new ExprInfix(POSITION, OP_EQUAL)
                                                        .addChild(new ExprNumber(POSITION, 5))
                                                        .addChild(new ExprInfix(POSITION, OP_SUBTRACT) // minus has > precedence than =
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
                                .addChild(new ExprInfix(POSITION, OP_OR)
                                        .addChild(new ExprInfix(POSITION, OP_AND)
                                                .addChild(new ExprUnary(POSITION, OP_NOT)
                                                        .addChild(new ExprNumber(POSITION, 1)))
                                                .addChild(new ExprNumber(POSITION, 2)))
                                        .addChild(new ExprInfix(POSITION, OP_XOR)
                                                .addChild(new ExprNumber(POSITION, 2))
                                                .addChild(new ExprInfix(POSITION, OP_EQUAL)
                                                        .addChild(new ExprNumber(POSITION, 5))
                                                        .addChild(new ExprInfix(POSITION, OP_SHR)
                                                                .addChild(new ExprInfix(POSITION, OP_SHL)
                                                                        .addChild(new ExprInfix(POSITION, OP_MULTIPLY)
                                                                                .addChild(new ExprUnary(POSITION, OP_SUBTRACT)
                                                                                        .addChild(new ExprNumber(POSITION, 5)))
                                                                                .addChild(new ExprNumber(POSITION, 6)))
                                                                        .addChild(new ExprInfix(POSITION, OP_SUBTRACT)
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
                                .addChild(new ExprInfix(POSITION, OP_OR_2)
                                        .addChild(new ExprInfix(POSITION, OP_AND_2)
                                                .addChild(new ExprUnary(POSITION, OP_NOT_2)
                                                        .addChild(new ExprNumber(POSITION, 1)))
                                                .addChild(new ExprNumber(POSITION, 2)))
                                        .addChild(new ExprInfix(POSITION, OP_XOR_2)
                                                .addChild(new ExprNumber(POSITION, 2))
                                                .addChild(new ExprInfix(POSITION, OP_EQUAL)
                                                        .addChild(new ExprNumber(POSITION, 5))
                                                        .addChild(new ExprInfix(POSITION, OP_SHR_2)
                                                                .addChild(new ExprInfix(POSITION, OP_SHL_2)
                                                                        .addChild(new ExprInfix(POSITION, OP_MULTIPLY)
                                                                                .addChild(new ExprUnary(POSITION, OP_SUBTRACT)
                                                                                        .addChild(new ExprNumber(POSITION, 5)))
                                                                                .addChild(new ExprNumber(POSITION, 6)))
                                                                        .addChild(new ExprInfix(POSITION, OP_SUBTRACT)
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
                                .addChild(new ExprInfix(POSITION, OP_MULTIPLY)
                                        .addChild(new ExprInfix(POSITION, OP_ADD)
                                                .addChild(new ExprNumber(POSITION, 2))
                                                .addChild(new ExprNumber(POSITION, 3)))
                                        .addChild(new ExprInfix(POSITION, OP_SUBTRACT)
                                                .addChild(new ExprNumber(POSITION, 4))
                                                .addChild(new ExprNumber(POSITION, 2))))),
                program
        );
    }
}
