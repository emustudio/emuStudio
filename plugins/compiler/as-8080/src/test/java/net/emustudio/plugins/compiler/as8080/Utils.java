package net.emustudio.plugins.compiler.as8080;

import net.emustudio.plugins.compiler.as8080.ast.NameSpace;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.visitors.CreateProgramVisitor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

import static net.emustudio.plugins.compiler.as8080.As8080Parser.*;
import static net.emustudio.plugins.compiler.as8080.As8080Parser.REG_M;
import static org.junit.Assert.assertEquals;

public class Utils {
    public static Map<String, Integer> registers = Map.of(
        "a", REG_A,
        "b", REG_B,
        "c", REG_C,
        "d", REG_D,
        "e", REG_E,
        "h", REG_H,
        "l", REG_L,
        "m", REG_M
    );

    public static Map<String, Integer> regPairsBD = Map.of(
        "b", REG_B,
        "d", REG_D
    );
    public static Map<String, Integer> regPairsBDHSP = Map.of(
        "b", REG_B,
        "d", REG_D,
        "h", REG_H,
        "sp", REG_SP
    );
    public static Map<String, Integer> regPairsBDHPSW = Map.of(
        "b", REG_B,
        "d", REG_D,
        "h", REG_H,
        "psw", REG_PSW
    );


    public static List<Token> getTokens(String variation) {
        As8080Lexer lexer = new As8080Lexer(CharStreams.fromString(variation));
        CommonTokenStream stream = new CommonTokenStream(lexer);
        stream.fill();
        return stream.getTokens();
    }

    public static ParseTree parse(String program) {
        As8080Lexer lexer = new As8080Lexer(CharStreams.fromString(program));
        CommonTokenStream stream = new CommonTokenStream(lexer);
        As8080Parser parser = new As8080Parser(stream);
        parser.removeErrorListeners();
        parser.addErrorListener(new ParserErrorListener());
        stream.fill();
        return parser.rStart();
    }

    public static Program parseProgram(String programString) {
        ParseTree tree = parse(programString);
        Program program = new Program();
        CreateProgramVisitor visitor = new CreateProgramVisitor(program);
        visitor.visit(tree);
        return program;
    }

    public static void assertTokenTypes(String variation, int... expectedTypes) {
        List<Token> tokens = getTokens(variation);
        assertTokenTypes(tokens, expectedTypes);
    }

    public static void assertTokenTypes(List<Token> tokens, int... expectedTypes) {
        assertEquals("Tokens: " + tokens, expectedTypes.length, tokens.size());
        for (int i = 0; i < expectedTypes.length; i++) {
            Token token = tokens.get(i);
            assertEquals(token.toString(), expectedTypes[i], token.getType());
        }
    }

    public static void assertTokenTypesIgnoreCase(String base, int... expectedTypes) {
        forStringCaseVariations(base, variation -> assertTokenTypes(variation, expectedTypes));
    }

    public static void forStringCaseVariations(String base, Consumer<String> f) {
        Random r = new Random();
        List<String> variations = new ArrayList<>();
        variations.add(base);
        variations.add(base.toLowerCase());
        variations.add(base.toUpperCase());
        for (int i = 0; i < 5; i++) {
            byte[] chars = base.getBytes();
            for (int j = 0; j < base.length(); j++) {
                if (r.nextBoolean()) {
                    chars[j] = Character.valueOf((char) chars[j]).toString().toUpperCase().getBytes()[0];
                } else {
                    chars[j] = Character.valueOf((char) chars[j]).toString().toLowerCase().getBytes()[0];
                }
            }
            variations.add(new String(chars));
        }
        for (String variation: variations) {
            f.accept(variation);
        }
    }

    public static void assertTrees(Node expected, Node result) {
        assertEquals("Children size does not match", expected.getChildren().size(), result.getChildren().size());
        assertEquals("Nodes are different", expected.getClass(), result.getClass());
        for (int i = 0; i < expected.getChildren().size(); i++) {
            Node expectedChild = expected.getChild(i);
            Node resultChild = result.getChild(i);
            assertTrees(expectedChild, resultChild);
        }
    }

    public static void write(File file, String content) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(content);
        writer.close();
    }
}
