/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
lexer grammar RAMLexer;

options {
    caseInsensitive = true;
}

COMMENT: ('//' | '--' | ';' | '#' ) ~[\r\n]*;
COMMENT2: '/*' .*? '*/';

OPCODE_HALT: 'halt';
OPCODE_READ: 'read';
OPCODE_WRITE: 'write';
OPCODE_LOAD: 'load';
OPCODE_STORE: 'store';
OPCODE_ADD: 'add';
OPCODE_SUB: 'sub';
OPCODE_MUL: 'mul';
OPCODE_DIV: 'div';
OPCODE_JMP: 'jmp';
OPCODE_JGTZ: 'jgtz';
OPCODE_JZ: 'jz';

OP_CONSTANT: '=';
OP_INDIRECT: '*';

PREP_INPUT: '<input>';

LIT_HEXNUMBER_1: [+-]? '0x' [0-9a-f]+;
LIT_NUMBER: [+-]? [0-9]+ 'd'?;
LIT_HEXNUMBER_2: [+-]? [0-9a-f]+ 'h';
LIT_OCTNUMBER: [+-]? [0-7]+ [oq];
LIT_BINNUMBER: [+-]? [01]+ 'b';
LIT_STRING_1: '\'' ~[']* '\'';
LIT_STRING_2: '"' ~["]* '"';

ID_IDENTIFIER: [a-z_?@] [a-z_?@0-9]*;
ID_LABEL: ID_IDENTIFIER ':';

WS : [ \t\f]+ -> channel(HIDDEN);
EOL: '\r'? '\n';

ERROR: .;
