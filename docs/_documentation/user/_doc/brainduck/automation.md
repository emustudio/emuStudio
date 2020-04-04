---
layout: default
title: Automation
nav_order: 1
parent: BrainDuck
permalink: /brainduck/automation
---

# Automation

BrainDuck is one of computers which allows automatic emulation. It means that it is possible to run the emulation from command line, while all necessary input and output is redirected from/to files. If user interaction is necessary, it is possible to run an interactive automation.

Suppose the BrainDuck computer is represented by abstract schema shown in `brainduck-intro` document. In that case, BrainDuck terminal is the only device dealing with I/O. If the emulation was executed in automatic non-interactive mode, it will recognize it and the input/output will be redirected from/to files.

Input file is called `brainduck-terminal.in` and must be placed in the directory from which emuStudio was executed. If the file does not exist, emuStudio will not run.

Output file is called `brainduck-terminal.out` and it will be created automatically, or appended when it exists in the location from which emuStudio was executed.

NOTE: Take care about end-of-line characters. Most of brainfuck programs count with Unix-like EOLs, i.e. characters with ASCII code 10. plugin `brainduck-terminal` interprets ENTER key in the interactive mode as Unix-like EOL. In the non-interactive mode, EOL may be of any-like type.

Command line for starting non-interactive automatic emulation:

    ./emuStudio --auto --config config/BrainDuck.toml --input examples/brainc-brainduck/mandelbrot.b


- computer configuration `config/BrainDuck.toml` will be loaded
- input file for compiler is one of the examples
- (`--auto`) automatic emulation mode will be performed
