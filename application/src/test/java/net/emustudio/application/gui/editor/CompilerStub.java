/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.emulib.plugin.compiler.Compiler;
import net.emustudio.emulib.plugin.compiler.CompilerListener;
import net.emustudio.emulib.plugin.compiler.LexicalAnalyzer;
import net.emustudio.emulib.io.FileType;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

final class CompilerStub implements Compiler {
    private final LexicalAnalyzer lexer;
    private final List<FileType> fileTypes;

    CompilerStub(LexicalAnalyzer lexer) {
        this(lexer, Collections.emptyList());
    }

    CompilerStub(LexicalAnalyzer lexer, List<FileType> fileTypes) {
        this.lexer = lexer;
        this.fileTypes = List.copyOf(fileTypes);
    }

    @Override
    public void addCompilerListener(CompilerListener listener) {
    }

    @Override
    public void removeCompilerListener(CompilerListener listener) {
    }

    @Override
    public void compile(Path inputPath, Optional<Path> outputPath) {
    }

    @Override
    public LexicalAnalyzer createLexer() {
        return lexer;
    }

    @Override
    public List<FileType> getSourceFileTypes() {
        return fileTypes;
    }

    @Override
    public String getTitle() {
        return "test";
    }
}
