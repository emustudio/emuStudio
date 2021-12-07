package net.emustudio.plugins.compiler.as8080;

import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.Statement;
import net.emustudio.plugins.compiler.as8080.ast.data.*;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrNoArgs;
import org.junit.Test;

import static net.emustudio.plugins.compiler.as8080.Utils.assertTrees;
import static net.emustudio.plugins.compiler.as8080.Utils.parseProgram;

public class ParseDataTest {

    @Test
    public void testDBstring1() {
        Program program = parseProgram("db 'hello,world!'");
        assertTrees(new Program()
                .addChild(new Statement()
                    .addChild(new Data()
                        .addChild(new DataDB()
                            .addChild(new DataPlainString("hello,world!"))))),
            program
        );
    }

    @Test
    public void testDBstring2() {
        Program program = parseProgram("db \"hello,world!\"");
        assertTrees(new Program()
                .addChild(new Statement()
                    .addChild(new Data()
                        .addChild(new DataDB()
                            .addChild(new DataPlainString("hello,world!"))))),
            program
        );
    }

    @Test
    public void testDBinstruction() {
        Program program = parseProgram("db stc");
        assertTrees(new Program()
                .addChild(new Statement()
                    .addChild(new Data()
                        .addChild(new DataDB()
                            .addChild(new InstrNoArgs(0))))),
            program
        );
    }

    @Test
    public void testMultipleDB() {
        Program program = parseProgram("db -1,2,3");
        assertTrees(new Program()
                .addChild(new Statement()
                    .addChild(new Data()
                        .addChild(new DataDB()
                            .addChild(new ExprNumber(-1))
                            .addChild(new ExprNumber(2))
                            .addChild(new ExprNumber(3))))),
            program
        );
    }

    @Test
    public void testDBwithNegativeValue() {
        Program program = parseProgram("db -1");
        assertTrees(new Program()
            .addChild(new Statement()
                .addChild(new Data()
                    .addChild(new DataDB().addChild(new ExprNumber(-1))))),
            program
        );
    }

    @Test
    public void testMultipleDW() {
        Program program = parseProgram("dw -1,2,3");
        assertTrees(new Program()
                .addChild(new Statement()
                    .addChild(new Data()
                        .addChild(new DataDW()
                            .addChild(new ExprNumber(-1))
                            .addChild(new ExprNumber(2))
                            .addChild(new ExprNumber(3))))),
            program
        );
    }

    @Test
    public void testDWwithNegativeValue() {
        Program program = parseProgram("dw -1");
        assertTrees(new Program()
                .addChild(new Statement()
                    .addChild(new Data()
                        .addChild(new DataDW()
                            .addChild(new ExprNumber(-1))))),
            program
        );
    }

    @Test
    public void testDS() {
        Program program = parseProgram("ds 0x55");
        assertTrees(new Program()
                .addChild(new Statement()
                    .addChild(new DataDS()
                        .addChild(new ExprNumber(0x55)))),
            program
        );
    }
}
