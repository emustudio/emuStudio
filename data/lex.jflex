/*
 * BDLexer.java
 *
 * (c) Copyright 2009, P. Jakubčo
 *
 * Lexical analyser for BrainDuck assembler
 *
 * KISS, YAGNI
 */

package brainduck.impl;

import plugins.compiler.ILexer;
import plugins.compiler.IToken;
import java.io.Reader;
import java.io.IOException;

%%

/* options */
%class BDLexer
%cup
%public
%implements ILexer
%line
%column
%char
%caseless
%unicode
%type tokenBD

%{
    @Override
    public tokenBD getSymbol() throws IOException {
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
    
    private tokenBD token(int id, int type) {
        return new tokenBD(id,type,yytext(),yyline,yycolumn,yychar);
    }
    
%}

%eofval{
  return token(tokenBD.EOF, IToken.TEOF);
%eofval}


Comment    = ";"[^\r\n]*
Eol        = \n|\r|\r\n
WhiteSpace = [\ \t\f]

%%

"inc"   { return token(tokenBD.INC,  IToken.RESERVED); }
"dec"   { return token(tokenBD.DEC,  IToken.RESERVED); }
"incv"  { return token(tokenBD.INCV, IToken.RESERVED); }
"decv"  { return token(tokenBD.DECV, IToken.RESERVED); }
"print" { return token(tokenBD.PRINT,IToken.RESERVED); }
"load"  { return token(tokenBD.LOAD, IToken.RESERVED); }
"loop"  { return token(tokenBD.LOOP, IToken.RESERVED); }
"endl"  { return token(tokenBD.ENDL, IToken.RESERVED); }

{WhiteSpace}   { }
{Eol}          { return token(tokenBD.EOL, IToken.SEPARATOR); }
{Comment}      { return token(tokenBD.TCOMMENT, IToken.COMMENT); }

//[^\n\r\ \t\f]+ { return token(tokenBD.error, tokenBD.ERROR); }
.              { return token(tokenBD.error, tokenBD.ERROR); }

