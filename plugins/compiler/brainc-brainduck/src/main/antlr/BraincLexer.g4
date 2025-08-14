/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
lexer grammar BraincLexer;

WS : (' ' | '\t') -> channel(HIDDEN);
fragment EOL: '\r'? '\n';

HALT: ';';
INC: '>';
DEC: '<';
INCV: '+';
DECV: '-';
PRINT: '.';
LOAD: ',';
LOOP: '[';
ENDL: ']';

COMMENT: ~[<>.;,+\-[\]\r\n \t] ~[\r\n]* EOL? | EOL;
