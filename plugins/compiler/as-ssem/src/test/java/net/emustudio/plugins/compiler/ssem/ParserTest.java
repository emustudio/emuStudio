package net.emustudio.plugins.compiler.ssem;

import net.emustudio.plugins.compiler.ssem.ast.Program;
import org.junit.Test;

import static net.emustudio.plugins.compiler.ssem.Utils.assertInstructions;
import static net.emustudio.plugins.compiler.ssem.Utils.parseProgram;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParserTest {

    @Test
    public void testParseEmptyLine() {
        Program program = parseProgram("");
        assertTrue(program.getInstructions().isEmpty());
    }

    @Test
    public void testParseLineNumberOnly() {
        Program program = parseProgram("10");
        assertTrue(program.getInstructions().isEmpty());
    }

    @Test
    public void testParseHexLineNumber() {
        Program program = parseProgram("0x10");
        assertTrue(program.getInstructions().isEmpty());
    }

    @Test
    public void testLineThenCommentWorks() {
        Program program = parseProgram("0x10 -- comment");
        assertTrue(program.getInstructions().isEmpty());
    }

    @Test
    public void testCommentOnly() {
        Program program = parseProgram("-- comment");
        assertTrue(program.getInstructions().isEmpty());
    }

    @Test
    public void testEolOnly() {
        Program program = parseProgram("\n");
        assertTrue(program.getInstructions().isEmpty());
    }

    @Test
    public void testParseInstructions() {
        Program program = parseProgram(
            "01 LDN 0x1F\n" +
                "-- 01 ldn 30\n" +
                "; 01 LDN 30\n" +
                "# 01 ldn 30\n" +
                "04 SUB 30 --comment1\n" +
                "05 STO 31 # comment2\n" +
                "06 STP ; comment3\n" +
                "07 CMP\n" +
                "08 NUM 4\n" +
                "09 BNUM 100\n\n\n"
        );
        assertInstructions(
            program,
            new Utils.ParsedInstruction(1, SSEMParser.LDN, 0x1F),
            new Utils.ParsedInstruction(4, SSEMParser.SUB, 30),
            new Utils.ParsedInstruction(5, SSEMParser.STO, 31),
            new Utils.ParsedInstruction(6, SSEMParser.STP, 0),
            new Utils.ParsedInstruction(7, SSEMParser.CMP, 0),
            new Utils.ParsedInstruction(8, SSEMParser.NUM, 4),
            new Utils.ParsedInstruction(9, SSEMParser.BNUM, 4)
        );
    }

    @Test
    public void testStartingPointIsAccepted() {
        Program program = parseProgram(
            "02 start\n" +
                "01 LDN 21\n" +
                "02 STP"
        );
        assertEquals(2, program.getStartLine());
    }

    @Test(expected = CompileException.class)
    public void testOperandBounds() {
        parseProgram("01 ldn 99\n");
    }

    @Test(expected = CompileException.class)
    public void testLineBounds() {
        parseProgram("99 ldn 1\n");
    }

    @Test(expected = CompileException.class)
    public void testParseInstructionWithoutOperand() {
        parseProgram("01 ldn");
    }

    @Test(expected = CompileException.class)
    public void testParseInstructionWithWrongArgument() {
        parseProgram("01 ldn fff");
    }

    @Test(expected = CompileException.class)
    public void testParseInstructionWithoutLine() {
        parseProgram("stp");
    }

    @Test(expected = CompileException.class)
    public void testParseTwoInstructionsWithoutEol() {
        parseProgram("01 stp 02 stp");
    }
}
