parser grammar RASPParser;

options {
   tokenVocab = RASPLexer;
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
  rInstruction
  | rInput
  | rOrg
  ;


rInstruction:
  op=OPCODE_READ n=rNumber                   # instrRegister
  | op=OPCODE_WRITE OP_CONSTANT n=rNumber    # instrConstant
  | op=OPCODE_WRITE n=rNumber                # instrRegister
  | op=OPCODE_LOAD OP_CONSTANT n=rNumber     # instrConstant
  | op=OPCODE_LOAD n=rNumber                 # instrRegister
  | op=OPCODE_STORE n=rNumber                # instrRegister
  | op=OPCODE_ADD OP_CONSTANT n=rNumber      # instrConstant
  | op=OPCODE_ADD n=rNumber                  # instrRegister
  | op=OPCODE_SUB OP_CONSTANT n=rNumber      # instrConstant
  | op=OPCODE_SUB n=rNumber                  # instrRegister
  | op=OPCODE_MUL OP_CONSTANT n=rNumber      # instrConstant
  | op=OPCODE_MUL n=rNumber                  # instrRegister
  | op=OPCODE_DIV OP_CONSTANT n=rNumber      # instrConstant
  | op=OPCODE_DIV n=rNumber                  # instrRegister
  | op=OPCODE_JMP id=ID_IDENTIFIER           # instrJump
  | op=OPCODE_JZ id=ID_IDENTIFIER            # instrJump
  | op=OPCODE_JGTZ id=ID_IDENTIFIER          # instrJump
  | op=OPCODE_HALT                           # instrNoOperand
  ;

rInput: PREP_INPUT rNumber+;

rOrg: PREP_ORG n=rNumber;

rNumber:
  n=LIT_HEXNUMBER_1
  | n=LIT_NUMBER
  | n=LIT_HEXNUMBER_2
  | n=LIT_OCTNUMBER
  | n=LIT_BINNUMBER
  ;
