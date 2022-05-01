lexer grammar BraincLexer;

WS:       [ \t\r\n] -> channel(HIDDEN);

HALT: ';';
INC: '>';
DEC: '<';
INCV: '+';
DECV: '-';
PRINT: '.';
LOAD: ',';
LOOP: '[';
ENDL: ']';

COMMENT:  .+?;
