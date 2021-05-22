package net.emustudio.plugins.compiler.ssem.ast;

import net.emustudio.plugins.compiler.ssem.SSEMLexer;
import net.emustudio.plugins.compiler.ssem.SSEMParser;
import org.antlr.v4.runtime.ANTLRErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProgramParserTest {

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
    public void testParseInstructions() {
        Program program = parseProgram(
          "01 LDN 29\n" +
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
        Map<Integer, Instruction> instructions = program.getInstructions();
        assertEquals(7, instructions.size());
        assertEquals(new Instruction(SSEMParser.LDN, 29), instructions.get(1));
        assertEquals(new Instruction(SSEMParser.SUB, 30), instructions.get(4));
        assertEquals(new Instruction(SSEMParser.STO, 31), instructions.get(5));
        assertEquals(new Instruction(SSEMParser.STP), instructions.get(6));
        assertEquals(new Instruction(SSEMParser.CMP), instructions.get(7));
        assertEquals(new Instruction(SSEMParser.NUM, 4), instructions.get(8));
        assertEquals(new Instruction(SSEMParser.BNUM, 4), instructions.get(9));
    }

    @Test
    public void testParseError() {
        String program = "001 B INS 011010";
        SSEMLexer lexer = new SSEMLexer(CharStreams.fromString(program));
        lexer.removeErrorListeners();
        List<Token> tokens = new ArrayList<>();
        while (!lexer._hitEOF) {
            tokens.add(lexer.nextToken());
        }
        System.out.println(tokens);
    }

    private void printProgram(ByteBuffer program) {
        byte[] array = program.array();
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 4; j++) {
                System.out.printf("%02d ", array[i * 4 + j]);
            }
            System.out.println();
        }
    }

    private Program parseProgram(String program) {
        return parse(createParser(program));
    }

    private Program parseProgram(String program, ANTLRErrorStrategy errorHandler) {
        SSEMParser parser = createParser(program);
        parser.setErrorHandler(errorHandler);
        return parse(parser);
    }

    private SSEMParser createParser(String program) {
        SSEMLexer lexer = new SSEMLexer(CharStreams.fromString(program));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new SSEMParser(tokens);
    }

    private Program parse(SSEMParser parser) {
        ParseTree tree = parser.start();
        ProgramParser programParser = new ProgramParser();
        programParser.visit(tree);
        return programParser.getProgram();
    }
}
