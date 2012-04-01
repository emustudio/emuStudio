/*
 * LexerBD.java
 *
 * Lexical analyser for BrainDuck assembler
 *
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2009-2012 Peter Jakubčo <pjakubco@gmail.com>
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

package brainc_brainduck.impl;

import emulib.plugins.compiler.ILexer;
import emulib.plugins.compiler.IToken;
import java.io.Reader;
import java.io.IOException;

%%

/* options */
%class LexerBD
%cup
%public
%implements ILexer
%line
%column
%char
%caseless
%unicode
%type TokenBD

%{
    @Override
    public TokenBD getSymbol() throws IOException {
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
        yyline = yychar = yycolumn = 0;
    }
    
    private TokenBD token(int id, int type, Object val,boolean initial) {
        return new TokenBD(id,type,yytext(),yyline,yycolumn,yychar,val,initial);
    }
    
%}

%eofval{
  return token(TokenBD.EOF, IToken.TEOF,null,false);
%eofval}


Comment    = ";"[^\r\n]*
Eol        = \n|\r|\r\n
WhiteSpace = [\ \t\f]
Number     = [0-9]+

%%

"halt"  { return token(TokenBD.HALT, IToken.RESERVED,null,true); }
"inc"   { return token(TokenBD.INC,  IToken.RESERVED,null,true); }
"dec"   { return token(TokenBD.DEC,  IToken.RESERVED,null,true); }
"incv"  { return token(TokenBD.INCV, IToken.RESERVED,null,true); }
"decv"  { return token(TokenBD.DECV, IToken.RESERVED,null,true); }
"print" { return token(TokenBD.PRINT,IToken.RESERVED,null,true); }
"load"  { return token(TokenBD.LOAD, IToken.RESERVED,null,true); }
"loop"  { return token(TokenBD.LOOP, IToken.RESERVED,null,true); }
"endl"  { return token(TokenBD.ENDL, IToken.RESERVED,null,true); }

{WhiteSpace}   { }
{Eol}          { return token(TokenBD.EOL, IToken.SEPARATOR,null,true); }
{Comment}      { return token(TokenBD.TCOMMENT, IToken.COMMENT,null,true); }
{Number}       { return token(TokenBD.NUMBER, IToken.LITERAL,yytext(),true); }

//[^\n\r\ \t\f]+ { return token(TokenBD.error, TokenBD.ERROR); }
.              { return token(TokenBD.error, TokenBD.ERROR,null,false); }

