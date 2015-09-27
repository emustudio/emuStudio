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

* Computer schema designer / editor
* Simple IDE
    - Source code editor with syntax highlighting
    - Ability to compile and directly transfer code to emulator
    - CPU debugger with interactive disassembler
* Components of a computer are independent plug-ins like CPU, memory and devices
    - Operating memory and devices can have their own GUI windows
* Automatic emulation run from command line
* Several predefined computer sets and plug-ins
* Consistent API for development of custom plug-ins

# Predefined computers, and hardware

* MITS Altair8800 including Intel 8080 or Zilog Z80 CPU, 88-DISK drive, 88-SIO board,
  LSI ADM-3A terminal, SIMH pseudo device, operating memory, including assembler compilers

* Random Access Machine simulator

* Brainfuck simulator (I call it BrainDuck)

# Compiling

The project is using Maven for managing dependencies and build process. Each module can be compiled separately,
but if you run the following command in the root directory, it will compile each module (results will be in particular
module directory):

```
mvn clean install
```

## Preparing emuStudio distribution

In order to package complete emuStudio with examples and all predefined computers, please go to subdirectory `release` and then
invoke the following command:

```
mvn clean install -P release
```

Inside `target` subdirectory will be a zip file containing full distribution of emuStudio.

# Running / installation

There are two ways of how to run emuStudio. In both cases, a prerequisite is to have a full distribution of emuStudio.
You can either download it from [project's web page](http://emustudio.sourceforge.net/downloads.html),
or you can prepare the distribution by yourself as was explained in the previous section.

## The "classic" way

The requirement is to have installed Java Runtime Environment (JRE) 1.8.

Unzip the emuStudio distribution zip file (`emuStudio-xxx-release.zip`) and run it using command:

```
java -jar emuStudio.jar
```

## Running using Vagrant

It is also possible to use [vagrant](https://www.vagrantup.com/) in order to run emuStudio. It works only when emuStudio
distribution is prepared manually.

After building the release, in the root directory of emuStudio source, run a script (now only for bash (Linux)):

```
bash> ./emuStudio-vagrant.sh
```

The script does this:

1. Bring up virtual machine using vagrant
2. Synchronize `release/target/` subdirectory with the machine,
3. Unzip the distribution zip file into VM's `/emustudio/` directory
4. Run emuStudio from there through SSH (with X forwarding). 

I see this very beneficial for developers, because this way is much faster and cleaner, especially when developers
want to try their changes. 

# License

This project is released under GNU GPL v2 license.
