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
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrNoArgs;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrRegExpr;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoLabel;
import net.emustudio.plugins.compiler.as8080.exceptions.FatalError;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static net.emustudio.plugins.compiler.as8080.As8080Parser.*;
import static net.emustudio.plugins.compiler.as8080.CompileError.ERROR_CANNOT_READ_FILE;
import static net.emustudio.plugins.compiler.as8080.Utils.*;
import static org.junit.Assert.assertTrue;

public class ExpandIncludesVisitorTest {
    private final static SourceCodePosition POSITION = new SourceCodePosition(0, 0, "");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testExpandInclude() {
        String filename = ExpandIncludesVisitorTest.class.getResource("/sample.asm").getFile();
        Program program = parseProgram("cmc\ninclude '" + filename + "'");
        ExpandIncludesVisitor visitor = new ExpandIncludesVisitor();
        visitor.visit(program);

        Node expected = new Program("")
                .addChild(new InstrNoArgs(POSITION, OPCODE_CMC))
                .addChild(new PseudoLabel(POSITION, "sample"))
                .addChild(new InstrRegExpr(POSITION, OPCODE_MVI, REG_A)
                        .addChild(new ExprNumber(POSITION, 0)))
                .addChild(new InstrNoArgs(POSITION, OPCODE_RET));

        assertTrees(expected, program);
    }

    @Test
    public void testExpandIncludeTwoTimes() throws IOException {
        File file = folder.newFile("file-a.asm");
        write(file, "rrc");

        Program program = parseProgram(
                "include '" + file.getPath() + "'\n" +
                        "include '" + file.getPath() + "'"
        );
        ExpandIncludesVisitor visitor = new ExpandIncludesVisitor();
        visitor.visit(program);

        Node expected = new Program("")
                .addChild(new InstrNoArgs(POSITION, OPCODE_RRC))
                .addChild(new InstrNoArgs(POSITION, OPCODE_RRC));

        assertTrees(expected, program);
    }

    @Test
    public void testNonExistingFileThrows() {
        Program program = parseProgram("cmc\ninclude 'non-existant.asm'");
        ExpandIncludesVisitor visitor = new ExpandIncludesVisitor();
        visitor.visit(program);
        assertTrue(program.env().hasError(ERROR_CANNOT_READ_FILE));
    }

    @Test(expected = FatalError.class)
    public void testIndefiniteLoopDetected() throws IOException {
        File fileA = folder.newFile("file-a.asm");
        File fileB = folder.newFile("file-b.asm");

        write(fileA, "include '" + fileB.getPath() + "'");
        write(fileB, "include '" + fileA.getPath() + "'");

        Program program = parseProgram("include '" + fileA.getPath() + "'");
        ExpandIncludesVisitor visitor = new ExpandIncludesVisitor();
        visitor.visit(program);
    }
}
