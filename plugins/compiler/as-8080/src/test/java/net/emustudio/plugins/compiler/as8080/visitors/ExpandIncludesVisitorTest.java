package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.ast.Label;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrNoArgs;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrRegExpr;
import net.emustudio.plugins.compiler.as8080.exceptions.used.FatalError;
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
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testExpandInclude() {
        String filename = ExpandIncludesVisitorTest.class.getResource("/sample.asm").getFile();
        Program program = parseProgram("cmc\ninclude '" + filename + "'");
        ExpandIncludesVisitor visitor = new ExpandIncludesVisitor();
        visitor.visit(program);

        Node expected = new Program()
            .addChild(new InstrNoArgs(0, 0, OPCODE_CMC))
            .addChild(new Label(0, 0, "sample")
                .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, REG_A)
                    .addChild(new ExprNumber(0, 0, 0))))
            .addChild(new InstrNoArgs(0, 0, OPCODE_RET));

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
            .addChild(new InstrNoArgs(0, 0, OPCODE_RRC))
            .addChild(new InstrNoArgs(0, 0, OPCODE_RRC));

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