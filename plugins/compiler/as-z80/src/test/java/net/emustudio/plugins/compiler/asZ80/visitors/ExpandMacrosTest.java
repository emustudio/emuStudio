package net.emustudio.plugins.compiler.asZ80.visitors;

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

import static net.emustudio.plugins.compiler.asZ80.Utils.*;

public class ExpandMacrosTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testMacroDefinitionThenMacroCall() {
        Program program = parseProgram("x macro\nendm\nx");
        ExpandMacrosVisitor macrosVisitor = new ExpandMacrosVisitor();
        macrosVisitor.visit(program);

        assertTrees(new Program()
                .addChild(new PseudoMacroCall(0, 0, "x")
                    .addChild(new PseudoMacroDef(0, 0, "x"))),
            program
        );
    }

    @Test
    public void testMacroCallThenMacroDefinition() {
        Program program = parseProgram("x\nx macro\nendm");
        ExpandMacrosVisitor macrosVisitor = new ExpandMacrosVisitor();
        macrosVisitor.visit(program);

        assertTrees(new Program()
                .addChild(new PseudoMacroCall(0, 0, "x")
                    .addChild(new PseudoMacroDef(0, 0, "x"))),
            program
        );
    }

    @Test
    public void testMacroCallThenMacroDefinitionThenMacroCall() {
        Program program = parseProgram("x\nx macro\nendm\nx");
        ExpandMacrosVisitor macrosVisitor = new ExpandMacrosVisitor();
        macrosVisitor.visit(program);

        assertTrees(new Program()
                .addChild(new PseudoMacroCall(0, 0, "x")
                    .addChild(new PseudoMacroDef(0, 0, "x")))
                .addChild(new PseudoMacroCall(0, 0, "x")
                    .addChild(new PseudoMacroDef(0, 0, "x"))),
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

        assertTrees(new Program()
                .addChild(new PseudoMacroCall(0, 0, "x")
                    .addChild(new PseudoMacroDef(0, 0, "x"))),
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

        assertTrees(new Program()
                .addChild(new PseudoMacroCall(0, 0, "x")
                    .addChild(new PseudoMacroArgument(0, 0)
                        .addChild(new ExprNumber(0, 0, 1)))
                    .addChild(new PseudoMacroArgument(0, 0)
                        .addChild(new ExprNumber(0, 0, 2)))
                    .addChild(new PseudoMacroArgument(0, 0)
                        .addChild(new ExprNumber(0, 0, 3)))
                    .addChild(new PseudoMacroDef(0, 0, "x")
                        .addChild(new PseudoMacroParameter(0, 0)
                            .addChild(new ExprId(0, 0, "u")))
                        .addChild(new PseudoMacroParameter(0, 0)
                            .addChild(new ExprId(0, 0, "v")))
                        .addChild(new PseudoMacroParameter(0, 0)
                            .addChild(new ExprId(0, 0, "w"))))),
            program
        );
    }
}
