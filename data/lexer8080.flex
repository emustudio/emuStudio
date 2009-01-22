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

package compiler8080;

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

    public void reset(int yyline, int yychar, int yycolumn) {
        yyreset(zzReader);
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
        yychar+text.length()));
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
        yychar,yychar+text.length()));
 }
"cmc" {
    lastToken = token8080.RESERVED_CMC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"inr" {
    lastToken = token8080.RESERVED_INR;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"dcr" {
    lastToken = token8080.RESERVED_DCR;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"cma" {
    lastToken = token8080.RESERVED_CMA;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"daa" {
    lastToken = token8080.RESERVED_DAA;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"nop" {
    lastToken = token8080.RESERVED_NOP;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"mov" {
    lastToken = token8080.RESERVED_MOV;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"stax" {
    lastToken = token8080.RESERVED_STAX;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"ldax" {
    lastToken = token8080.RESERVED_LDAX;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"add" {
    lastToken = token8080.RESERVED_ADD;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"adc" {
    lastToken = token8080.RESERVED_ADC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"sub" {
    lastToken = token8080.RESERVED_SUB;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"sbb" {
    lastToken = token8080.RESERVED_SBB;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"ana" {
    lastToken = token8080.RESERVED_ANA;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"xra" {
    lastToken = token8080.RESERVED_XRA;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"ora" {
    lastToken = token8080.RESERVED_ORA;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"cmp" {
    lastToken = token8080.RESERVED_CMP;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"rlc" {
    lastToken = token8080.RESERVED_RLC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"rrc" {
    lastToken = token8080.RESERVED_RRC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"ral" {
    lastToken = token8080.RESERVED_RAL;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"rar" {
    lastToken = token8080.RESERVED_RAR;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"push" {
    lastToken = token8080.RESERVED_PUSH;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"pop" {
    lastToken = token8080.RESERVED_POP;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"dad" {
    lastToken = token8080.RESERVED_DAD;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"inx" {
    lastToken = token8080.RESERVED_INX;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"dcx" {
    lastToken = token8080.RESERVED_DCX;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"xchg" {
    lastToken = token8080.RESERVED_XCHG;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"xthl" {
    lastToken = token8080.RESERVED_XTHL;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"sphl" {
    lastToken = token8080.RESERVED_SPHL;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"lxi" {
    lastToken = token8080.RESERVED_LXI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"mvi" {
    lastToken = token8080.RESERVED_MVI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"adi" {
    lastToken = token8080.RESERVED_ADI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"aci" {
    lastToken = token8080.RESERVED_ACI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"sui" {
    lastToken = token8080.RESERVED_SUI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"sbi" {
    lastToken = token8080.RESERVED_SBI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"ani" {
    lastToken = token8080.RESERVED_ANI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"xri" {
    lastToken = token8080.RESERVED_XRI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"ori" {
    lastToken = token8080.RESERVED_ORI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"cpi" {
    lastToken = token8080.RESERVED_CPI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"sta" {
    lastToken = token8080.RESERVED_STA;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"lda" {
    lastToken = token8080.RESERVED_LDA;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"shld" {
    lastToken = token8080.RESERVED_SHLD;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"lhld" {
    lastToken = token8080.RESERVED_LHLD;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"pchl" {
    lastToken = token8080.RESERVED_PCHL;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"jmp" {
    lastToken = token8080.RESERVED_JMP;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"jc" {
    lastToken = token8080.RESERVED_JC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"jnc" {
    lastToken = token8080.RESERVED_JNC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"jz" {
    lastToken = token8080.RESERVED_JZ;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"jnz"  {
    lastToken = token8080.RESERVED_JNZ;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"jp"  {
    lastToken = token8080.RESERVED_JP;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"jm"  {
    lastToken = token8080.RESERVED_JM;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"jpe"  {
    lastToken = token8080.RESERVED_JPE;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"jpo"  {
    lastToken = token8080.RESERVED_JPO;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"call"  {
    lastToken = token8080.RESERVED_CALL;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"cc"  {
    lastToken = token8080.RESERVED_CC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"cnc"  {
    lastToken = token8080.RESERVED_CNC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"cz"  {
    lastToken = token8080.RESERVED_CZ;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"cnz"  {
    lastToken = token8080.RESERVED_CNZ;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"cp"  {
    lastToken = token8080.RESERVED_CP;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"cm" {
    lastToken = token8080.RESERVED_CM;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"cpe" {
    lastToken = token8080.RESERVED_CPE;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"cpo"  {
    lastToken = token8080.RESERVED_CPO;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"ret"  {
    lastToken = token8080.RESERVED_RET;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"rc"  {
    lastToken = token8080.RESERVED_RC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"rnc"  {
    lastToken = token8080.RESERVED_RNC;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"rz"  {
    lastToken = token8080.RESERVED_RZ;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"rnz" {
    lastToken = token8080.RESERVED_RNZ;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"rm" {
    lastToken = token8080.RESERVED_RM;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"rp" {
    lastToken = token8080.RESERVED_RP;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"rpe" {
    lastToken = token8080.RESERVED_RPE;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"rpo" {
    lastToken = token8080.RESERVED_RPO;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"rst" {
    lastToken = token8080.RESERVED_RST;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"ei" {
    lastToken = token8080.RESERVED_EI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"di" {
    lastToken = token8080.RESERVED_DI;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"in" {
    lastToken = token8080.RESERVED_IN;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"out" {
    lastToken = token8080.RESERVED_OUT;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"hlt" {
    lastToken = token8080.RESERVED_HLT;
    String text = yytext();
    return (new token8080(lastToken,IToken.RESERVED,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}

/* preprocessor words */
"org" {
    lastToken = token8080.PREPROCESSOR_ORG;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"equ" {
    lastToken = token8080.PREPROCESSOR_EQU;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"set" {
    lastToken = token8080.PREPROCESSOR_SET;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"include" {
    lastToken = token8080.PREPROCESSOR_INCLUDE;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"if" {
    lastToken = token8080.PREPROCESSOR_IF;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"endif" {
    lastToken = token8080.PREPROCESSOR_ENDIF;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"macro" {
    lastToken = token8080.PREPROCESSOR_MACRO;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"endm" {
    lastToken = token8080.PREPROCESSOR_ENDM;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"db" {
    lastToken = token8080.PREPROCESSOR_DB;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"dw" {
    lastToken = token8080.PREPROCESSOR_DW;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"ds" {
    lastToken = token8080.PREPROCESSOR_DS;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"$" {
    lastToken = token8080.PREPROCESSOR_ADDR;
    String text = yytext();
    return (new token8080(lastToken,IToken.PREPROCESSOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}

/* registers */
"a" {
    lastToken = token8080.REGISTERS_A;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"b" {
    lastToken = token8080.REGISTERS_B;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"c" {
    lastToken = token8080.REGISTERS_C;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"d" {
    lastToken = token8080.REGISTERS_D;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"e" {
    lastToken = token8080.REGISTERS_E;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"h" {
    lastToken = token8080.REGISTERS_H;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"l" {
    lastToken = token8080.REGISTERS_L;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"m" {
    lastToken = token8080.REGISTERS_M;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"psw" {
    lastToken = token8080.REGISTERS_PSW;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"sp" {
    lastToken = token8080.REGISTERS_SP;
    String text = yytext();
    return (new token8080(lastToken,IToken.REGISTER,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}

/* separators */
"(" {
    lastToken = token8080.SEPARATOR_LPAR;
    String text = yytext();
    return (new token8080(lastToken,IToken.SEPARATOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
")" {
    lastToken = token8080.SEPARATOR_RPAR;
    String text = yytext();
    return (new token8080(lastToken,IToken.SEPARATOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
"," {
    lastToken = token8080.SEPARATOR_COMMA;
    String text = yytext();
    return (new token8080(lastToken,IToken.SEPARATOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
{Eol} {
    lastToken = token8080.SEPARATOR_EOL;
    String text = yytext();
    return (new token8080(lastToken,IToken.SEPARATOR,text,null,yyline,
        yycolumn,yychar,yychar+text.length()));
}
{WhiteSpace}+ { /* ignore white spaces */ }

/* operators */
"+" {
    lastToken = token8080.OPERATOR_ADD;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"-" {
    lastToken = token8080.OPERATOR_SUBTRACT;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"*" {
    lastToken = token8080.OPERATOR_MULTIPLY;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"/" {
    lastToken = token8080.OPERATOR_DIVIDE;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"=" {
    lastToken = token8080.OPERATOR_EQUAL;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"mod" {
    lastToken = token8080.OPERATOR_MOD;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"shr" {
    lastToken = token8080.OPERATOR_SHR;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"shl" {
    lastToken = token8080.OPERATOR_SHL;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"not" {
    lastToken = token8080.OPERATOR_NOT;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"and" {
    lastToken = token8080.OPERATOR_AND;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"or" {
    lastToken = token8080.OPERATOR_OR;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
"xor" {
    lastToken = token8080.OPERATOR_XOR;
    String text = yytext();
    return (new token8080(lastToken,IToken.OPERATOR,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}

/* comment */
{Comment} {
    lastToken = token8080.TCOMMENT;
    String text = yytext();
    return (new token8080(lastToken,IToken.COMMENT,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
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
        yycolumn,yychar,yychar+yytext().length()));
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
        yycolumn,yychar,yychar+yytext().length()));
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
        yycolumn,yychar,yychar+yytext().length()));
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
        yycolumn,yychar,yychar+yytext().length()));
}
{UnclosedString} {
    lastToken = token8080.ERROR_UNCLOSED_STRING;
    String text = yytext();
    return (new token8080(lastToken,IToken.ERROR,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
{String} {
    String text = yytext();
    String val = text.substring(1,text.length()-1);
    if (val.length() > 2) {
        lastToken = token8080.LITERAL_STRING;
        return (new token8080(lastToken,IToken.LITERAL,text,val,yyline,yycolumn,
            yychar,yychar+text.length()));
    }
    else {
        byte[] b = val.getBytes();
        int numval = b[0];
        for (int i = 1; i < b.length; i++)
            numval = (numval <<8) + b[i];
        if (numval > 255) lastToken = token8080.LITERAL_DECIMAL_16BIT;
        else lastToken = token8080.LITERAL_DECIMAL_8BIT;
        return (new token8080(lastToken,IToken.LITERAL,text,numval,yyline,yycolumn,
            yychar,yychar+text.length()));
    }
}
{Identifier} {
    lastToken = token8080.TIDENTIFIER;
    String text = yytext();
    Object val = text.toUpperCase();
    return (new token8080(lastToken,IToken.IDENTIFIER,text,val,yyline,
        yycolumn,yychar,yychar+text.length()));
}
{Label} {
    lastToken = token8080.TLABEL;
    String text = yytext();
    Object val = text.substring(0,text.length()-1).toUpperCase();
    return (new token8080(lastToken,IToken.LABEL,text,val,yyline,yycolumn,
        yychar,yychar+text.length()));
}
. { lastToken = token8080.ERROR_UNKNOWN_TOKEN;
    String text = yytext();
    return (new token8080(lastToken,IToken.ERROR,text,null,yyline,yycolumn,
        yychar,yychar+text.length()));
}
