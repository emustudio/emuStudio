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
%states STRING,IDENTIFIER, INPUT

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
        yybegin(YYINITIAL);
    }
    
    @Override
    public void reset() {
        yyline = yychar = yycolumn = 0;
        yybegin(YYINITIAL);
    }
    
    private tokenRAM token(int id, int type, Object val, boolean initial) {
        return new tokenRAM(id,type,yytext(),yyline,yycolumn,yychar,val,initial);
    }
    
%}

%eofval{
  return token(tokenRAM.EOF, IToken.TEOF,null,false);
%eofval}


Comment    = ";"[^\r\n]*
Eol        = \n|\r|\r\n
WhiteSpace = [\ \t\f]
Number     = [0-9]+

Identifier =([a-zA-Z_\?@])[a-zA-Z_\?@0-9]*
Label ={Identifier}[\:]
String = [^\ \t\f\n\r;]+

%%

<YYINITIAL> "halt"  { return token(tokenRAM.HALT, IToken.RESERVED,null,true); }
<YYINITIAL> "read"  { return token(tokenRAM.READ,  IToken.RESERVED,null,true); }
<YYINITIAL> "write" { return token(tokenRAM.WRITE,  IToken.RESERVED,null,true); }
<YYINITIAL> "load"  { return token(tokenRAM.LOAD, IToken.RESERVED,null,true); }
<YYINITIAL> "store" { return token(tokenRAM.STORE, IToken.RESERVED,null,true); }
<YYINITIAL> "add"   { return token(tokenRAM.ADD,IToken.RESERVED,null,true); }
<YYINITIAL> "sub"   { return token(tokenRAM.SUB, IToken.RESERVED,null,true); }
<YYINITIAL> "mul"   { return token(tokenRAM.MUL, IToken.RESERVED,null,true); }
<YYINITIAL> "div"   { return token(tokenRAM.DIV, IToken.RESERVED,null,true); }
<YYINITIAL> "jmp"   { yybegin(IDENTIFIER); return token(tokenRAM.JMP, IToken.RESERVED,null,true); }
<YYINITIAL> "jgtz"  { yybegin(IDENTIFIER); return token(tokenRAM.JGTZ, IToken.RESERVED,null,true); }
<YYINITIAL> "jz"    { yybegin(IDENTIFIER); return token(tokenRAM.JZ, IToken.RESERVED,null,true); }

<YYINITIAL> "="     { yybegin(STRING); return token(tokenRAM.DIRECT, IToken.OPERATOR,null,true); }
<YYINITIAL> "*"     { return token(tokenRAM.INDIRECT, IToken.OPERATOR,null,true); }

<YYINITIAL> "<input>" { yybegin(INPUT); return token(tokenRAM.INPUT, IToken.PREPROCESSOR, null,true); }

{WhiteSpace}   { }
<YYINITIAL> {Eol} { return token(tokenRAM.EOL, IToken.SEPARATOR,null,true); }
{Eol} { yybegin(YYINITIAL); return token(tokenRAM.EOL, IToken.SEPARATOR,null,false); }

<YYINITIAL> {Comment} { yybegin(YYINITIAL); return token(tokenRAM.TCOMMENT, IToken.COMMENT,null,true); }
{Comment}             { yybegin(YYINITIAL); return token(tokenRAM.TCOMMENT, IToken.COMMENT,null,false); }

<YYINITIAL> {Number} { return token(tokenRAM.NUMBER, IToken.LITERAL,yytext(),true); }
<YYINITIAL> {Label} { return token(tokenRAM.LABELL, IToken.LABEL,yytext(),true); }
<IDENTIFIER> {Identifier} { yybegin(YYINITIAL); return token(tokenRAM.IDENT, IToken.IDENTIFIER,yytext(),false); }

<STRING> {String} { yybegin(YYINITIAL); return token(tokenRAM.STRING, IToken.LITERAL,yytext(),false); }

<INPUT> {String} { return token(tokenRAM.STRING, IToken.PREPROCESSOR,yytext(),false); }


//[^\n\r\ \t\f]+ { return token(tokenRAM.error, tokenRAM.ERROR); }
.              { return token(tokenRAM.error, tokenRAM.ERROR,null,false); }

