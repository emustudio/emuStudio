/*
 * RAMLexer.java
 *
 * Lexical analyser for RAM compiler
 *
 * Copyright (C) 2009-2012 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package net.sf.emustudio.ram.compiler.impl;

import emulib.plugins.compiler.LexicalAnalyzer;
import emulib.plugins.compiler.Token;
import java.io.Reader;
import java.io.IOException;

%%

/* options */
%class RAMLexer
%cup
%public
%implements LexicalAnalyzer
%line
%column
%char
%caseless
%unicode
%type TokenRAM
%states STRING,IDENTIFIER, INPUT

%{
    @Override
    public TokenRAM getSymbol() throws IOException {
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
    
    private TokenRAM token(int id, int type, Object val, boolean initial) {
        return new TokenRAM(id,type,yytext(),yyline,yycolumn,yychar,val,initial);
    }
    
%}

%eofval{
  return token(TokenRAM.EOF, Token.TEOF,null,false);
%eofval}


Comment    = ";"[^\r\n]*
Eol        = \n|\r|\r\n
WhiteSpace = [\ \t\f]
Number     = [0-9]+

Identifier =([a-zA-Z_\?@])[a-zA-Z_\?@0-9]*
Label ={Identifier}[\:]
String = [^\ \t\f\n\r;]+

%%

<YYINITIAL> "halt"  { return token(TokenRAM.HALT, Token.RESERVED,null,true); }
<YYINITIAL> "read"  { return token(TokenRAM.READ,  Token.RESERVED,null,true); }
<YYINITIAL> "write" { return token(TokenRAM.WRITE,  Token.RESERVED,null,true); }
<YYINITIAL> "load"  { return token(TokenRAM.LOAD, Token.RESERVED,null,true); }
<YYINITIAL> "store" { return token(TokenRAM.STORE, Token.RESERVED,null,true); }
<YYINITIAL> "add"   { return token(TokenRAM.ADD,Token.RESERVED,null,true); }
<YYINITIAL> "sub"   { return token(TokenRAM.SUB, Token.RESERVED,null,true); }
<YYINITIAL> "mul"   { return token(TokenRAM.MUL, Token.RESERVED,null,true); }
<YYINITIAL> "div"   { return token(TokenRAM.DIV, Token.RESERVED,null,true); }
<YYINITIAL> "jmp"   { yybegin(IDENTIFIER); return token(TokenRAM.JMP, Token.RESERVED,null,true); }
<YYINITIAL> "jgtz"  { yybegin(IDENTIFIER); return token(TokenRAM.JGTZ, Token.RESERVED,null,true); }
<YYINITIAL> "jz"    { yybegin(IDENTIFIER); return token(TokenRAM.JZ, Token.RESERVED,null,true); }

<YYINITIAL> "="     { yybegin(STRING); return token(TokenRAM.DIRECT, Token.OPERATOR,null,true); }
<YYINITIAL> "*"     { return token(TokenRAM.INDIRECT, Token.OPERATOR,null,true); }

<YYINITIAL> "<input>" { yybegin(INPUT); return token(TokenRAM.INPUT, Token.PREPROCESSOR, null,true); }

{WhiteSpace}   { }
<YYINITIAL> {Eol} { return token(TokenRAM.EOL, Token.SEPARATOR,null,true); }
{Eol} { yybegin(YYINITIAL); return token(TokenRAM.EOL, Token.SEPARATOR,null,false); }

<YYINITIAL> {Comment} { yybegin(YYINITIAL); return token(TokenRAM.TCOMMENT, Token.COMMENT,null,true); }
{Comment}             { yybegin(YYINITIAL); return token(TokenRAM.TCOMMENT, Token.COMMENT,null,false); }

<YYINITIAL> {Number} { return token(TokenRAM.NUMBER, Token.LITERAL,yytext(),true); }
<YYINITIAL> {Label} { return token(TokenRAM.LABELL, Token.LABEL,yytext(),true); }
<IDENTIFIER> {Identifier} { yybegin(YYINITIAL); return token(TokenRAM.IDENT, Token.IDENTIFIER,yytext(),false); }

<STRING> {String} { yybegin(YYINITIAL); return token(TokenRAM.STRING, Token.LITERAL,yytext(),false); }

<INPUT> {String} { return token(TokenRAM.STRING, Token.PREPROCESSOR,yytext(),false); }


//[^\n\r\ \t\f]+ { return token(TokenRAM.error, TokenRAM.ERROR); }
.              { return token(TokenRAM.error, TokenRAM.ERROR,null,false); }

