---
layout: default
title: Compiler "ramc-ram"
nav_order: 2
parent: RAM
permalink: /ram/ramc-ram
---

# Compiler "ramc-ram"

RAM has a very simple assembler-like language, consisting of direct and indirect reading/writing from/to registers or input/output tape. In addition, there are three control-flow instructions.

## Language syntax

The program written for this compiler consists of two sections:


    INPUT section
    INSTRUCTIONS section

### Input section

The `INPUT` section contains definitions the content of input tape - one or more lines in the form:

    <input> ITEMS

where `ITEMS` is a space-separated list of inputs. Each input is one word - it might be any number or string. By default, every cell in the input tape is a string, and it is not interpreted as some data type. It is only in time when it is used - the instruction which works with the cell defines of which "type" it should be.

For example, input section might be:

    <input> 1 2 3 hello world!

In this case, there are five inputs: numbers 1,2,3, then word "hello" and the last one is "world!".

### Instructions section

There exist many possible formats or variations of RAM instructions, unfortunately the syntax is not very unified. I guess the reason is that RAM is not a real machine, and for the purposes of the algorithm analysis the machine is so simple that it's description is repeated in almost every paper where it appears.

For this reason, instructions format or the whole vocabulary might be different of what you expected or used for. We have to live with it; but the differences are really small.

Instructions should follow the Input section, but the sections can be mixed. It is just good practice to have input separated from the code. Each instruction must be on separate line, in the form:

    [LABEL:] INSTRUCTION [; optional comment]

Each instruction position can be optionally labelled with some identifier (`LABEL` field), followed by a colon (`:`) character. The labels can be then referred in other instructions.

Comments begin with a semicolon (`;`) character and continue to the end of the line. There are no multi-line comments.

Instructions consists of the operation code and optional operand, separated with space (` `).

Operation code is expressed as an abbreviation of corresponding operation (e.g. `SUB` for SUBtraction). Operand can be one of three types: constant (`=i`), direct operand (`i`), where `i` specifies the register index on tape and indirect operand (`*i`), where the address of operand specified is stored in register _R~i~_.

The following table describes all possible instructions, usable in the RAM simulator:

|---
| Instruction | Constant (`=i`)        | Direct (`i`)              | Indirect (`*i`) 
|-|-|-|-
| `READ`      |                        | _R~i~_ <- next input      |
| `WRITE`     | output <- _i_          | output <- _R~i~_          | output <- _M<R~i~>_
| `LOAD`      | _R~0~_ <- _i_          | _R~0~_ <- _R~i~_          | _R~0~_ <- _M<R~i~>_
| `STORE`     |                        | _R~i~_ <- _R~0~_          | _M<R~i~>_ <- _R~0~_
|---
| `ADD`       | _R~0~_ <- _R~0~_ + _i_ | _R~0~_ <- _R~0~_ + _R~i~_ | _R~0~_ <- _R~0~_ + _M<R~i~>_
| `SUB`       | _R~0~_ <- _R~0~_ - _i_ | _R~0~_ <- _R~0~_ - _R~i~_ | _R~0~_ <- _R~0~_ - _M<R~i~>_
| `MUL`       | _R~0~_ <- _R~0~_ * _i_ | _R~0~_ <- _R~0~_ * _R~i~_ | _R~0~_ <- _R~0~_ * _M<R~i~>_
| `DIV`       | _R~0~_ <- _R~0~_ / _i_ | _R~0~_ <- _R~0~_ / _R~i~_ | _R~0~_ <- _R~0~_ / _M<R~i~>_
|---
| `JMP`       |                        | _IP_ <- _i_               |
| `JZ`        |                        | *if* _R~0~_ == 0 *then* _IP_ <- _i_ |
| `JGTZ`      |                        | *if* _R~0~_ > 0 *then* _IP_ <- _i_  |
|---
| `HALT`      |                        | halts the simulation      |
|---

The table describes also the behavior of each instruction. Compiler does not care about the behavior, but about the instructions syntax, which is also incorporated in the table.

For example, this is a valid program:

```
; COPY(X,Y)
;
; input:  X -> r1
;         Y -> r2
;
; output: X -> Y
;         Y -> Y

<input> 3 4 world hello

<input> sss
; load X,Y
read 1
read 2

; load r.X, r.Y
read *1
read *2

; copy
load *2
store *1

halt
```
