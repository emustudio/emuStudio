---
layout: default
title: BrainDuck
nav_order: 3
has_children: true
permalink: /brainduck/
---

# BrainDuck

BrainDuck is an abstract computer for emuStudio, which mimics [brainfuck][brainfuck]{:target="_blank"} programming language. Originally, brainfuck was developed by [Urban Miller][miller]{:target="_blank"} and it is well-known fact that the language has minimalistic compiler and it's eight instructions don't prevent it to be Turing complete. Also, there exist many extensions of the language and there are organized programming contests in brainfuck worldwide. But all of that can be read at Wikipedia or at other sources.

BrainDuck architecture is just a name for virtual computer in emuStudio, and consists of these plugins:

- `brainc-brainduck`: Compiler of brainfuck language (original, without extensions)
- `brainduck-cpu`: Brainfuck emulator acting like CPU with two registers
- `brainduck-mem`: Virtual operating memory which holds both compiled brainfuck program and data
- `brainduck-terminal`: Virtual terminal for displaying the output and requesting for input.

BrainDuck is implemented as [von Neumann] computer. It means that program and data are shared in the same memory. This is not a common approach of implementing brainfuck interpreters, and it might be changed in the future.

As implementing a brainfuck interpreter, one must deal with several [portability issues][portability]{:target="_blank"}, which include:

- Memory cell size (solved in `brainduck-memory` and `brainduck-cpu`)
- Memory size (number of memory cells) (solved in `brainduck-memory`)
- End-of-line code (solved in `brainduck-terminal`)
- End-of-file behavior (solved in `brainduck-cpu` and `brainduck-terminal`)

The solution is spread across the plugins, as you can see in the parentheses. Chapters devoted to plugins' description contain specific information.

## BrainDuck for emuStudio

In order to use BrainDuck, there must be drawn the abstract schema, saved in the configuration file. Abstract schemas are drawn in the schema editor in emuStudio (please see emuStudio application documentation for more details). The following image shows the schema of BrainDuck computer:

![BrainDuck abstract schema]({{ site.baseurl}}/assets/brainduck/schema.png){:class="img-responsive" width="308px"}

The "->" arrows are in direction of dependency. So for example `brainc-brainduck` depends on `brainduck-mem`, because compiled programs are directly loaded into memory.

Between `brainduck-cpu` and `brainduck-terminal` exists bidirectional dependency, because input gained from terminal is passed to the CPU, and output is pushed from CPU to the terminal.

plugin `brainduck-cpu` also depends on `brainduck-mem`, because memory is a place where program and data are stored.

[brainfuck]: http://en.wikipedia.org/wiki/Brainfuck
[miller]: http://esolangs.org/wiki/Urban_M%C3%BCller
[vonneumann]: http://en.wikipedia.org/wiki/Von_Neumann_architecture
[portability]: http://en.wikipedia.org/wiki/Brainfuck#Portability_issues
