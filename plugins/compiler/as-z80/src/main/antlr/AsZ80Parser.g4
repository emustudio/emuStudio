parser grammar AsZ80Parser;

options {
   tokenVocab = AsZ80Lexer;
}

rStart:
 EOL* rLine? (EOL+ rLine)* EOL* EOF
 ;

rLine:
  label=ID_LABEL? EOL* statement=rStatement
  | label=ID_LABEL;

rStatement:
  instr=rInstruction
  | pseudo=rPseudoCode
  | data=rData
  ;

rInstruction:
  r8bitInstruction                                                              # instr8bit
  | opcode=OPCODE_LD SEP_LPAR nn=rExpression SEP_RPAR SEP_COMMA r=REG_A         # instrRef_NN_R
  | opcode=OPCODE_LD SEP_LPAR nn=rExpression SEP_RPAR SEP_COMMA r=REG_HL        # instrRef_NN_R
  | opcode=OPCODE_LD r=rRegister SEP_COMMA n=rExpression                        # instrR_N
  | opcode=OPCODE_LD r=REG_A SEP_COMMA SEP_LPAR nn=rExpression SEP_RPAR         # instrR_Ref_NN
  | opcode=OPCODE_LD rp=rRegPair SEP_COMMA nn=rExpression                       # instrRP_NN
  | opcode=OPCODE_LD rp=REG_HL SEP_COMMA SEP_LPAR nn=rExpression SEP_RPAR       # instrRP_Ref_NN
  | opcode=OPCODE_JR c=(COND_NZ|COND_Z|COND_NC|COND_C) SEP_COMMA n=rExpression  # instrC_N
  | opcode=OPCODE_JP c=cCondition SEP_COMMA nn=rExpression                      # instrC_NN
  | opcode=OPCODE_CALL c=cCondition SEP_COMMA nn=rExpression                    # instrC_NN
  | opcode=OPCODE_JR n=rExpression                                              # instrN
  | opcode=OPCODE_JP nn=rExpression                                             # instrNN
  | opcode=OPCODE_CALL nn=rExpression                                           # instrNN
  | opcode=OPCODE_ADD REG_A SEP_COMMA n=rExpression                             # instrN
  | opcode=OPCODE_ADC REG_A SEP_COMMA n=rExpression                             # instrN
  | opcode=OPCODE_OUT SEP_LPAR n=rExpression SEP_RPAR SEP_COMMA REG_A           # instrN
  | opcode=OPCODE_SUB n=rExpression                                             # instrN
  | opcode=OPCODE_IN REG_A SEP_COMMA SEP_LPAR n=rExpression SEP_RPAR            # instrN
  | opcode=OPCODE_SBC REG_A SEP_COMMA n=rExpression                             # instrN
  | opcode=OPCODE_AND n=rExpression                                             # instrN
  | opcode=OPCODE_XOR n=rExpression                                             # instrN
  | opcode=OPCODE_OR n=rExpression                                              # instrN
  | opcode=OPCODE_CP n=rExpression                                              # instrN


//#  | opcode=OPCODE_RLC
//#  | opcode=OPCODE_RRC
//#  | opcode=OPCODE_RL
//#  | opcode=OPCODE_RR
//#  | opcode=OPCODE_SLA
//#  | opcode=OPCODE_SRA
//#  | opcode=OPCODE_SLL
//#  | opcode=OPCODE_SRL

  ;

r8bitInstruction:
  opcode=OPCODE_LD SEP_LPAR rp=(REG_BC|REG_DE) SEP_RPAR SEP_COMMA REG_A     # instrRef_RP
  | opcode=OPCODE_JP SEP_LPAR REG_HL SEP_RPAR                               # instrRef_RP
  | opcode=OPCODE_LD REG_A SEP_COMMA SEP_LPAR rp=(REG_BC|REG_DE) SEP_RPAR   # instrA_Ref_RP
  | opcode=OPCODE_EX SEP_LPAR src=REG_SP SEP_RPAR SEP_COMMA dst=REG_HL      # instrRef_RP_RP
  | opcode=OPCODE_LD dst=rRegister SEP_COMMA src=rRegister                  # instrR_R
  | opcode=OPCODE_LD dst=REG_SP SEP_COMMA src=REG_HL                        # instrRP_RP
  | opcode=OPCODE_EX dst=REG_AF SEP_COMMA src=REG_AFF                       # instrRP_RP
  | opcode=OPCODE_EX dst=REG_DE SEP_COMMA src=REG_HL                        # instrRP_RP
  | opcode=OPCODE_INC rp=rRegPair                                           # instrRP
  | opcode=OPCODE_DEC rp=rRegPair                                           # instrRP
  | opcode=OPCODE_ADD REG_HL SEP_COMMA rp=rRegPair                          # instrRP
  | opcode=OPCODE_POP rp2=rRegPair2                                         # instrRP2
  | opcode=OPCODE_PUSH rp2=rRegPair2                                        # instrRP2
  | opcode=OPCODE_INC r=rRegister                                           # instrR
  | opcode=OPCODE_DEC r=rRegister                                           # instrR
  | opcode=OPCODE_ADD REG_A SEP_COMMA r=rRegister                           # instrR
  | opcode=OPCODE_ADC REG_A SEP_COMMA r=rRegister                           # instrR
  | opcode=OPCODE_SUB r=rRegister                                           # instrR
  | opcode=OPCODE_SBC REG_A SEP_COMMA r=rRegister                           # instrR
  | opcode=OPCODE_AND r=rRegister                                           # instrR
  | opcode=OPCODE_XOR r=rRegister                                           # instrR
  | opcode=OPCODE_OR r=rRegister                                            # instrR
  | opcode=OPCODE_CP r=rRegister                                            # instrR
  | opcode=OPCODE_RET c=cCondition                                          # instrC
  | opcode=OPCODE_RST n=rExpression                                         # instr8bitN
  | opcode=OPCODE_NOP                                                       # instr
  | opcode=OPCODE_RLCA                                                      # instr
  | opcode=OPCODE_RRCA                                                      # instr
  | opcode=OPCODE_RLA                                                       # instr
  | opcode=OPCODE_RRA                                                       # instr
  | opcode=OPCODE_DJNZ                                                      # instr
  | opcode=OPCODE_DAA                                                       # instr
  | opcode=OPCODE_CPL                                                       # instr
  | opcode=OPCODE_SCF                                                       # instr
  | opcode=OPCODE_CCF                                                       # instr
  | opcode=OPCODE_HALT                                                      # instr
  | opcode=OPCODE_RET                                                       # instr
  | opcode=OPCODE_EXX                                                       # instr
  | opcode=OPCODE_DI                                                        # instr
  | opcode=OPCODE_EI                                                        # instr
  ;

rRegister:
  r=REG_A
  | r=REG_B
  | r=REG_C
  | r=REG_D
  | r=REG_E
  | r=REG_H
  | r=REG_L
  | SEP_LPAR r=REG_HL SEP_RPAR
  ;

rRegPair:
  REG_BC
  | REG_DE
  | REG_HL
  | REG_SP
  ;

rRegPair2:
  REG_BC
  | REG_DE
  | REG_HL
  | REG_AF
  ;


cCondition:
  COND_C
  | COND_NC
  | COND_Z
  | COND_NZ
  | COND_M
  | COND_P
  | COND_PE
  | COND_PO
  ;

rPseudoCode:
  PREP_ORG expr=rExpression                                                                # pseudoOrg
  | id=ID_IDENTIFIER PREP_EQU expr=rExpression                                             # pseudoEqu
  | id=ID_IDENTIFIER PREP_SET expr=rExpression                                             # pseudoSet
  | PREP_IF expr=rExpression EOL (rLine EOL)* EOL* PREP_ENDIF                              # pseudoIf
  | id=ID_IDENTIFIER PREP_MACRO params=rMacroParameters? EOL (rLine EOL)* EOL* PREP_ENDM   # pseudoMacroDef
  | id=ID_IDENTIFIER args=rMacroArguments?                                                 # pseudoMacroCall
  | PREP_INCLUDE filename=(LIT_STRING_1|LIT_STRING_2)                                      # pseudoInclude
  ;

rMacroParameters:
  ID_IDENTIFIER (SEP_COMMA ID_IDENTIFIER)*
  ;

rMacroArguments:
  rExpression (SEP_COMMA rExpression)*
  ;

rData:
  PREP_DB rDBdata (SEP_COMMA rDBdata)*    # dataDB
  | PREP_DW rDWdata (SEP_COMMA rDWdata)*  # dataDW
  | PREP_DS data=rExpression              # dataDS
  ;

rDBdata:
  expr=rExpression
  | instr=r8bitInstruction
  ;

rDWdata:
  expr=rExpression
  ;

rExpression:
 <assoc=right> unaryop=(OP_ADD|OP_SUBTRACT|OP_NOT|OP_NOT_2) expr=rExpression                     # exprUnary
 | <assoc=left> expr1=rExpression op=(OP_MULTIPLY|OP_DIVIDE|OP_MOD|OP_MOD_2) expr2=rExpression   # exprInfix
 | <assoc=left> expr1=rExpression op=(OP_ADD|OP_SUBTRACT) expr2=rExpression                      # exprInfix
 | <assoc=left> expr1=rExpression op=(OP_SHL|OP_SHR|OP_SHR_2|OP_SHL_2) expr2=rExpression         # exprInfix
 | <assoc=right> expr1=rExpression op=(OP_GT|OP_GTE|OP_LT|OP_LTE) expr2=rExpression              # exprInfix
 | <assoc=left> expr1=rExpression op=(OP_AND|OP_AND_2) expr2=rExpression                         # exprInfix
 | <assoc=left> expr1=rExpression op=(OP_XOR|OP_XOR_2) expr2=rExpression                         # exprInfix
 | <assoc=left> expr1=rExpression op=(OP_OR|OP_OR_2) expr2=rExpression                           # exprInfix
 | <assoc=right> expr1=rExpression op=OP_EQUAL expr2=rExpression                                 # exprInfix
 | SEP_LPAR expr=rExpression SEP_RPAR                                                            # exprParens
 | num=LIT_NUMBER                                                                                # exprDec
 | num=LIT_HEXNUMBER_1                                                                           # exprHex1
 | num=LIT_HEXNUMBER_2                                                                           # exprHex2
 | num=LIT_OCTNUMBER                                                                             # exprOct
 | num=LIT_BINNUMBER                                                                             # exprBin
 | PREP_ADDR                                                                                     # exprCurrentAddress
 | id=ID_IDENTIFIER                                                                              # exprId
 | str=(LIT_STRING_1|LIT_STRING_2)                                                               # exprString
 ;
