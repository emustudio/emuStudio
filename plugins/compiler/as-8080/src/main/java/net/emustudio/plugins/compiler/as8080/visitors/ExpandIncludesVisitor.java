package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.As8080Lexer;
import net.emustudio.plugins.compiler.as8080.As8080Parser;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoInclude;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static net.emustudio.plugins.compiler.as8080.CompileError.couldNotReadFile;
import static net.emustudio.plugins.compiler.as8080.CompileError.infiniteLoopDetected;

/**
 * Integrate "include" files and remove PseudoInclude
 */
public class ExpandIncludesVisitor extends NodeVisitor {
    private final Set<String> includedFiles;
    private Optional<String> inputFileName = Optional.empty();

    public ExpandIncludesVisitor() {
        this.includedFiles = Collections.emptySet();
    }

    public ExpandIncludesVisitor(Set<String> includedFiles) {
        this.includedFiles = Objects.requireNonNull(includedFiles);
    }

    @Override
    public void visit(Program node) {
        this.inputFileName = node.getFileName();
        super.visit(node);
    }

    @Override
    public void visit(PseudoInclude node) {
        if (includedFiles.contains(node.filename)) {
            fatalError(infiniteLoopDetected(node, "include"));
        }

        try {
            String absoluteFileName = findAbsoluteFileName(node.filename);

            As8080Lexer lexer = new As8080Lexer(CharStreams.fromFileName(absoluteFileName));
            CommonTokenStream stream = new CommonTokenStream(lexer);
            As8080Parser parser = new As8080Parser(stream);
            stream.fill();
            ParseTree tree = parser.rStart();
            Program program = new Program(node.line, node.column, env);
            program.setFileName(absoluteFileName);

            new CreateProgramVisitor(program).visit(tree);

            Set<String> alreadyIncludedFiles = new HashSet<>(includedFiles);
            alreadyIncludedFiles.add(node.filename);
            new ExpandIncludesVisitor(alreadyIncludedFiles).visit(program);

            node.addChildren(program.getChildren());
            node.exclude();
        } catch (IOException e) {
            error(couldNotReadFile(node, node.filename, e));
        }
    }

    private String findAbsoluteFileName(String includeFileName) {
        File includeFile = new File(includeFileName);
        if (includeFile.isAbsolute()) {
            return includeFileName;
        }

        String includeFileNameNormalized = includeFileName.replace("\\", File.separator);
        return inputFileName
            .map(f -> f.replace("\\", File.separator))
            .map(File::new)
            .map(File::getParentFile)
            .map(File::toPath)
            .map(p -> p.resolve(includeFileNameNormalized))
            .map(Path::toString)
            .orElse(includeFileNameNormalized);
    }
}
