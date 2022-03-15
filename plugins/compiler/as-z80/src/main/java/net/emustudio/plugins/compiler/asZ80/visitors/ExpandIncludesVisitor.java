package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.asZ80.AsZ80Lexer;
import net.emustudio.plugins.compiler.asZ80.AsZ80Parser;
import net.emustudio.plugins.compiler.asZ80.ast.Program;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoInclude;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static net.emustudio.plugins.compiler.asZ80.CompileError.couldNotReadFile;
import static net.emustudio.plugins.compiler.asZ80.CompileError.infiniteLoopDetected;

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

            AsZ80Lexer lexer = new AsZ80Lexer(CharStreams.fromFileName(absoluteFileName));
            CommonTokenStream stream = new CommonTokenStream(lexer);
            AsZ80Parser parser = new AsZ80Parser(stream);
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

        return inputFileName
            .map(f -> f.replace("\\", File.separator))
            .map(File::new)
            .map(File::getParentFile)
            .map(File::toPath)
            .map(p -> p.resolve(includeFileName))
            .map(Path::toString)
            .orElse(includeFileName);
    }
}
