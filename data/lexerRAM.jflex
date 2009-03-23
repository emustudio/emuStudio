/*
 * RAMLexer.java
 *
 * (c) Copyright 2009, P. Jakubčo
 *
 * Lexical analyser for RAM compiler
 *
 * KISS, YAGNI
 */

package ram.impl;

import plugins.compiler.ILexer;
import plugins.compiler.IToken;
import java.io.Reader;
import java.io.IOException;

%%

/* options */
%class RAMLexer
%cup
%public
%implements ILexer
%line
%column
%char
%caseless
%unicode
%type tokenRAM

%{
    @Override
    public tokenRAM getSymbol() throws IOException {
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
    
    private tokenRAM token(int id, int type, Object val) {
        return new tokenRAM(id,type,yytext(),yyline,yycolumn,yychar,val);
    }
    
%}

%eofval{
  return token(tokenRAM.EOF, IToken.TEOF,null);
%eofval}


Comment    = ";"[^\r\n]*
Eol        = \n|\r|\r\n
WhiteSpace = [\ \t\f]
Number     = [0-9]+

Identifier =([a-zA-Z_\?@])[a-zA-Z_\?@0-9]*
Label ={Identifier}[\:]

%%

"halt"  { return token(tokenRAM.HALT, IToken.RESERVED,null); }
"read"  { return token(tokenRAM.READ,  IToken.RESERVED,null); }
"write" { return token(tokenRAM.WRITE,  IToken.RESERVED,null); }
"load"  { return token(tokenRAM.LOAD, IToken.RESERVED,null); }
"store" { return token(tokenRAM.STORE, IToken.RESERVED,null); }
"add"   { return token(tokenRAM.ADD,IToken.RESERVED,null); }
"sub"   { return token(tokenRAM.SUB, IToken.RESERVED,null); }
"mul"   { return token(tokenRAM.MUL, IToken.RESERVED,null); }
"div"   { return token(tokenRAM.DIV, IToken.RESERVED,null); }
"jmp"   { return token(tokenRAM.JMP, IToken.RESERVED,null); }
"jz"    { return token(tokenRAM.JZ, IToken.RESERVED,null); }

"="     { return token(tokenRAM.DIRECT, IToken.OPERATOR,null); }
"*"     { return token(tokenRAM.INDIRECT, IToken.OPERATOR,null); }

{WhiteSpace}   { }
{Eol}          { return token(tokenRAM.EOL, IToken.SEPARATOR,null); }
{Comment}      { return token(tokenRAM.TCOMMENT, IToken.COMMENT,null); }
{Number}       { return token(tokenRAM.NUMBER, IToken.LITERAL,yytext()); }
{Label}        { return token(tokenRAM.LABELL, IToken.LABEL,yytext()); }
{Identifier}   { return token(tokenRAM.IDENT, IToken.IDENTIFIER,yytext()); }

//[^\n\r\ \t\f]+ { return token(tokenRAM.error, tokenRAM.ERROR); }
.              { return token(tokenRAM.error, tokenRAM.ERROR,null); }

