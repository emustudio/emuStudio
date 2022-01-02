package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDS;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDW;
import net.emustudio.plugins.compiler.as8080.ast.data.DataPlainString;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprCurrentAddress;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprId;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprInfix;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrExpr;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrRegExpr;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.*;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static net.emustudio.plugins.compiler.as8080.As8080Parser.*;
import static net.emustudio.plugins.compiler.as8080.CompileError.ERROR_AMBIGUOUS_EXPRESSION;
import static net.emustudio.plugins.compiler.as8080.Utils.assertTrees;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EvaluateExprVisitorTest {

    @Test
    public void testEvaluateDB() {
        Program program = new Program();
        program.addChild(new DataDB(0, 0)
            .addChild(new ExprInfix(0, 0, OP_ADD)
                .addChild(new ExprNumber(0, 0, 1))
                .addChild(new ExprNumber(0, 0, 2))
            ).addChild(new DataPlainString(0, 0, "hello")));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrees(
            new Program()
                .addChild(new DataDB(0, 0)
                    .addChild(new Evaluated(0, 0)
                        .addChild(new ExprNumber(0, 0, 3)))
                    .addChild(new Evaluated(0, 0)
                        .addChild(new ExprNumber(0, 0, 'h'))
                        .addChild(new ExprNumber(0, 0, 'e'))
                        .addChild(new ExprNumber(0, 0, 'l'))
                        .addChild(new ExprNumber(0, 0, 'l'))
                        .addChild(new ExprNumber(0, 0, 'o')))),
            program
        );
    }

    @Test
    public void testEvaluateDW() {
        Program program = new Program();
        program
            .addChild(new DataDW(0, 0)
                .addChild(new ExprInfix(0, 0, OP_ADD)
                    .addChild(new ExprNumber(0, 0, 1))
                    .addChild(new ExprNumber(0, 0, 2))))
            .addChild(new DataDW(0, 0)
                .addChild(new ExprNumber(0, 0, 0)));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrees(
            new Program()
                .addChild(new DataDW(0, 0)
                    .addChild(new Evaluated(0, 0)
                        .addChild(new ExprNumber(0, 0, 3))))
                .addChild(new DataDW(0, 0)
                    .addChild(new Evaluated(0, 0)
                        .addChild(new ExprNumber(0, 0, 0)))),
            program
        );
        assertEquals(0, program.getChild(0).getAddress());
        assertEquals(2, program.getChild(1).getAddress());
    }

    @Test
    public void testEvaluateDS() {
        Program program = new Program();
        program
            .addChild(new DataDS(0, 0)
                .addChild(new ExprInfix(0, 0, OP_ADD)
                    .addChild(new ExprNumber(0, 0, 1))
                    .addChild(new ExprNumber(0, 0, 2))))
            .addChild(new DataDB(0, 0)
                .addChild(new ExprNumber(0, 0, 0)));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrees(
            new Program()
                .addChild(new DataDS(0, 0)
                    .addChild(new Evaluated(0, 0)
                        .addChild(new ExprNumber(0, 0, 3))))
                .addChild(new DataDB(0, 0)
                    .addChild(new Evaluated(0, 0)
                        .addChild(new ExprNumber(0, 0, 0)))),
            program
        );
        assertEquals(0, program.getChild(0).getAddress());
        assertEquals(3, program.getChild(1).getAddress());
    }

    @Test
    public void testEvaluateDSambiguous() {
        Program program = new Program();
        program
            .addChild(new DataDS(0, 0)
                .addChild(new ExprId(0, 0, "label")))
            .addChild(new PseudoLabel(0, 0, "label"));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasError(ERROR_AMBIGUOUS_EXPRESSION));
    }

    @Test
    public void testEvaluateDSconstReference() {
        Program program = new Program();
        program
            .addChild(new DataDS(0, 0)
                .addChild(new ExprId(0, 0, "label")))
            .addChild(new PseudoEqu(0, 0, "label")
                .addChild(new ExprNumber(0, 0, 5)));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());

        Optional<Evaluated> label = program.env().get("label");
        assertTrue(label.isPresent());
        assertEquals(0, label.get().getAddress());

        assertTrees(
            new Program()
                .addChild(new DataDS(0, 0)
                    .addChild(new Evaluated(0, 0)
                        .addChild(new ExprNumber(0, 0, 5)))),
            program
        );
    }

    @Test
    public void testEvaluateEQUfivePasses() {
        Program program = new Program();
        program
            .addChild(new PseudoEqu(0, 0, "one")
                .addChild(new ExprId(0, 0, "two")))
            .addChild(new PseudoEqu(0, 0, "two")
                .addChild(new ExprId(0, 0, "three")))
            .addChild(new PseudoEqu(0, 0, "three")
                .addChild(new ExprId(0, 0, "four")))
            .addChild(new PseudoEqu(0, 0, "four")
                .addChild(new ExprId(0, 0, "five")))
            .addChild(new PseudoEqu(0, 0, "five")
                .addChild(new ExprCurrentAddress(0, 0)));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());
        assertTrees(new Program(), program);

        List<String> constants = List.of("one", "two", "three", "four", "five");
        for (String c : constants) {
            Optional<Evaluated> constant = program.env().get(c);
            assertTrue(constant.isPresent());
            assertEquals(0, constant.get().getAddress());
            assertEquals(0, constant.get().getValue());
        }
    }

    @Test
    public void testEvaluateIFwithForwardConst() {
        Program program = new Program();
        program
            .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, REG_A)
                .addChild(new ExprId(0, 0, "const")))
            .addChild(new PseudoIf(0, 0)
                .addChild(new PseudoIfExpression(0, 0)
                    .addChild(new ExprInfix(0, 0, OP_EQUAL)
                        .addChild(new ExprCurrentAddress(0, 0))
                        .addChild(new ExprNumber(0, 0, 2))))
                .addChild(new InstrExpr(0, 0, OPCODE_RST)
                    .addChild(new ExprNumber(0, 0, 0))))
            .addChild(new PseudoEqu(0, 0, "const")
                .addChild(new ExprInfix(0, 0, OP_ADD)
                    .addChild(new ExprCurrentAddress(0, 0))
                    .addChild(new ExprCurrentAddress(0, 0))));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrees(
            new Program()
                .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, REG_A)
                    .addChild(new Evaluated(0, 0)
                        .addChild(new ExprNumber(0, 0, 6))))
                .addChild(new InstrExpr(0, 0, OPCODE_RST)
                    .addChild(new Evaluated(0, 0)
                        .addChild(new ExprNumber(0, 0, 0)))),
            program
        );
        assertEquals(0, program.getChild(0).getAddress());
        assertEquals(2, program.getChild(1).getAddress());
    }

    @Test
    public void testEvaluateSETforwardTwoTimes() {
        Program program = new Program();
        program
            .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, REG_A)
                .addChild(new ExprId(0, 0, "const")))
            .addChild(new PseudoSet(0, 0, "const")
                .addChild(new ExprNumber(0, 0, 1)))
            .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, REG_B)
                .addChild(new ExprId(0, 0, "const")))
            .addChild(new PseudoSet(0, 0, "const")
                .addChild(new ExprNumber(0, 0, 2)));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrees(
            new Program()
                .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, REG_A)
                    .addChild(new Evaluated(0, 0)
                        .addChild(new ExprNumber(0, 0, 1))))
                .addChild(new PseudoSet(0, 0, "const")
                    .addChild(new Evaluated(0, 0)
                        .addChild(new ExprNumber(0, 0, 1))))
                .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, REG_B)
                    .addChild(new Evaluated(0, 0)
                        .addChild(new ExprNumber(0, 0, 1))))
                .addChild(new PseudoSet(0, 0, "const")
                    .addChild(new Evaluated(0, 0)
                        .addChild(new ExprNumber(0, 0, 2)))),
            program
        );
    }

    @Test
    public void testEvaluateSETforwardMoreTimes() {
        Program program = new Program();
        program
            .addChild(new DataDB(0, 0)
                .addChild(new ExprId(0, 0, "id")))
            .addChild(new PseudoSet(0, 0, "id")
                .addChild(new ExprId(0, 0, "const")))
            .addChild(new PseudoSet(0, 0, "id")
                .addChild(new ExprNumber(0, 0, 2)))
            .addChild(new PseudoEqu(0, 0, "const")
                .addChild(new ExprNumber(0, 0, 1)));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrees(
            new Program()
                .addChild(new DataDB(0, 0)
                    .addChild(new Evaluated(0, 0)
                        .addChild(new ExprNumber(0, 0, 1))))
                .addChild(new PseudoSet(0, 0, "id")
                    .addChild(new Evaluated(0, 0)
                        .addChild(new ExprNumber(0, 0, 1))))
                .addChild(new PseudoSet(0, 0, "id")
                    .addChild(new Evaluated(0, 0)
                        .addChild(new ExprNumber(0, 0, 2)))),
            program
        );
    }

    @Test
    public void testTwoSETthenReference() {
        Program program = new Program();
        program
            .addChild(new PseudoSet(0, 0, "id")
                .addChild(new ExprId(0, 0, "const")))
            .addChild(new PseudoSet(0, 0, "id")
                .addChild(new ExprNumber(0, 0, 2)))
            .addChild(new PseudoEqu(0, 0, "const")
                .addChild(new ExprNumber(0, 0, 1)))
            .addChild(new DataDB(0, 0)
                .addChild(new ExprId(0, 0, "id")));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrees(
            new Program()
                .addChild(new PseudoSet(0, 0, "id")
                    .addChild(new Evaluated(0, 0)
                        .addChild(new ExprNumber(0, 0, 1))))
                .addChild(new PseudoSet(0, 0, "id")
                    .addChild(new Evaluated(0, 0)
                        .addChild(new ExprNumber(0, 0, 2))))
                .addChild(new DataDB(0, 0)
                    .addChild(new Evaluated(0, 0)
                        .addChild(new ExprNumber(0, 0, 2)))),
            program
        );
    }

}
