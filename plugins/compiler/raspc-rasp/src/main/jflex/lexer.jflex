/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
 * Copyright (C) 2020  Peter Jakubčo
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
package net.emustudio.plugins.compiler.raspc;

import java_cup.runtime.ComplexSymbolFactory.Location;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.Token;

import java.io.IOException;
import java.io.Reader;

%%

/*options*/
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

%{
    @Override
    public Token getToken() throws IOException {
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
    public void reset(Reader in, int line, int offset, int column, int lexerState) {
        yyreset(in);
        this.yyline = line;
        this.yychar = offset;
        this.yycolumn = column;
        this.zzLexicalState = lexerState;
    }

    private TokenImpl token(int id, int category) {
        Location left = new Location("", yyline+1,yycolumn+1, (int)yychar);
        Location right= new Location("", yyline+1,yycolumn+yylength(), (int)yychar+yylength());
        return new TokenImpl(id, category, zzLexicalState, yytext(), left, right);
    }

    private TokenImpl token(int id, int category, Object value) {
        Location left = new Location("", yyline+1,yycolumn+1, (int)yychar);
        Location right= new Location("", yyline+1,yycolumn+yylength(), (int)yychar+yylength());
        return new TokenImpl(id, category, zzLexicalState, yytext(), left, right, value);
    }
%}

%eofval{
	return token(TokenImpl.EOF, Token.TEOF);
%eofval}

comment = ";"[^\r\n]*
eol = \r|\n|\r\n
space = [\ \t\f]+ /*\f is page break*/
number = "-"?[0-9]+
identifier = [a-zA-Z][a-zA-Z0-9]*
label = {identifier}[":"]
operator_constant = "="
%%

/*reserved words*/
"read" {
	return token(TokenImpl.READ, Token.RESERVED);
}
"write" {
	return token(TokenImpl.WRITE, Token.RESERVED);
}
"load" {
	return token(TokenImpl.LOAD, Token.RESERVED);
}
"store" {
	return token(TokenImpl.STORE, Token.RESERVED);
}       
"add" {
	return token(TokenImpl.ADD, Token.RESERVED);
}
"sub" {
	return token(TokenImpl.SUB, Token.RESERVED);
}
"mul" {
	return token(TokenImpl.MUL, Token.RESERVED);
}
"div" {
	return token(TokenImpl.DIV, Token.RESERVED);
}
"jmp" {
	return token(TokenImpl.JMP, Token.RESERVED);
}
"jz" {
	return token(TokenImpl.JZ, Token.RESERVED);
}
"jgtz" {
	return token(TokenImpl.JGTZ, Token.RESERVED);
}
"halt" {
	return token(TokenImpl.HALT, Token.RESERVED);
}

/*preprocessor directives*/
"org" {
	return token(TokenImpl.ORG, Token.PREPROCESSOR);
}

"<value>" {
    return token(TokenImpl.TINPUT, Token.PREPROCESSOR);
}

/*separators*/
{eol} {
	return token(TokenImpl.SEPARATOR_EOL, Token.SEPARATOR);
}

{space} {
	/*ignoring */
}

/*comments*/
{comment} {
	return token(TokenImpl.TCOMMENT, Token.COMMENT);
}

/*literals*/
{number} {
	int value = Integer.parseInt(yytext());
	return token(TokenImpl.NUMBER, Token.LITERAL, value);
}

{identifier} {
	return token(TokenImpl.IDENT, Token.IDENTIFIER, yytext());
}

/*label*/
{label} {
	return token(TokenImpl.TLABEL, Token.LABEL, yytext());
}

/*operator for constants as operands*/
{operator_constant} {
	return token(TokenImpl.OPERATOR_CONSTANT, Token.OPERATOR);
}

/*error occurence*/
[^] {
	return token(TokenImpl.error, Token.ERROR);
}




	
        
	    
      
