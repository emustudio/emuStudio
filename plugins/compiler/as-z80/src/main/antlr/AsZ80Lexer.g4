/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
lexer grammar AsZ80Lexer;

options {
    caseInsensitive = true;
}

COMMENT: ('//' | '--' | ';' | '#' ) ~[\r\n]*;
COMMENT2: '/*' .*? '*/';

// reserved
OPCODE_ADC: 'adc';
OPCODE_ADD: 'add';
OPCODE_AND: 'and';
OPCODE_BIT: 'bit';
OPCODE_CALL: 'call' -> pushMode(CONDITION);
OPCODE_CCF: 'ccf';
OPCODE_CP: 'cp';
OPCODE_CPD: 'cpd';
OPCODE_CPDR: 'cpdr';
OPCODE_CPI: 'cpi';
OPCODE_CPIR: 'cpir';
OPCODE_CPL: 'cpl';
OPCODE_DAA: 'daa';
OPCODE_DEC: 'dec';
OPCODE_DI: 'di';
OPCODE_DJNZ: 'djnz';
OPCODE_EI: 'ei';
OPCODE_EX: 'ex';
OPCODE_EXX: 'exx';
OPCODE_HALT: 'halt';
OPCODE_IM: 'im' -> pushMode(IM_NUMBER);
OPCODE_IN: 'in';
OPCODE_INC: 'inc';
OPCODE_IND: 'ind';
OPCODE_INDR: 'indr';
OPCODE_INI: 'ini';
OPCODE_INIR: 'inir';
OPCODE_JP: 'jp' -> pushMode(CONDITION);
OPCODE_JR: 'jr' -> pushMode(CONDITION);
OPCODE_LD: 'ld';
OPCODE_LDD: 'ldd';
OPCODE_LDDR: 'lddr';
OPCODE_LDI: 'ldi';
OPCODE_LDIR: 'ldir';
OPCODE_NEG: 'neg';
OPCODE_NOP: 'nop';
OPCODE_OR: 'or';
OPCODE_OTDR: 'otdr';
OPCODE_OTIR: 'otir';
OPCODE_OUT: 'out';
OPCODE_OUTD: 'outd';
OPCODE_OUTI: 'outi';
OPCODE_POP: 'pop';
OPCODE_PUSH: 'push';
OPCODE_RES: 'res';
OPCODE_RET: 'ret' -> pushMode(CONDITION);
OPCODE_RETI: 'reti';
OPCODE_RETN: 'retn';
OPCODE_RL: 'rl';
OPCODE_RLA: 'rla';
OPCODE_RLC: 'rlc';
OPCODE_RLCA: 'rlca';
OPCODE_RLD: 'rld';
OPCODE_RR: 'rr';
OPCODE_RRA: 'rra';
OPCODE_RRC: 'rrc';
OPCODE_RRCA: 'rrca';
OPCODE_RRD: 'rrd';
OPCODE_RST: 'rst';
OPCODE_SBC: 'sbc';
OPCODE_SCF: 'scf';
OPCODE_SET: 'set';
OPCODE_SLA: 'sla';
OPCODE_SRA: 'sra';
OPCODE_SLL: 'sll';
OPCODE_SRL: 'srl';
OPCODE_SUB: 'sub';
OPCODE_XOR: 'xor';

mode CONDITION;
COND_C:  'c'    ({_input.LA(1) == -1 || (!Character.isLetterOrDigit((char)_input.LA(1)) && "_?@".indexOf((char)_input.LA(1)) == -1)}?) -> popMode;
COND_NC: 'nc'   ({_input.LA(1) == -1 || (!Character.isLetterOrDigit((char)_input.LA(1)) && "_?@".indexOf((char)_input.LA(1)) == -1)}?) -> popMode;
COND_Z:  'z'    ({_input.LA(1) == -1 || (!Character.isLetterOrDigit((char)_input.LA(1)) && "_?@".indexOf((char)_input.LA(1)) == -1)}?) -> popMode;
COND_NZ: 'nz'   ({_input.LA(1) == -1 || (!Character.isLetterOrDigit((char)_input.LA(1)) && "_?@".indexOf((char)_input.LA(1)) == -1)}?) -> popMode;
COND_M:  'm'    ({_input.LA(1) == -1 || (!Character.isLetterOrDigit((char)_input.LA(1)) && "_?@".indexOf((char)_input.LA(1)) == -1)}?) -> popMode;
COND_PE: 'pe'   ({_input.LA(1) == -1 || (!Character.isLetterOrDigit((char)_input.LA(1)) && "_?@".indexOf((char)_input.LA(1)) == -1)}?) -> popMode;
COND_PO: 'po'   ({_input.LA(1) == -1 || (!Character.isLetterOrDigit((char)_input.LA(1)) && "_?@".indexOf((char)_input.LA(1)) == -1)}?) -> popMode;
COND_P:  'p'    ({_input.LA(1) == -1 || (!Character.isLetterOrDigit((char)_input.LA(1)) && "_?@".indexOf((char)_input.LA(1)) == -1)}?) -> popMode;
COND_WS: [ \t\f]+ -> channel(HIDDEN);
ERROR_COND: () -> popMode,skip;

mode IM_NUMBER;
IM_01: '0/1' -> popMode;
IM_0: '0' -> popMode;
IM_1: '1' -> popMode;
IM_2: '2' -> popMode;
IM_WS: [ \t\f]+ -> channel(HIDDEN);
ERROR_IM: ({"012".indexOf((char) _input.LA(1)) == -1}?) -> popMode,skip;

mode DEFAULT_MODE;

// preprocessor
PREP_ORG: 'org';
PREP_EQU: 'equ';
PREP_VAR: 'var';
PREP_IF: 'if';
PREP_ENDIF: 'endif';
PREP_INCLUDE: 'include';
PREP_MACRO: 'macro';
PREP_ENDM: 'endm';
PREP_DB: 'db';
PREP_DW: 'dw';
PREP_DS: 'ds';
PREP_ADDR: '$';
PREP_END: 'end';

// registers
REG_A: 'a';
REG_B: 'b';
REG_C: 'c';
REG_D: 'd';
REG_E: 'e';
REG_H: 'h';
REG_L: 'l';
REG_IX: 'ix';
REG_IXH: 'ixh';
REG_IXL: 'ixl';
REG_IY: 'iy';
REG_IYH: 'iyh';
REG_IYL: 'iyl';
REG_BC: 'bc';
REG_DE: 'de';
REG_HL: 'hl';
REG_SP: 'sp';
REG_AF: 'af';
REG_AFF: 'af' '\'';
REG_I: 'i';
REG_R: 'r';

// operators
OP_MOD: 'mod';
OP_SHR: 'shr';
OP_SHL: 'shl';
OP_NOT: 'not';

// literals
LIT_HEXNUMBER_1: '0x' [0-9a-f]+;
LIT_NUMBER: [0-9]+ 'd'?;
LIT_HEXNUMBER_2: [0-9a-f]+ 'h';
LIT_OCTNUMBER: [0-7]+ [oq];
LIT_BINNUMBER: [01]+ 'b';
LIT_STRING_1: '\'' ~[']* '\'';
LIT_STRING_2: '"' ~["]* '"';

// other
ID_IDENTIFIER: [a-z_?@] [a-z_?@0-9]*;
ID_LABEL: ID_IDENTIFIER ':';

ERROR : ~([+* \t\f\r\n(),=/-]|'~'|'>'|'<'|'&'|'|'|'%'|'^')+;

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
OP_AND: '&';
OP_OR: '|';
OP_XOR: '^';

WS : [ \t\f]+ -> channel(HIDDEN);
EOL: '\r'? '\n';
