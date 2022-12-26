/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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

        String absoluteFileName = findAbsoluteFileName(node.filename);
        try {
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
        return inputFileName
                .map(f -> f.replace("/", File.separator))
                .map(f -> f.replace("\\", File.separator))
                .map(File::new)
                .map(File::getParentFile)
                .map(File::toPath)
                .map(p -> p.resolve(includeFileNameNormalized))
                .map(Path::toString)
                .orElse(includeFileNameNormalized);
    }
}
