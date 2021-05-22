lexer grammar SSEMLexer;

tokens {
  COMMENT,
  EOL, WS, BWS,
  JMP, JRP, JPR, JMR, LDN, STO, SUB, CMP, SKN, STP, HLT,
  START, NUM, BNUM,
  NUMBER, HEXNUMBER, BinaryNumber
}

WS : (' ' | '\t') -> channel(HIDDEN);
COMMENT: ('//' | '--' | ';' | '#' ) ~[\r\n]*;
EOL: '\r'? '\n';

fragment J: [jJ];
fragment M: [mM];
fragment P: [pP];
fragment R: [rR];
fragment L: [lL];
fragment D: [dD];
fragment N: [nN];
fragment S: [sS];
fragment T: [tT];
fragment O: [oO];
fragment U: [uU];
fragment B: [bB];
fragment C: [cC];
fragment K: [kK];
fragment H: [hH];
fragment A: [aA];
fragment I: [iI];

// reserved
JMP: J M P;
JRP: J R P;
JPR: J P R;
JMR: J M R;
LDN: L D N;
STO: S T O;
SUB: S U B;
CMP: C M P;
SKN: S K N;
STP: S T P;
HLT: H L T;

// preprocessor
START: S T A R T;
NUM: N U M;
BNUM: ((B N U M) | (B I N S)) -> pushMode(BIN);

// literals
NUMBER: [\-]? [0-9]+;
HEXNUMBER: [\-]? ('0x'|'0X') [0-9a-fA-F]+;

mode BIN;
BWS : (' ' | '\t') -> channel(HIDDEN);
BinaryNumber: [01]+ -> popMode;
