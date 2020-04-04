---
layout: default
title: Assembler "as-ssem"
nav_order: 1
parent: SSEM
permalink: /ssem/as-ssem
---

# Assembler "as-ssem"

Assembler "as-ssem" is a simple language which compiles SSEM instructions into binary output and SSEM memory.
Source code has `.ssem` file extension, and binary form has `.bin` file extension.

The instructions table follows (modified from [Wikipedia][programming]{:target="_blank"}):

|---
|Binary code |Mnemonic        |Action            |Operation
|-|-|-|-
|000         |JMP S           | S(L) -> CI       |Jump to the instruction at the address obtained from the specified memory address `S(L)` (absolute unconditional jump)
|100         |JRP / JPR / JMR S | CI + S(L) -> CI |Jump to the instruction at the program counter (`CI`) plus the relative value obtained from the specified memory address `S(L)` (relative unconditional jump)
|010         |LDN S           |-S(L) -> A       |Take the number from the specified memory address `S(L)`, negate it, and load it into the accumulator
|110         |STO S           |A -> S(L)        |Store the number in the accumulator to the specified memory address `S(L)`
|001 or 101  |SUB S           |A - S(L) -> A    |Subtract the number at the specified memory address `S(L)` from the value in accumulator, and store the result in the accumulator
|011         |CMP / SKN       |if A<0 then CI+1->CI |Skip next instruction if the accumulator contains a negative value
|111         |STP  / HLT      |Stop              |
|---

The instructions are stored in a memory, which had 32 cells. Each cell was 32 bits long, and each instruction fit into
exactly one cell. So each instruction has 32 bits. The bit representation was reversed, so the most and the least
significant bits were put on opposite sides. For example, value `3`, in common personal computers represented as `011`,
was in SSEM represented as `110`.

The instruction format is as follows:

| *Bit:*  | 00  | 01 | 02 | 03 | 04 | ... | 13 | 14 | 15 | ... | 31
| *Use:*  | L   | L  | L  | L  | L  |  0  | I  | I  | I  | 0   | 0
| *Value:*| 2^0 |    |    |    |    |     |    |    |    |     | 2^31


where bits `LLLLL` denote a "line", which is basically the memory address - index of a memory cell. It can be understood
as instruction operand. Bits `III` specify the instruction opcode (3 bits are enough for 7 instructions).

## Language syntax

### New-lines

New-line character (LF, CR, or CRLF) are delimiters of instructions, and the last character of the program.
Successive empty new-line characters will be ignored.

### Instructions

Assembler will support all forms of instructions. All instructions must start with a line number. For example:

    01 LDN 20

### Literals / constants

Raw number constants can be defined in separate lines using special preprocessor keywords. The first one is `NUM xxx`, where `xxx` is a number in either decimal or hexadecimal form. Hexadecimal format must start with prefix `0x`. For example:

    00 NUM 0x20
    01 NUM 1207943145

Another keyword is `BNUM xxx`, where `xxx` can be only a binary number. For example:

    01 BNUM 10011011111000101111110000111111

It means that the number will be stored untouched to the memory in the format as it appears in the binary form.

There exists also a third keyword, `BINS xxx`, with the exact meaning as `BNUM`.

For all constants, the following rules hold. Only integral constants are supported, and the allowed range is from 0 - 31 (maximum is 2^5).

### Comments

Only one-line comments will be supported, but of various forms. Generally, comment will be everything starting with some prefix until the end of the line. Comment prefixes are:

- Double-slash (`//`)
- Semi-colon (`;`)
- Double-dash (`--`)

## Example

For example, simple `5+3` addition can be implemented as follows:

    0 LDN 7 // load negative X into the accumulator
    1 SUB 8 // subtract Y from the value in the accumulator
    2 STO 9 // store the sum at address 7
    3 LDN 9 // A = -(-Sum)
    4 STO 9 // store sum
    5 HLT

    7 NUM 3 // X
    8 NUM 5 // Y
    9       // here will be the result

The accumulator should now contain value `8`, as well as memory cell at index 9.


[programming]: https://en.wikipedia.org/wiki/Manchester_Small-Scale_Experimental_Machine#Programming
