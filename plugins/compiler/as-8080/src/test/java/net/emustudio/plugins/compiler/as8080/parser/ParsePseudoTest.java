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
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprId;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprInfix;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrExpr;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrNoArgs;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.*;
import net.emustudio.plugins.compiler.as8080.exceptions.SyntaxErrorException;
import org.junit.Test;

import java.util.List;

import static net.emustudio.plugins.compiler.as8080.As8080Lexer.*;
import static net.emustudio.plugins.compiler.as8080.Utils.assertTrees;
import static net.emustudio.plugins.compiler.as8080.Utils.parseProgram;

public class ParsePseudoTest {
    private final static SourceCodePosition POSITION = new SourceCodePosition(0, 0, "");

    @Test
    public void testConstant() {
        Program program = parseProgram("here equ 0x55");
        assertTrees(new Program("")
                        .addChild(new PseudoEqu(POSITION, "here")
                                .addChild(new ExprNumber(POSITION, 0x55))),
                program
        );
    }

    @Test
    public void testVariable() {
        Program program = parseProgram("here set 0x55");
        assertTrees(new Program("")
                        .addChild(new PseudoSet(POSITION, "here")
                                .addChild(new ExprNumber(POSITION, 0x55))),
                program
        );
    }

    @Test
    public void testOrg() {
        Program program = parseProgram("org 55+88");
        assertTrees(new Program("")
                        .addChild(new PseudoOrg(POSITION)
                                .addChild(new ExprInfix(POSITION, OP_ADD)
                                        .addChild(new ExprNumber(POSITION, 55))
                                        .addChild(new ExprNumber(POSITION, 88)))),
                program
        );
    }

    @Test
    public void testIf() {
        Program program = parseProgram("if 1\n"
                + "  rrc\n"
                + "  rrc\n"
                + "endif");

        assertTrees(new Program("")
                        .addChild(new PseudoIf(POSITION)
                                .addChild(new PseudoIfExpression(POSITION)
                                        .addChild(new ExprNumber(POSITION, 1)))
                                .addChild(new InstrNoArgs(POSITION, OPCODE_RRC))
                                .addChild(new InstrNoArgs(POSITION, OPCODE_RRC))),
                program
        );
    }

    @Test
    public void testIfEmpty() {
        List<String> programs = List.of(
                "if 1\n\n\nendif",
                "if 1\n\nendif",
                "if 1\nendif"
        );

        for (String src : programs) {
            Program program = parseProgram(src);
            Node expected = new Program("")
                    .addChild(new PseudoIf(POSITION)
                            .addChild(new PseudoIfExpression(POSITION)
                                    .addChild(new ExprNumber(POSITION, 1))));
            assertTrees(expected, program);
        }
    }

    @Test(expected = SyntaxErrorException.class)
    public void testIfEndifMustBeOnNewLine() {
        parseProgram("if 1\nrrc\nrrc endif");
    }

    @Test
    public void testTwoLabelsInsideIf() {
        Program program = parseProgram("if 1\n"
                + "  label1:\n"
                + "  label2:\n"
                + "endif");

        assertTrees(new Program("")
                        .addChild(new PseudoIf(POSITION)
                                .addChild(new PseudoIfExpression(POSITION)
                                        .addChild(new ExprNumber(POSITION, 1)))
                                .addChild(new PseudoLabel(POSITION, "label1"))
                                .addChild(new PseudoLabel(POSITION, "label2"))),
                program
        );
    }

    @Test
    public void testInclude() {
        Program program = parseProgram("include 'filename.asm'");
        assertTrees(
                new Program("").addChild(new PseudoInclude(POSITION, "filename.asm")),
                program
        );
    }

    @Test
    public void testMacroDef() {
        Program program = parseProgram("shrt macro param1, param2\n"
                + "  rrc\n"
                + "  heylabel: ani 7Fh\n"
                + "endm\n\n");

        Node expected = new Program("")
                .addChild(new PseudoMacroDef(POSITION, "shrt")
                        .addChild(new PseudoMacroParameter(POSITION)
                                .addChild(new ExprId(POSITION, "param1")))
                        .addChild(new PseudoMacroParameter(POSITION)
                                .addChild(new ExprId(POSITION, "param2")))
                        .addChild(new InstrNoArgs(POSITION, OPCODE_RRC))
                        .addChild(new PseudoLabel(POSITION, "heylabel")
                                .addChild(new InstrExpr(POSITION, OPCODE_ANI)
                                        .addChild(new ExprNumber(POSITION, 0x7F)))));

        assertTrees(expected, program);
    }

    @Test
    public void testMacroDefEmpty() {
        List<String> programs = List.of(
                "shrt macro\n\n\nendm",
                "shrt macro\n\nendm",
                "shrt macro\nendm"
        );

        for (String src : programs) {
            Program program = parseProgram(src);
            Node expected = new Program("").addChild(new PseudoMacroDef(POSITION, "shrt"));
            assertTrees(expected, program);
        }
    }

    @Test(expected = SyntaxErrorException.class)
    public void testMacroDefEndmMustBeOnNewLine() {
        parseProgram("shrt macro\nrrc\nrrc endm");
    }

    @Test
    public void testMacroCallNoParams() {
        Program program = parseProgram("shrt");
        Node expected = new Program("").addChild(new PseudoMacroCall(POSITION, "shrt"));
        assertTrees(expected, program);
    }

    @Test
    public void testMacroCallWithParams() {
        Program program = parseProgram("shrt param1, 45");

        Node expected = new Program("")
                .addChild(new PseudoMacroCall(POSITION, "shrt")
                        .addChild(new PseudoMacroArgument(POSITION)
                                .addChild(new ExprId(POSITION, "param1")))
                        .addChild(new PseudoMacroArgument(POSITION)
                                .addChild(new ExprNumber(POSITION, 45))));

        assertTrees(expected, program);
    }
}
