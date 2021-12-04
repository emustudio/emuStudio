package net.emustudio.plugins.compiler.as8080;

import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.Statement;
import net.emustudio.plugins.compiler.as8080.ast.expr.Expr;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrNoArgs;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrRegExpr;
import net.emustudio.plugins.compiler.as8080.visitors.CreateProgramVisitor;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

import static net.emustudio.plugins.compiler.as8080.Utils.assertTrees;

public class ParserTest {

    @Test
    public void testParse() {
        ParseTree tree = Utils.parse("stc\nmvi a, 5");
        Program program = new Program();
        CreateProgramVisitor visitor = new CreateProgramVisitor(program);

        visitor.visit(tree);
        assertTrees(new Program()
            .addChild(new Statement()
                .addChild(new InstrNoArgs(0)))
            .addChild(new Statement()
                .addChild(new InstrRegExpr(0, 0, new Expr() {}))),
            program
        );
    }
}
