/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.rasp;

import net.emustudio.emulib.plugin.compiler.Token;
import net.emustudio.emulib.plugin.compiler.antlr.AntlrLexicalAnalyzer;
import net.emustudio.emulib.plugin.compiler.antlr.CharArrayCharStream;

import static net.emustudio.plugins.compiler.rasp.RASPParser.*;

public class LexicalAnalyzerImpl extends AntlrLexicalAnalyzer {
    public static final int[] tokenMap = new int[ERROR + 1];

    static {
        tokenMap[COMMENT] = Token.COMMENT;
        tokenMap[COMMENT2] = Token.COMMENT;
        tokenMap[EOL] = Token.WHITESPACE;
        tokenMap[WS] = Token.WHITESPACE;
        tokenMap[OPCODE_READ] = Token.RESERVED;
        tokenMap[OPCODE_WRITE] = Token.RESERVED;
        tokenMap[OPCODE_LOAD] = Token.RESERVED;
        tokenMap[OPCODE_STORE] = Token.RESERVED;
        tokenMap[OPCODE_ADD] = Token.RESERVED;
        tokenMap[OPCODE_HALT] = Token.RESERVED;
        tokenMap[OPCODE_DIV] = Token.RESERVED;
        tokenMap[OPCODE_MUL] = Token.RESERVED;
        tokenMap[OPCODE_SUB] = Token.RESERVED;
        tokenMap[OPCODE_JMP] = Token.RESERVED;
        tokenMap[OPCODE_JGTZ] = Token.RESERVED;
        tokenMap[OPCODE_JZ] = Token.RESERVED;

        tokenMap[PREP_ORG] = Token.PREPROCESSOR;
        tokenMap[PREP_INPUT] = Token.PREPROCESSOR;

        tokenMap[OP_CONSTANT] = Token.OPERATOR;

        tokenMap[LIT_NUMBER] = Token.LITERAL;
        tokenMap[LIT_HEXNUMBER_1] = Token.LITERAL;
        tokenMap[LIT_HEXNUMBER_2] = Token.LITERAL;
        tokenMap[LIT_OCTNUMBER] = Token.LITERAL;
        tokenMap[LIT_BINNUMBER] = Token.LITERAL;

        tokenMap[ID_IDENTIFIER] = Token.IDENTIFIER;
        tokenMap[ID_LABEL] = Token.LABEL;

        tokenMap[ERROR] = Token.ERROR;
    }

    public LexicalAnalyzerImpl(RASPLexer lexer) {
        super(lexer, tokenMap, ";");
    }

    @Override
    public java.util.List<Token> analyze(String source) {
        char[] sourceChars = source.toCharArray();
        RASPLexer editingLexer = new RASPLexer(new CharArrayCharStream(sourceChars, 0, sourceChars.length));
        editingLexer.removeErrorListeners();
        RASPParser parser = new RASPParser(new org.antlr.v4.runtime.CommonTokenStream(editingLexer));
        parser.removeErrorListeners(); parser.rStart();
        if (parser.getNumberOfSyntaxErrors() > 0) throw new IllegalArgumentException("Fix syntax errors before refactoring");
        char[] chars = source.toCharArray(); reset(chars, 0, chars.length);
        java.util.List<Token> result = new java.util.ArrayList<>();
        while (hasNext()) {
            Token token = next();
            String name = token.getType() == Token.LABEL ? token.getText().substring(0, token.getText().length() - 1)
                    : token.getType() == Token.IDENTIFIER ? token.getText() : null;
            int type = token.getType() == Token.LABEL ? Token.SYMBOL_DEFINITION
                    : token.getType() == Token.IDENTIFIER ? Token.SYMBOL_REFERENCE : token.getType();
            result.add(new net.emustudio.emulib.plugin.compiler.SourceToken(type, token.getOffset(), token.getText(), name,
                    name == null ? null : "", name == null ? null : ""));
        }
        return result;
    }

    @Override
    public String normalizeSymbolName(String name) { return name.toLowerCase(java.util.Locale.ROOT); }

    @Override
    public boolean isValidSymbolName(String name) {
        RASPLexer validation = new RASPLexer(org.antlr.v4.runtime.CharStreams.fromString(name));
        validation.removeErrorListeners();
        return validation.nextToken().getType() == RASPLexer.ID_IDENTIFIER
                && validation.nextToken().getType() == org.antlr.v4.runtime.Token.EOF;
    }
}
