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
        Location left = new Location("", yyline+1,yycolumn+1,yychar);
        Location right= new Location("", yyline+1,yycolumn+yylength(), yychar+yylength());
        return new TokenImpl(id, category, zzLexicalState, yytext(), left, right);
    }

    private TokenImpl token(int id, int category, Object value) {
        Location left = new Location("", yyline+1,yycolumn+1,yychar);
        Location right= new Location("", yyline+1,yycolumn+yylength(), yychar+yylength());
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

AnyChar =([^\'\n\r])
UnclosedString =('{AnyChar}+)
String ={UnclosedString}'

Identifier =([a-zA-Z_\?@])[a-zA-Z_\?@0-9]*
Label ={Identifier}[\:]

%%

/* reserved words */
"stc" { return token(TokenImpl.RESERVED_STC, Token.RESERVED);  }
"cmc" { return token(TokenImpl.RESERVED_CMC, Token.RESERVED);  }
"inr" { return token(TokenImpl.RESERVED_INR, Token.RESERVED);  }
"dcr" { return token(TokenImpl.RESERVED_DCR, Token.RESERVED);  }
"cma" { return token(TokenImpl.RESERVED_CMA, Token.RESERVED);  }
"daa" { return token(TokenImpl.RESERVED_DAA, Token.RESERVED);  }
"nop" { return token(TokenImpl.RESERVED_NOP, Token.RESERVED);  }
"mov" { return token(TokenImpl.RESERVED_MOV, Token.RESERVED);  }
"stax" { return token(TokenImpl.RESERVED_STAX, Token.RESERVED);  }
"ldax" { return token(TokenImpl.RESERVED_LDAX, Token.RESERVED);  }
"add" { return token(TokenImpl.RESERVED_ADD, Token.RESERVED);  }
"adc" { return token(TokenImpl.RESERVED_ADC, Token.RESERVED);  }
"sub" { return token(TokenImpl.RESERVED_SUB, Token.RESERVED);  }
"sbb" { return token(TokenImpl.RESERVED_SBB, Token.RESERVED);  }
"ana" { return token(TokenImpl.RESERVED_ANA, Token.RESERVED);  }
"xra" { return token(TokenImpl.RESERVED_XRA, Token.RESERVED);  }
"ora" { return token(TokenImpl.RESERVED_ORA, Token.RESERVED);  }
"cmp" { return token(TokenImpl.RESERVED_CMP, Token.RESERVED);  }
"rlc" { return token(TokenImpl.RESERVED_RLC, Token.RESERVED);  }
"rrc" { return token(TokenImpl.RESERVED_RRC, Token.RESERVED);  }
"ral" { return token(TokenImpl.RESERVED_RAL, Token.RESERVED);  }
"rar" { return token(TokenImpl.RESERVED_RAR, Token.RESERVED);  }
"push" { return token(TokenImpl.RESERVED_PUSH, Token.RESERVED);  }
"pop" { return token(TokenImpl.RESERVED_POP, Token.RESERVED);  }
"dad" { return token(TokenImpl.RESERVED_DAD, Token.RESERVED);  }
"inx" { return token(TokenImpl.RESERVED_INX, Token.RESERVED);  }
"dcx" { return token(TokenImpl.RESERVED_DCX, Token.RESERVED);  }
"xchg" { return token(TokenImpl.RESERVED_XCHG, Token.RESERVED);  }
"xthl" { return token(TokenImpl.RESERVED_XTHL, Token.RESERVED);  }
"sphl" { return token(TokenImpl.RESERVED_SPHL, Token.RESERVED);  }
"lxi" { return token(TokenImpl.RESERVED_LXI, Token.RESERVED);  }
"mvi" { return token(TokenImpl.RESERVED_MVI, Token.RESERVED);  }
"adi" { return token(TokenImpl.RESERVED_ADI, Token.RESERVED);  }
"aci" { return token(TokenImpl.RESERVED_ACI, Token.RESERVED);  }
"sui" { return token(TokenImpl.RESERVED_SUI, Token.RESERVED);  }
"sbi" { return token(TokenImpl.RESERVED_SBI, Token.RESERVED);  }
"ani" { return token(TokenImpl.RESERVED_ANI, Token.RESERVED);  }
"xri" { return token(TokenImpl.RESERVED_XRI, Token.RESERVED);  }
"ori" { return token(TokenImpl.RESERVED_ORI, Token.RESERVED);  }
"cpi" { return token(TokenImpl.RESERVED_CPI, Token.RESERVED);  }
"sta" { return token(TokenImpl.RESERVED_STA, Token.RESERVED);  }
"lda" { return token(TokenImpl.RESERVED_LDA, Token.RESERVED);  }
"shld" { return token(TokenImpl.RESERVED_SHLD, Token.RESERVED);  }
"lhld" { return token(TokenImpl.RESERVED_LHLD, Token.RESERVED);  }
"pchl" { return token(TokenImpl.RESERVED_PCHL, Token.RESERVED);  }
"jmp" { return token(TokenImpl.RESERVED_JMP, Token.RESERVED);  }
"jc" { return token(TokenImpl.RESERVED_JC, Token.RESERVED);  }
"jnc" { return token(TokenImpl.RESERVED_JNC, Token.RESERVED);  }
"jz" { return token(TokenImpl.RESERVED_JZ, Token.RESERVED);  }
"jnz"  { return token(TokenImpl.RESERVED_JNZ, Token.RESERVED);  }
"jp"  { return token(TokenImpl.RESERVED_JP, Token.RESERVED);  }
"jm"  { return token(TokenImpl.RESERVED_JM, Token.RESERVED);  }
"jpe"  { return token(TokenImpl.RESERVED_JPE, Token.RESERVED);  }
"jpo"  { return token(TokenImpl.RESERVED_JPO, Token.RESERVED);  }
"call"  { return token(TokenImpl.RESERVED_CALL, Token.RESERVED);  }
"cc"  { return token(TokenImpl.RESERVED_CC, Token.RESERVED);  }
"cnc"  { return token(TokenImpl.RESERVED_CNC, Token.RESERVED);  }
"cz"  { return token(TokenImpl.RESERVED_CZ, Token.RESERVED);  }
"cnz"  { return token(TokenImpl.RESERVED_CNZ, Token.RESERVED);  }
"cp"  { return token(TokenImpl.RESERVED_CP, Token.RESERVED);  }
"cm" { return token(TokenImpl.RESERVED_CM, Token.RESERVED);  }
"cpe" { return token(TokenImpl.RESERVED_CPE, Token.RESERVED);  }
"cpo"  { return token(TokenImpl.RESERVED_CPO, Token.RESERVED);  }
"ret"  { return token(TokenImpl.RESERVED_RET, Token.RESERVED);  }
"rc"  { return token(TokenImpl.RESERVED_RC, Token.RESERVED);  }
"rnc"  { return token(TokenImpl.RESERVED_RNC, Token.RESERVED);  }
"rz"  { return token(TokenImpl.RESERVED_RZ, Token.RESERVED);  }
"rnz" { return token(TokenImpl.RESERVED_RNZ, Token.RESERVED);  }
"rm" { return token(TokenImpl.RESERVED_RM, Token.RESERVED);  }
"rp" { return token(TokenImpl.RESERVED_RP, Token.RESERVED);  }
"rpe" { return token(TokenImpl.RESERVED_RPE, Token.RESERVED);  }
"rpo" { return token(TokenImpl.RESERVED_RPO, Token.RESERVED);  }
"rst" { return token(TokenImpl.RESERVED_RST, Token.RESERVED);  }
"ei" { return token(TokenImpl.RESERVED_EI, Token.RESERVED);  }
"di" { return token(TokenImpl.RESERVED_DI, Token.RESERVED);  }
"in" { return token(TokenImpl.RESERVED_IN, Token.RESERVED);  }
"out" { return token(TokenImpl.RESERVED_OUT, Token.RESERVED);  }
"hlt" { return token(TokenImpl.RESERVED_HLT, Token.RESERVED);  }

/* preprocessor words */
"org" { return token(TokenImpl.PREPROCESSOR_ORG, Token.PREPROCESSOR);  }
"equ" { return token(TokenImpl.PREPROCESSOR_EQU, Token.PREPROCESSOR);  }
"set" { return token(TokenImpl.PREPROCESSOR_SET, Token.PREPROCESSOR);  }
"include" { return token(TokenImpl.PREPROCESSOR_INCLUDE, Token.PREPROCESSOR);  }
"if" { return token(TokenImpl.PREPROCESSOR_IF, Token.PREPROCESSOR);  }
"endif" { return token(TokenImpl.PREPROCESSOR_ENDIF, Token.PREPROCESSOR);  }
"macro" { return token(TokenImpl.PREPROCESSOR_MACRO, Token.PREPROCESSOR);  }
"endm" { return token(TokenImpl.PREPROCESSOR_ENDM, Token.PREPROCESSOR);  }
"db" { return token(TokenImpl.PREPROCESSOR_DB, Token.PREPROCESSOR);  }
"dw" { return token(TokenImpl.PREPROCESSOR_DW, Token.PREPROCESSOR);  }
"ds" { return token(TokenImpl.PREPROCESSOR_DS, Token.PREPROCESSOR);  }
"$" { return token(TokenImpl.PREPROCESSOR_ADDR, Token.PREPROCESSOR);  }

/* registers */
"a" { return token(TokenImpl.REGISTERS_A, Token.REGISTER);  }
"b" { return token(TokenImpl.REGISTERS_B, Token.REGISTER);  }
"c" { return token(TokenImpl.REGISTERS_C, Token.REGISTER);  }
"d" { return token(TokenImpl.REGISTERS_D, Token.REGISTER);  }
"e" { return token(TokenImpl.REGISTERS_E, Token.REGISTER);  }
"h" { return token(TokenImpl.REGISTERS_H, Token.REGISTER);  }
"l" { return token(TokenImpl.REGISTERS_L, Token.REGISTER);  }
"m" { return token(TokenImpl.REGISTERS_M, Token.REGISTER);  }
"psw" { return token(TokenImpl.REGISTERS_PSW, Token.REGISTER);  }
"sp" { return token(TokenImpl.REGISTERS_SP, Token.REGISTER);  }

/* separators */
"(" { return token(TokenImpl.SEPARATOR_LPAR, Token.SEPARATOR);  }
")" { return token(TokenImpl.SEPARATOR_RPAR, Token.SEPARATOR);  }
"," { return token(TokenImpl.SEPARATOR_COMMA, Token.SEPARATOR);  }
{Eol} { return token(TokenImpl.SEPARATOR_EOL, Token.SEPARATOR);  }
{WhiteSpace}+ { /* ignore white spaces */ }

/* operators */
"+" { return token(TokenImpl.OPERATOR_ADD, Token.OPERATOR);  }
"-" { return token(TokenImpl.OPERATOR_SUBTRACT, Token.OPERATOR);  }
"*" { return token(TokenImpl.OPERATOR_MULTIPLY, Token.OPERATOR);  }
"/" { return token(TokenImpl.OPERATOR_DIVIDE, Token.OPERATOR);  }
"=" { return token(TokenImpl.OPERATOR_EQUAL, Token.OPERATOR);  }
"mod" { return token(TokenImpl.OPERATOR_MOD, Token.OPERATOR);  }
"shr" { return token(TokenImpl.OPERATOR_SHR, Token.OPERATOR);  }
"shl" { return token(TokenImpl.OPERATOR_SHL, Token.OPERATOR);  }
"not" { return token(TokenImpl.OPERATOR_NOT, Token.OPERATOR);  }
"and" { return token(TokenImpl.OPERATOR_AND, Token.OPERATOR);  }
"or" { return token(TokenImpl.OPERATOR_OR, Token.OPERATOR);  }
"xor" { return token(TokenImpl.OPERATOR_XOR, Token.OPERATOR);  }

/* comment */
{Comment} { return token(TokenImpl.TCOMMENT, Token.COMMENT);  }

/* literals */
{DecimalNum} {
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
    return token(TokenImpl.ERROR_UNCLOSED_STRING, Token.ERROR);
}
{String} {
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
    return token(TokenImpl.TIDENTIFIER, Token.IDENTIFIER, yytext().toUpperCase());
}
{Label} {
    String text = yytext();
    Object val = text.substring(0,text.length()-1).toUpperCase();
    return token(TokenImpl.TLABEL, Token.LABEL, val);
}
. {
    return token(TokenImpl.ERROR_UNKNOWN_TOKEN, Token.ERROR);
}
