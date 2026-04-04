/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
lexer grammar SSEMLexer;

options {
    caseInsensitive = true;
}

WS : (' ' | '\t') -> channel(HIDDEN);
COMMENT: ('//' | '--' | ';' | '#' ) ~[\r\n]*;
EOL: '\r'? '\n';

// reserved
JMP: 'jmp';
JPR: 'jpr' | 'jrp' | 'jmr';
LDN: 'ldn';
STO: 'sto';
SUB: 'sub';
CMP: 'cmp' | 'skn';
STP: 'stp' | 'hlt';

// preprocessor
START: 'start';
NUM: 'num';
BNUM: ('bnum' | 'bins') -> pushMode(BIN);

// literals
NUMBER: [\-]? [0-9]+;
HEXNUMBER: [\-]? '0x' [0-9a-f]+;
ERROR : .;

mode BIN;
BWS : (' ' | '\t') -> channel(HIDDEN);
BinaryNumber: [01]+ -> popMode;
BERROR : .;
