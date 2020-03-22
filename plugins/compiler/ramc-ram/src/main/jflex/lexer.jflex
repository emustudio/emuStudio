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
    public TokenImpl getSymbol() throws IOException {
        return next_token();
    }

    @Override
    public void reset(Reader in, int yyline, int yychar, int yycolumn) {
        yyreset(in);
        this.yyline = yyline;
        this.yychar = yychar;
        this.yycolumn = yycolumn;
        yybegin(YYINITIAL);
    }
    
    @Override
    public void reset() {
        yyline = yychar = yycolumn = 0;
        yybegin(YYINITIAL);
    }

    private TokenImpl token(int id, int category, boolean initial) {
        Location left = new Location("", yyline+1,yycolumn+1,yychar);
        Location right= new Location("", yyline+1,yycolumn+yylength(), yychar+yylength());
        return new TokenImpl(id, category, yytext(), left, right, initial);
    }

    private TokenImpl token(int id, int category, Object value, boolean initial) {
        Location left = new Location("", yyline+1,yycolumn+1,yychar);
        Location right= new Location("", yyline+1,yycolumn+yylength(), yychar+yylength());
        return new TokenImpl(id, category, yytext(), left, right, value, initial);
    }
%}

%eofval{
  return token(TokenImpl.EOF, Token.TEOF, true);
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

<YYINITIAL> "halt"  { return token(TokenImpl.HALT, Token.RESERVED,true); }
<YYINITIAL> "read"  { return token(TokenImpl.READ, Token.RESERVED,true); }
<YYINITIAL> "write" { return token(TokenImpl.WRITE, Token.RESERVED,true); }
<YYINITIAL> "load"  { return token(TokenImpl.LOAD, Token.RESERVED,true); }
<YYINITIAL> "store" { return token(TokenImpl.STORE, Token.RESERVED,true); }
<YYINITIAL> "add"   { return token(TokenImpl.ADD,Token.RESERVED,true); }
<YYINITIAL> "sub"   { return token(TokenImpl.SUB, Token.RESERVED,true); }
<YYINITIAL> "mul"   { return token(TokenImpl.MUL, Token.RESERVED,true); }
<YYINITIAL> "div"   { return token(TokenImpl.DIV, Token.RESERVED,true); }
<YYINITIAL> "jmp"   { yybegin(IDENTIFIER); return token(TokenImpl.JMP, Token.RESERVED,true); }
<YYINITIAL> "jgtz"  { yybegin(IDENTIFIER); return token(TokenImpl.JGTZ, Token.RESERVED,true); }
<YYINITIAL> "jz"    { yybegin(IDENTIFIER); return token(TokenImpl.JZ, Token.RESERVED,true); }

<YYINITIAL> "="     { yybegin(STRING); return token(TokenImpl.DIRECT, Token.OPERATOR,true); }
<YYINITIAL> "*"     { return token(TokenImpl.INDIRECT, Token.OPERATOR,true); }

<YYINITIAL> "<input>" { yybegin(INPUT); return token(TokenImpl.INPUT, Token.PREPROCESSOR, true); }

{WhiteSpace}   { }
<YYINITIAL> {Eol} { return token(TokenImpl.EOL, Token.SEPARATOR,null,true); }
{Eol} { yybegin(YYINITIAL); return token(TokenImpl.EOL, Token.SEPARATOR,null,false); }

<YYINITIAL> {Comment} { yybegin(YYINITIAL); return token(TokenImpl.TCOMMENT, Token.COMMENT,true); }
{Comment}             { yybegin(YYINITIAL); return token(TokenImpl.TCOMMENT, Token.COMMENT,false); }

<YYINITIAL> {Number} { return token(TokenImpl.NUMBER, Token.LITERAL,yytext(),true); }
<YYINITIAL> {Label} { return token(TokenImpl.LABELL, Token.LABEL,yytext(),true); }
<IDENTIFIER> {Identifier} { yybegin(YYINITIAL); return token(TokenImpl.IDENT, Token.IDENTIFIER,yytext(),false); }

<STRING> {String} { yybegin(YYINITIAL); return token(TokenImpl.STRING, Token.LITERAL,yytext(),false); }
<STRING> {MultiSpaceString} {
          yybegin(YYINITIAL);
          String tmp = yytext();
          return token(TokenImpl.STRING, Token.LITERAL,tmp.substring(1,tmp.length()-1),false); }

<INPUT> {String} { return token(TokenImpl.STRING, Token.PREPROCESSOR,yytext(),false); }
<INPUT> {MultiSpaceString} {
          String tmp = yytext();
          return token(TokenImpl.STRING, Token.PREPROCESSOR,tmp.substring(1,tmp.length()-1),false); }

. { return token(TokenImpl.error, TokenImpl.ERROR,false); }

