parser grammar As8080Parser;

options {
   tokenVocab = As8080Lexer;
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
  r8bitInstruction                                                                    # instr8bit
  | opcode=OPCODE_MVI reg=rRegister SEP_COMMA expr=rExpression                        # instrRegExpr
  | opcode=OPCODE_LXI regpair=(REG_B|REG_D|REG_H|REG_SP) SEP_COMMA expr=rExpression   # instrRegPairExpr
  | opcode=OPCODE_LDA expr=rExpression                                                # instrExpr
  | opcode=OPCODE_STA expr=rExpression                                                # instrExpr
  | opcode=OPCODE_LHLD expr=rExpression                                               # instrExpr
  | opcode=OPCODE_SHLD expr=rExpression                                               # instrExpr
  | opcode=OPCODE_ADI expr=rExpression                                                # instrExpr
  | opcode=OPCODE_ACI expr=rExpression                                                # instrExpr
  | opcode=OPCODE_SUI expr=rExpression                                                # instrExpr
  | opcode=OPCODE_SBI expr=rExpression                                                # instrExpr
  | opcode=OPCODE_ANI expr=rExpression                                                # instrExpr
  | opcode=OPCODE_ORI expr=rExpression                                                # instrExpr
  | opcode=OPCODE_XRI expr=rExpression                                                # instrExpr
  | opcode=OPCODE_CPI expr=rExpression                                                # instrExpr
  | opcode=OPCODE_JMP expr=rExpression                                                # instrExpr
  | opcode=OPCODE_JC expr=rExpression                                                 # instrExpr
  | opcode=OPCODE_JNC expr=rExpression                                                # instrExpr
  | opcode=OPCODE_JZ expr=rExpression                                                 # instrExpr
  | opcode=OPCODE_JNZ expr=rExpression                                                # instrExpr
  | opcode=OPCODE_JM expr=rExpression                                                 # instrExpr
  | opcode=OPCODE_JP expr=rExpression                                                 # instrExpr
  | opcode=OPCODE_JPE expr=rExpression                                                # instrExpr
  | opcode=OPCODE_JPO expr=rExpression                                                # instrExpr
  | opcode=OPCODE_CALL expr=rExpression                                               # instrExpr
  | opcode=OPCODE_CC expr=rExpression                                                 # instrExpr
  | opcode=OPCODE_CNC expr=rExpression                                                # instrExpr
  | opcode=OPCODE_CZ expr=rExpression                                                 # instrExpr
  | opcode=OPCODE_CNZ expr=rExpression                                                # instrExpr
  | opcode=OPCODE_CM expr=rExpression                                                 # instrExpr
  | opcode=OPCODE_CP expr=rExpression                                                 # instrExpr
  | opcode=OPCODE_CPE expr=rExpression                                                # instrExpr
  | opcode=OPCODE_CPO expr=rExpression                                                # instrExpr
  | opcode=OPCODE_IN expr=rExpression                                                 # instrExpr
  | opcode=OPCODE_OUT expr=rExpression                                                # instrExpr
  ;

r8bitInstruction:
  opcode=OPCODE_STC                                                                   # instrNoArgs
  | opcode=OPCODE_CMC                                                                 # instrNoArgs
  | opcode=OPCODE_CMA                                                                 # instrNoArgs
  | opcode=OPCODE_DAA                                                                 # instrNoArgs
  | opcode=OPCODE_NOP                                                                 # instrNoArgs
  | opcode=OPCODE_RLC                                                                 # instrNoArgs
  | opcode=OPCODE_RRC                                                                 # instrNoArgs
  | opcode=OPCODE_RAL                                                                 # instrNoArgs
  | opcode=OPCODE_RAR                                                                 # instrNoArgs
  | opcode=OPCODE_XCHG                                                                # instrNoArgs
  | opcode=OPCODE_XTHL                                                                # instrNoArgs
  | opcode=OPCODE_SPHL                                                                # instrNoArgs
  | opcode=OPCODE_PCHL                                                                # instrNoArgs
  | opcode=OPCODE_RET                                                                 # instrNoArgs
  | opcode=OPCODE_RC                                                                  # instrNoArgs
  | opcode=OPCODE_RNC                                                                 # instrNoArgs
  | opcode=OPCODE_RZ                                                                  # instrNoArgs
  | opcode=OPCODE_RNZ                                                                 # instrNoArgs
  | opcode=OPCODE_RM                                                                  # instrNoArgs
  | opcode=OPCODE_RP                                                                  # instrNoArgs
  | opcode=OPCODE_RPE                                                                 # instrNoArgs
  | opcode=OPCODE_RPO                                                                 # instrNoArgs
  | opcode=OPCODE_EI                                                                  # instrNoArgs
  | opcode=OPCODE_DI                                                                  # instrNoArgs
  | opcode=OPCODE_HLT                                                                 # instrNoArgs
  | opcode=OPCODE_INR reg=rRegister                                                   # instrReg
  | opcode=OPCODE_DCR reg=rRegister                                                   # instrReg
  | opcode=OPCODE_ADD reg=rRegister                                                   # instrReg
  | opcode=OPCODE_ADC reg=rRegister                                                   # instrReg
  | opcode=OPCODE_SUB reg=rRegister                                                   # instrReg
  | opcode=OPCODE_SBB reg=rRegister                                                   # instrReg
  | opcode=OPCODE_ANA reg=rRegister                                                   # instrReg
  | opcode=OPCODE_XRA reg=rRegister                                                   # instrReg
  | opcode=OPCODE_ORA reg=rRegister                                                   # instrReg
  | opcode=OPCODE_CMP reg=rRegister                                                   # instrReg
  | opcode=OPCODE_MOV dst=rRegister SEP_COMMA src=rRegister                           # instrRegReg
  | opcode=OPCODE_STAX regpair=(REG_B|REG_D)                                          # instrRegPair
  | opcode=OPCODE_LDAX regpair=(REG_B|REG_D)                                          # instrRegPair
  | opcode=OPCODE_PUSH regpair=(REG_B|REG_D|REG_H|REG_PSW)                            # instrRegPair
  | opcode=OPCODE_POP regpair=(REG_B|REG_D|REG_H|REG_PSW)                             # instrRegPair
  | opcode=OPCODE_DAD regpair=(REG_B|REG_D|REG_H|REG_SP)                              # instrRegPair
  | opcode=OPCODE_INX regpair=(REG_B|REG_D|REG_H|REG_SP)                              # instrRegPair
  | opcode=OPCODE_DCX regpair=(REG_B|REG_D|REG_H|REG_SP)                              # instrRegPair
  | opcode=OPCODE_RST expr=rExpression                                                # instr8bitExpr
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
  PREP_ORG expr=rExpression                                                                # pseudoOrg
  | id=ID_IDENTIFIER PREP_EQU expr=rExpression                                             # pseudoEqu
  | id=ID_IDENTIFIER PREP_SET expr=rExpression                                             # pseudoVar
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
 | <assoc=right> expr1=rExpression op=OP_EQUAL expr2=rExpression                                 # exprInfix
 | <assoc=left> expr1=rExpression op=(OP_AND|OP_AND_2) expr2=rExpression                         # exprInfix
 | <assoc=left> expr1=rExpression op=(OP_XOR|OP_XOR_2) expr2=rExpression                         # exprInfix
 | <assoc=left> expr1=rExpression op=(OP_OR|OP_OR_2) expr2=rExpression                           # exprInfix
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
