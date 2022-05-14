/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
parser grammar AsZ80Parser;

options {
   tokenVocab = AsZ80Lexer;
}

rStart:
 (rLine EOL rLine)* (PREP_END comment (EOL comment)*)? EOF
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
  | pseudo=rPseudoCode
  | data=rData
  ;

rInstruction:
  opcode=OPCODE_RLC d=rDisplacement SEP_COMMA r=rRegister2                      # instrXDCB_R
  | opcode=OPCODE_RRC d=rDisplacement SEP_COMMA r=rRegister2                    # instrXDCB_R
  | opcode=OPCODE_RL d=rDisplacement SEP_COMMA r=rRegister2                     # instrXDCB_R
  | opcode=OPCODE_RR d=rDisplacement SEP_COMMA r=rRegister2                     # instrXDCB_R
  | opcode=OPCODE_SLA d=rDisplacement SEP_COMMA r=rRegister2                    # instrXDCB_R
  | opcode=OPCODE_SRA d=rDisplacement SEP_COMMA r=rRegister2                    # instrXDCB_R
  | opcode=OPCODE_SLL d=rDisplacement SEP_COMMA r=rRegister2                    # instrXDCB_R
  | opcode=OPCODE_SRL d=rDisplacement SEP_COMMA r=rRegister2                    # instrXDCB_R
  | opcode=OPCODE_SRL d=rDisplacement                                           # instrXDCB
  | opcode=OPCODE_RLC d=rDisplacement                                           # instrXDCB
  | opcode=OPCODE_RRC d=rDisplacement                                           # instrXDCB
  | opcode=OPCODE_RL d=rDisplacement                                            # instrXDCB
  | opcode=OPCODE_RR d=rDisplacement                                            # instrXDCB
  | opcode=OPCODE_SLA d=rDisplacement                                           # instrXDCB
  | opcode=OPCODE_SRA d=rDisplacement                                           # instrXDCB
  | opcode=OPCODE_SLL d=rDisplacement                                           # instrXDCB
  | opcode=OPCODE_BIT n=rExpression SEP_COMMA d=rDisplacement                   # instrXDCB_N
  | opcode=OPCODE_RES n=rExpression SEP_COMMA d=rDisplacement                   # instrXDCB_N
  | opcode=OPCODE_SET n=rExpression SEP_COMMA d=rDisplacement                   # instrXDCB_N
  | opcode=OPCODE_RES n=rExpression SEP_COMMA d=rDisplacement SEP_COMMA r=rRegister2  # instrXDCB_N_R
  | opcode=OPCODE_SET n=rExpression SEP_COMMA d=rDisplacement SEP_COMMA r=rRegister2  # instrXDCB_N_R

  | opcode=OPCODE_IN r=rRegister2 SEP_COMMA SEP_LPAR REG_C SEP_RPAR             # instrED_R2
  | opcode=OPCODE_OUT SEP_LPAR REG_C SEP_RPAR SEP_COMMA r=rRegister2            # instrED_R2
  | opcode=OPCODE_IN SEP_LPAR REG_C SEP_RPAR                                    # instrED_C
  | opcode=OPCODE_OUT SEP_LPAR REG_C SEP_RPAR SEP_COMMA n=LIT_NUMBER            # instrED_C
  | opcode=OPCODE_SBC REG_HL SEP_COMMA rp=rRegPair                              # instrED_RP
  | opcode=OPCODE_ADC REG_HL SEP_COMMA rp=rRegPair                              # instrED_RP
  | opcode=OPCODE_LD SEP_LPAR nn=rExpression SEP_RPAR SEP_COMMA rp=(REG_BC|REG_DE|REG_SP)  # instrED_NN_RP
  | opcode=OPCODE_LD rp=(REG_BC|REG_DE|REG_SP) SEP_COMMA SEP_LPAR nn=rExpression SEP_RPAR  # instrED_RP_NN
  | opcode=OPCODE_IM im=(IM_0|IM_1|IM_2|IM_01)                                  # instrED_IM
  | opcode=OPCODE_LD dst=REG_I SEP_COMMA src=REG_A                              # instrED_RIA_RIA
  | opcode=OPCODE_LD dst=REG_A SEP_COMMA src=REG_I                              # instrED_RIA_RIA
  | opcode=OPCODE_LD dst=REG_R SEP_COMMA src=REG_A                              # instrED_RIA_RIA
  | opcode=OPCODE_LD dst=REG_A SEP_COMMA src=REG_R                              # instrED_RIA_RIA
  | opcode=OPCODE_NEG                                                           # instrED
  | opcode=OPCODE_RETN                                                          # instrED
  | opcode=OPCODE_RETI                                                          # instrED
  | opcode=OPCODE_LDI                                                           # instrED
  | opcode=OPCODE_LDIR                                                          # instrED
  | opcode=OPCODE_CPI                                                           # instrED
  | opcode=OPCODE_CPIR                                                          # instrED
  | opcode=OPCODE_INI                                                           # instrED
  | opcode=OPCODE_INIR                                                          # instrED
  | opcode=OPCODE_OUTI                                                          # instrED
  | opcode=OPCODE_OTIR                                                          # instrED
  | opcode=OPCODE_LDD                                                           # instrED
  | opcode=OPCODE_LDDR                                                          # instrED
  | opcode=OPCODE_CPD                                                           # instrED
  | opcode=OPCODE_CPDR                                                          # instrED
  | opcode=OPCODE_IND                                                           # instrED
  | opcode=OPCODE_INDR                                                          # instrED
  | opcode=OPCODE_OUTD                                                          # instrED
  | opcode=OPCODE_OTDR                                                          # instrED
  | opcode=OPCODE_RLD                                                           # instrED
  | opcode=OPCODE_RRD                                                           # instrED

  | opcode=OPCODE_RLC r=rRegister                                               # instrCB
  | opcode=OPCODE_RRC r=rRegister                                               # instrCB
  | opcode=OPCODE_RL r=rRegister                                                # instrCB
  | opcode=OPCODE_RR r=rRegister                                                # instrCB
  | opcode=OPCODE_SLA r=rRegister                                               # instrCB
  | opcode=OPCODE_SRA r=rRegister                                               # instrCB
  | opcode=OPCODE_SLL r=rRegister                                               # instrCB
  | opcode=OPCODE_SRL r=rRegister                                               # instrCB
  | opcode=OPCODE_BIT n=rExpression SEP_COMMA r=rRegister                       # instrCB_N_R
  | opcode=OPCODE_RES n=rExpression SEP_COMMA r=rRegister                       # instrCB_N_R
  | opcode=OPCODE_SET n=rExpression SEP_COMMA r=rRegister                       # instrCB_N_R

  | opcode=OPCODE_LD ii=rII SEP_COMMA SEP_LPAR nn=rExpression SEP_RPAR          # instrXD_II_Ref_NN // must be above plain nn
  | opcode=OPCODE_LD ii=rII SEP_COMMA nn=rExpression                            # instrXD_II_NN
  | opcode=OPCODE_ADD ii=rII SEP_COMMA rp=(REG_BC|REG_DE|REG_IX|REG_IY|REG_SP)  # instrXD_II_RP
  | opcode=OPCODE_LD SEP_LPAR nn=rExpression SEP_RPAR SEP_COMMA ii=rII          # instrXD_Ref_NN_II
  | opcode=OPCODE_LD ii=rII_HL SEP_COMMA n=rExpression                          # instrXD_IIHL_N
  | opcode=OPCODE_LD d=rDisplacement SEP_COMMA n=rExpression                    # instrXD_Ref_II_N_N

  | opcode=OPCODE_LD ii=rII_HL SEP_COMMA r=rRegisterII                          # instrXD_IIHL_R
  | opcode=OPCODE_LD r=rRegisterII SEP_COMMA ii=rII_HL                          # instrXD_R_IIHL
  | opcode=OPCODE_LD d=rDisplacement SEP_COMMA r=rRegister2                     # instrXD_Ref_II_N_R
  | opcode=OPCODE_LD r=rRegister2 SEP_COMMA d=rDisplacement                     # instrXD_R_Ref_II_N

  | opcode=OPCODE_INC d=rDisplacement                                           # instrXD_Ref_II_N
  | opcode=OPCODE_DEC d=rDisplacement                                           # instrXD_Ref_II_N
  | opcode=OPCODE_ADD REG_A SEP_COMMA d=rDisplacement                           # instrXD_Ref_II_N
  | opcode=OPCODE_ADC REG_A SEP_COMMA d=rDisplacement                           # instrXD_Ref_II_N
  | opcode=OPCODE_SBC REG_A SEP_COMMA d=rDisplacement                           # instrXD_Ref_II_N
  | opcode=OPCODE_SUB d=rDisplacement                                           # instrXD_Ref_II_N
  | opcode=OPCODE_AND d=rDisplacement                                           # instrXD_Ref_II_N
  | opcode=OPCODE_XOR d=rDisplacement                                           # instrXD_Ref_II_N
  | opcode=OPCODE_OR d=rDisplacement                                            # instrXD_Ref_II_N
  | opcode=OPCODE_CP d=rDisplacement                                            # instrXD_Ref_II_N

  | opcode=OPCODE_INC ii=rII_HL                                                 # instrXD_IIHL
  | opcode=OPCODE_DEC ii=rII_HL                                                 # instrXD_IIHL
  | opcode=OPCODE_ADD REG_A SEP_COMMA ii=rII_HL                                 # instrXD_IIHL
  | opcode=OPCODE_ADC REG_A SEP_COMMA ii=rII_HL                                 # instrXD_IIHL
  | opcode=OPCODE_SBC REG_A SEP_COMMA ii=rII_HL                                 # instrXD_IIHL
  | opcode=OPCODE_SUB ii=rII_HL                                                 # instrXD_IIHL
  | opcode=OPCODE_AND ii=rII_HL                                                 # instrXD_IIHL
  | opcode=OPCODE_XOR ii=rII_HL                                                 # instrXD_IIHL
  | opcode=OPCODE_OR ii=rII_HL                                                  # instrXD_IIHL
  | opcode=OPCODE_CP ii=rII_HL                                                  # instrXD_IIHL

  | opcode=OPCODE_INC ii=rII                                                    # instrXD_II
  | opcode=OPCODE_DEC ii=rII                                                    # instrXD_II
  | opcode=OPCODE_POP ii=rII                                                    # instrXD_II
  | opcode=OPCODE_JP SEP_LPAR ii=rII SEP_RPAR                                   # instrXD_II
  | opcode=OPCODE_LD REG_SP SEP_COMMA ii=rII                                    # instrXD_II
  | opcode=OPCODE_EX SEP_LPAR REG_SP SEP_RPAR SEP_COMMA ii=rII                  # instrXD_II
  | opcode=OPCODE_PUSH ii=rII                                                   # instrXD_II

  | opcode=OPCODE_LD SEP_LPAR nn=rExpression SEP_RPAR SEP_COMMA r=REG_HL        # instrRef_NN_R
  | opcode=OPCODE_LD SEP_LPAR nn=rExpression SEP_RPAR SEP_COMMA r=REG_A         # instrRef_NN_R

  | opcode=OPCODE_LD rp=REG_HL SEP_COMMA SEP_LPAR nn=rExpression SEP_RPAR       # instrRP_Ref_NN // x=0, z=2, q=1, p=2

  | opcode=OPCODE_LD r=REG_A SEP_COMMA SEP_LPAR nn=rExpression SEP_RPAR         # instrR_Ref_NN  // x=0, z=2, q=1, p=3

  | opcode=OPCODE_LD REG_A SEP_COMMA SEP_LPAR rp=(REG_BC|REG_DE) SEP_RPAR       # instrA_Ref_RP // x=0, z=2, q=1, p=rp

  | opcode=OPCODE_LD SEP_LPAR rp=(REG_BC|REG_DE) SEP_RPAR SEP_COMMA REG_A       # instrRef_RP   // x=0, z=2, q=0, p=rp
  | opcode=OPCODE_JP SEP_LPAR rp=REG_HL SEP_RPAR                                # instrRef_RP   // x=3, z=1, q=1, p=2
  | opcode=OPCODE_JP rp=REG_HL                                                  # instrRef_RP   // x=3, z=1, q=1, p=2

  | opcode=OPCODE_EX SEP_LPAR dst=REG_SP SEP_RPAR SEP_COMMA src=REG_HL          # instrRef_RP_RP // x=3, z=3, y=4

  | opcode=OPCODE_LD dst=rRegister SEP_COMMA src=rRegister                      # instrR_R      // x=1, y=dst, z=src
  | opcode=OPCODE_LD r=rRegister SEP_COMMA n=rExpression                        # instrR_N      // x=0, z=6, y=r

  | opcode=OPCODE_JR c=(COND_NZ|COND_Z|COND_NC|COND_C) SEP_COMMA n=rExpression  # instrC_N    // x=0, z=0, y=4..7

  | opcode=OPCODE_RET c=cCondition                                              # instrC        // x=3, z=0, y=cc

  | opcode=OPCODE_JP c=cCondition SEP_COMMA nn=rExpression                      # instrC_NN     // x=3, z=2, y=cc
  | opcode=OPCODE_CALL c=cCondition SEP_COMMA nn=rExpression                    # instrC_NN     // x=3, z=4, y=cc

  | opcode=OPCODE_LD rp=rRegPair SEP_COMMA nn=rExpression                       # instrRP_NN  // x=0, z=1, q=0, p=rp

  | opcode=OPCODE_EX dst=REG_AF SEP_COMMA src=REG_AFF                           # instrRP_RP  // x=0, z=0, y=1
  | opcode=OPCODE_LD dst=REG_SP SEP_COMMA src=REG_HL                            # instrRP_RP    // x=3, z=1, q=1, p=3
  | opcode=OPCODE_EX dst=REG_DE SEP_COMMA src=REG_HL                            # instrRP_RP    // x=3, z=3, y=5

  | opcode=OPCODE_JP nn=rExpression                                             # instrNN       // x=3, z=3, y=0
  | opcode=OPCODE_CALL nn=rExpression                                           # instrNN       // x=3, z=5, q=1, p=0

  | opcode=OPCODE_DJNZ n=rExpression                                            # instrN      // x=0, z=0, y=2
  | opcode=OPCODE_JR n=rExpression                                              # instrN      // x=0, z=0, y=3
  | opcode=OPCODE_OUT SEP_LPAR n=rExpression SEP_RPAR SEP_COMMA REG_A           # instrN        // x=3, z=3, y=2
  | opcode=OPCODE_IN REG_A SEP_COMMA SEP_LPAR n=rExpression SEP_RPAR            # instrN        // x=3, z=3, y=3
  | opcode=OPCODE_ADD REG_A SEP_COMMA n=rExpression                             # instrN        // x=3, z=6, y=alu
  | opcode=OPCODE_ADC REG_A SEP_COMMA n=rExpression                             # instrN        // x=3, z=6, y=alu
  | opcode=OPCODE_SUB n=rExpression                                             # instrN        // x=3, z=6, y=alu
  | opcode=OPCODE_SBC REG_A SEP_COMMA n=rExpression                             # instrN        // x=3, z=6, y=alu
  | opcode=OPCODE_AND n=rExpression                                             # instrN        // x=3, z=6, y=alu
  | opcode=OPCODE_XOR n=rExpression                                             # instrN        // x=3, z=6, y=alu
  | opcode=OPCODE_OR n=rExpression                                              # instrN        // x=3, z=6, y=alu
  | opcode=OPCODE_CP n=rExpression                                              # instrN        // x=3, z=6, y=alu
  | opcode=OPCODE_RST n=rExpression                                             # instrN        // x=3, z=7, y=N/8

  | opcode=OPCODE_ADD REG_HL SEP_COMMA rp=rRegPair                              # instrRP     // x=0, z=1, q=1, p=rp
  | opcode=OPCODE_INC rp=rRegPair                                               # instrRP       // x=0, z=3, q=0, p=rp
  | opcode=OPCODE_DEC rp=rRegPair                                               # instrRP       // x=0, z=3, q=1, p=rp

  | opcode=OPCODE_POP rp2=rRegPair2                                             # instrRP2      // x=3, z=1, q=0, p=rp2
  | opcode=OPCODE_PUSH rp2=rRegPair2                                            # instrRP2      // x=3, z=5, q=0, p=rp2

  | opcode=OPCODE_ADD REG_A SEP_COMMA r=rRegister                               # instrR        // x=2, y=alu, z=r
  | opcode=OPCODE_ADC REG_A SEP_COMMA r=rRegister                               # instrR        // x=2, y=alu, z=r
  | opcode=OPCODE_SUB r=rRegister                                               # instrR        // x=2, y=alu, z=r
  | opcode=OPCODE_SBC REG_A SEP_COMMA r=rRegister                               # instrR        // x=2, y=alu, z=r
  | opcode=OPCODE_AND r=rRegister                                               # instrR        // x=2, y=alu, z=r
  | opcode=OPCODE_XOR r=rRegister                                               # instrR        // x=2, y=alu, z=r
  | opcode=OPCODE_OR r=rRegister                                                # instrR        // x=2, y=alu, z=r
  | opcode=OPCODE_CP r=rRegister                                                # instrR        // x=2, y=alu, z=r
  | opcode=OPCODE_INC r=rRegister                                               # instrR        // x=0, z=4, y=r
  | opcode=OPCODE_DEC r=rRegister                                               # instrR        // x=0, z=5, y=r

  | opcode=OPCODE_NOP                                                           # instr         // x=0, z=0, y=0
  | opcode=OPCODE_RLCA                                                          # instr         // x=0, z=7, y=0
  | opcode=OPCODE_RRCA                                                          # instr         // x=0, z=7, y=1
  | opcode=OPCODE_RLA                                                           # instr         // x=0, z=7, y=2
  | opcode=OPCODE_RRA                                                           # instr         // x=0, z=7, y=3
  | opcode=OPCODE_DAA                                                           # instr         // x=0, z=7, y=4
  | opcode=OPCODE_CPL                                                           # instr         // x=0, z=7, y=5
  | opcode=OPCODE_SCF                                                           # instr         // x=0, z=7, y=6
  | opcode=OPCODE_CCF                                                           # instr         // x=0, z=7, y=7
  | opcode=OPCODE_HALT                                                          # instr         // x=1, z=6, y=6
  | opcode=OPCODE_RET                                                           # instr         // x=3, z=1, q=1, p=0
  | opcode=OPCODE_EXX                                                           # instr         // x=3, z=1, q=1, p=1
  | opcode=OPCODE_DI                                                            # instr         // x=3, z=3, y=6
  | opcode=OPCODE_EI                                                            # instr         // x=3, z=3, y=7
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

rRegister2:
  REG_B
  | REG_C
  | REG_D
  | REG_E
  | REG_H
  | REG_L
  | REG_A
  ;

rRegisterII:
  REG_B
  | REG_C
  | REG_D
  | REG_E
  | REG_IXH
  | REG_IXL
  | REG_IYH
  | REG_IYL
  | REG_A
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

rII: REG_IX | REG_IY;
rII_HL: REG_IXH | REG_IYH | REG_IXL | REG_IYL;

rDisplacement: SEP_LPAR ii=rII '+' n=rExpression SEP_RPAR;

rPseudoCode:
  PREP_ORG expr=rExpression                                                                # pseudoOrg
  | id=ID_IDENTIFIER PREP_EQU expr=rExpression                                             # pseudoEqu
  | id=ID_IDENTIFIER PREP_VAR expr=rExpression                                             # pseudoVar
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
  | instr=rInstruction
  ;

rDWdata:
  expr=rExpression
  ;

rExpression:
 SEP_LPAR expr=rExpression SEP_RPAR                                                              # exprParens
 | <assoc=right> unaryop=(OP_ADD|OP_SUBTRACT|OP_NOT|OP_NOT_2) expr=rExpression                   # exprUnary
 | <assoc=left> expr1=rExpression op=(OP_MULTIPLY|OP_DIVIDE|OP_MOD|OP_MOD_2) expr2=rExpression   # exprInfix
 | <assoc=left> expr1=rExpression op=(OP_ADD|OP_SUBTRACT) expr2=rExpression                      # exprInfix
 | <assoc=left> expr1=rExpression op=(OP_SHL|OP_SHR|OP_SHR_2|OP_SHL_2) expr2=rExpression         # exprInfix
 | <assoc=right> expr1=rExpression op=(OP_GT|OP_GTE|OP_LT|OP_LTE) expr2=rExpression              # exprInfix
 | <assoc=right> expr1=rExpression op=OP_EQUAL expr2=rExpression                                 # exprInfix
 | <assoc=left> expr1=rExpression op=OP_AND expr2=rExpression                                    # exprInfix
 | <assoc=left> expr1=rExpression op=OP_XOR expr2=rExpression                                    # exprInfix
 | <assoc=left> expr1=rExpression op=OP_OR expr2=rExpression                                     # exprInfix
 | num=LIT_NUMBER                                                                                # exprDec
 | num=LIT_HEXNUMBER_1                                                                           # exprHex1
 | num=LIT_HEXNUMBER_2                                                                           # exprHex2
 | num=LIT_OCTNUMBER                                                                             # exprOct
 | num=LIT_BINNUMBER                                                                             # exprBin
 | PREP_ADDR                                                                                     # exprCurrentAddress
 | id=ID_IDENTIFIER                                                                              # exprId
 | str=(LIT_STRING_1|LIT_STRING_2)                                                               # exprString
 ;
