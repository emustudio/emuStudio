/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package net.sf.emustudio.intel8080.assembler.impl;

import emulib.plugins.compiler.LexicalAnalyzer;
import emulib.plugins.compiler.Token;
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
%type Tokens

%{
    private int lastToken;

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
%}
%eofval{
    lastToken = Tokens.EOF;
    String text = yytext();
    return (new Tokens(lastToken,lastToken,text,null,yyline,yycolumn,yychar, yychar+text.length(),true));
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
"stc" {
    lastToken = Tokens.RESERVED_STC;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
 }
"cmc" {
    lastToken = Tokens.RESERVED_CMC;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"inr" {
    lastToken = Tokens.RESERVED_INR;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"dcr" {
    lastToken = Tokens.RESERVED_DCR;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cma" {
    lastToken = Tokens.RESERVED_CMA;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"daa" {
    lastToken = Tokens.RESERVED_DAA;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"nop" {
    lastToken = Tokens.RESERVED_NOP;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"mov" {
    lastToken = Tokens.RESERVED_MOV;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"stax" {
    lastToken = Tokens.RESERVED_STAX;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"ldax" {
    lastToken = Tokens.RESERVED_LDAX;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"add" {
    lastToken = Tokens.RESERVED_ADD;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"adc" {
    lastToken = Tokens.RESERVED_ADC;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"sub" {
    lastToken = Tokens.RESERVED_SUB;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"sbb" {
    lastToken = Tokens.RESERVED_SBB;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"ana" {
    lastToken = Tokens.RESERVED_ANA;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"xra" {
    lastToken = Tokens.RESERVED_XRA;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"ora" {
    lastToken = Tokens.RESERVED_ORA;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cmp" {
    lastToken = Tokens.RESERVED_CMP;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rlc" {
    lastToken = Tokens.RESERVED_RLC;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rrc" {
    lastToken = Tokens.RESERVED_RRC;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"ral" {
    lastToken = Tokens.RESERVED_RAL;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rar" {
    lastToken = Tokens.RESERVED_RAR;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"push" {
    lastToken = Tokens.RESERVED_PUSH;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"pop" {
    lastToken = Tokens.RESERVED_POP;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"dad" {
    lastToken = Tokens.RESERVED_DAD;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"inx" {
    lastToken = Tokens.RESERVED_INX;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"dcx" {
    lastToken = Tokens.RESERVED_DCX;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"xchg" {
    lastToken = Tokens.RESERVED_XCHG;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"xthl" {
    lastToken = Tokens.RESERVED_XTHL;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"sphl" {
    lastToken = Tokens.RESERVED_SPHL;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"lxi" {
    lastToken = Tokens.RESERVED_LXI;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"mvi" {
    lastToken = Tokens.RESERVED_MVI;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"adi" {
    lastToken = Tokens.RESERVED_ADI;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"aci" {
    lastToken = Tokens.RESERVED_ACI;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"sui" {
    lastToken = Tokens.RESERVED_SUI;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"sbi" {
    lastToken = Tokens.RESERVED_SBI;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"ani" {
    lastToken = Tokens.RESERVED_ANI;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"xri" {
    lastToken = Tokens.RESERVED_XRI;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"ori" {
    lastToken = Tokens.RESERVED_ORI;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cpi" {
    lastToken = Tokens.RESERVED_CPI;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"sta" {
    lastToken = Tokens.RESERVED_STA;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"lda" {
    lastToken = Tokens.RESERVED_LDA;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"shld" {
    lastToken = Tokens.RESERVED_SHLD;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"lhld" {
    lastToken = Tokens.RESERVED_LHLD;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"pchl" {
    lastToken = Tokens.RESERVED_PCHL;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"jmp" {
    lastToken = Tokens.RESERVED_JMP;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"jc" {
    lastToken = Tokens.RESERVED_JC;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"jnc" {
    lastToken = Tokens.RESERVED_JNC;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"jz" {
    lastToken = Tokens.RESERVED_JZ;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"jnz"  {
    lastToken = Tokens.RESERVED_JNZ;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"jp"  {
    lastToken = Tokens.RESERVED_JP;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"jm"  {
    lastToken = Tokens.RESERVED_JM;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"jpe"  {
    lastToken = Tokens.RESERVED_JPE;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"jpo"  {
    lastToken = Tokens.RESERVED_JPO;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"call"  {
    lastToken = Tokens.RESERVED_CALL;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cc"  {
    lastToken = Tokens.RESERVED_CC;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cnc"  {
    lastToken = Tokens.RESERVED_CNC;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cz"  {
    lastToken = Tokens.RESERVED_CZ;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cnz"  {
    lastToken = Tokens.RESERVED_CNZ;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cp"  {
    lastToken = Tokens.RESERVED_CP;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cm" {
    lastToken = Tokens.RESERVED_CM;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cpe" {
    lastToken = Tokens.RESERVED_CPE;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cpo"  {
    lastToken = Tokens.RESERVED_CPO;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"ret"  {
    lastToken = Tokens.RESERVED_RET;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rc"  {
    lastToken = Tokens.RESERVED_RC;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rnc"  {
    lastToken = Tokens.RESERVED_RNC;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rz"  {
    lastToken = Tokens.RESERVED_RZ;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rnz" {
    lastToken = Tokens.RESERVED_RNZ;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rm" {
    lastToken = Tokens.RESERVED_RM;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rp" {
    lastToken = Tokens.RESERVED_RP;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rpe" {
    lastToken = Tokens.RESERVED_RPE;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rpo" {
    lastToken = Tokens.RESERVED_RPO;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rst" {
    lastToken = Tokens.RESERVED_RST;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"ei" {
    lastToken = Tokens.RESERVED_EI;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"di" {
    lastToken = Tokens.RESERVED_DI;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"in" {
    lastToken = Tokens.RESERVED_IN;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"out" {
    lastToken = Tokens.RESERVED_OUT;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"hlt" {
    lastToken = Tokens.RESERVED_HLT;
    String text = yytext();
    return (new Tokens(lastToken,Token.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}

/* preprocessor words */
"org" {
    lastToken = Tokens.PREPROCESSOR_ORG;
    String text = yytext();
    return (new Tokens(lastToken,Token.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"equ" {
    lastToken = Tokens.PREPROCESSOR_EQU;
    String text = yytext();
    return (new Tokens(lastToken,Token.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"set" {
    lastToken = Tokens.PREPROCESSOR_SET;
    String text = yytext();
    return (new Tokens(lastToken,Token.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"include" {
    lastToken = Tokens.PREPROCESSOR_INCLUDE;
    String text = yytext();
    return (new Tokens(lastToken,Token.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"if" {
    lastToken = Tokens.PREPROCESSOR_IF;
    String text = yytext();
    return (new Tokens(lastToken,Token.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"endif" {
    lastToken = Tokens.PREPROCESSOR_ENDIF;
    String text = yytext();
    return (new Tokens(lastToken,Token.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"macro" {
    lastToken = Tokens.PREPROCESSOR_MACRO;
    String text = yytext();
    return (new Tokens(lastToken,Token.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"endm" {
    lastToken = Tokens.PREPROCESSOR_ENDM;
    String text = yytext();
    return (new Tokens(lastToken,Token.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"db" {
    lastToken = Tokens.PREPROCESSOR_DB;
    String text = yytext();
    return (new Tokens(lastToken,Token.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"dw" {
    lastToken = Tokens.PREPROCESSOR_DW;
    String text = yytext();
    return (new Tokens(lastToken,Token.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"ds" {
    lastToken = Tokens.PREPROCESSOR_DS;
    String text = yytext();
    return (new Tokens(lastToken,Token.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"$" {
    lastToken = Tokens.PREPROCESSOR_ADDR;
    String text = yytext();
    return (new Tokens(lastToken,Token.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}

/* registers */
"a" {
    lastToken = Tokens.REGISTERS_A;
    String text = yytext();
    return (new Tokens(lastToken,Token.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"b" {
    lastToken = Tokens.REGISTERS_B;
    String text = yytext();
    return (new Tokens(lastToken,Token.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"c" {
    lastToken = Tokens.REGISTERS_C;
    String text = yytext();
    return (new Tokens(lastToken,Token.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"d" {
    lastToken = Tokens.REGISTERS_D;
    String text = yytext();
    return (new Tokens(lastToken,Token.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"e" {
    lastToken = Tokens.REGISTERS_E;
    String text = yytext();
    return (new Tokens(lastToken,Token.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"h" {
    lastToken = Tokens.REGISTERS_H;
    String text = yytext();
    return (new Tokens(lastToken,Token.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"l" {
    lastToken = Tokens.REGISTERS_L;
    String text = yytext();
    return (new Tokens(lastToken,Token.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"m" {
    lastToken = Tokens.REGISTERS_M;
    String text = yytext();
    return (new Tokens(lastToken,Token.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"psw" {
    lastToken = Tokens.REGISTERS_PSW;
    String text = yytext();
    return (new Tokens(lastToken,Token.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"sp" {
    lastToken = Tokens.REGISTERS_SP;
    String text = yytext();
    return (new Tokens(lastToken,Token.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}

/* separators */
"(" {
    lastToken = Tokens.SEPARATOR_LPAR;
    String text = yytext();
    return (new Tokens(lastToken,Token.SEPARATOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
")" {
    lastToken = Tokens.SEPARATOR_RPAR;
    String text = yytext();
    return (new Tokens(lastToken,Token.SEPARATOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"," {
    lastToken = Tokens.SEPARATOR_COMMA;
    String text = yytext();
    return (new Tokens(lastToken,Token.SEPARATOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
{Eol} {
    lastToken = Tokens.SEPARATOR_EOL;
    String text = yytext();
    return (new Tokens(lastToken,Token.SEPARATOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
{WhiteSpace}+ { /* ignore white spaces */ }

/* operators */
"+" {
    lastToken = Tokens.OPERATOR_ADD;
    String text = yytext();
    return (new Tokens(lastToken,Token.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"-" {
    lastToken = Tokens.OPERATOR_SUBTRACT;
    String text = yytext();
    return (new Tokens(lastToken,Token.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"*" {
    lastToken = Tokens.OPERATOR_MULTIPLY;
    String text = yytext();
    return (new Tokens(lastToken,Token.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"/" {
    lastToken = Tokens.OPERATOR_DIVIDE;
    String text = yytext();
    return (new Tokens(lastToken,Token.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"=" {
    lastToken = Tokens.OPERATOR_EQUAL;
    String text = yytext();
    return (new Tokens(lastToken,Token.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"mod" {
    lastToken = Tokens.OPERATOR_MOD;
    String text = yytext();
    return (new Tokens(lastToken,Token.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"shr" {
    lastToken = Tokens.OPERATOR_SHR;
    String text = yytext();
    return (new Tokens(lastToken,Token.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"shl" {
    lastToken = Tokens.OPERATOR_SHL;
    String text = yytext();
    return (new Tokens(lastToken,Token.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"not" {
    lastToken = Tokens.OPERATOR_NOT;
    String text = yytext();
    return (new Tokens(lastToken,Token.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"and" {
    lastToken = Tokens.OPERATOR_AND;
    String text = yytext();
    return (new Tokens(lastToken,Token.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"or" {
    lastToken = Tokens.OPERATOR_OR;
    String text = yytext();
    return (new Tokens(lastToken,Token.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"xor" {
    lastToken = Tokens.OPERATOR_XOR;
    String text = yytext();
    return (new Tokens(lastToken,Token.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}

/* comment */
{Comment} {
    lastToken = Tokens.TCOMMENT;
    String text = yytext();
    return (new Tokens(lastToken,Token.COMMENT,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}

/* literals */
{DecimalNum} {
    String text = yytext();
    text = text.replaceFirst("[dD]","");
    int num=0;
    int tokenType;
    try {
        num = Integer.parseInt(text,10);
        if (num > 65535) { // || num < -32768) {
            lastToken = Tokens.ERROR_DECIMAL_SIZE;
            tokenType = Token.ERROR;
        } else if (num > 255) { // || num < -128) {
            lastToken = Tokens.LITERAL_DECIMAL_16BIT;
            tokenType = Token.LITERAL;
        } else {
            lastToken = Tokens.LITERAL_DECIMAL_8BIT;
            tokenType = Token.LITERAL;
        }
    } catch (NumberFormatException e) {
        lastToken = Tokens.ERROR_DECIMAL_SIZE;
        tokenType = Token.LITERAL;
    }
    return (new Tokens(lastToken,tokenType,yytext(),(Object)num,yyline,
        yycolumn,yychar,yychar+yytext().length(),true));
}
{OctalNum} {
    String text = yytext();
    int num=0;
    int tokenType;
    text = text.replaceFirst("[oOqQ]","");
    try {

        num = Integer.parseInt(text,8);
        if (num > 65535) { // || num < -32768) {
            lastToken = Tokens.ERROR_DECIMAL_SIZE;
            tokenType = Token.ERROR;
        } else if (num > 255) { // || num < -128) {
            lastToken = Tokens.LITERAL_DECIMAL_16BIT;
            tokenType = Token.LITERAL;
        } else {
            lastToken = Tokens.LITERAL_DECIMAL_8BIT;
            tokenType = Token.LITERAL;
        }
    } catch (NumberFormatException e) {
        lastToken = Tokens.ERROR_DECIMAL_SIZE;
        tokenType = Token.ERROR;
    }
    return (new Tokens(lastToken,tokenType,yytext(),(Object)num,yyline,
        yycolumn,yychar,yychar+yytext().length(),true));
}
{HexaNum} {
    String text = yytext();
    int num=0;
    int tokenType;
    text = text.replaceFirst("[hH]","");
    try {
        num = Integer.parseInt(text,16);
        if (num > 65535) {
            lastToken = Tokens.ERROR_DECIMAL_SIZE;
            tokenType = Token.ERROR;
        } else if (num > 255) {
            lastToken = Tokens.LITERAL_DECIMAL_16BIT;
            tokenType = Token.LITERAL;
        } else {
            lastToken = Tokens.LITERAL_DECIMAL_8BIT;
            tokenType = Token.LITERAL;
        }
    } catch (NumberFormatException e) {
        lastToken = Tokens.ERROR_DECIMAL_SIZE;
        tokenType = Token.ERROR;
    }
    return (new Tokens(lastToken,tokenType,yytext(),(Object)num,yyline,
        yycolumn,yychar,yychar+yytext().length(),true));
}
{BinaryNum} {
    String text = yytext();
    int num=0;
    int tokenType;
    text = text.replaceFirst("[bB]","");
    try {
        num = Integer.parseInt(text,2);
        if (num > 65535) {
            lastToken = Tokens.ERROR_DECIMAL_SIZE;
            tokenType = Token.ERROR;
        } else if (num > 255) {
            lastToken = Tokens.LITERAL_DECIMAL_16BIT;
            tokenType = Token.LITERAL;
        } else {
            lastToken = Tokens.LITERAL_DECIMAL_8BIT;
            tokenType = Token.LITERAL;
        }
    } catch (NumberFormatException e) {
        lastToken = Tokens.ERROR_DECIMAL_SIZE;
        tokenType = Token.ERROR;
    }
    return (new Tokens(lastToken,tokenType,yytext(),(Object)num,yyline,
        yycolumn,yychar,yychar+yytext().length(),true));
}
{UnclosedString} {
    lastToken = Tokens.ERROR_UNCLOSED_STRING;
    String text = yytext();
    return (new Tokens(lastToken,Token.ERROR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
{String} {
    String text = yytext();
    String val = text.substring(1,text.length()-1);
    if (val.length() > 1) {
        lastToken = Tokens.LITERAL_STRING;
        return (new Tokens(lastToken,Token.LITERAL,text,val,yyline,yycolumn,
            yychar,yychar+text.length(),true));
    }
    else {
        byte[] b = val.getBytes();
        int numval = b[0];
        for (int i = 1; i < b.length; i++)
            numval = (numval <<8) + b[i];
        if (numval > 255) lastToken = Tokens.LITERAL_DECIMAL_16BIT;
        else lastToken = Tokens.LITERAL_DECIMAL_8BIT;
        return (new Tokens(lastToken,Token.LITERAL,text,numval,yyline,yycolumn,
            yychar,yychar+text.length(),true));
    }
}
{Identifier} {
    lastToken = Tokens.TIDENTIFIER;
    String text = yytext();
    Object val = text.toUpperCase();
    return (new Tokens(lastToken,Token.IDENTIFIER,text,val,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
{Label} {
    lastToken = Tokens.TLABEL;
    String text = yytext();
    Object val = text.substring(0,text.length()-1).toUpperCase();
    return (new Tokens(lastToken,Token.LABEL,text,val,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
. { lastToken = Tokens.ERROR_UNKNOWN_TOKEN;
    String text = yytext();
    return (new Tokens(lastToken,Token.ERROR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
