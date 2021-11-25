package net.emustudio.plugins.compiler.as8080;

import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

public class ParserTest {

    @Test
    public void testParse() {
        ParseTree tree = Utils.parse("stc\nmvi a, 5");

        System.out.println(tree.toStringTree());

    }
}
