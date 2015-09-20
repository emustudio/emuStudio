# Welcome to emuStudio
[![Build Status](https://travis-ci.org/vbmacher/emuStudio.png)](https://travis-ci.org/vbmacher/emuStudio)
[![Coverage Status](https://coveralls.io/repos/vbmacher/emuStudio/badge.png?branch=branch-0_39)](https://coveralls.io/r/vbmacher/emuStudio?branch=branch-0_39)

emuStudio is free and versatile platform/framework for *doing* the computer emulation. It is specially designed for
students and programmers which want to learn how computers work while having fun in the same time. It has been used
at Technical University of Košice (Slovakia) with very good responses of students and teachers since 2007.

Computer emulation has been always fun, since it's a process of "building a machine" in front of you, no matter if you
are a professional or amateur. When finished, it is then able to run programs or games written by other people, while
these programs don't even recognize they are being run in *your* software, not on a hardware.

There are many tutorials for emulation of various CPUs or other hardware and they might be useful when
implementing them using emuStudio.

From the user perspective, emuStudio is a desktop application which can emulate some predefined virtual computer.
The computers consist of components, which are separate modules and can be programmed independently. User just
select those components, connect them with lines into a schema, and the computer is ready for emulation.

# Features highlight

* Written in Java

* Main module with Graphical User Interface (GUI)

* Simple computer schema editor

* Source code editor with support of syntax highlighting

* CPU debugger with basic and advanced abilities

* Operating memory and devices can have their own GUI windows

* Automatic emulation run from command line

* Several predefined computer sets and plug-ins

* Consistent API for development of custom plug-ins

# Predefined computers, and hardware

* MITS Altair8800 including Intel 8080 or Zilog Z80 CPU, 88-DISK drive, 88-SIO board,
  LSI ADM-3A terminal, SIMH pseudo device, operating memory, including assembler compilers

* Random Access Machine simulator

* Brainfuck simulator

# Compiling

The project is using Maven for managing dependencies and build process. Each module can be compiled separately,
but if you run the following command in the root directory, it will compile each module (results will be in particular
module directory):

```
mvn clean install
```

In order to package complete emuStudio with examples and all plug-ins, please go to subdirectory `release` and then
invoke the following command:

```
mvn clean install -P release
```

Inside `target` subdirectory will be a zip file containing full distribution of emuStudio.

# Running / installation

Prerequisite is to have a full distribution of emuStudio, which can be either downloaded from [project's web page](http://emustudio.sourceforge.net/downloads.html),
or when the project was compiled, in a project subdirectory `release/target/emuStudio-xxx-release.zip`.

Requirement is to have installed Java Runtime Environment (JRE) 1.8.

Then unzip the emuStudio distribution file, and run it using command:

```
java -jar emuStudio.jar
```

# License

This project is released under GNU GPL v2 license.
