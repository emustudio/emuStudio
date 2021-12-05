package net.emustudio.plugins.compiler.as8080;

import net.emustudio.plugins.compiler.as8080.ast.Label;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.Statement;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprId;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprInfix;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrExpr;
import net.emustudio.plugins.compiler.as8080.ast.instr.InstrNoArgs;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.*;
import org.junit.Test;

import static net.emustudio.plugins.compiler.as8080.As8080Lexer.*;
import static net.emustudio.plugins.compiler.as8080.Utils.assertTrees;
import static net.emustudio.plugins.compiler.as8080.Utils.parseProgram;

public class ParsePseudoTest {

    @Test
    public void testConstant() {
        Program program = parseProgram("here equ 0x55");
        assertTrees(new Program()
                .addChild(new Statement()
                    .addChild(new PseudoEqu("here")
                        .addChild(new ExprNumber(0x55)))),
            program
        );
    }

    @Test
    public void testVariable() {
        Program program = parseProgram("here set 0x55");
        assertTrees(new Program()
                .addChild(new Statement()
                    .addChild(new PseudoSet("here")
                        .addChild(new ExprNumber(0x55)))),
            program
        );
    }

    @Test
    public void testOrg() {
        Program program = parseProgram("org 55+88");
        assertTrees(new Program()
                .addChild(new Statement()
                    .addChild(new PseudoOrg()
                        .addChild(new ExprInfix(OP_ADD)
                            .addChild(new ExprNumber(55))
                            .addChild(new ExprNumber(88))))),
            program
        );
    }

    @Test
    public void testIf() {
        Program program = parseProgram("if 1\n"
            + "  rrc\n"
            + "  rrc\n"
            + "endif");

        assertTrees(new Program()
                .addChild(new Statement()
                    .addChild(new PseudoIf()
                        .addChild(new ExprNumber(1))
                        .addChild(new Statement()
                            .addChild(new InstrNoArgs(OPCODE_RRC)))
                        .addChild(new Statement()
                            .addChild(new InstrNoArgs(OPCODE_RRC))))),
            program
        );
    }

    @Test
    public void testTwoLabelsInsideIf() {
        Program program = parseProgram("if 1\n"
            + "  label1:\n"
            + "  label2:\n"
            + "endif");

        assertTrees(new Program()
                .addChild(new Statement()
                    .addChild(new PseudoIf()
                        .addChild(new ExprNumber(1))
                        .addChild(new Statement()
                            .addChild(new Label("label1")))
                        .addChild(new Statement()
                            .addChild(new Label("label2"))))),
            program
        );
    }

    @Test
    public void testInclude() {
        Program program = parseProgram("include 'filename.asm'");
        assertTrees(new Program()
                .addChild(new Statement()
                    .addChild(new PseudoInclude("filename.asm"))),
            program
        );
    }

    @Test
    public void testMacroDef() {
        Program program = parseProgram("shrt macro param1, param2\n"
            + "  rrc\n"
            + "  heylabel: ani 7Fh\n"
            + "endm\n\n");

        Node expected = new Program()
            .addChild(new Statement()
                .addChild(new PseudoMacroDef("shrt")
                    .addChild(new ExprId("param1"))
                    .addChild(new ExprId("param2"))
                    .addChild(new Statement()
                        .addChild(new InstrNoArgs(OPCODE_RRC)))
                    .addChild(new Statement()
                        .addChild(new Label("heylabel"))
                        .addChild(new InstrExpr(OPCODE_ANI)
                            .addChild(new ExprNumber(0x7F))))))
            .addChild(new Statement());

        assertTrees(expected, program);
    }

    @Test
    public void testMacroCallNoParams() {
        Program program = parseProgram("shrt");

        Node expected = new Program()
            .addChild(new Statement()
                .addChild(new PseudoMacroCall("shrt")));

        assertTrees(expected, program);
    }

    @Test
    public void testMacroCallWithParams() {
        Program program = parseProgram("shrt param1, 45");

        Node expected = new Program()
            .addChild(new Statement()
                .addChild(new PseudoMacroCall("shrt")
                    .addChild(new ExprId("param1"))
                    .addChild(new ExprNumber(45))));

        assertTrees(expected, program);
    }
}
