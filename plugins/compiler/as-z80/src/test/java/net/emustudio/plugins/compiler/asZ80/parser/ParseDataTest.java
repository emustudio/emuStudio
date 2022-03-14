package net.emustudio.plugins.compiler.asZ80.parser;

import net.emustudio.plugins.compiler.asZ80.ast.Program;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDB;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDS;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDW;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprString;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprUnary;
import net.emustudio.plugins.compiler.asZ80.ast.instr.Instr;
import org.junit.Test;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.OPCODE_RET;
import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.OP_SUBTRACT;
import static net.emustudio.plugins.compiler.asZ80.Utils.assertTrees;
import static net.emustudio.plugins.compiler.asZ80.Utils.parseProgram;

public class ParseDataTest {

    @Test
    public void testDBstring1() {
        Program program = parseProgram("db 'hello'");
        assertTrees(new Program()
                .addChild(new DataDB(0, 0)
                    .addChild(new ExprString(0, 0, "hello"))),
            program
        );
    }

    @Test
    public void testDBstring2() {
        Program program = parseProgram("db \"hello\"");
        assertTrees(new Program()
                .addChild(new DataDB(0, 0)
                    .addChild(new ExprString(0, 0, "hello"))),
            program
        );
    }

    @Test
    public void testDBinstruction() {
        Program program = parseProgram("db ret");
        assertTrees(
            new Program()
                .addChild(new DataDB(0, 0)
                    .addChild(new Instr(0, 0, OPCODE_RET, 3, 1, 1))),
            program
        );
    }

    @Test
    public void testMultipleDB() {
        Program program = parseProgram("db -1,2,3");
        assertTrees(new Program()
                .addChild(new DataDB(0, 0)
                    .addChild(new ExprUnary(0, 0, OP_SUBTRACT)
                        .addChild(new ExprNumber(0, 0, 1)))
                    .addChild(new ExprNumber(0, 0, 2))
                    .addChild(new ExprNumber(0, 0, 3))),
            program
        );
    }

    @Test
    public void testDBwithNegativeValue() {
        Program program = parseProgram("db -1");
        assertTrees(new Program()
                .addChild(new DataDB(0, 0)
                    .addChild(new ExprUnary(0, 0, OP_SUBTRACT)
                        .addChild(new ExprNumber(0, 0, 1)))),
            program
        );
    }

    @Test
    public void testMultipleDBstringNumberString() {
        Program program = parseProgram("db -1,'hello',3");
        assertTrees(new Program()
                .addChild(new DataDB(0, 0)
                    .addChild(new ExprUnary(0, 0, OP_SUBTRACT)
                        .addChild(new ExprNumber(0, 0, 1)))
                    .addChild(new ExprString(0, 0, "hello"))
                    .addChild(new ExprNumber(0, 0, 3))),
            program
        );
    }

    @Test
    public void testMultipleDW() {
        Program program = parseProgram("dw -1,2,3");
        assertTrees(new Program()
                .addChild(new DataDW(0, 0)
                    .addChild(new ExprUnary(0, 0, OP_SUBTRACT)
                        .addChild(new ExprNumber(0, 0, 1)))
                    .addChild(new ExprNumber(0, 0, 2))
                    .addChild(new ExprNumber(0, 0, 3))),
            program
        );
    }

    @Test
    public void testDWwithNegativeValue() {
        Program program = parseProgram("dw -1");
        assertTrees(new Program()
                .addChild(new DataDW(0, 0)
                    .addChild(new ExprUnary(0, 0, OP_SUBTRACT)
                        .addChild(new ExprNumber(0, 0, 1)))),
            program
        );
    }

    @Test
    public void testDS() {
        Program program = parseProgram("ds 0x55");
        assertTrees(new Program()
                .addChild(new DataDS(0, 0)
                    .addChild(new ExprNumber(0, 0, 0x55))),
            program
        );
    }
}