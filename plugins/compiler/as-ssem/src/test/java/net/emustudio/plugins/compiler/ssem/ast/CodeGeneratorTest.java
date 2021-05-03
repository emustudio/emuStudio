package net.emustudio.plugins.compiler.ssem.ast;

import net.emustudio.plugins.compiler.ssem.SSEMLexer;
import net.emustudio.plugins.compiler.ssem.SSEMParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

import java.nio.ByteBuffer;

public class CodeGeneratorTest {

    @Test
    public void testSomething() {
        String program =
            "10 start\n" +
                "10 stp";

        SSEMLexer lexer = new SSEMLexer(CharStreams.fromString(program));
        lexer.addErrorListener(new ConsoleErrorListener());

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SSEMParser parser = new SSEMParser(tokens);

        parser.addErrorListener(new ConsoleErrorListener());
        ParseTree tree = parser.start();

        ProgramParser programParser = new ProgramParser();
        programParser.visit(tree);

        Program programx = programParser.getProgram();
        System.out.println(programx);

        CodeGenerator code = new CodeGenerator();
        ByteBuffer buffer = code.generateCode(programx);

        printProgram(buffer);
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
}
