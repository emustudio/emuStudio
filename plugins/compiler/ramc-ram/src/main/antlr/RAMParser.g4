parser grammar RAMParser;

options {
   tokenVocab = RAMLexer;
}

rStart:
 (rLine EOL rLine)* EOF
 | rLine EOF
 ;

rLine:
  label=ID_LABEL? statement=rStatement comment
  | label=ID_LABEL comment
  | comment
  ;

comment: COMMENT? | COMMENT2?;

rStatement:
  instr=rInstruction
  | value=rInput
  ;

rInstruction:
  op=OPCODE_READ n=rNumber
  | op=OPCODE_READ d=OP_INDIRECT n=rNumber
  | op=OPCODE_WRITE d=OP_CONSTANT v=rValue
  | op=OPCODE_WRITE d=OP_INDIRECT n=rNumber
  | op=OPCODE_WRITE n=rNumber
  | op=OPCODE_LOAD d=OP_CONSTANT v=rValue
  | op=OPCODE_LOAD d=OP_INDIRECT n=rNumber
  | op=OPCODE_LOAD n=rNumber
  | op=OPCODE_STORE n=rNumber
  | op=OPCODE_STORE d=OP_INDIRECT n=rNumber
  | op=OPCODE_ADD d=OP_CONSTANT v=rValue
  | op=OPCODE_ADD d=OP_INDIRECT n=rNumber
  | op=OPCODE_ADD n=rNumber
  | op=OPCODE_SUB d=OP_CONSTANT v=rValue
  | op=OPCODE_SUB d=OP_INDIRECT n=rNumber
  | op=OPCODE_SUB n=rNumber
  | op=OPCODE_MUL d=OP_CONSTANT v=rValue
  | op=OPCODE_MUL d=OP_INDIRECT n=rNumber
  | op=OPCODE_MUL n=rNumber
  | op=OPCODE_DIV d=OP_CONSTANT v=rValue
  | op=OPCODE_DIV d=OP_INDIRECT n=rNumber
  | op=OPCODE_DIV n=rNumber
  | op=OPCODE_JMP id=ID_IDENTIFIER
  | op=OPCODE_JZ id=ID_IDENTIFIER
  | op=OPCODE_JGTZ id=ID_IDENTIFIER
  | op=OPCODE_HALT
  ;

rInput: PREP_INPUT rValue+;

rValue:
  v=LIT_HEXNUMBER_1
  | v=LIT_NUMBER
  | v=LIT_HEXNUMBER_2
  | v=LIT_OCTNUMBER
  | v=LIT_BINNUMBER
  | v=LIT_STRING_1
  | v=LIT_STRING_2
  ;

rNumber:
  n=LIT_HEXNUMBER_1
  | n=LIT_NUMBER
  | n=LIT_HEXNUMBER_2
  | n=LIT_OCTNUMBER
  | n=LIT_BINNUMBER
  ;
