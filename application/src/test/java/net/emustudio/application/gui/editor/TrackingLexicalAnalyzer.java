/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;

import java.util.Iterator;

final class TrackingLexicalAnalyzer implements LexicalAnalyzer {
    private final net.emustudio.emulib.plugins.compiler.Token[] tokens;

    int index;
    int nextCalls;
    boolean iteratorUsed;
    String lastResetInput;

    TrackingLexicalAnalyzer(net.emustudio.emulib.plugins.compiler.Token... tokens) {
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
    public void reset(char[] array, int offset, int length) {
        lastResetInput = new String(array, offset, length);
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
