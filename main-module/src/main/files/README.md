# Welcome to emuStudio

emuStudio is a desktop application which allows to write programs and emulate computers.
In addition, it is easily extensible - acting as a framework it encourages developers to write their computer emulators.
It is versatile, free, and written in Java.

The main goal is to support learning about older but important computers or abstract machines.
I think it is also appropriate for schools; for example, emuStudio is used at
[Technical University of Košice](http://www.fei.tuke.sk/en) since 2007.

# License

This project is released under GNU GPL v2 license.

# Supported host platform

Currently supported are Linux and Windows. Mac is NOT supported, but it might work to some extent.

# Available computer emulators

* MITS Altair8800 with two processor choices: Intel 8080 or Zilog Z80 CPU. Includes 88-DISK drive, 88-SIO board,
  LSI ADM-3A terminal, SIMH pseudo device (partially reimplemented from simh emulator), operating memory,
  and 8080 + Z80 assembler compilers

* Manchester Small-Scale Experimental Machine (SSEM) emulator, with programming tutorials

* Random Access Machine (RAM) simulator

* Random Access Stored Program (RASP) machine simulator

* Brainfuck interpreter (designed as a "computer" which I called BrainDuck)

# Installation

Please follow the instructions on [project's web page](https://vbmacher.github.io/emuStudio/download/).
To run the project from the command line, type the following:

        java -jar "emuStudio.jar" 

