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
package net.emustudio.plugins.compiler.as8080;

import java_cup.runtime.ComplexSymbolFactory.Location;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.Token;

import java.io.IOException;
import java.io.Reader;

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

%{
    @Override
    public Token getSymbol() throws IOException {
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
        this.yyline = 0;
        this.yychar = 0;
        this.yycolumn = 0;
    }

    private TokenImpl token(int type, int category, boolean initial) {
        Location left = new Location("", yyline+1,yycolumn+1,yychar);
        Location right= new Location("", yyline+1,yycolumn+yylength(), yychar+yylength());
        return new TokenImpl(type, category, yytext(), left, right, initial);
    }

    private TokenImpl token(int type, int category, Object value, boolean initial) {
        Location left = new Location("", yyline+1,yycolumn+1,yychar);
        Location right= new Location("", yyline+1,yycolumn+yylength(), yychar+yylength());
        return new TokenImpl(type, category, yytext(), left, right, value, initial);
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

AnyChar =([^\'\n\r])
UnclosedString =('{AnyChar}+)
String ={UnclosedString}'

Identifier =([a-zA-Z_\?@])[a-zA-Z_\?@0-9]*
Label ={Identifier}[\:]

%%

/* reserved words */
"stc" { return token(TokenImpl.RESERVED_STC, Token.RESERVED, true);  }
"cmc" { return token(TokenImpl.RESERVED_CMC, Token.RESERVED, true);  }
"inr" { return token(TokenImpl.RESERVED_INR, Token.RESERVED, true);  }
"dcr" { return token(TokenImpl.RESERVED_DCR, Token.RESERVED, true);  }
"cma" { return token(TokenImpl.RESERVED_CMA, Token.RESERVED, true);  }
"daa" { return token(TokenImpl.RESERVED_DAA, Token.RESERVED, true);  }
"nop" { return token(TokenImpl.RESERVED_NOP, Token.RESERVED, true);  }
"mov" { return token(TokenImpl.RESERVED_MOV, Token.RESERVED, true);  }
"stax" { return token(TokenImpl.RESERVED_STAX, Token.RESERVED, true);  }
"ldax" { return token(TokenImpl.RESERVED_LDAX, Token.RESERVED, true);  }
"add" { return token(TokenImpl.RESERVED_ADD, Token.RESERVED, true);  }
"adc" { return token(TokenImpl.RESERVED_ADC, Token.RESERVED, true);  }
"sub" { return token(TokenImpl.RESERVED_SUB, Token.RESERVED, true);  }
"sbb" { return token(TokenImpl.RESERVED_SBB, Token.RESERVED, true);  }
"ana" { return token(TokenImpl.RESERVED_ANA, Token.RESERVED, true);  }
"xra" { return token(TokenImpl.RESERVED_XRA, Token.RESERVED, true);  }
"ora" { return token(TokenImpl.RESERVED_ORA, Token.RESERVED, true);  }
"cmp" { return token(TokenImpl.RESERVED_CMP, Token.RESERVED, true);  }
"rlc" { return token(TokenImpl.RESERVED_RLC, Token.RESERVED, true);  }
"rrc" { return token(TokenImpl.RESERVED_RRC, Token.RESERVED, true);  }
"ral" { return token(TokenImpl.RESERVED_RAL, Token.RESERVED, true);  }
"rar" { return token(TokenImpl.RESERVED_RAR, Token.RESERVED, true);  }
"push" { return token(TokenImpl.RESERVED_PUSH, Token.RESERVED, true);  }
"pop" { return token(TokenImpl.RESERVED_POP, Token.RESERVED, true);  }
"dad" { return token(TokenImpl.RESERVED_DAD, Token.RESERVED, true);  }
"inx" { return token(TokenImpl.RESERVED_INX, Token.RESERVED, true);  }
"dcx" { return token(TokenImpl.RESERVED_DCX, Token.RESERVED, true);  }
"xchg" { return token(TokenImpl.RESERVED_XCHG, Token.RESERVED, true);  }
"xthl" { return token(TokenImpl.RESERVED_XTHL, Token.RESERVED, true);  }
"sphl" { return token(TokenImpl.RESERVED_SPHL, Token.RESERVED, true);  }
"lxi" { return token(TokenImpl.RESERVED_LXI, Token.RESERVED, true);  }
"mvi" { return token(TokenImpl.RESERVED_MVI, Token.RESERVED, true);  }
"adi" { return token(TokenImpl.RESERVED_ADI, Token.RESERVED, true);  }
"aci" { return token(TokenImpl.RESERVED_ACI, Token.RESERVED, true);  }
"sui" { return token(TokenImpl.RESERVED_SUI, Token.RESERVED, true);  }
"sbi" { return token(TokenImpl.RESERVED_SBI, Token.RESERVED, true);  }
"ani" { return token(TokenImpl.RESERVED_ANI, Token.RESERVED, true);  }
"xri" { return token(TokenImpl.RESERVED_XRI, Token.RESERVED, true);  }
"ori" { return token(TokenImpl.RESERVED_ORI, Token.RESERVED, true);  }
"cpi" { return token(TokenImpl.RESERVED_CPI, Token.RESERVED, true);  }
"sta" { return token(TokenImpl.RESERVED_STA, Token.RESERVED, true);  }
"lda" { return token(TokenImpl.RESERVED_LDA, Token.RESERVED, true);  }
"shld" { return token(TokenImpl.RESERVED_SHLD, Token.RESERVED, true);  }
"lhld" { return token(TokenImpl.RESERVED_LHLD, Token.RESERVED, true);  }
"pchl" { return token(TokenImpl.RESERVED_PCHL, Token.RESERVED, true);  }
"jmp" { return token(TokenImpl.RESERVED_JMP, Token.RESERVED, true);  }
"jc" { return token(TokenImpl.RESERVED_JC, Token.RESERVED, true);  }
"jnc" { return token(TokenImpl.RESERVED_JNC, Token.RESERVED, true);  }
"jz" { return token(TokenImpl.RESERVED_JZ, Token.RESERVED, true);  }
"jnz"  { return token(TokenImpl.RESERVED_JNZ, Token.RESERVED, true);  }
"jp"  { return token(TokenImpl.RESERVED_JP, Token.RESERVED, true);  }
"jm"  { return token(TokenImpl.RESERVED_JM, Token.RESERVED, true);  }
"jpe"  { return token(TokenImpl.RESERVED_JPE, Token.RESERVED, true);  }
"jpo"  { return token(TokenImpl.RESERVED_JPO, Token.RESERVED, true);  }
"call"  { return token(TokenImpl.RESERVED_CALL, Token.RESERVED, true);  }
"cc"  { return token(TokenImpl.RESERVED_CC, Token.RESERVED, true);  }
"cnc"  { return token(TokenImpl.RESERVED_CNC, Token.RESERVED, true);  }
"cz"  { return token(TokenImpl.RESERVED_CZ, Token.RESERVED, true);  }
"cnz"  { return token(TokenImpl.RESERVED_CNZ, Token.RESERVED, true);  }
"cp"  { return token(TokenImpl.RESERVED_CP, Token.RESERVED, true);  }
"cm" { return token(TokenImpl.RESERVED_CM, Token.RESERVED, true);  }
"cpe" { return token(TokenImpl.RESERVED_CPE, Token.RESERVED, true);  }
"cpo"  { return token(TokenImpl.RESERVED_CPO, Token.RESERVED, true);  }
"ret"  { return token(TokenImpl.RESERVED_RET, Token.RESERVED, true);  }
"rc"  { return token(TokenImpl.RESERVED_RC, Token.RESERVED, true);  }
"rnc"  { return token(TokenImpl.RESERVED_RNC, Token.RESERVED, true);  }
"rz"  { return token(TokenImpl.RESERVED_RZ, Token.RESERVED, true);  }
"rnz" { return token(TokenImpl.RESERVED_RNZ, Token.RESERVED, true);  }
"rm" { return token(TokenImpl.RESERVED_RM, Token.RESERVED, true);  }
"rp" { return token(TokenImpl.RESERVED_RP, Token.RESERVED, true);  }
"rpe" { return token(TokenImpl.RESERVED_RPE, Token.RESERVED, true);  }
"rpo" { return token(TokenImpl.RESERVED_RPO, Token.RESERVED, true);  }
"rst" { return token(TokenImpl.RESERVED_RST, Token.RESERVED, true);  }
"ei" { return token(TokenImpl.RESERVED_EI, Token.RESERVED, true);  }
"di" { return token(TokenImpl.RESERVED_DI, Token.RESERVED, true);  }
"in" { return token(TokenImpl.RESERVED_IN, Token.RESERVED, true);  }
"out" { return token(TokenImpl.RESERVED_OUT, Token.RESERVED, true);  }
"hlt" { return token(TokenImpl.RESERVED_HLT, Token.RESERVED, true);  }

/* preprocessor words */
"org" { return token(TokenImpl.PREPROCESSOR_ORG, Token.PREPROCESSOR, true);  }
"equ" { return token(TokenImpl.PREPROCESSOR_EQU, Token.PREPROCESSOR, true);  }
"set" { return token(TokenImpl.PREPROCESSOR_SET, Token.PREPROCESSOR, true);  }
"include" { return token(TokenImpl.PREPROCESSOR_INCLUDE, Token.PREPROCESSOR, true);  }
"if" { return token(TokenImpl.PREPROCESSOR_IF, Token.PREPROCESSOR, true);  }
"endif" { return token(TokenImpl.PREPROCESSOR_ENDIF, Token.PREPROCESSOR, true);  }
"macro" { return token(TokenImpl.PREPROCESSOR_MACRO, Token.PREPROCESSOR, true);  }
"endm" { return token(TokenImpl.PREPROCESSOR_ENDM, Token.PREPROCESSOR, true);  }
"db" { return token(TokenImpl.PREPROCESSOR_DB, Token.PREPROCESSOR, true);  }
"dw" { return token(TokenImpl.PREPROCESSOR_DW, Token.PREPROCESSOR, true);  }
"ds" { return token(TokenImpl.PREPROCESSOR_DS, Token.PREPROCESSOR, true);  }
"$" { return token(TokenImpl.PREPROCESSOR_ADDR, Token.PREPROCESSOR, true);  }

/* registers */
"a" { return token(TokenImpl.REGISTERS_A, Token.REGISTER, true);  }
"b" { return token(TokenImpl.REGISTERS_B, Token.REGISTER, true);  }
"c" { return token(TokenImpl.REGISTERS_C, Token.REGISTER, true);  }
"d" { return token(TokenImpl.REGISTERS_D, Token.REGISTER, true);  }
"e" { return token(TokenImpl.REGISTERS_E, Token.REGISTER, true);  }
"h" { return token(TokenImpl.REGISTERS_H, Token.REGISTER, true);  }
"l" { return token(TokenImpl.REGISTERS_L, Token.REGISTER, true);  }
"m" { return token(TokenImpl.REGISTERS_M, Token.REGISTER, true);  }
"psw" { return token(TokenImpl.REGISTERS_PSW, Token.REGISTER, true);  }
"sp" { return token(TokenImpl.REGISTERS_SP, Token.REGISTER, true);  }

/* separators */
"(" { return token(TokenImpl.SEPARATOR_LPAR, Token.SEPARATOR, true);  }
")" { return token(TokenImpl.SEPARATOR_RPAR, Token.SEPARATOR, true);  }
"," { return token(TokenImpl.SEPARATOR_COMMA, Token.SEPARATOR, true);  }
{Eol} { return token(TokenImpl.SEPARATOR_EOL, Token.SEPARATOR, true);  }
{WhiteSpace}+ { /* ignore white spaces */ }

/* operators */
"+" { return token(TokenImpl.OPERATOR_ADD, Token.OPERATOR, true);  }
"-" { return token(TokenImpl.OPERATOR_SUBTRACT, Token.OPERATOR, true);  }
"*" { return token(TokenImpl.OPERATOR_MULTIPLY, Token.OPERATOR, true);  }
"/" { return token(TokenImpl.OPERATOR_DIVIDE, Token.OPERATOR, true);  }
"=" { return token(TokenImpl.OPERATOR_EQUAL, Token.OPERATOR, true);  }
"mod" { return token(TokenImpl.OPERATOR_MOD, Token.OPERATOR, true);  }
"shr" { return token(TokenImpl.OPERATOR_SHR, Token.OPERATOR, true);  }
"shl" { return token(TokenImpl.OPERATOR_SHL, Token.OPERATOR, true);  }
"not" { return token(TokenImpl.OPERATOR_NOT, Token.OPERATOR, true);  }
"and" { return token(TokenImpl.OPERATOR_AND, Token.OPERATOR, true);  }
"or" { return token(TokenImpl.OPERATOR_OR, Token.OPERATOR, true);  }
"xor" { return token(TokenImpl.OPERATOR_XOR, Token.OPERATOR, true);  }

/* comment */
{Comment} { return token(TokenImpl.TCOMMENT, Token.COMMENT, true);  }

/* literals */
{DecimalNum} {
    String text = yytext();
    text = text.replaceFirst("[dD]","");
    int num=0;
    int tokenId;
    int tokenType = Token.LITERAL;

    try {
        num = Integer.parseInt(text,10);
        if (num > 65535) {
            tokenId = TokenImpl.ERROR_DECIMAL_SIZE;
            tokenType = Token.ERROR;
        } else if (num > 255) { // || num < -128) {
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
    String text = yytext().replaceFirst("[oOqQ]","");
    int num=0;
    int tokenId;
    int tokenType = Token.LITERAL;

    try {
        num = Integer.parseInt(text,8);
        if (num > 65535) { // || num < -32768) {
            tokenId = TokenImpl.ERROR_DECIMAL_SIZE;
            tokenType = Token.ERROR;
        } else if (num > 255) { // || num < -128) {
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
    String text = yytext().replaceFirst("[hH]","");
    int num=0;
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
    String text = yytext().replaceFirst("[bB]","");
    int num=0;
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
    return token(TokenImpl.ERROR_UNCLOSED_STRING, Token.ERROR, true);
}
{String} {
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
    return token(TokenImpl.TIDENTIFIER, Token.IDENTIFIER, yytext().toUpperCase(), true);
}
{Label} {
    String text = yytext();
    Object val = text.substring(0,text.length()-1).toUpperCase();
    return token(TokenImpl.TLABEL, Token.LABEL, val, true);
}
. {
    return token(TokenImpl.ERROR_UNKNOWN_TOKEN, Token.ERROR, true);
}
