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
    public TokenImpl getSymbol() throws IOException {
        return next_token();
    }

    public void reset(Reader in, int yyline, int yychar, int yycolumn) {
        yyreset(in);
        this.yyline = yyline;
        this.yychar = yychar;
        this.yycolumn = yycolumn;
    }
    
    public void reset() {
        this.yyline = 0;
        this.yychar = 0;
        this.yycolumn = 0;
    }

    private TokenImpl token(int id, int category, boolean initial) {
        Location left = new Location("", yyline+1,yycolumn+1,yychar);
        Location right= new Location("", yyline+1,yycolumn+yylength(), yychar+yylength());
        return new TokenImpl(id, category, yytext(), left, right, initial);
    }

    private TokenImpl token(int id, int category, Object value, boolean initial) {
        Location left = new Location("", yyline+1,yycolumn+1,yychar);
        Location right= new Location("", yyline+1,yycolumn+yylength(), yychar+yylength());
        return new TokenImpl(id, category, yytext(), left, right, value, initial);
    }
%}
%eofval{
    return token(TokenImpl.EOF, Token.TEOF, true);
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
    return token(TokenImpl.RESERVED_ADC, Token.RESERVED, true);
 }
"add" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_ADD, Token.RESERVED, true);
}
"and" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_AND, Token.RESERVED, true);
}
"bit" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_BIT, Token.RESERVED, true);
}
"call" { yybegin(CONDITION);
    return token(TokenImpl.RESERVED_CALL, Token.RESERVED, true);
}
"ccf" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_CCF, Token.RESERVED, true);
}
"cp" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_CP, Token.RESERVED, true);
}
"cpd" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_CPD, Token.RESERVED, true);
}
"cpdr" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_CPDR, Token.RESERVED, true);
}
"cpi" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_CPI, Token.RESERVED, true);
}
"cpir" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_CPIR, Token.RESERVED, true);
}
"cpl" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_CPL, Token.RESERVED, true);
}
"daa" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_DAA, Token.RESERVED, true);
}
"dec" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_DEC, Token.RESERVED, true);
}
"di" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_DI, Token.RESERVED, true);
}
"djnz" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_DJNZ, Token.RESERVED, true);
}
"ei" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_EI, Token.RESERVED, true);
}
"ex" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_EX, Token.RESERVED, true);
}
"exx" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_EXX, Token.RESERVED, true);
}
"halt" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_HALT, Token.RESERVED, true);
}
"im" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_IM, Token.RESERVED, true);
}
"in" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_IN, Token.RESERVED, true);
}
"inc" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_INC, Token.RESERVED, true);
}
"ind" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_IND, Token.RESERVED, true);
}
"indr" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_INDR, Token.RESERVED, true);
}
"ini" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_INI, Token.RESERVED, true);
}
"inir" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_INIR, Token.RESERVED, true);
}
"jp" { yybegin(CONDITION);
    return token(TokenImpl.RESERVED_JP, Token.RESERVED, true);
}
"jr" { yybegin(CONDITION);
    return token(TokenImpl.RESERVED_JR, Token.RESERVED, true);
}
"ld" { yybegin(LD);
    return token(TokenImpl.RESERVED_LD, Token.RESERVED, true);
}
"ldd" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_LDD, Token.RESERVED, true);
}
"lddr" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_LDDR, Token.RESERVED, true);
}
"ldi" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_LDI, Token.RESERVED, true);
}
"ldir" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_LDIR, Token.RESERVED, true);
}
"neg" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_NEG, Token.RESERVED, true);
}
"nop" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_NOP, Token.RESERVED, true);
}
"or" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_OR, Token.RESERVED, true);
}
"otdr" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_OTDR, Token.RESERVED, true);
}
"otir" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_OTIR, Token.RESERVED, true);
}
"out" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_OUT, Token.RESERVED, true);
}
"outd" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_OUTD, Token.RESERVED, true);
}
"outi" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_OUTI, Token.RESERVED, true);
}
"pop" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_POP, Token.RESERVED, true);
}
"push" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_PUSH, Token.RESERVED, true);
}
"res" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RES, Token.RESERVED, true);
}
"ret" { yybegin(CONDITION);
    return token(TokenImpl.RESERVED_RET, Token.RESERVED, true);
}
"reti" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RETI, Token.RESERVED, true);
}
"retn" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RETN, Token.RESERVED, true);
}
"rl" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RL, Token.RESERVED, true);
}
"rla"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RLA, Token.RESERVED, true);
}
"rlc"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RLC, Token.RESERVED, true);
}
"rlca"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RLCA, Token.RESERVED, true);
}
"rld"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RLD, Token.RESERVED, true);
}
"rr"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RR, Token.RESERVED, true);
}
"rra"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RRA, Token.RESERVED, true);
}
"rrc"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RRC, Token.RESERVED, true);
}
"rrca"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RRCA, Token.RESERVED, true);
}
"rrd"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RRD, Token.RESERVED, true);
}
"rst"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_RST, Token.RESERVED, true);
}
"sbc"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_SBC, Token.RESERVED, true);
}
"scf" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_SCF, Token.RESERVED, true);
}
"set" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_SET, Token.RESERVED, true);
}
"sla"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_SLA, Token.RESERVED, true);
}
"sra"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_SRA, Token.RESERVED, true);
}
"sll"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_SLL, Token.RESERVED, true);
}
"srl"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_SRL, Token.RESERVED, true);
}
"sub"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_SUB, Token.RESERVED, true);
}
"xor"  { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_XOR, Token.RESERVED, true);
}
/* CALL,JP,JR,RET */
<CONDITION> "c" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_C, Token.RESERVED, false);
}
<CONDITION> "nc" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_NC, Token.RESERVED, false);
}
<CONDITION> "z" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_Z, Token.RESERVED, false);
}
<CONDITION> "nz" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_NZ, Token.RESERVED, false);
}
<CONDITION> "m" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_M, Token.RESERVED, false);
}
<CONDITION> "p" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_P, Token.RESERVED, false);
}
<CONDITION> "pe" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_PE, Token.RESERVED, false);
}
<CONDITION> "po" { yybegin(YYINITIAL);
    return token(TokenImpl.RESERVED_PO, Token.RESERVED, false);
}

/* preprocessor words */
"org" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_ORG, Token.PREPROCESSOR, true);
}
"equ" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_EQU, Token.PREPROCESSOR, true);
}
"var" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_VAR, Token.PREPROCESSOR, true);
}
"if" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_IF, Token.PREPROCESSOR, true);
}
"endif" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_ENDIF, Token.PREPROCESSOR, true);
}
"macro" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_MACRO, Token.PREPROCESSOR, true);
}
"endm" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_ENDM, Token.PREPROCESSOR, true);
}
"db" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_DB, Token.PREPROCESSOR, true);
}
"dw" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_DW, Token.PREPROCESSOR, true);
}
"ds" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_DS, Token.PREPROCESSOR, true);
}
"$" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_ADDR, Token.PREPROCESSOR, true);
}
"include" { yybegin(YYINITIAL);
    return token(TokenImpl.PREPROCESSOR_INCLUDE, Token.PREPROCESSOR, true);
}

/* registers */
<LD> "a" { yybegin(LD_A);
    return token(TokenImpl.REGISTERS_A, Token.REGISTER, false);
}
"a" {
    return token(TokenImpl.REGISTERS_A, Token.REGISTER, true);
}
"b" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_B, Token.REGISTER, true);
}
"c" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_C, Token.REGISTER, true);
}
"d" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_D, Token.REGISTER, true);
}
"e" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_E, Token.REGISTER, true);
}
"h" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_H, Token.REGISTER, true);
}
"l" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_L, Token.REGISTER, true);
}
<LD> "ix" { yybegin(LD_II);
    return token(TokenImpl.REGISTERS_IX, Token.REGISTER, false);
}
"ix" {
    return token(TokenImpl.REGISTERS_IX, Token.REGISTER, true);
}
<LD> "iy" { yybegin(LD_II);
    return token(TokenImpl.REGISTERS_IY, Token.REGISTER, false);
}
"iy" {
    return token(TokenImpl.REGISTERS_IY, Token.REGISTER, true);
}
<LD> "sp" { yybegin(LD_RR);
    return token(TokenImpl.REGISTERS_SP, Token.REGISTER, false);
}
"sp" {
    return token(TokenImpl.REGISTERS_SP, Token.REGISTER, true);
}
<LD> "bc" { yybegin(LD_RR);
    return token(TokenImpl.REGISTERS_BC, Token.REGISTER, false);
}
"bc" {
    return token(TokenImpl.REGISTERS_BC, Token.REGISTER, true);
}
<LD> "de" { yybegin(LD_RR);
    return token(TokenImpl.REGISTERS_DE, Token.REGISTER, false);
}
"de" {
    return token(TokenImpl.REGISTERS_DE, Token.REGISTER, true);
}
<LD> "hl" { yybegin(LD_RR);
    return token(TokenImpl.REGISTERS_HL, Token.REGISTER, false);
}
"hl" {
    return token(TokenImpl.REGISTERS_HL, Token.REGISTER, true);
}
"af" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_AF, Token.REGISTER, true);
}
"af'" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_AFF, Token.REGISTER, true);
}
"i" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_I, Token.REGISTER, true);
}
"r" { yybegin(YYINITIAL);
    return token(TokenImpl.REGISTERS_R, Token.REGISTER, true);
}

/* separators */
<LD_X_COMMA> "(" { yybegin(YYINITIAL);
    return token(TokenImpl.SEPARATOR_INDEXLPAR, Token.SEPARATOR, false);
}
"(" {
    return token(TokenImpl.SEPARATOR_LPAR, Token.SEPARATOR, true);
}
")" { yybegin(YYINITIAL);
    return token(TokenImpl.SEPARATOR_RPAR, Token.SEPARATOR, true);
}
<LD_A,LD_RR,LD_II> "," { yybegin(LD_X_COMMA);
    return token(TokenImpl.SEPARATOR_COMMA, Token.SEPARATOR, false);
}
<YYINITIAL> "," {
    return token(TokenImpl.SEPARATOR_COMMA, Token.SEPARATOR, true);
}
{Eol} { yybegin(YYINITIAL);
    return token(TokenImpl.SEPARATOR_EOL, Token.SEPARATOR, true);
}
{WhiteSpace}+ { /* ignore white spaces */ }

/* operators */
"+" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_ADD, Token.OPERATOR, true);
}
"-" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_SUBTRACT, Token.OPERATOR, true);
}
"*" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_MULTIPLY, Token.OPERATOR, true);
}
"/" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_DIVIDE, Token.OPERATOR, true);
}
"=" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_EQUAL, Token.OPERATOR, true);
}
">" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_GREATER, Token.OPERATOR, true);
}
"<" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_LESS, Token.OPERATOR, true);
}
">=" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_GE, Token.OPERATOR, true);
}
"<=" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_LE, Token.OPERATOR, true);
}
"%" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_MOD, Token.OPERATOR, true);
}
">>" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_SHR, Token.OPERATOR, true);
}
"<<" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_SHL, Token.OPERATOR, true);
}
"!" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_NOT, Token.OPERATOR, true);
}
"&" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_AND, Token.OPERATOR, true);
}
"|" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_OR, Token.OPERATOR, true);
}
"~" { yybegin(YYINITIAL);
    return token(TokenImpl.OPERATOR_XOR, Token.OPERATOR, true);
}

/* comment */
{Comment} {
    yybegin(YYINITIAL);
    return token(TokenImpl.TCOMMENT, Token.COMMENT, true);
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
    return token(tokenId, tokenType, (Object)num, true);
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
    return token(tokenId, tokenType, (Object)num, true);
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
    return token(tokenId, tokenType, (Object)num, true);
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
    return token(tokenId, tokenType, (Object)num, true);
}
{UnclosedString} {
    yybegin(YYINITIAL);
    return token(TokenImpl.ERROR_UNCLOSED_STRING, Token.ERROR, true);
}
{String} {
    yybegin(YYINITIAL);

    String text = yytext();
    String val = text.substring(1,text.length()-1);
    if (val.length() > 1) {
        return token(TokenImpl.LITERAL_STRING, Token.LITERAL, val, true);
    } else {
        byte[] b = val.getBytes();
        int numval = b[0];
        for (int i = 1; i < b.length; i++)
            numval = (numval <<8) + b[i];

        int tokenId = (numval > 255) ? TokenImpl.LITERAL_DECIMAL_16BIT : TokenImpl.LITERAL_DECIMAL_8BIT;
        return token(tokenId, Token.LITERAL, numval, true);
    }
}
{Identifier} {
    yybegin(YYINITIAL);
    return token(TokenImpl.TIDENTIFIER, Token.IDENTIFIER, yytext().toUpperCase(), true);
}
{Label} {
    yybegin(YYINITIAL);
    String text = yytext();
    Object val = text.substring(0,text.length()-1).toUpperCase();
    return token(TokenImpl.TLABEL, Token.LABEL, val, true);
}
. {
    yybegin(YYINITIAL);
    return token(TokenImpl.ERROR_UNKNOWN_TOKEN, Token.ERROR, true);
}
