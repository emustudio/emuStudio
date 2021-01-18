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
package net.emustudio.plugins.compiler.asZ80;

import java_cup.runtime.ComplexSymbolFactory.Location;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.Token;

import java.io.*;

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
%states CONDITION,LD,LD_A,LD_RR,LD_II,LD_X_COMMA

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

Comment =(";"[^\r\n]*)

Eol =[\n]|[\r]|[\n][\r]
WhiteSpace =([ ]|[\t]|[\f])

DecimalNum =[0-9]+[dD]?
OctalNum =[0-7]+[oOqQ]
HexaPostfix =([0-9a-fA-F]*[hH])
HexaNum =[0-9]{HexaPostfix}
BinaryNum =[0-1]+[bB]

AnyChar =([^\"\n\r])
UnclosedString =(\"{AnyChar}+)
String ={UnclosedString}\"

Identifier =([a-zA-Z_\?@])[a-zA-Z_\?@0-9]*
Label ={Identifier}[\:]

%%

/* reserved words */
"adc" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_ADC, Token.RESERVED);
 }
"add" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_ADD, Token.RESERVED);
}
"and" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_AND, Token.RESERVED);
}
"bit" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_BIT, Token.RESERVED);
}
"call" { yybegin(CONDITION);
    return token(TokenImpl.RESERVED_CALL, Token.RESERVED);
}
"ccf" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_CCF, Token.RESERVED);
}
"cp" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_CP, Token.RESERVED);
}
"cpd" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_CPD, Token.RESERVED);
}
"cpdr" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_CPDR, Token.RESERVED);
}
"cpi" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_CPI, Token.RESERVED);
}
"cpir" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_CPIR, Token.RESERVED);
}
"cpl" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_CPL, Token.RESERVED);
}
"daa" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_DAA, Token.RESERVED);
}
"dec" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_DEC, Token.RESERVED);
}
"di" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_DI, Token.RESERVED);
}
"djnz" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_DJNZ, Token.RESERVED);
}
"ei" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_EI, Token.RESERVED);
}
"ex" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_EX, Token.RESERVED);
}
"exx" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_EXX, Token.RESERVED);
}
"halt" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_HALT, Token.RESERVED);
}
"im" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_IM, Token.RESERVED);
}
"in" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_IN, Token.RESERVED);
}
"inc" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_INC, Token.RESERVED);
}
"ind" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_IND, Token.RESERVED);
}
"indr" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_INDR, Token.RESERVED);
}
"ini" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_INI, Token.RESERVED);
}
"inir" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_INIR, Token.RESERVED);
}
"jp" { yybegin(CONDITION);
    return token(TokenImpl.RESERVED_JP, Token.RESERVED);
}
"jr" { yybegin(CONDITION);
    return token(TokenImpl.RESERVED_JR, Token.RESERVED);
}
"ld" { yybegin(LD);
    return token(TokenImpl.RESERVED_LD, Token.RESERVED);
}
"ldd" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_LDD, Token.RESERVED);
}
"lddr" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_LDDR, Token.RESERVED);
}
"ldi" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_LDI, Token.RESERVED);
}
"ldir" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_LDIR, Token.RESERVED);
}
"neg" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_NEG, Token.RESERVED);
}
"nop" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_NOP, Token.RESERVED);
}
"or" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_OR, Token.RESERVED);
}
"otdr" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_OTDR, Token.RESERVED);
}
"otir" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_OTIR, Token.RESERVED);
}
"out" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_OUT, Token.RESERVED);
}
"outd" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_OUTD, Token.RESERVED);
}
"outi" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_OUTI, Token.RESERVED);
}
"pop" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_POP, Token.RESERVED);
}
"push" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_PUSH, Token.RESERVED);
}
"res" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RES, Token.RESERVED);
}
"ret" { yybegin(CONDITION);
    return token(TokenImpl.RESERVED_RET, Token.RESERVED);
}
"reti" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RETI, Token.RESERVED);
}
"retn" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RETN, Token.RESERVED);
}
"rl" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RL, Token.RESERVED);
}
"rla"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RLA, Token.RESERVED);
}
"rlc"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RLC, Token.RESERVED);
}
"rlca"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RLCA, Token.RESERVED);
}
"rld"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RLD, Token.RESERVED);
}
"rr"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RR, Token.RESERVED);
}
"rra"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RRA, Token.RESERVED);
}
"rrc"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RRC, Token.RESERVED);
}
"rrca"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RRCA, Token.RESERVED);
}
"rrd"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RRD, Token.RESERVED);
}
"rst"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RST, Token.RESERVED);
}
"sbc"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_SBC, Token.RESERVED);
}
"scf" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_SCF, Token.RESERVED);
}
"set" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_SET, Token.RESERVED);
}
"sla"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_SLA, Token.RESERVED);
}
"sra"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_SRA, Token.RESERVED);
}
"sll"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_SLL, Token.RESERVED);
}
"srl"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_SRL, Token.RESERVED);
}
"sub"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_SUB, Token.RESERVED);
}
"xor"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_XOR, Token.RESERVED);
}
/* CALL,JP,JR,RET */
<CONDITION> "c" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_C, Token.RESERVED);
}
<CONDITION> "nc" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_NC, Token.RESERVED);
}
<CONDITION> "z" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_Z, Token.RESERVED);
}
<CONDITION> "nz" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_NZ, Token.RESERVED);
}
<CONDITION> "m" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_M, Token.RESERVED);
}
<CONDITION> "p" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_P, Token.RESERVED);
}
<CONDITION> "pe" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_PE, Token.RESERVED);
}
<CONDITION> "po" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_PO, Token.RESERVED);
}

/* preprocessor words */
"org" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_ORG, Token.PREPROCESSOR);
}
"equ" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_EQU, Token.PREPROCESSOR);
}
"var" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_VAR, Token.PREPROCESSOR);
}
"if" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_IF, Token.PREPROCESSOR);
}
"endif" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_ENDIF, Token.PREPROCESSOR);
}
"macro" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_MACRO, Token.PREPROCESSOR);
}
"endm" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_ENDM, Token.PREPROCESSOR);
}
"db" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_DB, Token.PREPROCESSOR);
}
"dw" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_DW, Token.PREPROCESSOR);
}
"ds" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_DS, Token.PREPROCESSOR);
}
"$" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_ADDR, Token.PREPROCESSOR);
}
"include" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_INCLUDE, Token.PREPROCESSOR);
}

/* registers */
<LD> "a" { yybegin(LD_A);
    return token(TokenImpl.REGISTERS_A, Token.REGISTER);
}
"a" {
    return token(TokenImpl.REGISTERS_A, Token.REGISTER);
}
"b" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_B, Token.REGISTER);
}
"c" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_C, Token.REGISTER);
}
"d" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_D, Token.REGISTER);
}
"e" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_E, Token.REGISTER);
}
"h" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_H, Token.REGISTER);
}
"l" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_L, Token.REGISTER);
}
<LD> "ix" { yybegin(LD_II);
    return token(TokenImpl.REGISTERS_IX, Token.REGISTER);
}
"ix" {
    return token(TokenImpl.REGISTERS_IX, Token.REGISTER);
}
<LD> "iy" { yybegin(LD_II);
    return token(TokenImpl.REGISTERS_IY, Token.REGISTER);
}
"iy" {
    return token(TokenImpl.REGISTERS_IY, Token.REGISTER);
}
<LD> "sp" { yybegin(LD_RR);
    return token(TokenImpl.REGISTERS_SP, Token.REGISTER);
}
"sp" {
    return token(TokenImpl.REGISTERS_SP, Token.REGISTER);
}
<LD> "bc" { yybegin(LD_RR);
    return token(TokenImpl.REGISTERS_BC, Token.REGISTER);
}
"bc" {
    return token(TokenImpl.REGISTERS_BC, Token.REGISTER);
}
<LD> "de" { yybegin(LD_RR);
    return token(TokenImpl.REGISTERS_DE, Token.REGISTER);
}
"de" {
    return token(TokenImpl.REGISTERS_DE, Token.REGISTER);
}
<LD> "hl" { yybegin(LD_RR);
    return token(TokenImpl.REGISTERS_HL, Token.REGISTER);
}
"hl" {
    return token(TokenImpl.REGISTERS_HL, Token.REGISTER);
}
"af" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_AF, Token.REGISTER);
}
"af'" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_AFF, Token.REGISTER);
}
"i" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_I, Token.REGISTER);
}
"r" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_R, Token.REGISTER);
}

/* separators */
<LD_X_COMMA> "(" { yybegin(YYINITIAL);
    return token(TokenImpl.SEPARATOR_INDEXLPAR, Token.SEPARATOR);
}
"(" {
    return token(TokenImpl.SEPARATOR_LPAR, Token.SEPARATOR);
}
")" { yybegin(YYINITIAL);
    return token(TokenImpl.SEPARATOR_RPAR, Token.SEPARATOR);
}
<LD_A,LD_RR,LD_II> "," { yybegin(LD_X_COMMA);
    return token(TokenImpl.SEPARATOR_COMMA, Token.SEPARATOR);
}
<YYINITIAL> "," {
    return token(TokenImpl.SEPARATOR_COMMA, Token.SEPARATOR);
}
{Eol} { yybegin(YYINITIAL);
    return token(TokenImpl.SEPARATOR_EOL, Token.SEPARATOR);
}
{WhiteSpace}+ { /* ignore white spaces */ }

/* operators */
"+" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_ADD, Token.OPERATOR);
}
"-" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_SUBTRACT, Token.OPERATOR);
}
"*" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_MULTIPLY, Token.OPERATOR);
}
"/" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_DIVIDE, Token.OPERATOR);
}
"=" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_EQUAL, Token.OPERATOR);
}
">" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_GREATER, Token.OPERATOR);
}
"<" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_LESS, Token.OPERATOR);
}
">=" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_GE, Token.OPERATOR);
}
"<=" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_LE, Token.OPERATOR);
}
"%" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_MOD, Token.OPERATOR);
}
">>" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_SHR, Token.OPERATOR);
}
"<<" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_SHL, Token.OPERATOR);
}
"!" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_NOT, Token.OPERATOR);
}
"&" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_AND, Token.OPERATOR);
}
"|" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_OR, Token.OPERATOR);
}
"~" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_XOR, Token.OPERATOR);
}

/* comment */
{Comment} {
    yybegin(YYINITIAL);
    return token(TokenImpl.TCOMMENT, Token.COMMENT);
}

/* literals */
{DecimalNum} {
    yybegin(YYINITIAL);

    String text = yytext().replaceFirst("[dD]","");
    int num = 0;
    int tokenId;
    int tokenType = Token.LITERAL;

    try {
        num = Integer.parseInt(text,10);
        if (num > 65535) {
            tokenId = TokenImpl.ERROR_DECIMAL_SIZE;
            tokenType = Token.ERROR;
        } else if (num > 255) {
            tokenId = TokenImpl.LITERAL_DECIMAL_16BIT;
        } else {
            tokenId = TokenImpl.LITERAL_DECIMAL_8BIT;
        }
    } catch (NumberFormatException e) {
        tokenId = TokenImpl.ERROR_DECIMAL_SIZE;
        tokenType = Token.ERROR;
    }
    return token(tokenId, tokenType, (Object)num);
}
{OctalNum} {
    yybegin(YYINITIAL);

    String text = yytext().replaceFirst("[oOqQ]","");
    int num = 0;
    int tokenId;
    int tokenType = Token.LITERAL;

    try {
        num = Integer.parseInt(text,8);
        if (num > 65535) {
            tokenId = TokenImpl.ERROR_DECIMAL_SIZE;
            tokenType = Token.ERROR;
        } else if (num > 255) {
            tokenId = TokenImpl.LITERAL_DECIMAL_16BIT;
        } else {
            tokenId = TokenImpl.LITERAL_DECIMAL_8BIT;
        }
    } catch (NumberFormatException e) {
        tokenId = TokenImpl.ERROR_DECIMAL_SIZE;
        tokenType = Token.ERROR;
    }
    return token(tokenId, tokenType, (Object)num);
}
{HexaNum} {
    yybegin(YYINITIAL);

    String text = yytext().replaceFirst("[hH]","");
    int num = 0;
    int tokenId;
    int tokenType = Token.LITERAL;

    try {
        num = Integer.parseInt(text,16);
        if (num > 65535) {
            tokenId = TokenImpl.ERROR_DECIMAL_SIZE;
            tokenType = Token.ERROR;
        } else if (num > 255) {
            tokenId = TokenImpl.LITERAL_DECIMAL_16BIT;
        } else {
            tokenId = TokenImpl.LITERAL_DECIMAL_8BIT;
        }
    } catch (NumberFormatException e) {
        tokenId = TokenImpl.ERROR_DECIMAL_SIZE;
        tokenType = Token.ERROR;
    }
    return token(tokenId, tokenType, (Object)num);
}
{BinaryNum} {
    yybegin(YYINITIAL);

    String text = yytext().replaceFirst("[bB]","");
    int num = 0;
    int tokenId;
    int tokenType = Token.LITERAL;

    try {
        num = Integer.parseInt(text,2);
        if (num > 65535) {
            tokenId = TokenImpl.ERROR_DECIMAL_SIZE;
            tokenType = Token.ERROR;
        } else if (num > 255) {
            tokenId = TokenImpl.LITERAL_DECIMAL_16BIT;
        } else {
            tokenId = TokenImpl.LITERAL_DECIMAL_8BIT;
        }
    } catch (NumberFormatException e) {
        tokenId = TokenImpl.ERROR_DECIMAL_SIZE;
        tokenType = Token.ERROR;
    }
    return token(tokenId, tokenType, (Object)num);
}
{UnclosedString} {
    yybegin(YYINITIAL);
    return token(TokenImpl.ERROR_UNCLOSED_STRING, Token.ERROR);
}
{String} {
    yybegin(YYINITIAL);

    String text = yytext();
    String val = text.substring(1,text.length()-1);
    if (val.length() > 1) {
        return token(TokenImpl.LITERAL_STRING, Token.LITERAL, val);
    } else {
        byte[] b = val.getBytes();
        int numval = b[0];
        for (int i = 1; i < b.length; i++)
            numval = (numval <<8) + b[i];

        int tokenId = (numval > 255) ? TokenImpl.LITERAL_DECIMAL_16BIT : TokenImpl.LITERAL_DECIMAL_8BIT;
        return token(tokenId, Token.LITERAL, numval);
    }
}
{Identifier} {
    yybegin(YYINITIAL);
    return token(TokenImpl.TIDENTIFIER, Token.IDENTIFIER, yytext().toUpperCase());
}
{Label} {
    yybegin(YYINITIAL);
    String text = yytext();
    Object val = text.substring(0,text.length()-1).toUpperCase();
    return token(TokenImpl.TLABEL, Token.LABEL, val);
}
. {
    yybegin(YYINITIAL);
    return token(TokenImpl.error, Token.ERROR);
}
