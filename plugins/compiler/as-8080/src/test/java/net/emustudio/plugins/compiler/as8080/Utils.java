package net.emustudio.plugins.compiler.as8080;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class Utils {
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
        stream.fill();
        return parser.rStart();
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
        for (String variation : variations) {
            assertTokenTypes(variation, expectedTypes);
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
}
