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
    
    private TokenImpl token(int id, int type, Object val, boolean initial) {
        return new TokenImpl(id,type,yytext(),yyline,yycolumn,yychar,val,initial);
    }
    
%}

%eofval{
  return token(TokenImpl.EOF, Token.TEOF,null,false);
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

<YYINITIAL> "halt"  { return token(TokenImpl.HALT, Token.RESERVED,null,true); }
<YYINITIAL> "read"  { return token(TokenImpl.READ,  Token.RESERVED,null,true); }
<YYINITIAL> "write" { return token(TokenImpl.WRITE,  Token.RESERVED,null,true); }
<YYINITIAL> "load"  { return token(TokenImpl.LOAD, Token.RESERVED,null,true); }
<YYINITIAL> "store" { return token(TokenImpl.STORE, Token.RESERVED,null,true); }
<YYINITIAL> "add"   { return token(TokenImpl.ADD,Token.RESERVED,null,true); }
<YYINITIAL> "sub"   { return token(TokenImpl.SUB, Token.RESERVED,null,true); }
<YYINITIAL> "mul"   { return token(TokenImpl.MUL, Token.RESERVED,null,true); }
<YYINITIAL> "div"   { return token(TokenImpl.DIV, Token.RESERVED,null,true); }
<YYINITIAL> "jmp"   { yybegin(IDENTIFIER); return token(TokenImpl.JMP, Token.RESERVED,null,true); }
<YYINITIAL> "jgtz"  { yybegin(IDENTIFIER); return token(TokenImpl.JGTZ, Token.RESERVED,null,true); }
<YYINITIAL> "jz"    { yybegin(IDENTIFIER); return token(TokenImpl.JZ, Token.RESERVED,null,true); }

<YYINITIAL> "="     { yybegin(STRING); return token(TokenImpl.DIRECT, Token.OPERATOR,null,true); }
<YYINITIAL> "*"     { return token(TokenImpl.INDIRECT, Token.OPERATOR,null,true); }

<YYINITIAL> "<input>" { yybegin(INPUT); return token(TokenImpl.INPUT, Token.PREPROCESSOR, null,true); }

{WhiteSpace}   { }
<YYINITIAL> {Eol} { return token(TokenImpl.EOL, Token.SEPARATOR,null,true); }
{Eol} { yybegin(YYINITIAL); return token(TokenImpl.EOL, Token.SEPARATOR,null,false); }

<YYINITIAL> {Comment} { yybegin(YYINITIAL); return token(TokenImpl.TCOMMENT, Token.COMMENT,null,true); }
{Comment}             { yybegin(YYINITIAL); return token(TokenImpl.TCOMMENT, Token.COMMENT,null,false); }

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


//[^\n\r\ \t\f]+ { return token(TokenImpl.error, TokenImpl.ERROR); }
.              { return token(TokenImpl.error, TokenImpl.ERROR,null,false); }

