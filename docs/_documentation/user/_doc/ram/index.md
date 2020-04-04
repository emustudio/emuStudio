---
layout: default
title: RAM
nav_order: 4
has_children: true
permalink: /ram/
---

# Random Access Machine (RAM)

Random Access Machine (RAM) is an abstract machine, invented to study algorithmic complexity of programs written on register-based computers. It is equivalent to Turing machine, and has a close relationship with a so-called [Harvard computer architecture][harvard]{:target="_blank"}, which has separated storage for program and data. The implication of this model is that it is not possible to modify instructions.

RAM machine consists of several parts: input tape (read-only), output tape (write-only), program memory,
data memory or registers (read/write) and a control unit ("engine"), as can be seen in the following image:

![RAM machine]({{ site.baseurl }}/assets/ram/ram-machine.svg){:.img-responsive width="85%"}

Input tape acts as a water-tap; the input data can be read from it, causing the input head moving to the next unread symbol. The head can never return to previously read symbol.

Output tape, on the other hand, acts as a sink. The output data can be written to it, causing the output head moving to the next "empty" symbol. The head can also never return to the previously written symbol.

Data memory - registers tape - represents the random-access memory. It consists of so-called registers, abstract cells with arbitrary size. These registers are ordered - each one has assigned the index - its position within the tape, called the _address_. The tape head can move arbitrarily up and down - but it has its minimum position. It is the first register, _R~0~_, called the accumulator. Below there are unlimited number of higher-positioned registers.

The role of accumulator is kind of special - it often acts as an implicit operand for many instructions, or implicit place for storing the result of such instructions.

Program memory is a bounded ordered sequence of registers; each of them is identified by its index within the tape, called address. Data memory is also ordered sequence of registers, but like the I/O tapes - bounded just from one side.

Since RAM machine is somewhat abstract, it frees the user from thinking about some issues, and just assumes that:

- The size of the problem is always small enough to fit in the RAM memory,
- Data used within the computation are always small enough to fit in one register.

The RAM virtual machine in emuStudio consists of the following plugins:

- `ramc-ram`: Compiler of the RAM language, very simple "assembler"-like language
- `ram-cpu`: RAM simulator engine
- `ram-mem`: Program memory
- `abstractTape-ram`: Device which represents the "tape" used in RAM, other than program memory. The abstract schema
                      must define three instances of this device, representing register, input and output tapes.

## RAM in emuStudio

In order to use RAM, there must exist the abstract schema of the "computer", saved in the configuration file. Abstract schemas are drawn in the schema editor in emuStudio (please see emuStudio main module documentation for more details). The following image shows the schema of RAM machine simulator:

![RAM abstract schema]({{ site.baseurl }}/assets/ram/ram-schema.png){:.img-responsive width="405px"}

The "->" arrows are in direction of dependency. So for example `ramc-ram` depends on `ram-mem`, because compiled programs are directly loaded into memory.

The roles of the abstract tapes are assigned by the RAM "CPU" on runtime.


[harvard]: https://en.wikipedia.org/wiki/Harvard_architecture
[vonneumann]: https://en.wikipedia.org/wiki/Von_Neumann_architecture
