/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.AsZ80Lexer;
import net.emustudio.plugins.compiler.asZ80.AsZ80Parser;
import net.emustudio.plugins.compiler.asZ80.ast.Program;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoInclude;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static net.emustudio.plugins.compiler.asZ80.CompileError.couldNotReadFile;
import static net.emustudio.plugins.compiler.asZ80.CompileError.infiniteLoopDetected;

/**
 * Integrate "include" files and remove PseudoInclude
 */
public class ExpandIncludesVisitor extends NodeVisitor {
    private final Set<String> includedFiles;
    private String inputFileName;

    public ExpandIncludesVisitor() {
        this.includedFiles = Collections.emptySet();
    }

    public ExpandIncludesVisitor(Set<String> includedFiles) {
        this.includedFiles = Objects.requireNonNull(includedFiles);
    }

    @Override
    public void visit(Program node) {
        this.inputFileName = node.position.fileName;
        super.visit(node);
    }

    @Override
    public void visit(PseudoInclude node) {
        if (includedFiles.contains(node.filename)) {
            fatalError(infiniteLoopDetected(node, "include"));
        }

        String absoluteFileName = findAbsoluteFileName(node.filename);
        try {
            AsZ80Lexer lexer = new AsZ80Lexer(CharStreams.fromFileName(absoluteFileName));
            CommonTokenStream stream = new CommonTokenStream(lexer);
            AsZ80Parser parser = new AsZ80Parser(stream);
            stream.fill();
            ParseTree tree = parser.rStart();
            Program program = new Program(new SourceCodePosition(node.position.line, node.position.column, absoluteFileName), env);

            new CreateProgramVisitor(program).visit(tree);

            Set<String> alreadyIncludedFiles = new HashSet<>(includedFiles);
            alreadyIncludedFiles.add(node.filename);
            new ExpandIncludesVisitor(alreadyIncludedFiles).visit(program);

            node.addChildren(program.getChildren());
            node.exclude();
        } catch (IOException e) {
            error(couldNotReadFile(node, absoluteFileName, e));
        }
    }

    private String findAbsoluteFileName(String includeFileName) {
        File includeFile = new File(includeFileName);
        if (includeFile.isAbsolute()) {
            return includeFileName;
        }

        String includeFileNameNormalized = includeFileName
                .replace("/", File.separator)
                .replace("\\", File.separator);

        if (inputFileName != null) {
            String inputFixed = inputFileName.replace("/", File.separator).replace("\\", File.separator);
            File parentFile = new File(inputFixed).getParentFile();
            if (parentFile != null) {
                return parentFile.toPath().resolve(includeFileNameNormalized).toString();
            }
            return includeFileNameNormalized;
        } else {
            return includeFileNameNormalized;
        }
    }
}
