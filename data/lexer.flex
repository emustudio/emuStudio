/*
 * lexerZ80.flex
 *
 * (c) Copyright 2008-2009, vbmacher
 *
 * Lexical analyser for Z80 assembler
 *
 * KEEP IT SIMPLE STUPID
 * sometimes just: YOU AREN'T GONNA NEED IT
 *
 */

package impl;

import plugins.compiler.*;
import java.io.*;

%%

/* options */
%class lexerZ80
%cup
%public
%implements ILexer
%line
%column
%char
%caseless
%unicode
%type tokenZ80
%states CONDITION,LD,LD_A,LD_RR,LD_II,LD_X_COMMA

%{
    private int lastToken;
    private String lastText; // token string holder

    public tokenZ80 getSymbol() throws IOException {
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
    lastToken = tokenZ80.EOF; lastText = yytext();
    return (new tokenZ80(lastToken,lastToken,lastText,null,yyline,yycolumn,yychar,
        yychar+lastText.length()));
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
    lastToken = tokenZ80.RESERVED_ADC; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
 }
"add" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_ADD; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"and" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_AND; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"bit" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_BIT; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"call" { yybegin(CONDITION);
    lastToken = tokenZ80.RESERVED_CALL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"ccf" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_CCF; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"cp" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_CP; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"cpd" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_CPD; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"cpdr" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_CPDR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"cpi" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_CPI; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"cpir" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_CPIR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"cpl" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_CPL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"daa" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_DAA; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"dec" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_DEC; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"di" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_DI; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"djnz" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_DJNZ; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"ei" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_EI; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"ex" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_EX; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"exx" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_EXX; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"halt" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_HALT; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"im" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_IM; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"in" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_IN; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"inc" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_INC; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"ind" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_IND; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"indr" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_INDR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"ini" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_INI; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"inir" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_INIR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"jp" { yybegin(CONDITION);
    lastToken = tokenZ80.RESERVED_JP; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"jr" { yybegin(CONDITION);
    lastToken = tokenZ80.RESERVED_JR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"ld" { yybegin(LD);
    lastToken = tokenZ80.RESERVED_LD; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"ldd" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_LDD; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"lddr" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_LDDR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"ldi" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_LDI; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"ldir" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_LDIR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"neg" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_NEG; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"nop" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_NOP; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"or" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_OR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"otdr" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_OTDR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"otir" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_OTIR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"out" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_OUT; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"outd" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_OUTD; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"outi" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_OUTI; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"pop" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_POP; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"push" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_PUSH; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"res" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RES; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"ret" { yybegin(CONDITION);
    lastToken = tokenZ80.RESERVED_RET; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"reti" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RETI; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"retn" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RETN; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"rl" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"rla"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RLA; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"rlc"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RLC; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"rlca"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RLCA; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"rld"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RLD; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"rr"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"rra"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RRA; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"rrc"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RRC; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"rrca"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RRCA; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"rrd"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RRD; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"rst"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RST; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"sbc"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_SBC; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"scf" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_SCF; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"set" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_SET; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"sla"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_SLA; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"sra"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_SRA; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"sll"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_SLL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"srl"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_SRL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"sub"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_SUB; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"xor"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_XOR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
/* CALL,JP,JR,RET */
<CONDITION> "c" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_C; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
<CONDITION> "nc" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_NC; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
<CONDITION> "z" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_Z; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
<CONDITION> "nz" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_NZ; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
<CONDITION> "m" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_M; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
<CONDITION> "p" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_P; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
<CONDITION> "pe" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_PE; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
<CONDITION> "po" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_PO; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}

/* preprocessor words */
"org" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_ORG; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"equ" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_EQU; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"var" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_VAR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"if" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_IF; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"endif" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_ENDIF; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"macro" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_MACRO; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"endm" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_ENDM; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"db" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_DB; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"dw" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_DW; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"ds" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_DS; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"$" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_ADDR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"include" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_INCLUDE; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}

/* registers */
<LD> "a" { yybegin(LD_A);
    lastToken = tokenZ80.REGISTERS_A; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"a" {
    lastToken = tokenZ80.REGISTERS_A; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"b" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_B; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"c" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_C; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"d" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_D; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"e" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_E; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"h" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_H; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"l" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_L; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
<LD> "ix" { yybegin(LD_II);
    lastToken = tokenZ80.REGISTERS_IX; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"ix" {
    lastToken = tokenZ80.REGISTERS_IX; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
<LD> "iy" { yybegin(LD_II);
    lastToken = tokenZ80.REGISTERS_IY; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"iy" {
    lastToken = tokenZ80.REGISTERS_IY; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
<LD> "sp" { yybegin(LD_RR);
    lastToken = tokenZ80.REGISTERS_SP; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"sp" {
    lastToken = tokenZ80.REGISTERS_SP; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
<LD> "bc" { yybegin(LD_RR);
    lastToken = tokenZ80.REGISTERS_BC; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"bc" {
    lastToken = tokenZ80.REGISTERS_BC; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
<LD> "de" { yybegin(LD_RR);
    lastToken = tokenZ80.REGISTERS_DE; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"de" {
    lastToken = tokenZ80.REGISTERS_DE; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
<LD> "hl" { yybegin(LD_RR);
    lastToken = tokenZ80.REGISTERS_HL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"hl" {
    lastToken = tokenZ80.REGISTERS_HL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"af" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_AF; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"af'" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_AFF; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"i" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_I; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"r" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_R; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}

/* separators */
<LD_X_COMMA> "(" { yybegin(YYINITIAL);
    lastToken = tokenZ80.SEPARATOR_INDEXLPAR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.SEPARATOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
"(" {
    lastToken = tokenZ80.SEPARATOR_LPAR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.SEPARATOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
")" { yybegin(YYINITIAL);
    lastToken = tokenZ80.SEPARATOR_RPAR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.SEPARATOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
<LD_A,LD_RR,LD_II> "," { yybegin(LD_X_COMMA);
    lastToken = tokenZ80.SEPARATOR_COMMA; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.SEPARATOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
<YYINITIAL> "," {
    lastToken = tokenZ80.SEPARATOR_COMMA; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.SEPARATOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
{Eol} { yybegin(YYINITIAL);
    lastToken = tokenZ80.SEPARATOR_EOL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.SEPARATOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
{WhiteSpace}+ { /* ignore white spaces */ }

/* operators */
"+" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_ADD; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"-" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_SUBTRACT; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"*" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_MULTIPLY; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"/" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_DIVIDE; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"=" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_EQUAL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
">" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_GREATER; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"<" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_LESS; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
">=" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_GE; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"<=" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_LE; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"%" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_MOD; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
">>" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_SHR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"<<" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_SHL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"!" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_NOT; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"&" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_AND; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"|" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_OR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
"~" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_XOR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}

/* comment */
{Comment} { yybegin(YYINITIAL);
    lastToken = tokenZ80.TCOMMENT; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.COMMENT,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}

/* literals */
{DecimalNum} { lastText = yytext(); yybegin(YYINITIAL);
    lastText = lastText.replaceFirst("[dD]","");
    int num=0;
    int tokenType = 0;
    try {
        num = Integer.parseInt(lastText,10);
        if (num > 65535) { // || num < -32768) {
            lastToken = tokenZ80.ERROR_DECIMAL_SIZE;
            tokenType = IToken.ERROR;
        } else if (num > 255) { // || num < -128) {
            lastToken = tokenZ80.LITERAL_DECIMAL_16BIT;
            tokenType = IToken.LITERAL;
        } else {
            lastToken = tokenZ80.LITERAL_DECIMAL_8BIT;
            tokenType = IToken.LITERAL;
        }
    } catch (NumberFormatException e) {
        lastToken = tokenZ80.ERROR_DECIMAL_SIZE;
        tokenType = IToken.LITERAL;
    }
    return (new tokenZ80(lastToken,tokenType,yytext(),(Object)num,yyline,
        yycolumn,yychar,yychar+yytext().length()));
}
{OctalNum} { lastText = yytext(); yybegin(YYINITIAL);
    int num=0;
    int tokenType=0;
    lastText = lastText.replaceFirst("[oOqQ]","");
    try {

        num = Integer.parseInt(lastText,8);
        if (num > 65535) { // || num < -32768) {
            lastToken = tokenZ80.ERROR_DECIMAL_SIZE;
            tokenType = IToken.ERROR;
        } else if (num > 255) { // || num < -128) {
            lastToken = tokenZ80.LITERAL_DECIMAL_16BIT;
            tokenType = IToken.LITERAL;
        } else {
            lastToken = tokenZ80.LITERAL_DECIMAL_8BIT;
            tokenType = IToken.LITERAL;
        }
    } catch (NumberFormatException e) {
        lastToken = tokenZ80.ERROR_DECIMAL_SIZE;
        tokenType = IToken.ERROR;
    }
    return (new tokenZ80(lastToken,tokenType,yytext(),(Object)num,yyline,
        yycolumn,yychar,yychar+yytext().length()));
}
{HexaNum} { lastText = yytext(); yybegin(YYINITIAL);
    int num=0;
    int tokenType=0;
    lastText = lastText.replaceFirst("[hH]","");
    try {
        num = Integer.parseInt(lastText,16);
        if (num > 65535) {
            lastToken = tokenZ80.ERROR_DECIMAL_SIZE;
            tokenType = IToken.ERROR;
        } else if (num > 255) {
            lastToken = tokenZ80.LITERAL_DECIMAL_16BIT;
            tokenType = IToken.LITERAL;
        } else {
            lastToken = tokenZ80.LITERAL_DECIMAL_8BIT;
            tokenType = IToken.LITERAL;
        }
    } catch (NumberFormatException e) {
        lastToken = tokenZ80.ERROR_DECIMAL_SIZE;
        tokenType = IToken.ERROR;
    }
    return (new tokenZ80(lastToken,tokenType,yytext(),(Object)num,yyline,
        yycolumn,yychar,yychar+yytext().length()));
}
{BinaryNum} { lastText = yytext(); yybegin(YYINITIAL);
    int num=0;
    int tokenType=0;
    lastText = lastText.replaceFirst("[bB]","");
    try {
        num = Integer.parseInt(lastText,2);
        if (num > 65535) {
            lastToken = tokenZ80.ERROR_DECIMAL_SIZE;
            tokenType = IToken.ERROR;
        } else if (num > 255) {
            lastToken = tokenZ80.LITERAL_DECIMAL_16BIT;
            tokenType = IToken.LITERAL;
        } else {
            lastToken = tokenZ80.LITERAL_DECIMAL_8BIT;
            tokenType = IToken.LITERAL;
        }
    } catch (NumberFormatException e) {
        lastToken = tokenZ80.ERROR_DECIMAL_SIZE;
        tokenType = IToken.ERROR;
    }
    return (new tokenZ80(lastToken,tokenType,yytext(),(Object)num,yyline,
        yycolumn,yychar,yychar+yytext().length()));
}
{UnclosedString} { yybegin(YYINITIAL);
    lastToken = tokenZ80.ERROR_UNCLOSED_STRING; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.ERROR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
{String} { lastText = yytext(); yybegin(YYINITIAL);
    String val = lastText.substring(1,lastText.length()-1);
    if (val.length() > 2) {
        lastToken = tokenZ80.LITERAL_STRING;
        return (new tokenZ80(lastToken,IToken.LITERAL,lastText,val,yyline,yycolumn,
            yychar,yychar+lastText.length()));
    }
    else {
        byte[] b = val.getBytes();
        int numval = b[0];
        for (int i = 1; i < b.length; i++)
            numval = (numval <<8) + b[i];
        if (numval > 255) lastToken = tokenZ80.LITERAL_DECIMAL_16BIT;
        else lastToken = tokenZ80.LITERAL_DECIMAL_8BIT;
        return (new tokenZ80(lastToken,IToken.LITERAL,lastText,numval,yyline,yycolumn,
            yychar,yychar+lastText.length()));
    }
}
{Identifier} { yybegin(YYINITIAL);
    lastToken = tokenZ80.TIDENTIFIER; lastText = yytext();
    Object val = lastText.toUpperCase();
    return (new tokenZ80(lastToken,IToken.IDENTIFIER,lastText,val,yyline,
        yycolumn,yychar,yychar+lastText.length()));
}
{Label} {
    lastToken = tokenZ80.TLABEL; lastText = yytext(); yybegin(YYINITIAL);
    Object val = lastText.substring(0,lastText.length()-1).toUpperCase();
    return (new tokenZ80(lastToken,IToken.LABEL,lastText,val,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
. { lastToken = tokenZ80.ERROR_UNKNOWN_TOKEN; lastText = yytext(); yybegin(YYINITIAL);
    return (new tokenZ80(lastToken,IToken.ERROR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length()));
}
