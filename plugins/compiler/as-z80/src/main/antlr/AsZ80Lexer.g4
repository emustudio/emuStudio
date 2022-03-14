lexer grammar AsZ80Lexer;

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
fragment Y: [yY];
fragment Z: [zZ];


// reserved
OPCODE_ADC: A D C;
OPCODE_ADD: A D D;
OPCODE_AND: A N D;
OPCODE_BIT: B I T;
OPCODE_CALL: C A L L -> pushMode(CONDITION);
OPCODE_CCF: C C F;
OPCODE_CP: C P;
OPCODE_CPD: C P D;
OPCODE_CPDR: C P D R;
OPCODE_CPI: C P I;
OPCODE_CPIR: C P I R;
OPCODE_CPL: C P L;
OPCODE_DAA: D A A;
OPCODE_DEC: D E C;
OPCODE_DI: D I;
OPCODE_DJNZ: D J N Z;
OPCODE_EI: E I;
OPCODE_EX: E X;
OPCODE_EXX: E X X;
OPCODE_HALT: H A L T;
OPCODE_IM: I M -> pushMode(IM_NUMBER);
OPCODE_IN: I N;
OPCODE_INC: I N C;
OPCODE_IND: I N D;
OPCODE_INDR: I N D R;
OPCODE_INI: I N I;
OPCODE_INIR: I N I R;
OPCODE_JP: J P -> pushMode(CONDITION);
OPCODE_JR: J R -> pushMode(CONDITION);
OPCODE_LD: L D;
OPCODE_LDD: L D D;
OPCODE_LDDR: L D D R;
OPCODE_LDI: L D I;
OPCODE_LDIR: L D I R;
OPCODE_NEG: N E G;
OPCODE_NOP: N O P;
OPCODE_OR: O R;
OPCODE_OTDR: O T D R;
OPCODE_OTIR: O T I R;
OPCODE_OUT: O U T;
OPCODE_OUTD: O U T D;
OPCODE_OUTI: O U T I;
OPCODE_POP: P O P;
OPCODE_PUSH: P U S H;
OPCODE_RES: R E S;
OPCODE_RET: R E T -> pushMode(CONDITION);
OPCODE_RETI: R E T I;
OPCODE_RETN: R E T N;
OPCODE_RL: R L;
OPCODE_RLA: R L A;
OPCODE_RLC: R L C;
OPCODE_RLCA: R L C A;
OPCODE_RLD: R L D;
OPCODE_RR: R R;
OPCODE_RRA: R R A;
OPCODE_RRC: R R C;
OPCODE_RRCA: R R C A;
OPCODE_RRD: R R D;
OPCODE_RST: R S T;
OPCODE_SBC: S B C;
OPCODE_SCF: S C F;
OPCODE_SET: S E T;
OPCODE_SLA: S L A;
OPCODE_SRA: S R A;
OPCODE_SLL: S L L;
OPCODE_SRL: S R L;
OPCODE_SUB: S U B;
OPCODE_XOR: X O R;

mode CONDITION;
COND_C:  C   ({(_input.LA(1) == -1) || (" ,\t\f\n\r;#/'\"()[]{}!=-+*<>\\%^&|$.".indexOf((char)_input.LA(1)) != -1) }?) -> popMode;
COND_NC: N C ({(_input.LA(1) == -1) || (" ,\t\f\n\r;#/'\"()[]{}!=-+*<>\\%^&|$.".indexOf((char)_input.LA(1)) != -1) }?) -> popMode;
COND_Z:  Z   ({(_input.LA(1) == -1) || (" ,\t\f\n\r;#/'\"()[]{}!=-+*<>\\%^&|$.".indexOf((char)_input.LA(1)) != -1) }?) -> popMode;
COND_NZ: N Z ({(_input.LA(1) == -1) || (" ,\t\f\n\r;#/'\"()[]{}!=-+*<>\\%^&|$.".indexOf((char)_input.LA(1)) != -1) }?) -> popMode;
COND_M:  M   ({(_input.LA(1) == -1) || (" ,\t\f\n\r;#/'\"()[]{}!=-+*<>\\%^&|$.".indexOf((char)_input.LA(1)) != -1) }?) -> popMode;
COND_PE: P E ({(_input.LA(1) == -1) || (" ,\t\f\n\r;#/'\"()[]{}!=-+*<>\\%^&|$.".indexOf((char)_input.LA(1)) != -1) }?) -> popMode;
COND_PO: P O ({(_input.LA(1) == -1) || (" ,\t\f\n\r;#/'\"()[]{}!=-+*<>\\%^&|$.".indexOf((char)_input.LA(1)) != -1) }?) -> popMode;
COND_P:  P   ({(_input.LA(1) == -1) || (" ,\t\f\n\r;#/'\"()[]{}!=-+*<>\\%^&|$.".indexOf((char)_input.LA(1)) != -1) }?) -> popMode;
COND_WS: [ \t\f]+ -> channel(HIDDEN);
ERROR_COND: () -> popMode,channel(HIDDEN);

mode IM_NUMBER;
IM_01: '0/1' -> popMode;
IM_0: '0' -> popMode;
IM_1: '1' -> popMode;
IM_2: '2' -> popMode;
IM_WS: [ \t\f]+ -> channel(HIDDEN);
ERROR_IM: ({"012".indexOf((char) _input.LA(1)) == -1}?) -> popMode,channel(HIDDEN);

mode DEFAULT_MODE;

// preprocessor
PREP_ORG: O R G;
PREP_EQU: E Q U;
PREP_VAR: V A R;
PREP_IF: I F;
PREP_ENDIF: E N D I F;
PREP_INCLUDE: I N C L U D E;
PREP_MACRO: M A C R O;
PREP_ENDM: E N D M;
PREP_DB: D B;
PREP_DW: D W;
PREP_DS: D S;
PREP_ADDR: '$';

// registers
REG_A: A;
REG_B: B;
REG_C: C;
REG_D: D;
REG_E: E;
REG_H: H;
REG_L: L;
REG_IX: I X;
REG_IXH: I X H;
REG_IXL: I X L;
REG_IY: I Y;
REG_IYH: I Y H;
REG_IYL: I Y L;
REG_BC: B C;
REG_DE: D E;
REG_HL: H L;
REG_SP: S P;
REG_AF: A F;
REG_AFF: A F '\'';
REG_I: I;
REG_R: R;

// operators
OP_MOD: M O D;
OP_SHR: S H R;
OP_SHL: S H L;
OP_NOT: N O T;

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
OP_AND: '&';
OP_OR: '|';
OP_XOR: '^';

WS : [ \t\f]+ -> channel(HIDDEN);
EOL: '\r'? '\n';
