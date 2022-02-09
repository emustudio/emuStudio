package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.Program;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDB;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDS;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDW;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprId;
import net.emustudio.plugins.compiler.asZ80.ast.instr.InstrN;
import net.emustudio.plugins.compiler.asZ80.ast.instr.InstrR_N;
import net.emustudio.plugins.compiler.asZ80.ast.instr.InstrRP_NN;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoMacroArgument;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoMacroCall;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoOrg;
import org.junit.Test;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static net.emustudio.plugins.compiler.asZ80.CompileError.ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED;
import static net.emustudio.plugins.compiler.asZ80.Utils.assertTrees;
import static org.junit.Assert.assertTrue;

public class CheckExprSizesVisitorTest {

    @Test
    public void testDBoneByte() {
        Program program = new Program();
        program
            .addChild(new DataDB(0, 0)
                .addChild(new Evaluated(0,0, 0xFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testDBtwoBytes() {
        Program program = new Program();
        program
            .addChild(new DataDB(0, 0)
                .addChild(new Evaluated(0,0, 0xFF))
                .addChild(new Evaluated(0,0, 0x100))); // bad size

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testDWtwoBytes() {
        Program program = new Program();
        program
            .addChild(new DataDW(0, 0)
                .addChild(new Evaluated(0,0, 0xFFFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testDWthreeBytes() {
        Program program = new Program();
        program
            .addChild(new DataDW(0, 0)
                .addChild(new Evaluated(0,0, 0xFFFF))
                .addChild(new Evaluated(0,0, 0x10000)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testDStwoBytes() {
        Program program = new Program();
        program
            .addChild(new DataDS(0, 0)
                .addChild(new Evaluated(0,0, 0xFFFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testDSthreeBytes() {
        Program program = new Program();
        program
            .addChild(new DataDS(0, 0)
                .addChild(new Evaluated(0,0, 0x10000)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testInstrExprTwoBytes() {
        Program program = new Program();
        program
            .addChild(new InstrN(0, 0, OPCODE_ADI)
                .addChild(new Evaluated(0,0, 0xFF00)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testInstrExprThreeBytes() {
        Program program = new Program();
        program
            .addChild(new InstrN(0, 0, OPCODE_JMP)
                .addChild(new Evaluated(0,0, 0xFF000)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testInstrRegExprOneByte() {
        Program program = new Program();
        program
            .addChild(new InstrR_N(0, 0, OPCODE_MVI, REG_A)
                .addChild(new Evaluated(0,0, 0xFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testInstrRegExprTwoBytes() {
        Program program = new Program();
        program
            .addChild(new InstrR_N(0, 0, OPCODE_MVI, REG_A)
                .addChild(new Evaluated(0,0, 0x100))); // bad size

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testInstrRegPairExprTwoBytes() {
        Program program = new Program();
        program
            .addChild(new InstrRP_NN(0, 0, OPCODE_LXI, REG_B)
                .addChild(new Evaluated(0,0, 0xFFFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testInstrRegPairExprThreeBytes() {
        Program program = new Program();
        program
            .addChild(new InstrRP_NN(0, 0, OPCODE_LXI, REG_B)
                .addChild(new Evaluated(0,0, 0x10000))); // bad size

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_EXPRESSION_IS_BIGGER_THAN_EXPECTED));
    }

    @Test
    public void testPseudoOrgTwoBytes() {
        Program program = new Program();
        program
            .addChild(new PseudoOrg(0, 0)
                .addChild(new Evaluated(0,0, 0xFFFF)));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testPseudoOrgThreeBytes() {
        Program program = new Program();
        program
            .addChild(new PseudoOrg(0, 0)
                .addChild(new Evaluated(0,0, 0x10000))); // bad size

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
                .addChild(new InstrRP_NN(0, 0, OPCODE_LXI, REG_B)
                    .addChild(new Evaluated(0, 0, 0)))
                .addChild(new PseudoMacroCall(0, 0, "y")
                    .addChild(new PseudoMacroArgument(0, 0)
                        .addChild(new ExprId(0, 0, "arg"))
                        .addChild(new Evaluated(0, 0, 1)))
                    .addChild(new InstrRP_NN(0, 0, OPCODE_LXI, REG_B)
                        .addChild(new Evaluated(0, 0, 1))))
                .addChild(new InstrRP_NN(0, 0, OPCODE_LXI, REG_B)
                    .addChild(new Evaluated(0, 0, 0))));

        CheckExprSizesVisitor visitor = new CheckExprSizesVisitor();
        visitor.visit(program);

        assertTrees(
            new Program()
                .addChild(new PseudoMacroCall(0, 0, "x")
                    .addChild(new InstrRP_NN(0, 0, OPCODE_LXI, REG_B)
                        .addChild(new Evaluated(0, 0, 0)))
                    .addChild(new PseudoMacroCall(0, 0, "y")
                        .addChild(new InstrRP_NN(0, 0, OPCODE_LXI, REG_B)
                            .addChild(new Evaluated(0, 0, 1))))
                    .addChild(new InstrRP_NN(0, 0, OPCODE_LXI, REG_B)
                        .addChild(new Evaluated(0, 0, 0)))),
            program
        );
    }
}
