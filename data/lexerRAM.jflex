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
%states STRING,IDENTIFIER

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
String = [^\ \t\f\n\r;]+

%%

<YYINITIAL> "halt"  { return token(tokenRAM.HALT, IToken.RESERVED,null); }
<YYINITIAL> "read"  { return token(tokenRAM.READ,  IToken.RESERVED,null); }
<YYINITIAL> "write" { return token(tokenRAM.WRITE,  IToken.RESERVED,null); }
<YYINITIAL> "load"  { return token(tokenRAM.LOAD, IToken.RESERVED,null); }
<YYINITIAL> "store" { return token(tokenRAM.STORE, IToken.RESERVED,null); }
<YYINITIAL> "add"   { return token(tokenRAM.ADD,IToken.RESERVED,null); }
<YYINITIAL> "sub"   { return token(tokenRAM.SUB, IToken.RESERVED,null); }
<YYINITIAL> "mul"   { return token(tokenRAM.MUL, IToken.RESERVED,null); }
<YYINITIAL> "div"   { return token(tokenRAM.DIV, IToken.RESERVED,null); }
<YYINITIAL> "jmp"   { yybegin(IDENTIFIER); return token(tokenRAM.JMP, IToken.RESERVED,null); }
<YYINITIAL> "jgtz"  { yybegin(IDENTIFIER); return token(tokenRAM.JGTZ, IToken.RESERVED,null); }
<YYINITIAL> "jz"    { yybegin(IDENTIFIER); return token(tokenRAM.JZ, IToken.RESERVED,null); }

<YYINITIAL> "="     { yybegin(STRING); return token(tokenRAM.DIRECT, IToken.OPERATOR,null); }
<YYINITIAL> "*"     { return token(tokenRAM.INDIRECT, IToken.OPERATOR,null); }

{WhiteSpace}   { }
{Eol}          { return token(tokenRAM.EOL, IToken.SEPARATOR,null); }
{Comment}      { return token(tokenRAM.TCOMMENT, IToken.COMMENT,null); }
<YYINITIAL> {Number} { return token(tokenRAM.NUMBER, IToken.LITERAL,yytext()); }
<YYINITIAL> {Label} { return token(tokenRAM.LABELL, IToken.LABEL,yytext()); }
<IDENTIFIER> {Identifier} { yybegin(YYINITIAL); return token(tokenRAM.IDENT, IToken.IDENTIFIER,yytext()); }

<STRING> {String} { yybegin(YYINITIAL); return token(tokenRAM.STRING, IToken.LITERAL,yytext()); }

//[^\n\r\ \t\f]+ { return token(tokenRAM.error, tokenRAM.ERROR); }
.              { return token(tokenRAM.error, tokenRAM.ERROR,null); }

