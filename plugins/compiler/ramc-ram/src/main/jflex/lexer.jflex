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
package net.emustudio.plugins.compiler.ramc;

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
%states STRING,IDENTIFIER, INPUT

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

    private TokenImpl token(int id, int category, Object value) {
        Location left = new Location("", yyline+1,yycolumn+1, (int)yychar);
        Location right= new Location("", yyline+1,yycolumn+yylength(), (int)yychar+yylength());
        return new TokenImpl(id, category, zzLexicalState, yytext(), left, right, value);
    }
%}

%eofval{
  return token(TokenImpl.EOF, Token.TEOF);
%eofval}


Comment    = ";"[^\r\n]*
Eol        = \n|\r|\r\n
WhiteSpace = [\ \t\f]
Number     = [0-9]+

Identifier =([a-zA-Z_\?@])[a-zA-Z_\?@0-9]*
Label ={Identifier}[\:]
String = [^\ \t\f\n\r;]+
MultiSpaceString = \"[^\"]*\"

%%

<YYINITIAL> "halt"  { return token(TokenImpl.HALT, Token.RESERVED); }
<YYINITIAL> "read"  { return token(TokenImpl.READ, Token.RESERVED); }
<YYINITIAL> "write" { return token(TokenImpl.WRITE, Token.RESERVED); }
<YYINITIAL> "load"  { return token(TokenImpl.LOAD, Token.RESERVED); }
<YYINITIAL> "store" { return token(TokenImpl.STORE, Token.RESERVED); }
<YYINITIAL> "add"   { return token(TokenImpl.ADD,Token.RESERVED); }
<YYINITIAL> "sub"   { return token(TokenImpl.SUB, Token.RESERVED); }
<YYINITIAL> "mul"   { return token(TokenImpl.MUL, Token.RESERVED); }
<YYINITIAL> "div"   { return token(TokenImpl.DIV, Token.RESERVED); }
<YYINITIAL> "jmp"   { yybegin(IDENTIFIER); return token(TokenImpl.JMP, Token.RESERVED); }
<YYINITIAL> "jgtz"  { yybegin(IDENTIFIER); return token(TokenImpl.JGTZ, Token.RESERVED); }
<YYINITIAL> "jz"    { yybegin(IDENTIFIER); return token(TokenImpl.JZ, Token.RESERVED); }

<YYINITIAL> "="     { yybegin(STRING); return token(TokenImpl.DIRECT, Token.OPERATOR); }
<YYINITIAL> "*"     { return token(TokenImpl.INDIRECT, Token.OPERATOR); }

<YYINITIAL> "<input>" { yybegin(INPUT); return token(TokenImpl.INPUT, Token.PREPROCESSOR); }

{WhiteSpace}   { }
<YYINITIAL> {Eol} { return token(TokenImpl.EOL, Token.SEPARATOR,null); }
{Eol} { yybegin(YYINITIAL); return token(TokenImpl.EOL, Token.SEPARATOR,null); }

<YYINITIAL> {Comment} { yybegin(YYINITIAL); return token(TokenImpl.TCOMMENT, Token.COMMENT); }
{Comment}             { yybegin(YYINITIAL); return token(TokenImpl.TCOMMENT, Token.COMMENT); }

<YYINITIAL> {Number} { return token(TokenImpl.NUMBER, Token.LITERAL,yytext()); }
<YYINITIAL> {Label} { return token(TokenImpl.LABELL, Token.LABEL,yytext()); }
<IDENTIFIER> {Identifier} { yybegin(YYINITIAL); return token(TokenImpl.IDENT, Token.IDENTIFIER,yytext()); }

<STRING> {String} { yybegin(YYINITIAL); return token(TokenImpl.STRING, Token.LITERAL,yytext()); }
<STRING> {MultiSpaceString} {
          yybegin(YYINITIAL);
          String tmp = yytext();
          return token(TokenImpl.STRING, Token.LITERAL,tmp.substring(1,tmp.length()-1)); }

<INPUT> {String} { return token(TokenImpl.STRING, Token.PREPROCESSOR,yytext()); }
<INPUT> {MultiSpaceString} {
          String tmp = yytext();
          return token(TokenImpl.STRING, Token.PREPROCESSOR,tmp.substring(1,tmp.length()-1)); }

. { return token(TokenImpl.error, TokenImpl.ERROR); }
