package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.Either;
import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.NeedMorePass;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDS;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDW;
import net.emustudio.plugins.compiler.as8080.ast.data.DataPlainString;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprId;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprInfix;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoEqu;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoLabel;
import org.junit.Test;

import static net.emustudio.plugins.compiler.as8080.As8080Parser.OP_ADD;
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
                    .addChild(new Evaluated(0, 0, 0, 1)
                        .addChild(new ExprNumber(0, 0, 3))
                    ).addChild(new Evaluated(0, 0, 1, 5)
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
                    .addChild(new Evaluated(0, 0, 0, 2)
                        .addChild(new ExprNumber(0, 0, 3))))
                .addChild(new DataDW(0, 0)
                    .addChild(new Evaluated(0, 0, 2, 2)
                        .addChild(new ExprNumber(0, 0, 0)))),
            program
        );
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
                    .addChild(new Evaluated(0, 0, 0, 0)
                        .addChild(new ExprNumber(0, 0, 3))))
                .addChild(new DataDB(0, 0)
                    .addChild(new Evaluated(0, 0, 3, 1)
                        .addChild(new ExprNumber(0, 0, 0)))),
            program
        );
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
    public void testEvaluateDScontReference() {
        Program program = new Program();
        program
            .addChild(new DataDS(0, 0)
                .addChild(new ExprId(0, 0, "label")))
            .addChild(new PseudoEqu(0, 0, "label")
                .addChild(new ExprNumber(0, 0, 5)));

        EvaluateExprVisitor visitor = new EvaluateExprVisitor();
        visitor.visit(program);

        assertTrue(program.env().hasNoErrors());

        Either<NeedMorePass, Evaluated> label = program.env().get("label").orElseThrow();
        assertTrue(label.isRight());
        assertEquals(5, label.right.getSizeBytes());
        assertEquals(0, label.right.address);

        assertTrees(
            new Program()
                .addChild(new DataDS(0, 0)
                    .addChild(new ExprId(0, 0, "label"))),
            program
        );
    }


}
