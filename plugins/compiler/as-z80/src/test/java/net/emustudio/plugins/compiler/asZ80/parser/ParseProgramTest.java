/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.parser;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Program;
import net.emustudio.plugins.compiler.asZ80.ast.instr.Instr;
import net.emustudio.plugins.compiler.asZ80.ast.instr.InstrXD;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprUnary;
import org.junit.Test;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static net.emustudio.plugins.compiler.asZ80.Utils.assertTrees;
import static net.emustudio.plugins.compiler.asZ80.Utils.parseProgram;

public class ParseProgramTest {
    private final static SourceCodePosition POSITION = new SourceCodePosition(0, 0, "");

    @Test
    public void testEmptyProgram() {
        Program program = parseProgram("");
        assertTrees(new Program(""), program);
    }

    @Test
    public void testSingleInstrNoNewline() {
        Program program = parseProgram("nop");
        assertTrees(new Program("").addChild(new Instr(POSITION, OPCODE_NOP, 0, 0, 0)), program);
    }

    @Test
    public void testSingleInstrWithNewline() {
        Program program = parseProgram("nop\n");
        assertTrees(new Program("").addChild(new Instr(POSITION, OPCODE_NOP, 0, 0, 0)), program);
    }

    @Test
    public void testMultipleInstrTrailingNewline() {
        Program program = parseProgram("nop\nhalt\n");
        assertTrees(new Program("")
                        .addChild(new Instr(POSITION, OPCODE_NOP, 0, 0, 0))
                        .addChild(new Instr(POSITION, OPCODE_HALT, 1, 6, 6)),
                program);
    }

    @Test
    public void testMultipleInstrNoTrailingNewline() {
        Program program = parseProgram("nop\nhalt");
        assertTrees(new Program("")
                        .addChild(new Instr(POSITION, OPCODE_NOP, 0, 0, 0))
                        .addChild(new Instr(POSITION, OPCODE_HALT, 1, 6, 6)),
                program);
    }

    @Test
    public void testCommentOnly() {
        Program program = parseProgram("; just a comment");
        assertTrees(new Program(""), program);
    }

    @Test
    public void testEmptyLines() {
        Program program = parseProgram("\n\n\nnop\n\n");
        assertTrees(new Program("").addChild(new Instr(POSITION, OPCODE_NOP, 0, 0, 0)), program);
    }

    @Test
    public void testDisplacementMinusInParser() {
        // ld a, (ix - 5) should parse with ExprUnary wrapping the displacement expression
        Program program = parseProgram("ld a, (ix - 5)");
        assertTrees(new Program("")
                        .addChild(new InstrXD(POSITION, OPCODE_LD, 0xDD, 1, 7, 6)
                                .addChild(new ExprUnary(POSITION, OP_SUBTRACT)
                                        .addChild(new ExprNumber(POSITION, 5)))),
                program);
    }

    @Test
    public void testDisplacementPlusInParser() {
        // ld a, (ix + 5) should parse normally (no ExprUnary)
        Program program = parseProgram("ld a, (ix + 5)");
        assertTrees(new Program("")
                        .addChild(new InstrXD(POSITION, OPCODE_LD, 0xDD, 1, 7, 6)
                                .addChild(new ExprNumber(POSITION, 5))),
                program);
    }
}

