/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.compiler.ssem;

import java_cup.runtime.ComplexSymbolFactory.Location;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.Token;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.emulib.runtime.helpers.RadixUtils;

import java.io.IOException;
import java.io.Reader;

%%

/* options */
%class LexerImpl
%cup
%public
%implements LexicalAnalyzer, Symbols
%line
%column
%char
%caseless
%unicode
%type TokenImpl

%{
    @Override
    public Token getSymbol() throws IOException {
        return next_token();
    }

    @Override
    public void reset(Reader in, int yyline, int yychar, int yycolumn) {
        yyreset(in);
        this.yyline = yyline;
        this.yychar = yychar;
        this.yycolumn = yycolumn;
    }

    @Override
    public void reset() {
        this.yyline = 0;
        this.yychar = 0;
        this.yycolumn = 0;
    }

    private TokenImpl token(int id, int category) {
        Location left = new Location("", yyline+1,yycolumn+1,yychar);
        Location right= new Location("", yyline+1,yycolumn+yylength(), yychar+yylength());
        return new TokenImpl(id, category, yytext(), left, right);
    }

    private TokenImpl token(int id, int category, Object value) {
        Location left = new Location("", yyline+1,yycolumn+1,yychar);
        Location right= new Location("", yyline+1,yycolumn+yylength(), yychar+yylength());
        return new TokenImpl(id, category, yytext(), left, right, value);
    }
%}

%eofval{
    return token(EOF, Token.TEOF);
%eofval}

comment = "//"[^\r\n]*
comment2 = "--"[^\r\n]*
comment3 = ";"[^\r\n]*
eol = \r|\n|\r\n
space = [ \t\f]+
number = \-?[0-9]+
hexnumber = \-?0x[0-9a-fA-F]+
binnumber = [01]+

%xstate BIN

%%

<YYINITIAL> {
    /* reserved words */
    "jmp" {
        return token(JMP, Token.RESERVED);
    }
    "jrp" {
        return token(JPR, Token.RESERVED);
    }
    "jpr" {
        return token(JPR, Token.RESERVED);
    }
    "jmr" {
        return token(JPR, Token.RESERVED);
    }
    "ldn" {
        return token(LDN, Token.RESERVED);
    }
    "sto" {
        return token(STO, Token.RESERVED);
    }
    "sub" {
        return token(SUB, Token.RESERVED);
    }
    "cmp" {
        return token(CMP, Token.RESERVED);
    }
    "skn" {
        return token(CMP, Token.RESERVED);
    }
    "stp" {
        return token(STP, Token.RESERVED);
    }
    "hlt" {
        return token(STP, Token.RESERVED);
    }

    /* special */
    "start:" {
        return token(START, Token.PREPROCESSOR);
    }
    "num" {
        return token(NUM, Token.PREPROCESSOR);
    }
    "bnum" {
        yybegin(BIN);
        return token(BNUM, Token.PREPROCESSOR);
    }
    "bins" {
        yybegin(BIN);
        return token(BNUM, Token.PREPROCESSOR);
    }

    /* comment */
    {comment} {
        return token(TCOMMENT, Token.COMMENT);
    }
    {comment2} {
        return token(TCOMMENT, Token.COMMENT);
    }
    {comment3} {
        return token(TCOMMENT, Token.COMMENT);
    }

    /* literals */
    {number} {
        int num = Integer.parseInt(yytext(), 10);
        return token(NUMBER, Token.LITERAL, num);
    }

    {hexnumber} {
        int num = Integer.decode(yytext());
        return token(NUMBER, Token.LITERAL, num);
    }
}

/* separators */
<YYINITIAL, BIN> {eol} {
    return token(SEPARATOR_EOL, Token.SEPARATOR);
}
<YYINITIAL, BIN> {space} { /* ignore white spaces */ }

<BIN> {

    {binnumber} {
        yybegin(YYINITIAL);

        byte[] numberArray = RadixUtils.convertToNumber(yytext(), 2, 4);
        int num = NumberUtils.reverseBits(
            NumberUtils.readInt(
                NumberUtils.toObjectArray(numberArray), NumberUtils.Strategy.LITTLE_ENDIAN
            ), 32
        );

        return token(NUMBER, Token.LITERAL, num);
    }

    [^] {
        yybegin(YYINITIAL);
    }

}

/* error fallback */
[^] {
    return token(ERROR_UNKNOWN_TOKEN, Token.ERROR);
}
