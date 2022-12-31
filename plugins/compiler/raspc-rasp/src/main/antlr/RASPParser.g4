/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
