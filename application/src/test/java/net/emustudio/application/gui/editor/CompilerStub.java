/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.CompilerListener;
import net.emustudio.emulib.plugins.compiler.FileExtension;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

final class CompilerStub implements Compiler {
    private final LexicalAnalyzer lexer;
    private final List<FileExtension> fileExtensions;

    CompilerStub(LexicalAnalyzer lexer) {
        this(lexer, Collections.emptyList());
    }

    CompilerStub(LexicalAnalyzer lexer, List<FileExtension> fileExtensions) {
        this.lexer = lexer;
        this.fileExtensions = List.copyOf(fileExtensions);
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
    public List<FileExtension> getSourceFileExtensions() {
        return fileExtensions;
    }

    @Override
    public void reset() {
    }

    @Override
    public void initialize() throws PluginInitializationException {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void showSettings(JFrame parent) {
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public String getTitle() {
        return "test";
    }

    @Override
    public String getVersion() {
        return "test";
    }

    @Override
    public String getCopyright() {
        return "test";
    }

    @Override
    public String getDescription() {
        return "test";
    }
}
