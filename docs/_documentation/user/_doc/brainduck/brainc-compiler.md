---
layout: default
title: Compiler "brainc-brainduck"
nav_order: 2
parent: BrainDuck
permalink: /brainduck/compiler
---

# Compiler "brainc-brainduck"

BrainDuck compiler is used as a part of BrainDuck computer, which acts as a translator of *brainfuck* "human-readable" language into binary form, used by BrainDuck CPU. Those instructions and their binary codes have no relation with brainfuck itself, therefore the computer is not called *brainfuck computer*, because it is **not** brainfuck. But it does not mean you cannot write and run brainfuck programs in it :)

At first, each compiler, including BrainDuck compiler, provides lexical analyzer for help with tokenize of the source code, used in syntax highlighting. Secondly, the compile *compiles* the source code into other (usually binary) form which is then understood by CPU.

Compilation takes part by user request (clicking on 'compile' icon in the main window). After compilation is successful, the compiler usually loads the translated program into operating memory, and saves the translation into a file. So it is with BrainDuck compiler. Files have `.hex` extension (format is called [Intel HEX][intelhex]{:target="_blank"}).

## Language Syntax

Language of BrainDuck compiler is almost identical with the original brainfuck. However, brainfuck interpreter is not specified well-enough, so there are open questions how to treat with some special situations, which are described below.

Generally, the language knows eight instructions. They are best described when they are compared with C language equivalent. Brainfuck uses only single data pointer called `P`, pointing to bounded memory. The boundary is specified in `brainduck-mem` plugin.

NOTE: BrainDuck architecture conforms to true von-Neumann model, instead of classic Harvard-style interpreters. It means that program memory and data memory are not separated. The data pointer is therefore not initialized to 0 as programmers might expect and potentially there can be written brainfuck programs with self-modifications.

|---
|Brainfuck instruction   | C language equivalent
|-|-
| `>`                    | `P++`
| `<`                    | `P--`
| `+`                    | `++*P`
| `-`                    | `--*P`
| `,`                    | `*P = getchar()`
| `.`                    | `putchar(*P);`
| `[`                    | `while (*P) {`
| `]`                    | `}`
|---


The compiler is supplied with many example programs written in brainfuck.

## Additional details

Specification of brainfuck language or interpreter implementation is not complete. There are left some details which might be solved differently in different implementations. In this version of BrainDuck implementation in emuStudio, the details are solved in the fixed way, as described below.

### Comments

The compiler takes as a comment everything which is not the brainfuck instruction. From the first occurence of unknown character, everything to the end of the line is treated as comment. Exceptions are whitespaces, tabulators, and newlines. This practically means that it is impossible to write brainfuck program with syntax errors.

In the following example, everything starting with `#` is treated as comment, up to end of the line.

    ++++[-] # Useless program in brainfuck. [-] clears the content of the memory cell.

### Cell size

A memory cell has 8-bits (cells are bytes).

### Memory size

Memory size is defined in `brainduck-mem` plugin. In this version of emuStudio, it is 65536 cells.

### End-of-line code

EOL is defined in `brainduck-terminal` plugin. In the current version of emuStudio, it is a Newline character with ASCII code 10.

### End-of-file behavior

EOF is defined in `brainduck-cpu` and `brainduck-terminal` plugins. In the current version of emuStudio, current cell (where `P` is pointing at) is changed to value 0. This is not how original brainfuck behaves, which is doing no change to the cell on EOF.

[intelhex]: http://en.wikipedia.org/wiki/Intel_HEX
