/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rsyntaxtextarea.folding.FoldParserManager;

final class FoldSupport {
    static final String COMPILER_STYLE = "text/emustudio";
    static void install(RSyntaxTextArea pane, net.emustudio.emulib.plugins.compiler.Compiler compiler) {
        if (compiler != null) pane.putClientProperty("sourceLexer", (java.util.function.Supplier<net.emustudio.emulib.plugins.compiler.LexicalAnalyzer>) compiler::createLexer);
        FoldParserManager.get().addFoldParserMapping(COMPILER_STYLE, new SourceFoldParser());
        // The document's token maker is installed separately; this style selects the fold parser.
        ((AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance()).putMapping(
                COMPILER_STYLE, "org.fife.ui.rsyntaxtextarea.modes.PlainTextTokenMaker");
        pane.setSyntaxEditingStyle(COMPILER_STYLE);
        pane.setCodeFoldingEnabled(true);
    }
}
