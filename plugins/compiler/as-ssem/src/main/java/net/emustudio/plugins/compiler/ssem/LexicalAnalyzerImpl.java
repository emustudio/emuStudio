/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ssem;

import net.emustudio.emulib.plugin.compiler.Token;
import net.emustudio.emulib.plugin.compiler.antlr.AntlrLexicalAnalyzer;

import static net.emustudio.plugins.compiler.ssem.SSEMLexer.*;

public class LexicalAnalyzerImpl extends AntlrLexicalAnalyzer {
    public static final int[] tokenMap = new int[SSEMLexer.BERROR + 1];

    static {
        tokenMap[COMMENT] = Token.COMMENT;
        tokenMap[EOL] = Token.WHITESPACE;
        tokenMap[WS] = Token.WHITESPACE;
        tokenMap[BWS] = Token.WHITESPACE;
        tokenMap[JMP] = Token.RESERVED;
        tokenMap[JPR] = Token.RESERVED;
        tokenMap[LDN] = Token.RESERVED;
        tokenMap[STO] = Token.RESERVED;
        tokenMap[SUB] = Token.RESERVED;
        tokenMap[CMP] = Token.RESERVED;
        tokenMap[STP] = Token.RESERVED;
        tokenMap[START] = Token.LABEL;
        tokenMap[NUM] = Token.PREPROCESSOR;
        tokenMap[BNUM] = Token.PREPROCESSOR;
        tokenMap[NUMBER] = Token.LITERAL;
        tokenMap[HEXNUMBER] = Token.LITERAL;
        tokenMap[BinaryNumber] = Token.LITERAL;
        tokenMap[ERROR] = Token.ERROR;
        tokenMap[BERROR] = Token.ERROR;
    }

    public LexicalAnalyzerImpl(SSEMLexer lexer) {
        super(lexer, tokenMap, ";");
    }
}
