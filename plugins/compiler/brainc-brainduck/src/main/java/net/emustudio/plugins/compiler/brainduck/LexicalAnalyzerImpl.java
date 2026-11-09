/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.brainduck;

import net.emustudio.emulib.plugin.compiler.Token;
import net.emustudio.emulib.plugin.compiler.antlr.AntlrLexicalAnalyzer;

import static net.emustudio.plugins.compiler.brainduck.BraincLexer.*;

public class LexicalAnalyzerImpl extends AntlrLexicalAnalyzer {
    public static final int[] tokenMap = new int[COMMENT + 1];

    static {
        tokenMap[COMMENT] = Token.COMMENT;
        tokenMap[WS] = Token.WHITESPACE;
        tokenMap[DEC] = Token.RESERVED;
        tokenMap[DECV] = Token.RESERVED;
        tokenMap[INC] = Token.RESERVED;
        tokenMap[INCV] = Token.RESERVED;
        tokenMap[PRINT] = Token.RESERVED;
        tokenMap[LOAD] = Token.RESERVED;
        tokenMap[LOOP] = Token.BLOCK_START;
        tokenMap[HALT] = Token.RESERVED;
        tokenMap[ENDL] = Token.BLOCK_END;
    }

    public LexicalAnalyzerImpl(BraincLexer lexer) {
        super(lexer, tokenMap, "//");
    }
}
