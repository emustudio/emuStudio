---
layout: default
title: CPU "rasp-cpu"
nav_order: 2
parent: RASP
permalink: /rasp/rasp-cpu
---

# CPU "rasp-cpu"

RASP CPU is the core of the RASP virtual computer. Its purpose is to execute RASP program composed of RASP instructions which is stored in the RASP memory. The CPU is therefore connected with the memory. 

To run 'interactive' RASP programs (programs reading user input and writing something to the output), input and output tapes are needed. They are included there in default RASP configuration.

## Supported instructions

RASP CPU emulator supports the following instructions:

|---
|Operation code | Instruction mnemonic code | Semantics
|-|-|-
|`1` |`READ i` |read from input tape into register `Ri`
|`2` |`WRITE =i` |write constant `i` onto the output tape
|`3` |`WRITE i` |write the content of the register `Ri` onto the output tape
|`4` |`LOAD =i` |load the accumulator (register `R0`) with the `i` constant
|`5` |`LOAD i` |load the accumulator (register `R0`) with the content of `Ri` register
|`6` |`STORE i` |store the accumulator (register `R0`) content into register `Ri`
|`7` |`ADD =i` |increase accumulator (register `R0`) value by constant `i`
|`8` |`ADD i` |increase accumulator (register `R0`) value by the value of register `Ri`
|`9` |`SUB =i` |decrease accumulator (register `R0`) value by constant `i`
|`10` |`SUB i` |decrease accumlator (register R0) by the value of register `Ri`
|`11`|`MUL =i` |multiply accumulator (register R0) by constant `i`
|`12` |`MUL i` |multiply accumulator (register R0) by the value of register `Ri`
|`13` |`DIV =i` |divide accumulator (register R0) by constant `i`
|`14` |`DIV i` |divide accumulator (register R0) by the value of register `Ri`
|`15` |`JMP l` |set instruction pointer (`IP`) to the address pointed to by the label `l`
|`16` |`JZ l` |set instruction pointer (`IP`) to the address pointed to by the label `l` if value of the accumulator (register R0) is zero (`R0 = 0`)
|`17` |`JGTZ l` |set instruction pointer (`IP`) to the address pointed to by the label `l` if value of the accumulator (register R0) is greater than zero (`R0 > 0`)
|`18` |`HALT` |finish program execution
|---

## CPU Status panel

There is a simple GUI window provided for the RASP CPU. It displays the two most important values: 

- current value of the accumulator (`R0` register)
- current value of the instruction pointer (`IP`) which points to current position within the executed program 

Also, information about the current RUNNING STATUS is displayed.

Following figure shows a screenshot:

![RASP CPU status panel]({{ site.baseurl }}/assets/rasp/statusPanel.png)
