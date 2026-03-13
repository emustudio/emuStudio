/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.CompilerListener;
import net.emustudio.emulib.plugins.compiler.FileExtension;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import org.junit.Test;

import javax.swing.*;
import javax.swing.text.Segment;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.fife.ui.rsyntaxtextarea.Token.IDENTIFIER;
import static org.fife.ui.rsyntaxtextarea.Token.NULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class RTokenMakerTest {

    @Test
    public void testGetTokenListConsumesLexerDirectly() {
        TrackingLexicalAnalyzer lexer = new TrackingLexicalAnalyzer(
                token(net.emustudio.emulib.plugins.compiler.Token.IDENTIFIER, 0, "LABEL"),
                token(net.emustudio.emulib.plugins.compiler.Token.EOF, 5, "")
        );
        RTokenMaker tokenMaker = new RTokenMaker(new CompilerStub(lexer));

        org.fife.ui.rsyntaxtextarea.Token tokenList = tokenMaker.getTokenList(
                new Segment("LABEL".toCharArray(), 0, 5), NULL, 0
        );

        assertEquals("LABEL", lexer.lastResetInput);
        assertFalse(lexer.iteratorUsed);
        assertEquals(2, lexer.nextCalls);
        assertNotNull(tokenList);
        assertEquals(IDENTIFIER, tokenList.getType());
        assertNotNull(tokenList.getNextToken());
        assertEquals(NULL, tokenList.getNextToken().getType());
    }

    private static net.emustudio.emulib.plugins.compiler.Token token(int type, int offset, String text) {
        return new net.emustudio.emulib.plugins.compiler.Token() {
            @Override
            public int getType() {
                return type;
            }

            @Override
            public int getOffset() {
                return offset;
            }

            @Override
            public String getText() {
                return text;
            }
        };
    }

    private static final class TrackingLexicalAnalyzer implements LexicalAnalyzer {
        private final net.emustudio.emulib.plugins.compiler.Token[] tokens;

        private int index;
        private int nextCalls;
        private boolean iteratorUsed;
        private String lastResetInput;

        private TrackingLexicalAnalyzer(net.emustudio.emulib.plugins.compiler.Token... tokens) {
            this.tokens = tokens;
        }

        @Override
        public net.emustudio.emulib.plugins.compiler.Token next() {
            nextCalls++;
            return tokens[index++];
        }

        @Override
        public boolean hasNext() {
            return index < tokens.length;
        }

        @Override
        public void reset(InputStream input) throws IOException {
            reset(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }

        @Override
        public void reset(String input) {
            lastResetInput = input;
            index = 0;
            nextCalls = 0;
            iteratorUsed = false;
        }

        @Override
        public Iterator<net.emustudio.emulib.plugins.compiler.Token> iterator() {
            iteratorUsed = true;
            throw new AssertionError("RTokenMaker should consume lexer tokens directly");
        }
    }

    private static final class CompilerStub implements Compiler {
        private final LexicalAnalyzer lexer;

        private CompilerStub(LexicalAnalyzer lexer) {
            this.lexer = lexer;
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
            return Collections.emptyList();
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
}
