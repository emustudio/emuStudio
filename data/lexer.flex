/*
 * lexerZ80.flex
 *
 * Lexical analyser for Z80 assembler
 *
 * KEEP IT SIMPLE STUPID
 * sometimes just: YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2008-2010 Peter Jakubèo <pjakubco at gmail.com>
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
    lastToken = tokenZ80.EOF; lastText = yytext();
    return (new tokenZ80(lastToken,lastToken,lastText,null,yyline,yycolumn,yychar,
        yychar+lastText.length(),false));
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
        yychar,yychar+lastText.length(),true));
 }
"add" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_ADD; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"and" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_AND; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"bit" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_BIT; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"call" { yybegin(CONDITION);
    lastToken = tokenZ80.RESERVED_CALL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"ccf" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_CCF; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"cp" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_CP; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"cpd" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_CPD; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"cpdr" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_CPDR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"cpi" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_CPI; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"cpir" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_CPIR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"cpl" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_CPL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"daa" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_DAA; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"dec" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_DEC; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"di" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_DI; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"djnz" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_DJNZ; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"ei" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_EI; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"ex" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_EX; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"exx" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_EXX; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"halt" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_HALT; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"im" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_IM; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"in" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_IN; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"inc" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_INC; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"ind" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_IND; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"indr" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_INDR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"ini" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_INI; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"inir" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_INIR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"jp" { yybegin(CONDITION);
    lastToken = tokenZ80.RESERVED_JP; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"jr" { yybegin(CONDITION);
    lastToken = tokenZ80.RESERVED_JR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"ld" { yybegin(LD);
    lastToken = tokenZ80.RESERVED_LD; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"ldd" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_LDD; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"lddr" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_LDDR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"ldi" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_LDI; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"ldir" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_LDIR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"neg" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_NEG; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"nop" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_NOP; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"or" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_OR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"otdr" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_OTDR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"otir" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_OTIR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"out" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_OUT; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"outd" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_OUTD; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"outi" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_OUTI; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"pop" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_POP; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"push" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_PUSH; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"res" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RES; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"ret" { yybegin(CONDITION);
    lastToken = tokenZ80.RESERVED_RET; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"reti" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RETI; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"retn" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RETN; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"rl" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"rla"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RLA; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"rlc"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RLC; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"rlca"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RLCA; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"rld"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RLD; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"rr"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"rra"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RRA; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"rrc"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RRC; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"rrca"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RRCA; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"rrd"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RRD; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"rst"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_RST; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"sbc"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_SBC; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"scf" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_SCF; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"set" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_SET; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"sla"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_SLA; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"sra"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_SRA; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"sll"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_SLL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"srl"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_SRL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"sub"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_SUB; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"xor"  { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_XOR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
/* CALL,JP,JR,RET */
<CONDITION> "c" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_C; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),false));
}
<CONDITION> "nc" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_NC; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),false));
}
<CONDITION> "z" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_Z; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),false));
}
<CONDITION> "nz" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_NZ; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),false));
}
<CONDITION> "m" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_M; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),false));
}
<CONDITION> "p" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_P; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),false));
}
<CONDITION> "pe" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_PE; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),false));
}
<CONDITION> "po" { yybegin(YYINITIAL);
    lastToken = tokenZ80.RESERVED_PO; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.RESERVED,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),false));
}

/* preprocessor words */
"org" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_ORG; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"equ" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_EQU; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"var" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_VAR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"if" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_IF; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"endif" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_ENDIF; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"macro" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_MACRO; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"endm" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_ENDM; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"db" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_DB; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"dw" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_DW; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"ds" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_DS; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"$" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_ADDR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"include" { yybegin(YYINITIAL);
    lastToken = tokenZ80.PREPROCESSOR_INCLUDE; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.PREPROCESSOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}

/* registers */
<LD> "a" { yybegin(LD_A);
    lastToken = tokenZ80.REGISTERS_A; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),false));
}
"a" {
    lastToken = tokenZ80.REGISTERS_A; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"b" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_B; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"c" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_C; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"d" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_D; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"e" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_E; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"h" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_H; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"l" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_L; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
<LD> "ix" { yybegin(LD_II);
    lastToken = tokenZ80.REGISTERS_IX; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),false));
}
"ix" {
    lastToken = tokenZ80.REGISTERS_IX; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
<LD> "iy" { yybegin(LD_II);
    lastToken = tokenZ80.REGISTERS_IY; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),false));
}
"iy" {
    lastToken = tokenZ80.REGISTERS_IY; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
<LD> "sp" { yybegin(LD_RR);
    lastToken = tokenZ80.REGISTERS_SP; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),false));
}
"sp" {
    lastToken = tokenZ80.REGISTERS_SP; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
<LD> "bc" { yybegin(LD_RR);
    lastToken = tokenZ80.REGISTERS_BC; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),false));
}
"bc" {
    lastToken = tokenZ80.REGISTERS_BC; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
<LD> "de" { yybegin(LD_RR);
    lastToken = tokenZ80.REGISTERS_DE; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),false));
}
"de" {
    lastToken = tokenZ80.REGISTERS_DE; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
<LD> "hl" { yybegin(LD_RR);
    lastToken = tokenZ80.REGISTERS_HL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),false));
}
"hl" {
    lastToken = tokenZ80.REGISTERS_HL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"af" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_AF; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"af'" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_AFF; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"i" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_I; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
"r" { yybegin(YYINITIAL);
    lastToken = tokenZ80.REGISTERS_R; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.REGISTER,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}

/* separators */
<LD_X_COMMA> "(" { yybegin(YYINITIAL);
    lastToken = tokenZ80.SEPARATOR_INDEXLPAR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.SEPARATOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),false));
}
"(" {
    lastToken = tokenZ80.SEPARATOR_LPAR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.SEPARATOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
")" { yybegin(YYINITIAL);
    lastToken = tokenZ80.SEPARATOR_RPAR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.SEPARATOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
<LD_A,LD_RR,LD_II> "," { yybegin(LD_X_COMMA);
    lastToken = tokenZ80.SEPARATOR_COMMA; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.SEPARATOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),false));
}
<YYINITIAL> "," {
    lastToken = tokenZ80.SEPARATOR_COMMA; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.SEPARATOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
{Eol} { yybegin(YYINITIAL);
    lastToken = tokenZ80.SEPARATOR_EOL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.SEPARATOR,lastText,null,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
{WhiteSpace}+ { /* ignore white spaces */ }

/* operators */
"+" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_ADD; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"-" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_SUBTRACT; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"*" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_MULTIPLY; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"/" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_DIVIDE; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"=" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_EQUAL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
">" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_GREATER; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"<" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_LESS; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
">=" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_GE; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"<=" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_LE; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"%" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_MOD; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
">>" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_SHR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"<<" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_SHL; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"!" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_NOT; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"&" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_AND; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"|" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_OR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
"~" { yybegin(YYINITIAL);
    lastToken = tokenZ80.OPERATOR_XOR; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.OPERATOR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}

/* comment */
{Comment} { yybegin(YYINITIAL);
    lastToken = tokenZ80.TCOMMENT; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.COMMENT,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
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
        yycolumn,yychar,yychar+yytext().length(),false));
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
        yycolumn,yychar,yychar+yytext().length(),false));
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
        yycolumn,yychar,yychar+yytext().length(),false));
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
        yycolumn,yychar,yychar+yytext().length(),false));
}
{UnclosedString} { yybegin(YYINITIAL);
    lastToken = tokenZ80.ERROR_UNCLOSED_STRING; lastText = yytext();
    return (new tokenZ80(lastToken,IToken.ERROR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
{String} { lastText = yytext(); yybegin(YYINITIAL);
    String val = lastText.substring(1,lastText.length()-1);
    if (val.length() > 2) {
        lastToken = tokenZ80.LITERAL_STRING;
        return (new tokenZ80(lastToken,IToken.LITERAL,lastText,val,yyline,yycolumn,
            yychar,yychar+lastText.length(),true));
    }
    else {
        byte[] b = val.getBytes();
        int numval = b[0];
        for (int i = 1; i < b.length; i++)
            numval = (numval <<8) + b[i];
        if (numval > 255) lastToken = tokenZ80.LITERAL_DECIMAL_16BIT;
        else lastToken = tokenZ80.LITERAL_DECIMAL_8BIT;
        return (new tokenZ80(lastToken,IToken.LITERAL,lastText,numval,yyline,yycolumn,
            yychar,yychar+lastText.length(),true));
    }
}
{Identifier} { yybegin(YYINITIAL);
    lastToken = tokenZ80.TIDENTIFIER; lastText = yytext();
    Object val = lastText.toUpperCase();
    return (new tokenZ80(lastToken,IToken.IDENTIFIER,lastText,val,yyline,
        yycolumn,yychar,yychar+lastText.length(),true));
}
{Label} {
    lastToken = tokenZ80.TLABEL; lastText = yytext(); yybegin(YYINITIAL);
    Object val = lastText.substring(0,lastText.length()-1).toUpperCase();
    return (new tokenZ80(lastToken,IToken.LABEL,lastText,val,yyline,yycolumn,
        yychar,yychar+lastText.length(),true));
}
. { lastToken = tokenZ80.ERROR_UNKNOWN_TOKEN; lastText = yytext(); yybegin(YYINITIAL);
    return (new tokenZ80(lastToken,IToken.ERROR,lastText,null,yyline,yycolumn,
        yychar,yychar+lastText.length(),false));
}
