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
    
    private tokenBD token(int id, int type, Object val) {
        return new tokenBD(id,type,yytext(),yyline,yycolumn,yychar,val);
    }
    
%}

%eofval{
  return token(tokenBD.EOF, IToken.TEOF,null);
%eofval}


Comment    = ";"[^\r\n]*
Eol        = \n|\r|\r\n
WhiteSpace = [\ \t\f]
Number     = [0-9]+

%%

"halt"  { return token(tokenBD.HALT, IToken.RESERVED,null); }
"inc"   { return token(tokenBD.INC,  IToken.RESERVED,null); }
"dec"   { return token(tokenBD.DEC,  IToken.RESERVED,null); }
"incv"  { return token(tokenBD.INCV, IToken.RESERVED,null); }
"decv"  { return token(tokenBD.DECV, IToken.RESERVED,null); }
"print" { return token(tokenBD.PRINT,IToken.RESERVED,null); }
"load"  { return token(tokenBD.LOAD, IToken.RESERVED,null); }
"loop"  { return token(tokenBD.LOOP, IToken.RESERVED,null); }
"endl"  { return token(tokenBD.ENDL, IToken.RESERVED,null); }

{WhiteSpace}   { }
{Eol}          { return token(tokenBD.EOL, IToken.SEPARATOR,null); }
{Comment}      { return token(tokenBD.TCOMMENT, IToken.COMMENT,null); }
{Number}       { return token(tokenBD.NUMBER, IToken.LITERAL,yytext()); }

//[^\n\r\ \t\f]+ { return token(tokenBD.error, tokenBD.ERROR); }
.              { return token(tokenBD.error, tokenBD.ERROR,null); }

