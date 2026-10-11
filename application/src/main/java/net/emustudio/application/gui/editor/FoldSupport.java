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
        // This named style selects the generic fold parser, even without a compiler.
        ((AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance()).putMapping(
                COMPILER_STYLE, "org.fife.ui.rsyntaxtextarea.modes.PlainTextTokenMaker");
        if (compiler != null) {
            // A per-document factory keeps the named style when loading or sharing a document.
            // RSyntaxDocument.setSyntaxStyle(TokenMaker) instead changes it to "text/unknown".
            TokenMakerFactory fallback = TokenMakerFactory.getDefaultInstance();
            ((RSyntaxDocument) pane.getDocument()).setTokenMakerFactory(new TokenMakerFactory() {
                @Override protected TokenMaker getTokenMakerImpl(String key) {
                    return COMPILER_STYLE.equals(key) ? new RTokenMaker(compiler) : fallback.getTokenMaker(key);
                }
                @Override public java.util.Set<String> keySet() { return fallback.keySet(); }
            });
        }
        pane.setSyntaxEditingStyle(COMPILER_STYLE);
        pane.setCodeFoldingEnabled(true);
    }
}
