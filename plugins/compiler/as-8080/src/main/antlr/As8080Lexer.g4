/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
lexer grammar As8080Lexer;

options {
    caseInsensitive = true;
}

COMMENT: ('//' | '--' | ';' | '#' ) ~[\r\n]*;
COMMENT2: '/*' .*? '*/';

// reserved
OPCODE_STC: 'stc';
OPCODE_CMC: 'cmc';
OPCODE_INR: 'inr';
OPCODE_DCR: 'dcr';
OPCODE_CMA: 'cma';
OPCODE_DAA: 'daa';
OPCODE_NOP: 'nop';
OPCODE_MOV: 'mov';
OPCODE_STAX: 'stax';
OPCODE_LDAX: 'ldax';
OPCODE_ADD: 'add';
OPCODE_ADC: 'adc';
OPCODE_SUB: 'sub';
OPCODE_SBB: 'sbb';
OPCODE_ANA: 'ana';
OPCODE_XRA: 'xra';
OPCODE_ORA: 'ora';
OPCODE_CMP: 'cmp';
OPCODE_RLC: 'rlc';
OPCODE_RRC: 'rrc';
OPCODE_RAL: 'ral';
OPCODE_RAR: 'rar';
OPCODE_PUSH: 'push';
OPCODE_POP: 'pop';
OPCODE_DAD: 'dad';
OPCODE_INX: 'inx';
OPCODE_DCX: 'dcx';
OPCODE_XCHG: 'xchg';
OPCODE_XTHL: 'xthl';
OPCODE_SPHL: 'sphl';
OPCODE_LXI: 'lxi';
OPCODE_MVI: 'mvi';
OPCODE_ADI: 'adi';
OPCODE_ACI: 'aci';
OPCODE_SUI: 'sui';
OPCODE_SBI: 'sbi';
OPCODE_ANI: 'ani';
OPCODE_XRI: 'xri';
OPCODE_ORI: 'ori';
OPCODE_CPI: 'cpi';
OPCODE_STA: 'sta';
OPCODE_LDA: 'lda';
OPCODE_SHLD: 'shld';
OPCODE_LHLD: 'lhld';
OPCODE_PCHL: 'pchl';
OPCODE_JMP: 'jmp';
OPCODE_JC: 'jc';
OPCODE_JNC: 'jnc';
OPCODE_JZ: 'jz';
OPCODE_JNZ: 'jnz';
OPCODE_JP: 'jp';
OPCODE_JM: 'jm';
OPCODE_JPE: 'jpe';
OPCODE_JPO: 'jpo';
OPCODE_CALL: 'call';
OPCODE_CC: 'cc';
OPCODE_CNC: 'cnc';
OPCODE_CZ: 'cz';
OPCODE_CNZ: 'cnz';
OPCODE_CP: 'cp';
OPCODE_CM: 'cm';
OPCODE_CPE: 'cpe';
OPCODE_CPO: 'cpo';
OPCODE_RET: 'ret';
OPCODE_RC: 'rc';
OPCODE_RNC: 'rnc';
OPCODE_RZ: 'rz';
OPCODE_RNZ: 'rnz';
OPCODE_RM: 'rm';
OPCODE_RP: 'rp';
OPCODE_RPE: 'rpe';
OPCODE_RPO: 'rpo';
OPCODE_RST: 'rst';
OPCODE_EI: 'ei';
OPCODE_DI: 'di';
OPCODE_IN: 'in';
OPCODE_OUT: 'out';
OPCODE_HLT: 'hlt';

// preprocessor
PREP_ORG: 'org';
PREP_EQU: 'equ';
PREP_SET: 'set' | 'var';
PREP_INCLUDE: 'include';
PREP_IF: 'if';
PREP_ENDIF: 'endif';
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
REG_M: 'm';
REG_PSW: 'psw';
REG_SP: 'sp';

// operators
OP_MOD: 'mod';
OP_SHR: 'shr';
OP_SHL: 'shl';
OP_NOT: 'not';
OP_AND: 'and';
OP_OR: 'or';
OP_XOR: 'xor';

// literals
LIT_HEXNUMBER_1: '0x' [0-9a-f]+;
LIT_NUMBER: [0-9]+ 'd'?;
LIT_HEXNUMBER_2: [0-9a-f]+ 'h';
LIT_OCTNUMBER: [0-7]+ [oq];
LIT_BINNUMBER: [01]+ 'b';
LIT_STRING_1: '\'' ~[']* '\'';
LIT_STRING_2: '"' ~["]* '"';

// other
ID_IDENTIFIER: [a-z_?@.] [a-z_?@.0-9]*;
ID_LABEL: ID_IDENTIFIER ':';

ERROR : ~([+:.* \t\f\r\n(),=/-]|'~'|'>'|'<'|'&'|'|'|'%'|'^')+ | ':' | '.';

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
