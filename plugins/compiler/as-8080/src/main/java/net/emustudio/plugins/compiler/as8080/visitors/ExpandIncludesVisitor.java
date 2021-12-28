package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Lexer;
import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoInclude;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static net.emustudio.plugins.compiler.as8080.CompileError.couldNotReadFile;
import static net.emustudio.plugins.compiler.as8080.CompileError.infiniteLoopDetected;

/**
 * Integrate "include" files and remove PseudoInclude
 */
public class ExpandIncludesVisitor extends NodeVisitor {
    private final Set<String> includedFiles; // TODO: windows platform case-insensitive!

    public ExpandIncludesVisitor() {
        this.includedFiles = Collections.emptySet();
    }

    public ExpandIncludesVisitor(Set<String> includedFiles) {
        this.includedFiles = Objects.requireNonNull(includedFiles);
    }

    @Override
    public void visit(PseudoInclude node) {
        if (includedFiles.contains(node.filename)) {
            fatalError(infiniteLoopDetected(node, "include"));
        }

        try {
            As8080Lexer lexer = new As8080Lexer(CharStreams.fromFileName(node.filename));
            CommonTokenStream stream = new CommonTokenStream(lexer);
            As8080Parser parser = new As8080Parser(stream);
            stream.fill();
            ParseTree tree = parser.rStart();
            Program program = new Program(node.line, node.column, env);
            program.setFileName(node.filename);

            new CreateProgramVisitor(program).visit(tree);

            Set<String> alreadyIncludedFiles = new HashSet<>(includedFiles);
            alreadyIncludedFiles.add(node.filename);
            new ExpandIncludesVisitor(alreadyIncludedFiles).visit(program);

            node.remove().ifPresent(parent -> parent.addChildren(program.getChildren())); // TODO: custom error space?
        } catch (IOException e) {
            error(couldNotReadFile(node, node.filename, e));
        }
    }
}
