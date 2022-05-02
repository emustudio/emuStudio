package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.Program;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDB;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDS;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDW;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprId;
import net.emustudio.plugins.compiler.asZ80.ast.instr.Instr;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoMacroArgument;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoMacroCall;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoOrg;
import org.junit.Test;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static net.emustudio.plugins.compiler.asZ80.CompileError.ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED;
import static net.emustudio.plugins.compiler.asZ80.CompileError.ERROR_VALUE_OUT_OF_BOUNDS;
import static net.emustudio.plugins.compiler.asZ80.Utils.assertTrees;
import static org.junit.Assert.assertTrue;

public class CheckExprSizesVisitorTest {

    @Test
    public void testDBoneByte() {
        Program program = new Program();
        program
            .addChild(new DataDB(0, 0)
                .addChild(new Evaluated(0, 0, 0xFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testDBtwoBytes() {
        Program program = new Program();
        program
            .addChild(new DataDB(0, 0)
                .addChild(new Evaluated(0, 0, 0xFF))
                .addChild(new Evaluated(0, 0, 0x100))); // bad size

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testDWtwoBytes() {
        Program program = new Program();
        program
            .addChild(new DataDW(0, 0)
                .addChild(new Evaluated(0, 0, 0xFFFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testDWthreeBytes() {
        Program program = new Program();
        program
            .addChild(new DataDW(0, 0)
                .addChild(new Evaluated(0, 0, 0xFFFF))
                .addChild(new Evaluated(0, 0, 0x10000)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testDStwoBytes() {
        Program program = new Program();
        program
            .addChild(new DataDS(0, 0)
                .addChild(new Evaluated(0, 0, 0xFFFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testDSthreeBytes() {
        Program program = new Program();
        program
            .addChild(new DataDS(0, 0)
                .addChild(new Evaluated(0, 0, 0x10000)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testInstrExprTwoBytes() {
        Program program = new Program();
        program
            .addChild(new Instr(0, 0, OPCODE_ADD, 3, 0, 6).setSizeBytes(2)
                .addChild(new Evaluated(0, 0, 0xFF00).setSizeBytes(1)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_VALUE_OUT_OF_BOUNDS));
    }

    @Test
    public void testInstrExprThreeBytes() {
        Program program = new Program();
        program
            .addChild(new Instr(0, 0, OPCODE_JP, 3, 0, 3).setSizeBytes(3)
                .addChild(new Evaluated(0, 0, 0xFF000).setSizeBytes(2)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_VALUE_OUT_OF_BOUNDS));
    }

    @Test
    public void testInstrRegExprOneByte() {
        Program program = new Program();
        program
            .addChild(new Instr(0, 0, OPCODE_LD, 0, 7, 6).setSizeBytes(2)
                .addChild(new Evaluated(0, 0, 0xFF).setSizeBytes(1)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testInstrRegExprTwoBytes() {
        Program program = new Program();
        program
            .addChild(new Instr(0, 0, OPCODE_LD, 0, 7, 6).setSizeBytes(2)
                .addChild(new Evaluated(0, 0, 0x100).setSizeBytes(1))); // bad size

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_VALUE_OUT_OF_BOUNDS));
    }

    @Test
    public void testInstrRegPairExprTwoBytes() {
        Program program = new Program();
        program
            .addChild(new Instr(0, 0, OPCODE_LD, 0, 0, 1).setSizeBytes(3)
                .addChild(new Evaluated(0, 0, 0xFFFF).setSizeBytes(2)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testInstrRegPairExprThreeBytes() {
        Program program = new Program();
        program
            .addChild(new Instr(0, 0, OPCODE_LD, 0, 0, 1).setSizeBytes(3)
                .addChild(new Evaluated(0, 0, 0x10000).setSizeBytes(2))); // bad size

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_VALUE_OUT_OF_BOUNDS));
    }

    @Test
    public void testPseudoOrgTwoBytes() {
        Program program = new Program();
        program
            .addChild(new PseudoOrg(0, 0).setSizeBytes(2)
                .addChild(new Evaluated(0, 0, 0xFFFF).setSizeBytes(2)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testPseudoOrgThreeBytes() {
        Program program = new Program();
        program
            .addChild(new PseudoOrg(0, 0).setSizeBytes(2)
                .addChild(new Evaluated(0, 0, 0x10000).setSizeBytes(2))); // bad size

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
                .addChild(new Instr(0, 0, OPCODE_LD, 0, 0, 1).setSizeBytes(3)
                    .addChild(new Evaluated(0, 0, 0).setSizeBytes(2)))
                .addChild(new PseudoMacroCall(0, 0, "y")
                    .addChild(new PseudoMacroArgument(0, 0)
                        .addChild(new ExprId(0, 0, "arg"))
                        .addChild(new Evaluated(0, 0, 1)))
                    .addChild(new Instr(0, 0, OPCODE_LD, 0, 0, 1).setSizeBytes(3)
                        .addChild(new Evaluated(0, 0, 1).setSizeBytes(2))))
                .addChild(new Instr(0, 0, OPCODE_LD, 0, 0, 1).setSizeBytes(3)
                    .addChild(new Evaluated(0, 0, 0).setSizeBytes(2))));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrees(
            new Program()
                .addChild(new PseudoMacroCall(0, 0, "x")
                    .addChild(new Instr(0, 0, OPCODE_LD, 0, 0, 1).setSizeBytes(3)
                        .addChild(new Evaluated(0, 0, 0).setSizeBytes(2)))
                    .addChild(new PseudoMacroCall(0, 0, "y")
                        .addChild(new Instr(0, 0, OPCODE_LD, 0, 0, 1).setSizeBytes(3)
                            .addChild(new Evaluated(0, 0, 1).setSizeBytes(2))))
                    .addChild(new Instr(0, 0, OPCODE_LD, 0, 0, 1).setSizeBytes(3)
                        .addChild(new Evaluated(0, 0, 0).setSizeBytes(2)))),
            program
        );
    }
}
