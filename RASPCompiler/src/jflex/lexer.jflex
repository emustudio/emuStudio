package sk.tuke.emustudio.rasp.compiler;

import emulib.plugins.compiler.LexicalAnalyzer;
import emulib.plugins.compiler.Token;
import java.io.IOException;
import java.io.Reader;

%%

/*options for the lexer*/

%class LexerImpl  /*name of lexer class*/
%cup  /*switch to CUP parser generator compatibility*/
%public  
%implements LexicalAnalyzer, Symbols /*interfaces that resulting lexer implements*/
/*switch line and column counting on*/
%line
%column
/*turn character counting on (from the beginning of input to beginning of current token)*/
%char
%caseless /*ignore case in source file*/
%unicode
/*specify type of the return value of the scanning method*/
%type TokenImpl
        
%{
        @Override
	public Token getSymbol() throws IOException{
		return next_token();
	}

	@Override
	public void reset(Reader in, int yyline, int yychar, int yycolumn){	
		yyreset(in);
		this.yyline = yyline;
		this.yychar = yychar;
		this.yycolumn = yycolumn;
	}

	@Override
	public void reset(){
		this.yyline = 0;
		this.yychar = 0;
		this.yycolumn = 0;
	}	

	private TokenImpl token(int id, int type){
		return new TokenImpl(id, type, yytext(), yyline, yycolumn, yychar);
	}
	
	private TokenImpl token(int id, int type, Object value){
		return new TokenImpl(id, type, yytext(), yyline, yycolumn, yychar, value);
	}

%}

%eofval{
	return token(EOF, Token.TEOF);
%eofval}

comment = ";"[^\r\n]*
eol = \r|\n|\r\n
space = [\ \t\f]+ /*\f is page break*/
number = \-?[0-9]+
identifier = [a-zA-Z][a-zA-Z0-9]*
label = {identifier}[\:]
operator_constant = "="

%%

/*reserved words*/
"read" {
	return token(READ, Token.RESERVED);
}
"write" {
	return token(WRITE, Token.RESERVED);
}
"load" {
	return token(LOAD, Token.RESERVED);
}
"store" {
	return token(STORE, Token.RESERVED);
}       
"add" {
	return token(ADD, Token.RESERVED);
}
"sub" {
	return token(SUB, Token.RESERVED);
}
"mul" {
	return token(MUL, Token.RESERVED);
}
"div" {
	return token(DIV, Token.RESERVED);
}
"jmp" {
	return token(JMP, Token.RESERVED);
}
"jz" {
	return token(JZ, Token.RESERVED);
}
"jgtz" {
	return token(JGTZ, Token.RESERVED);
}
"halt" {
	return token(HALT, Token.RESERVED);
}

/*preprocessor directives*/
"org" {
	return token(ORG, Token.PREPROCESSOR); 
}

/*separators*/
{eol} {
	return token(SEPARATOR_EOL, Token.SEPARATOR);
}

{space} {
	/*ignoring */
}

/*comments*/
{comment} {
	return token(TCOMMENT, Token.COMMENT);
}

/*literals*/
{number} {
	int value = Integer.parseInt(yytext());
	return token(NUMBER, Token.LITERAL, value);
}

{identifier} {
	return token(IDENT, Token.IDENTIFIER, yytext());
}

/*label*/
{label} {	
	return token(TLABEL, Token.LABEL, yytext());
}

/*operator for constants as operands*/
{operator_constant} {
	return token(OPERATOR_CONSTANT, Token.OPERATOR);
}

/*error occurence*/
[^] {
	return token(ERROR_UNKNOWN_TOKEN, Token.ERROR);
}




	
        
	    
      
