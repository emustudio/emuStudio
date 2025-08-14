/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Program;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprId;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoMacroArgument;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoMacroCall;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoMacroDef;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoMacroParameter;
import net.emustudio.plugins.compiler.asZ80.exceptions.FatalError;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static net.emustudio.plugins.compiler.asZ80.CompileError.ERROR_NOT_DEFINED;
import static net.emustudio.plugins.compiler.asZ80.Utils.*;
import static org.junit.Assert.assertTrue;

public class ExpandMacrosTest {
    private final static SourceCodePosition POSITION = new SourceCodePosition(0, 0, "");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testMacroDefinitionThenMacroCall() {
        Program program = parseProgram("x macro\nendm\nx");
        ExpandMacrosVisitor macrosVisitor = new ExpandMacrosVisitor();
        macrosVisitor.visit(program);

        assertTrees(new Program("")
                        .addChild(new PseudoMacroCall(POSITION, "x")
                                .addChild(new PseudoMacroDef(POSITION, "x"))),
                program
        );
    }

    @Test
    public void testMacroCallThenMacroDefinition() {
        Program program = parseProgram("x\nx macro\nendm");
        ExpandMacrosVisitor macrosVisitor = new ExpandMacrosVisitor();
        macrosVisitor.visit(program);

        assertTrees(new Program("")
                        .addChild(new PseudoMacroCall(POSITION, "x")
                                .addChild(new PseudoMacroDef(POSITION, "x"))),
                program
        );
    }

    @Test
    public void testMacroCallThenMacroDefinitionThenMacroCall() {
        Program program = parseProgram("x\nx macro\nendm\nx");
        ExpandMacrosVisitor macrosVisitor = new ExpandMacrosVisitor();
        macrosVisitor.visit(program);

        assertTrees(new Program("")
                        .addChild(new PseudoMacroCall(POSITION, "x")
                                .addChild(new PseudoMacroDef(POSITION, "x")))
                        .addChild(new PseudoMacroCall(POSITION, "x")
                                .addChild(new PseudoMacroDef(POSITION, "x"))),
                program
        );
    }

    @Test
    public void testMacroCallThenMacroDefinitionInsideInclude() throws IOException {
        File file = folder.newFile("file.asm");
        write(file, "x macro\nendm");

        Program program = parseProgram("x\ninclude '" + file.getPath() + "'");
        ExpandIncludesVisitor includesVisitor = new ExpandIncludesVisitor();
        includesVisitor.visit(program);
        ExpandMacrosVisitor macrosVisitor = new ExpandMacrosVisitor();
        macrosVisitor.visit(program);

        assertTrees(new Program("")
                        .addChild(new PseudoMacroCall(POSITION, "x")
                                .addChild(new PseudoMacroDef(POSITION, "x"))),
                program
        );
    }

    @Test(expected = FatalError.class)
    public void testTheSameMacroCallInsideMacroDefinition() {
        Program program = parseProgram("x macro\nx\nendm");
        ExpandMacrosVisitor macrosVisitor = new ExpandMacrosVisitor();
        macrosVisitor.visit(program);
    }

    @Test(expected = FatalError.class)
    public void testMacroCallComplexInfiniteLoop() {
        Program program = parseProgram("x macro\ny\nendm\ny macro\nx\nendm");
        ExpandMacrosVisitor macrosVisitor = new ExpandMacrosVisitor();
        macrosVisitor.visit(program);
    }

    @Test
    public void testMacroCallWithArguments() {
        Program program = parseProgram("x 1,2,3\nx macro u,v,w\nendm");
        ExpandMacrosVisitor macrosVisitor = new ExpandMacrosVisitor();
        macrosVisitor.visit(program);

        assertTrees(new Program("")
                        .addChild(new PseudoMacroCall(POSITION, "x")
                                .addChild(new PseudoMacroArgument(POSITION)
                                        .addChild(new ExprNumber(POSITION, 1)))
                                .addChild(new PseudoMacroArgument(POSITION)
                                        .addChild(new ExprNumber(POSITION, 2)))
                                .addChild(new PseudoMacroArgument(POSITION)
                                        .addChild(new ExprNumber(POSITION, 3)))
                                .addChild(new PseudoMacroDef(POSITION, "x")
                                        .addChild(new PseudoMacroParameter(POSITION)
                                                .addChild(new ExprId(POSITION, "u")))
                                        .addChild(new PseudoMacroParameter(POSITION)
                                                .addChild(new ExprId(POSITION, "v")))
                                        .addChild(new PseudoMacroParameter(POSITION)
                                                .addChild(new ExprId(POSITION, "w"))))),
                program
        );
    }


    @Test
    public void testUnknownMacro() {
        Program program = parseProgram("x 1,2,3\n");
        ExpandMacrosVisitor macrosVisitor = new ExpandMacrosVisitor();
        macrosVisitor.visit(program);
        assertTrue(program.env().hasError(ERROR_NOT_DEFINED));
    }
}
