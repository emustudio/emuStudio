# ![emuStudio logo](resources/logo-white.png "emuStudio logo") Welcome to emuStudio

![emuStudio Build](https://github.com/emustudio/emuStudio/workflows/emuStudio%20Build/badge.svg)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

[emuStudio](https://www.emustudio.net/) is a desktop application used for computer emulation and writing programs
for emulated computers. It extensible; it encourages developers to write their own computer emulators.

The main goal of emuStudio is to support the "compile-load-emulate" workflow, aiming at students or anyone to help to
learn about older but important computers or even abstract machines.

emuStudio is very appropriate for use at schools, e.g. when students are doing first steps in assembler, or when they
are taught about computer history. For example, emuStudio is used at
the [Technical University of Košice](http://www.fei.tuke.sk/en)
since 2007.

## Available emulators

* [MITS Altair8800](https://www.emustudio.net/documentation/user/altair8800/)

* [Manchester SSEM](https://www.emustudio.net/documentation/user/ssem/)

* [Random Access Machine (RAM)](https://www.emustudio.net/documentation/user/ram/)

* [Random Access Stored Program (RASP)](https://www.emustudio.net/documentation/user/rasp/)

* [BrainDuck (brainfuck interpreter)](https://www.emustudio.net/documentation/user/brainduck/)

## BIG THANKS

emuStudio was written based on existing emulators, sites and existing documentation of real hardware. For example:

Projects:
- [simh](http://simh.trailing-edge.com/) project, which was the main inspiration for Altair8800 computer
- [MAME](https://www.mamedev.org/) project, which helped with resolving a lot of bugs in a correct implementation of
  some 8080 and Z80 CPU instructions

Sites:
- [David Sharp's SSEM site](https://www.davidsharp.com/baby/), main inspiration for SSEM implementation
- [Esolang's BrainFuck site](https://esolangs.org/wiki/Brainfuck), main inspiration for Brainfuck implementation
- [DeRamp Altair](https://deramp.com/altair.html), more inspiration for Altair8800
- [Altair Clone](https://altairclone.com/), more inspiration for Altair8800
- [Study of techniques for emulation programming](http://www.xsim.com/papers/Bario.2001.emubook.pdf), emulation techniques classic
- [8080 instruction table](https://tobiasvl.github.io/optable/intel-8080/classic)

Discord:
- [Discord Emulation Development](https://discord.com/channels/465585922579103744/channel-browser)

## Getting started

At first, either compile or [download](https://www.emustudio.net/download/) emuStudio.
The prerequisite is to have installed **Java, at least version 11**
(download [here](https://www.oracle.com/java/technologies/javase-downloads.html)).

Then, unzip the tar/zip file (`emuStudio-xxx.zip`) and run it using command:

- On Linux / Mac

```
> ./emuStudio
```

- On Windows:

```
> emuStudio.bat
```

NOTE: Currently supported are Linux and Windows. Mac is NOT supported, but it might work to some extent.

For more information, please read [user documentation](https://www.emustudio.net/documentation/user/introduction/).

## Contributing

Anyone can contribute. Before start, please read
[developer documentation](https://www.emustudio.net/documentation/developer/introduction/),
which includes information like:

- Which tools to use and how to set up the environment
- How to compile emuStudio and prepare local releases
- Which git branch to use
- Code architecture, naming conventions, best practices

### Related projects

There exist some additional projects, which are used by emuStudio, useful for contributors:

- [emuLib](https://github.com/emustudio/emuLib) - a shared runtime library
- [Edigen](https://github.com/emustudio/edigen) - instruction decoder and disassembler generator
- [Edigen Gradle plugin](https://github.com/emustudio/edigen-gradle-plugin) - Edigen Gradle plugin
- [CPU testing suite](https://github.com/emustudio/cpu-testsuite) - a JUnit-based test suite for comfortable testing of CPU
  plugins
- [emuStudio website](https://github.com/emustudio/emustudio.github.io) - emuStudio website
