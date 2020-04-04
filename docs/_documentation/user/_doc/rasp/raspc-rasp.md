---
layout: default
title: Compiler "raspc-rasp"
nav_order: 1
parent: RASP
permalink: /rasp/raspc-rasp
---

# Compiler "raspc-rasp"

RASP compiler is included in the architecture of RASP virtual computer in emuStudio. Its purpose is to translate source code of a RASP program into the form executable by the RASP CPU emulator. 

It includes lexical analyzer which is responsible for recognising lexical units within the source code. It also enables syntax highlighting. Then, the code goes through syntax analysis which checks for syntax errors and builds up the syntactic tree. The tree is then passed through and binary code executable by the emulator is generated. 

As with the other emuStudio compilers, you start the compilation by the "Compile source" icon in the main menu. The result of the compilation is saved into a binary file with the `.bin` extension and also loaded into the RASP operating memory. 

## Language syntax

### Program start definition

As RASP is a von-Neumann computer, both the program and the data reside in the same memory module. Therefore, we need to clearly specify, where the program start address is as CPU must know where to start emulation. We do so by the `org` directive followed by a positive ineteger. Program start definition *must* be the *first line* of the source code. Please, *do not* place any *empty lines* before it. Here, an example follows:

`org 5`

WARNING: If you do not specify the program start address, or if you set it to `0` (`org 0`), the default pre-set value will be used, which is `20`. The compiler will warn you about this, so do not worry. Setting program start to `0` is not allowed as register `R0` is used as the *accumulator* and therefore any instruction written here would be overwritten sooner or later.

### Supported instructions

All RASP emulator supported instructions together with their semantics are available in the RASP CPU documentation. There are three types of operands of those instructions:

- register - e.g. `READ 1`
- constant - e.g. `WRITE =2` - you specify that operand of an instruction should be interpreted as a constant by the `=` character.
- label, which is operand of jump instructions (`JMP`, `JZ`, `JGTZ`), e.g. `JMP *label*`

### Line of code structure

Each RASP instruction with its operand *MUST* be on a separate line, otherwise the code will not compile. 

A single line of code consists of *instruction* followed by its *operand*. The line can be optionally started with a *label*. It is possible to put the label and the instruction on two separate lines, however, please, *do not* place any *empy lines* between the label and the instruction.

Example (`WRITE 2` can be on a separate line):

`*labelName:* WRITE 2`

### Comments

RASP compiler supports one-line comments. You start them with a semicolon (`;`):

`;this is a comment`

You can append a comment to an existing line, e.g.

`write 2 _;comment_`

or put it on a completely new line.




