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
lexer grammar RAMLexer;

COMMENT: ('//' | '--' | ';' | '#' ) ~[\r\n]*;
COMMENT2: '/*' .*? '*/';

fragment A: [aA];
fragment B: [bB];
fragment D: [dD];
fragment E: [eE];
fragment G: [gG];
fragment H: [hH];
fragment I: [iI];
fragment J: [jJ];
fragment L: [lL];
fragment M: [mM];
fragment N: [nN];
fragment O: [oO];
fragment P: [pP];
fragment R: [rR];
fragment S: [sS];
fragment T: [tT];
fragment U: [uU];
fragment V: [vV];
fragment W: [wW];
fragment X: [xX];
fragment Z: [zZ];

OPCODE_HALT: H A L T;
OPCODE_READ: R E A D;
OPCODE_WRITE: W R I T E;
OPCODE_LOAD: L O A D;
OPCODE_STORE: S T O R E;
OPCODE_ADD: A D D;
OPCODE_SUB: S U B;
OPCODE_MUL: M U L;
OPCODE_DIV: D I V;
OPCODE_JMP: J M P;
OPCODE_JGTZ: J G T Z;
OPCODE_JZ: J Z;

OP_CONSTANT: '=';
OP_INDIRECT: '*';

PREP_INPUT: '<' I N P U T '>';

LIT_HEXNUMBER_1: '0' X [0-9a-fA-F]+;
LIT_NUMBER: [0-9]+ D?;
LIT_HEXNUMBER_2: [0-9a-fA-F]+ H;
LIT_OCTNUMBER: [0-7]+ [oOqQ];
LIT_BINNUMBER: [01]+ B;
LIT_STRING_1: '\'' ~[']* '\'';
LIT_STRING_2: '"' ~["]* '"';

ID_IDENTIFIER: [a-zA-Z_?@] [a-zA-Z_?@0-9]*;
ID_LABEL: ID_IDENTIFIER ':';

WS : [ \t\f]+ -> channel(HIDDEN);
EOL: '\r'? '\n';

ERROR: .;
