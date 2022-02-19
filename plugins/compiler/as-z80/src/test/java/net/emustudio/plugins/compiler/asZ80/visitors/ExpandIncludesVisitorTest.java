package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoLabel;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.ast.Program;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.asZ80.ast.instr.Instr;
import net.emustudio.plugins.compiler.asZ80.exceptions.FatalError;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static net.emustudio.plugins.compiler.asZ80.CompileError.ERROR_CANNOT_READ_FILE;
import static net.emustudio.plugins.compiler.asZ80.Utils.*;
import static org.junit.Assert.assertTrue;

public class ExpandIncludesVisitorTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testExpandInclude() {
        String filename = ExpandIncludesVisitorTest.class.getResource("/sample.asm").getFile();
        Program program = parseProgram("cmc\ninclude '" + filename + "'");
        ExpandIncludesVisitor visitor = new ExpandIncludesVisitor();
        visitor.visit(program);

        Node expected = new Program()
            .addChild(new Instr(0, 0, OPCODE_CCF, 0, 7, 7))
            .addChild(new PseudoLabel(0, 0, "sample")
                .addChild(new Instr(0, 0, OPCODE_LD, 0, 7, 6)
                    .addChild(new ExprNumber(0, 0, 0))))
            .addChild(new Instr(0, 0, OPCODE_RET, 3, 1, 1));

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

        Node expected = new Program()
            .addChild(new Instr(0, 0, OPCODE_RRCA, 0, 1, 7))
            .addChild(new Instr(0, 0, OPCODE_RRCA, 0, 1, 7));

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
