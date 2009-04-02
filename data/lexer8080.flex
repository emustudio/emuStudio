/*
 * lexer8080.flex
 *
 * Lexical analyser for 8080 compiler
 *
 * KEEP IT SIMPLE STUPID
 * sometimes just: YOU AREN'T GONNA NEED IT
 *
 * created with JFlex
 *
 */

package assembler8080.impl;

import plugins.compiler.*;
import java.io.*;
import java_cup.runtime.*;

%%

/* options */
%class lexer8080
%cup
%public
%implements ILexer
%line
%column
%char
%caseless
%unicode
%type token8080

%{
    private int lastToken;

    public token8080 getSymbol() throws IOException {
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
%}
%eofval{
    lastToken = token8080.EOF;
    String text = yytext();
    return (new token8080(lastToken,lastToken,text,null,yyline,yycolumn,yychar,
        yychar+text.length(),true));
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
    lastToken = token8080.RESERVED_STC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
 }
"cmc" {
    lastToken = token8080.RESERVED_CMC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"inr" {
    lastToken = token8080.RESERVED_INR;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"dcr" {
    lastToken = token8080.RESERVED_DCR;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cma" {
    lastToken = token8080.RESERVED_CMA;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"daa" {
    lastToken = token8080.RESERVED_DAA;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"nop" {
    lastToken = token8080.RESERVED_NOP;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"mov" {
    lastToken = token8080.RESERVED_MOV;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"stax" {
    lastToken = token8080.RESERVED_STAX;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"ldax" {
    lastToken = token8080.RESERVED_LDAX;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"add" {
    lastToken = token8080.RESERVED_ADD;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"adc" {
    lastToken = token8080.RESERVED_ADC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"sub" {
    lastToken = token8080.RESERVED_SUB;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"sbb" {
    lastToken = token8080.RESERVED_SBB;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"ana" {
    lastToken = token8080.RESERVED_ANA;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"xra" {
    lastToken = token8080.RESERVED_XRA;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"ora" {
    lastToken = token8080.RESERVED_ORA;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cmp" {
    lastToken = token8080.RESERVED_CMP;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rlc" {
    lastToken = token8080.RESERVED_RLC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rrc" {
    lastToken = token8080.RESERVED_RRC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"ral" {
    lastToken = token8080.RESERVED_RAL;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rar" {
    lastToken = token8080.RESERVED_RAR;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"push" {
    lastToken = token8080.RESERVED_PUSH;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"pop" {
    lastToken = token8080.RESERVED_POP;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"dad" {
    lastToken = token8080.RESERVED_DAD;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"inx" {
    lastToken = token8080.RESERVED_INX;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"dcx" {
    lastToken = token8080.RESERVED_DCX;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"xchg" {
    lastToken = token8080.RESERVED_XCHG;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"xthl" {
    lastToken = token8080.RESERVED_XTHL;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"sphl" {
    lastToken = token8080.RESERVED_SPHL;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"lxi" {
    lastToken = token8080.RESERVED_LXI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"mvi" {
    lastToken = token8080.RESERVED_MVI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"adi" {
    lastToken = token8080.RESERVED_ADI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"aci" {
    lastToken = token8080.RESERVED_ACI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"sui" {
    lastToken = token8080.RESERVED_SUI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"sbi" {
    lastToken = token8080.RESERVED_SBI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"ani" {
    lastToken = token8080.RESERVED_ANI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"xri" {
    lastToken = token8080.RESERVED_XRI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"ori" {
    lastToken = token8080.RESERVED_ORI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cpi" {
    lastToken = token8080.RESERVED_CPI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"sta" {
    lastToken = token8080.RESERVED_STA;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"lda" {
    lastToken = token8080.RESERVED_LDA;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"shld" {
    lastToken = token8080.RESERVED_SHLD;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"lhld" {
    lastToken = token8080.RESERVED_LHLD;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"pchl" {
    lastToken = token8080.RESERVED_PCHL;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"jmp" {
    lastToken = token8080.RESERVED_JMP;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"jc" {
    lastToken = token8080.RESERVED_JC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"jnc" {
    lastToken = token8080.RESERVED_JNC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"jz" {
    lastToken = token8080.RESERVED_JZ;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"jnz"  {
    lastToken = token8080.RESERVED_JNZ;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"jp"  {
    lastToken = token8080.RESERVED_JP;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"jm"  {
    lastToken = token8080.RESERVED_JM;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"jpe"  {
    lastToken = token8080.RESERVED_JPE;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"jpo"  {
    lastToken = token8080.RESERVED_JPO;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"call"  {
    lastToken = token8080.RESERVED_CALL;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cc"  {
    lastToken = token8080.RESERVED_CC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cnc"  {
    lastToken = token8080.RESERVED_CNC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cz"  {
    lastToken = token8080.RESERVED_CZ;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cnz"  {
    lastToken = token8080.RESERVED_CNZ;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cp"  {
    lastToken = token8080.RESERVED_CP;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cm" {
    lastToken = token8080.RESERVED_CM;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cpe" {
    lastToken = token8080.RESERVED_CPE;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"cpo"  {
    lastToken = token8080.RESERVED_CPO;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"ret"  {
    lastToken = token8080.RESERVED_RET;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rc"  {
    lastToken = token8080.RESERVED_RC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rnc"  {
    lastToken = token8080.RESERVED_RNC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rz"  {
    lastToken = token8080.RESERVED_RZ;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rnz" {
    lastToken = token8080.RESERVED_RNZ;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rm" {
    lastToken = token8080.RESERVED_RM;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rp" {
    lastToken = token8080.RESERVED_RP;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rpe" {
    lastToken = token8080.RESERVED_RPE;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rpo" {
    lastToken = token8080.RESERVED_RPO;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"rst" {
    lastToken = token8080.RESERVED_RST;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"ei" {
    lastToken = token8080.RESERVED_EI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"di" {
    lastToken = token8080.RESERVED_DI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"in" {
    lastToken = token8080.RESERVED_IN;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"out" {
    lastToken = token8080.RESERVED_OUT;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"hlt" {
    lastToken = token8080.RESERVED_HLT;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}

/* preprocessor words */
"org" {
    lastToken = token8080.PREPROCESSOR_ORG;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"equ" {
    lastToken = token8080.PREPROCESSOR_EQU;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"set" {
    lastToken = token8080.PREPROCESSOR_SET;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"include" {
    lastToken = token8080.PREPROCESSOR_INCLUDE;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"if" {
    lastToken = token8080.PREPROCESSOR_IF;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"endif" {
    lastToken = token8080.PREPROCESSOR_ENDIF;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"macro" {
    lastToken = token8080.PREPROCESSOR_MACRO;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"endm" {
    lastToken = token8080.PREPROCESSOR_ENDM;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"db" {
    lastToken = token8080.PREPROCESSOR_DB;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"dw" {
    lastToken = token8080.PREPROCESSOR_DW;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"ds" {
    lastToken = token8080.PREPROCESSOR_DS;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"$" {
    lastToken = token8080.PREPROCESSOR_ADDR;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}

/* registers */
"a" {
    lastToken = token8080.REGISTERS_A;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"b" {
    lastToken = token8080.REGISTERS_B;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"c" {
    lastToken = token8080.REGISTERS_C;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"d" {
    lastToken = token8080.REGISTERS_D;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"e" {
    lastToken = token8080.REGISTERS_E;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"h" {
    lastToken = token8080.REGISTERS_H;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"l" {
    lastToken = token8080.REGISTERS_L;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"m" {
    lastToken = token8080.REGISTERS_M;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"psw" {
    lastToken = token8080.REGISTERS_PSW;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"sp" {
    lastToken = token8080.REGISTERS_SP;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}

/* separators */
"(" {
    lastToken = token8080.SEPARATOR_LPAR;
    String text = yytext();
    return (new token8080(lastToken,IToken.SEPARATOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
")" {
    lastToken = token8080.SEPARATOR_RPAR;
    String text = yytext();
    return (new token8080(lastToken,IToken.SEPARATOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
"," {
    lastToken = token8080.SEPARATOR_COMMA;
    String text = yytext();
    return (new token8080(lastToken,IToken.SEPARATOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
{Eol} {
    lastToken = token8080.SEPARATOR_EOL;
    String text = yytext();
    return (new token8080(lastToken,IToken.SEPARATOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
{WhiteSpace}+ { /* ignore white spaces */ }

/* operators */
"+" {
    lastToken = token8080.OPERATOR_ADD;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"-" {
    lastToken = token8080.OPERATOR_SUBTRACT;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"*" {
    lastToken = token8080.OPERATOR_MULTIPLY;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"/" {
    lastToken = token8080.OPERATOR_DIVIDE;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"=" {
    lastToken = token8080.OPERATOR_EQUAL;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"mod" {
    lastToken = token8080.OPERATOR_MOD;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"shr" {
    lastToken = token8080.OPERATOR_SHR;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"shl" {
    lastToken = token8080.OPERATOR_SHL;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"not" {
    lastToken = token8080.OPERATOR_NOT;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"and" {
    lastToken = token8080.OPERATOR_AND;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"or" {
    lastToken = token8080.OPERATOR_OR;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
"xor" {
    lastToken = token8080.OPERATOR_XOR;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}

/* comment */
{Comment} {
    lastToken = token8080.TCOMMENT;
    String text = yytext();
    return (new token8080(lastToken,IToken.COMMENT,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}

/* literals */
{DecimalNum} {
    String text = yytext();
    text = text.replaceFirst("[dD]","");
    int num=0;
    int tokenType = 0;
    try {
        num = Integer.parseInt(text,10);
        if (num > 65535) { // || num < -32768) {
            lastToken = token8080.ERROR_DECIMAL_SIZE;
            tokenType = IToken.ERROR;
        } else if (num > 255) { // || num < -128) {
            lastToken = token8080.LITERAL_DECIMAL_16BIT;
            tokenType = IToken.LITERAL;
        } else {
            lastToken = token8080.LITERAL_DECIMAL_8BIT;
            tokenType = IToken.LITERAL;
        }
    } catch (NumberFormatException e) {
        lastToken = token8080.ERROR_DECIMAL_SIZE;
        tokenType = IToken.LITERAL;
    }
    return (new token8080(lastToken,tokenType,yytext(),(Object)num,yyline,
        yycolumn,yychar,yychar+yytext().length(),true));
}
{OctalNum} {
    String text = yytext();
    int num=0;
    int tokenType=0;
    text = text.replaceFirst("[oOqQ]","");
    try {

        num = Integer.parseInt(text,8);
        if (num > 65535) { // || num < -32768) {
            lastToken = token8080.ERROR_DECIMAL_SIZE;
            tokenType = IToken.ERROR;
        } else if (num > 255) { // || num < -128) {
            lastToken = token8080.LITERAL_DECIMAL_16BIT;
            tokenType = IToken.LITERAL;
        } else {
            lastToken = token8080.LITERAL_DECIMAL_8BIT;
            tokenType = IToken.LITERAL;
        }
    } catch (NumberFormatException e) {
        lastToken = token8080.ERROR_DECIMAL_SIZE;
        tokenType = IToken.ERROR;
    }
    return (new token8080(lastToken,tokenType,yytext(),(Object)num,yyline,
        yycolumn,yychar,yychar+yytext().length(),true));
}
{HexaNum} {
    String text = yytext();
    int num=0;
    int tokenType=0;
    text = text.replaceFirst("[hH]","");
    try {
        num = Integer.parseInt(text,16);
        if (num > 65535) {
            lastToken = token8080.ERROR_DECIMAL_SIZE;
            tokenType = IToken.ERROR;
        } else if (num > 255) {
            lastToken = token8080.LITERAL_DECIMAL_16BIT;
            tokenType = IToken.LITERAL;
        } else {
            lastToken = token8080.LITERAL_DECIMAL_8BIT;
            tokenType = IToken.LITERAL;
        }
    } catch (NumberFormatException e) {
        lastToken = token8080.ERROR_DECIMAL_SIZE;
        tokenType = IToken.ERROR;
    }
    return (new token8080(lastToken,tokenType,yytext(),(Object)num,yyline,
        yycolumn,yychar,yychar+yytext().length(),true));
}
{BinaryNum} {
    String text = yytext();
    int num=0;
    int tokenType=0;
    text = text.replaceFirst("[bB]","");
    try {
        num = Integer.parseInt(text,2);
        if (num > 65535) {
            lastToken = token8080.ERROR_DECIMAL_SIZE;
            tokenType = IToken.ERROR;
        } else if (num > 255) {
            lastToken = token8080.LITERAL_DECIMAL_16BIT;
            tokenType = IToken.LITERAL;
        } else {
            lastToken = token8080.LITERAL_DECIMAL_8BIT;
            tokenType = IToken.LITERAL;
        }
    } catch (NumberFormatException e) {
        lastToken = token8080.ERROR_DECIMAL_SIZE;
        tokenType = IToken.ERROR;
    }
    return (new token8080(lastToken,tokenType,yytext(),(Object)num,yyline,
        yycolumn,yychar,yychar+yytext().length(),true));
}
{UnclosedString} {
    lastToken = token8080.ERROR_UNCLOSED_STRING;
    String text = yytext();
    return (new token8080(lastToken,IToken.ERROR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
{String} {
    String text = yytext();
    String val = text.substring(1,text.length()-1);
    if (val.length() > 2) {
        lastToken = token8080.LITERAL_STRING;
        return (new token8080(lastToken,IToken.LITERAL,text,val,yyline,yycolumn,
            yychar,yychar+text.length(),true));
    }
    else {
        byte[] b = val.getBytes();
        int numval = b[0];
        for (int i = 1; i < b.length; i++)
            numval = (numval <<8) + b[i];
        if (numval > 255) lastToken = token8080.LITERAL_DECIMAL_16BIT;
        else lastToken = token8080.LITERAL_DECIMAL_8BIT;
        return (new token8080(lastToken,IToken.LITERAL,text,numval,yyline,yycolumn,
            yychar,yychar+text.length(),true));
    }
}
{Identifier} {
    lastToken = token8080.TIDENTIFIER;
    String text = yytext();
    Object val = text.toUpperCase();
    return (new token8080(lastToken,IToken.IDENTIFIER,text,val,yyline,
        yycolumn,yychar,yychar+text.length(),true));
}
{Label} {
    lastToken = token8080.TLABEL;
    String text = yytext();
    Object val = text.substring(0,text.length()-1).toUpperCase();
    return (new token8080(lastToken,IToken.LABEL,text,val,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
. { lastToken = token8080.ERROR_UNKNOWN_TOKEN;
    String text = yytext();
    return (new token8080(lastToken,IToken.ERROR,text,null,yyline,yycolumn,
        yychar,yychar+text.length(),true));
}
