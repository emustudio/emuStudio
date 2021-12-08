package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Lexer;
import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.ast.NameSpace;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoInclude;
import net.emustudio.plugins.compiler.as8080.exceptions.CompileException;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ExpandIncludesVisitor extends NodeVisitor {
    private NameSpace parentEnv;
    private final Set<String> includedFiles;

    public ExpandIncludesVisitor() {
        this.includedFiles = Collections.emptySet();
    }

    public ExpandIncludesVisitor(Set<String> includedFiles) {
        this.includedFiles = Objects.requireNonNull(includedFiles);
    }

    @Override
    public void visit(Program node) {
        parentEnv = node.env();
        visitChildren(node);
    }

    @Override
    public void visit(PseudoInclude node) {
        if (includedFiles.contains(node.filename)) {
            throw new CompileException(node.line, node.column, "Infinite INCLUDE loop detected");
        }

        try {
            As8080Lexer lexer = new As8080Lexer(CharStreams.fromFileName(node.filename));
            CommonTokenStream stream = new CommonTokenStream(lexer);
            As8080Parser parser = new As8080Parser(stream);
            stream.fill();
            ParseTree tree = parser.rStart();
            Program program = new Program(node.line, node.column, parentEnv);
            program.setFileName(node.filename);

            new CreateProgramVisitor(program).visit(tree);

            Set<String> alreadyIncludedFiles = new HashSet<>(includedFiles);
            alreadyIncludedFiles.add(node.filename);

            new ExpandIncludesVisitor(alreadyIncludedFiles).visit(program);

            node.getParent().ifPresent(parent -> {
                parent.removeChild(node);
                parent.addChild(program);
            });
        } catch (IOException e) {
            throw new CompileException(node.line, node.column, "Could not read file: " + node.filename, e);
        }
    }
}
