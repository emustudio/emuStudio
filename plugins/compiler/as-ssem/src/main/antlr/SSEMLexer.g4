/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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

lexer grammar SSEMLexer;

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
JPR: (J P R) | (J R P) | (J M R);
LDN: L D N;
STO: S T O;
SUB: S U B;
CMP: (C M P) | (S K N);
STP: (S T P) | (H L T);

// preprocessor
START: S T A R T;
NUM: N U M;
BNUM: ((B N U M) | (B I N S)) -> pushMode(BIN);

// literals
NUMBER: [\-]? [0-9]+;
HEXNUMBER: [\-]? ('0x'|'0X') [0-9a-fA-F]+;
ERROR : .;

mode BIN;
BWS : (' ' | '\t') -> channel(HIDDEN);
BinaryNumber: [01]+ -> popMode;
BERROR : .;
