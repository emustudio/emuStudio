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
  rInstruction
  | rInput
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
