parser grammar As8080Parser;

options {
   tokenVocab = As8080Lexer;
}

rStart:
 (rLine EOL rLine)* EOF
 | rLine EOF
 ;

rLine:
  label=ID_LABEL? statement=rStatement? rComment
  | rComment
  ;

rComment: COMMENT? ;

rStatement:
  instr=rInstruction
  | pseudo=rPseudoCode
  | data=rData
  ;

rInstruction:
  opcode=OPCODE_STC
  | opcode=OPCODE_CMC
  | opcode=OPCODE_CMA
  | opcode=OPCODE_DAA
  | opcode=OPCODE_NOP
  | opcode=OPCODE_RLC
  | opcode=OPCODE_RRC
  | opcode=OPCODE_RAL
  | opcode=OPCODE_RAR
  | opcode=OPCODE_XCHG
  | opcode=OPCODE_XTHL
  | opcode=OPCODE_SPHL
  | opcode=OPCODE_PCHL
  | opcode=OPCODE_RET
  | opcode=OPCODE_RC
  | opcode=OPCODE_RNC
  | opcode=OPCODE_RZ
  | opcode=OPCODE_RNZ
  | opcode=OPCODE_RM
  | opcode=OPCODE_RP
  | opcode=OPCODE_RPE
  | opcode=OPCODE_RPO
  | opcode=OPCODE_EI
  | opcode=OPCODE_DI
  | opcode=OPCODE_HLT
  | opcode=OPCODE_INR reg=rRegister
  | opcode=OPCODE_DCR reg=rRegister
  | opcode=OPCODE_ADD reg=rRegister
  | opcode=OPCODE_ADC reg=rRegister
  | opcode=OPCODE_SUB reg=rRegister
  | opcode=OPCODE_SBB reg=rRegister
  | opcode=OPCODE_ANA reg=rRegister
  | opcode=OPCODE_XRA reg=rRegister
  | opcode=OPCODE_ORA reg=rRegister
  | opcode=OPCODE_CMP reg=rRegister
  | opcode=OPCODE_MOV dst=rRegister SEP_COMMA src=rRegister
  | opcode=OPCODE_STAX regpair=(REG_B|REG_D)
  | opcode=OPCODE_LDAX regpair=(REG_B|REG_D)
  | opcode=OPCODE_PUSH regpair=(REG_B|REG_D|REG_H|REG_PSW)
  | opcode=OPCODE_POP regpair=(REG_B|REG_D|REG_H|REG_PSW)
  | opcode=OPCODE_DAD regpair=(REG_B|REG_D|REG_H|REG_SP)
  | opcode=OPCODE_INX regpair=(REG_B|REG_D|REG_H|REG_SP)
  | opcode=OPCODE_DCX regpair=(REG_B|REG_D|REG_H|REG_SP)
  | opcode=OPCODE_LXI regpair=(REG_B|REG_D|REG_H|REG_SP) SEP_COMMA expr=rExpression
  | opcode=OPCODE_MVI reg=rRegister SEP_COMMA expr=rExpression
  | opcode=OPCODE_ADI expr=rExpression
  | opcode=OPCODE_ACI expr=rExpression
  | opcode=OPCODE_SUI expr=rExpression
  | opcode=OPCODE_SBI expr=rExpression
  | opcode=OPCODE_ANI expr=rExpression
  | opcode=OPCODE_XRI expr=rExpression
  | opcode=OPCODE_ORI expr=rExpression
  | opcode=OPCODE_CPI expr=rExpression
  | opcode=OPCODE_STA expr=rExpression
  | opcode=OPCODE_LDA expr=rExpression
  | opcode=OPCODE_SHLD expr=rExpression
  | opcode=OPCODE_LHLD expr=rExpression
  | opcode=OPCODE_JMP expr=rExpression
  | opcode=OPCODE_JC expr=rExpression
  | opcode=OPCODE_JNC expr=rExpression
  | opcode=OPCODE_JZ expr=rExpression
  | opcode=OPCODE_JNZ expr=rExpression
  | opcode=OPCODE_JM expr=rExpression
  | opcode=OPCODE_JP expr=rExpression
  | opcode=OPCODE_JPE expr=rExpression
  | opcode=OPCODE_JPO expr=rExpression
  | opcode=OPCODE_CALL expr=rExpression
  | opcode=OPCODE_CC expr=rExpression
  | opcode=OPCODE_CNC expr=rExpression
  | opcode=OPCODE_CZ expr=rExpression
  | opcode=OPCODE_CNZ expr=rExpression
  | opcode=OPCODE_CM expr=rExpression
  | opcode=OPCODE_CP expr=rExpression
  | opcode=OPCODE_CPE expr=rExpression
  | opcode=OPCODE_CPO expr=rExpression
  | opcode=OPCODE_RST expr=rExpression
  | opcode=OPCODE_IN expr=rExpression
  | opcode=OPCODE_OUT expr=rExpression
  ;

rRegister:
  REG_A
  | REG_B
  | REG_C
  | REG_D
  | REG_E
  | REG_H
  | REG_L
  | REG_M
  ;

rPseudoCode:
  PREP_ORG expr=rExpression
  | id=ID_IDENTIFIER PREP_EQU expr=rExpression
  | id=ID_IDENTIFIER PREP_SET expr=rExpression
  | PREP_IF expr=rExpression rComment EOL statement=rStatement EOL PREP_ENDIF
  | id=ID_IDENTIFIER PREP_MACRO macro=rMacro? rComment EOL statement=rStatement PREP_ENDM
  | id=ID_IDENTIFIER macroCall=rMacroCall
  | PREP_INCLUDE filename=(LIT_STRING_1|LIT_STRING_2)
  ;

rMacro:
  id=ID_IDENTIFIER (SEP_COMMA ids=ID_IDENTIFIER)*
  ;

// todo: assign variables??
rMacroCall:
  expr=rExpression (SEP_COMMA exprs=rExpression)*
  ;

rData:
  PREP_DB db=rDB
  | PREP_DW dw=rDW
  | PREP_DS ds=rExpression
  ;

rDB:
  data=rDBdata (SEP_COMMA data=rDBdata)*
  ;

rDW:
  data=rDWdata (SEP_COMMA data=rDWdata)*
  ;

rDBdata:
  expr=rExpression
  | str=(LIT_STRING_1|LIT_STRING_2)
  | instr=rInstruction
  ;

rDWdata:
  expr=rExpression
  ;

rExpression:
 SEP_LPAR expr=rExpression SEP_RPAR
 | num=LIT_NUMBER
 | num=LIT_HEXNUMBER_1
 | num=LIT_HEXNUMBER_2
 | num=LIT_OCTNUMBER
 | num=LIT_BINNUMBER
 | id=PREP_ADDR
 | id=ID_IDENTIFIER
 | unaryop=(OP_ADD|OP_SUBTRACT) expr=rExpression
 | expr1=rExpression op=(OP_MULTIPLY|OP_DIVIDE|OP_MOD|OP_SHL|OP_SHR) expr2=rExpression
 | expr1=rExpression op=(OP_ADD|OP_SUBTRACT) expr2=rExpression
 | unaryop=OP_NOT expr=rExpression
 | expr1=rExpression op=OP_AND expr2=rExpression
 | expr1=rExpression op=(OP_OR|OP_XOR) expr2=rExpression
 ;
