/*
 * Lexical analyser for BrainDuck assembler
 *
 * Copyright (C) 2009-2014 Peter Jakubčo
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

package net.sf.emustudio.brainduck.brainc.impl;

import emulib.plugins.compiler.LexicalAnalyzer;
import emulib.plugins.compiler.Token;
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
%type Tokens

%{
    @Override
    public Tokens getSymbol() throws IOException {
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
    
    private Tokens token(int id, int type, Object val,boolean initial) {
        return new Tokens(id,type,yytext(),yyline,yycolumn,yychar,val,initial);
    }
    
%}

%eofval{
  return token(Tokens.EOF, Token.TEOF, null, false);
%eofval}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace     = {LineTerminator} | [ \t\f]

Comment        = {WhiteSpace}+ | {LineTerminator}+ | [^<>+\-\.,\[\];\t\f\r\n]+ {InputCharacter}*

%%

";"  { return token(Tokens.HALT, Token.RESERVED,null,true); }
">"   { return token(Tokens.INC,  Token.RESERVED,null,true); }
"<"   { return token(Tokens.DEC,  Token.RESERVED,null,true); }
"+"  { return token(Tokens.INCV, Token.RESERVED,null,true); }
"-"  { return token(Tokens.DECV, Token.RESERVED,null,true); }
"." { return token(Tokens.PRINT,Token.RESERVED,null,true); }
","  { return token(Tokens.LOAD, Token.RESERVED,null,true); }
"["  { return token(Tokens.LOOP, Token.RESERVED,null,true); }
"]"  { return token(Tokens.ENDL, Token.RESERVED,null,true); }

{Comment}      { return token(Tokens.TCOMMENT, Token.COMMENT,null,true); }

