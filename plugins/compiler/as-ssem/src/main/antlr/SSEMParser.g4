parser grammar SSEMParser;

options {
   tokenVocab = SSEMLexer;
}

start:
 (line EOL line)* EOF
 | line EOF
 ;

line:
  linenumber=(NUMBER|HEXNUMBER) command=statement? comment
  | comment
  ;

comment: COMMENT? ;

statement:
  instr=START
  | instr=JMP operand=(NUMBER|HEXNUMBER)
  | instr=JPR operand=(NUMBER|HEXNUMBER)
  | instr=LDN operand=(NUMBER|HEXNUMBER)
  | instr=STO operand=(NUMBER|HEXNUMBER)
  | instr=SUB operand=(NUMBER|HEXNUMBER)
  | instr=CMP
  | instr=STP
  | instr=NUM operand=(NUMBER|HEXNUMBER)
  | instr=BNUM operand=BinaryNumber
  ;
