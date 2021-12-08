package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Lexer;
import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoInclude;
import net.emustudio.plugins.compiler.as8080.exceptions.CompileException;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;

public class ExpandIncludesVisitor extends NodeVisitor {

    @Override
    public void visit(PseudoInclude node) {
        try {
            As8080Lexer lexer = new As8080Lexer(CharStreams.fromFileName(node.filename));
            CommonTokenStream stream = new CommonTokenStream(lexer);
            As8080Parser parser = new As8080Parser(stream);
            stream.fill();
            ParseTree tree = parser.rStart();
            Program program = new Program(node.line, node.column);

            new CreateProgramVisitor(program).visit(tree);

            node.getParent().ifPresent(parent -> {
                parent.removeChild(node);
                parent.addChild(program);
            });
        } catch (IOException e) {
            throw new CompileException(node.line, node.column, "Could not read file: " + node.filename, e);
        }
    }
}
