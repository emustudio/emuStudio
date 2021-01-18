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
package net.emustudio.plugins.compiler.brainc;

import java_cup.runtime.ComplexSymbolFactory.Location;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.Token;

import java.io.Reader;
import java.io.IOException;

%%

/* options */
%class LexerImpl
%cup
%public
%implements LexicalAnalyzer
%line
%column
%char
%caseless
%unicode
%type TokenImpl

%{
    @Override
    public Token getToken() throws IOException {
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
    public void reset(Reader in, int line, int offset, int column, int lexerState) {
        yyreset(in);
        this.yyline = line;
        this.yychar = offset;
        this.yycolumn = column;
        this.zzLexicalState = lexerState;
    }

    private TokenImpl token(int id, int category) {
        Location left = new Location("", yyline+1,yycolumn+1, (int)yychar);
        Location right= new Location("", yyline+1,yycolumn+yylength(), (int)yychar+yylength());
        return new TokenImpl(id, category, zzLexicalState, yytext(), left, right);
    }
%}
%eofval{
    return token(TokenImpl.EOF, Token.TEOF);
%eofval}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace     = {LineTerminator} | [ \t\f]

Comment        = [^<>+\-\.,\[\]; \t\f\r\n]+ {InputCharacter}*

%%

";"  { return token(TokenImpl.HALT, Token.RESERVED); }
">"  { return token(TokenImpl.INC,  Token.RESERVED); }
"<"  { return token(TokenImpl.DEC,  Token.RESERVED); }
"+"  { return token(TokenImpl.INCV, Token.RESERVED); }
"-"  { return token(TokenImpl.DECV, Token.RESERVED); }
"."  { return token(TokenImpl.PRINT,Token.RESERVED); }
","  { return token(TokenImpl.LOAD, Token.RESERVED); }
"["  { return token(TokenImpl.LOOP, Token.RESERVED); }
"]"  { return token(TokenImpl.ENDL, Token.RESERVED); }

{Comment}          { return token(TokenImpl.TCOMMENT, Token.COMMENT); }
{WhiteSpace}+      { return token(TokenImpl.TCOMMENT, Token.COMMENT); }
{LineTerminator}+  { return token(TokenImpl.TCOMMENT, Token.COMMENT); }
