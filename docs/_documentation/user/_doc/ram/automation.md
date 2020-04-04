---
layout: default
title: Automation
nav_order: 1
parent: RAM
permalink: /ram/automation
---

# Automation

RAM is one of computers which supports automatic emulation. In general, automatic emulation can be interactive, or not interactive. In case of the RAM emulator, only non-interactive emulation is useful. It is because during emulation it is not possible to interact (e.g. pass new input to the input tape) in any way.

Changes to any abstract tapes are written to the corresponding output file (see abstract tape documentation for more information). 

Command line for starting non-interactive automatic emulation:

    ./emuStudio --config config/RAM.toml --input examples/ramc-ram/factorial.ram --auto --nogui

- configuration `config/RAM.toml` will be loaded
- input file for compiler is one of the examples
- (`--auto`) automatic emulation mode will be performed
- (`--nogui`) non-interactive version will be set

After the run, the following output on the stdout can be expected:

```
[INFO] Loading virtual computer: RAM
[INFO] All plugins were loaded successfully.
[INFO] Being verbose. Writing to file:registers_(storage_tape).out
[INFO] Being verbose. Writing to file:input_tape.out
[INFO] Being verbose. Writing to file:output_tape.out
[INFO] Starting emulation automatization...
[INFO] Compiler: RAM Compiler
[INFO] CPU: Random Access Machine (RAM)
[INFO] Memory: RAM Program Tape
[INFO] Memory size: 0
[INFO] Device #00: Abstract tape
[INFO] Device #01: Abstract tape
[INFO] Device #02: Abstract tape
[INFO] Compiling input file: examples/ramc-ram/factorial.ram
[INFO] Compiler started working.
[INFO] [Info    (000)] RAM Compiler, version 0.39-SNAPSHOT
[INFO] [Info    (000)] Compile was successful.
[INFO] [Info    (000)] Compiled file was loaded into operating memory.
[INFO] [Info    (000)] Compilation was saved to the file: factorial.ro
[INFO] Compiler finished successfully.
[INFO] Program start address: 0000h
[INFO] Resetting CPU...
[INFO] Running emulation...
[INFO] Normal stop
[INFO] Instruction position = 0011h
[INFO] Emulation completed
```

Then, in the current working directory, there will be created three new files:

- `input_tape.out`
- `registers_(storage_tape).out`
- `output_tape.out`

The format of the files is described in abstract tape documentation.
