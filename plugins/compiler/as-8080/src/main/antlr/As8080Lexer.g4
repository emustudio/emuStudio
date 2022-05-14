/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
lexer grammar As8080Lexer;

COMMENT: ('//' | '--' | ';' | '#' ) ~[\r\n]*;
COMMENT2: '/*' .*? '*/';

fragment A: [aA];
fragment B: [bB];
fragment C: [cC];
fragment D: [dD];
fragment E: [eE];
fragment F: [fF];
fragment G: [gG];
fragment H: [hH];
fragment I: [iI];
fragment J: [jJ];
fragment L: [lL];
fragment M: [mM];
fragment N: [nN];
fragment O: [oO];
fragment P: [pP];
fragment Q: [qQ];
fragment R: [rR];
fragment S: [sS];
fragment T: [tT];
fragment U: [uU];
fragment V: [vV];
fragment W: [wW];
fragment X: [xX];
fragment Z: [zZ];


// reserved
OPCODE_STC: S T C;
OPCODE_CMC: C M C;
OPCODE_INR: I N R;
OPCODE_DCR: D C R;
OPCODE_CMA: C M A;
OPCODE_DAA: D A A;
OPCODE_NOP: N O P;
OPCODE_MOV: M O V;
OPCODE_STAX: S T A X;
OPCODE_LDAX: L D A X;
OPCODE_ADD: A D D;
OPCODE_ADC: A D C;
OPCODE_SUB: S U B;
OPCODE_SBB: S B B;
OPCODE_ANA: A N A;
OPCODE_XRA: X R A;
OPCODE_ORA: O R A;
OPCODE_CMP: C M P;
OPCODE_RLC: R L C;
OPCODE_RRC: R R C;
OPCODE_RAL: R A L;
OPCODE_RAR: R A R;
OPCODE_PUSH: P U S H;
OPCODE_POP: P O P;
OPCODE_DAD: D A D;
OPCODE_INX: I N X;
OPCODE_DCX: D C X;
OPCODE_XCHG: X C H G;
OPCODE_XTHL: X T H L;
OPCODE_SPHL: S P H L;
OPCODE_LXI: L X I;
OPCODE_MVI: M V I;
OPCODE_ADI: A D I;
OPCODE_ACI: A C I;
OPCODE_SUI: S U I;
OPCODE_SBI: S B I;
OPCODE_ANI: A N I;
OPCODE_XRI: X R I;
OPCODE_ORI: O R I;
OPCODE_CPI: C P I;
OPCODE_STA: S T A;
OPCODE_LDA: L D A;
OPCODE_SHLD: S H L D;
OPCODE_LHLD: L H L D;
OPCODE_PCHL: P C H L;
OPCODE_JMP: J M P;
OPCODE_JC: J C;
OPCODE_JNC: J N C;
OPCODE_JZ: J Z;
OPCODE_JNZ: J N Z;
OPCODE_JP: J P;
OPCODE_JM: J M;
OPCODE_JPE: J P E;
OPCODE_JPO: J P O;
OPCODE_CALL: C A L L;
OPCODE_CC: C C;
OPCODE_CNC: C N C;
OPCODE_CZ: C Z;
OPCODE_CNZ: C N Z;
OPCODE_CP: C P;
OPCODE_CM: C M;
OPCODE_CPE: C P E;
OPCODE_CPO: C P O;
OPCODE_RET: R E T;
OPCODE_RC: R C;
OPCODE_RNC: R N C;
OPCODE_RZ: R Z;
OPCODE_RNZ: R N Z;
OPCODE_RM: R M;
OPCODE_RP: R P;
OPCODE_RPE: R P E;
OPCODE_RPO: R P O;
OPCODE_RST: R S T;
OPCODE_EI: E I;
OPCODE_DI: D I;
OPCODE_IN: I N;
OPCODE_OUT: O U T;
OPCODE_HLT: H L T;

// preprocessor
PREP_ORG: O R G;
PREP_EQU: E Q U;
PREP_SET: ((S E T) | (V A R));
PREP_INCLUDE: I N C L U D E;
PREP_IF: I F;
PREP_ENDIF: E N D I F;
PREP_MACRO: M A C R O;
PREP_ENDM: E N D M;
PREP_DB: D B;
PREP_DW: D W;
PREP_DS: D S;
PREP_ADDR: '$';
PREP_END: E N D;

// registers
REG_A: A;
REG_B: B;
REG_C: C;
REG_D: D;
REG_E: E;
REG_H: H;
REG_L: L;
REG_M: M;
REG_PSW: P S W;
REG_SP: S P;

// operators
OP_MOD: M O D;
OP_SHR: S H R;
OP_SHL: S H L;
OP_NOT: N O T;
OP_AND: A N D;
OP_OR: O R;
OP_XOR: X O R;

// literals
LIT_HEXNUMBER_1: '0' X [0-9a-fA-F]+;
LIT_NUMBER: [0-9]+ D?;
LIT_HEXNUMBER_2: [0-9a-fA-F]+ H;
LIT_OCTNUMBER: [0-7]+ [oOqQ];
LIT_BINNUMBER: [01]+ B;
LIT_STRING_1: '\'' ~[']* '\'';
LIT_STRING_2: '"' ~["]* '"';

// other

ID_IDENTIFIER: [a-zA-Z_?@] [a-zA-Z_?@0-9]*;
ID_LABEL: ID_IDENTIFIER ':';

ERROR : ~([+* \t\f\r\n(),=/-]|'~'|'>'|'<'|'&'|'|'|'%'|'^')+; // below: everything which does not require space

//\+\*
// separators - not requiring space inbetween
SEP_LPAR: '(';
SEP_RPAR: ')';
SEP_COMMA: ',';

// operators not requiring space inbetween
OP_ADD: '+';
OP_SUBTRACT: '-';
OP_MULTIPLY: '*';
OP_DIVIDE: '/';
OP_EQUAL: '=';
OP_GT: '>';
OP_GTE: '>=';
OP_LT: '<';
OP_LTE: '<=';
OP_MOD_2: '%';
OP_SHR_2: '>>';
OP_SHL_2: '<<';
OP_NOT_2: '~';
OP_AND_2: '&';
OP_OR_2: '|';
OP_XOR_2: '^';

WS : [ \t\f]+ -> channel(HIDDEN);
EOL: '\r'? '\n';
