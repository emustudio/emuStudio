parser grammar BraincParser;

options {
   tokenVocab = BraincLexer;
}

start: line* EOF
 ;

line:
  command=statement COMMENT?
  | COMMENT
  ;

statement:
  instr=HALT
  | instr=INC
  | instr=DEC
  | instr=INCV
  | instr=DECV
  | instr=PRINT
  | instr=LOAD
  | instr=LOOP
  | instr=ENDL
  ;
