# Welcome to emuStudio
[![Build Status](https://travis-ci.org/vbmacher/emuStudio.svg?branch=branch-0_39)](https://travis-ci.org/vbmacher/emuStudio)

emuStudio is a desktop application which allows to write programs and emulate computers.
More generally, it is a platform for using, and framework for programming various computer emulators.
It is versatile, free, and written in Java.

The main goal is to support teaching older but important computers and abstract machines. For example,
students can write their programming assignments in emuStudio IDE, which then will be run on the emulated
computer. The output might be displayed on emulated screen, or written to some file having the possibility
of creating automated "assignment checking" process.

emuStudio is used at [Technical University of Košice](http://www.fei.tuke.sk/en) since 2007.

Besides, developer's API is very simple and powerful enough to enable to write own computer components.
The components are implemented as emuStudio plugins: compilers, CPUs, operating memories, and I/O devices.
Some can be compatible, some not. Compatible components can be arranged into working schemas of computer which
makes the versatility of emuStudio.
 
NOTE: There are many tutorials for emulation of various CPUs or other hardware and they might be useful when
implementing them using emuStudio.

# License

This project is released under GNU GPL v2 license.

# Supported host platform

Currently supported are Linux and Windows. Mac is NOT supported, but it might work to some extent.

# Available computer emulators

* MITS Altair8800 with choosable processor: Intel 8080 or Zilog Z80 CPU. Includes 88-DISK drive, 88-SIO board,
  LSI ADM-3A terminal, SIMH pseudo device (partially reimplemented from simh emulator), operating memory,
  and 8080 + Z80 assembler compilers

* Manchester Small-Scale Experimental Machine (SSEM) emulator, with programming tutorials

* Random Access Machine (RAM) simulator

* Random Access Stored Program (RASP) machine simulator 

* Brainfuck interpreter (designed as a "computer" which I called BrainDuck)

# For developers

The project uses Maven for managing dependencies and build process. Each module can be compiled separately,
but if you run the following command in the root directory, it will compile each module (results will be in
particular module directory):

```
mvn clean install
```

## Creating a custom "release"

In order to package complete emuStudio with examples and all predefined computers, at first compile the whole
project. Then go to directory `release` and invoke the following command:

```
mvn clean install -P release
```

Inside the `target` subdirectory will be a zip file containing the custom release of emuStudio.

# Running / installation

There are two ways of how to run emuStudio. In both cases, a prerequisite is to have a full release of emuStudio.
You can either download it from [project's web page](http://emustudio.sourceforge.net/downloads.html),
or you can prepare it according to the previous section.

## The "classic" way

The requirement is to have installed Java Runtime Environment (JRE) 1.8.

Unzip the emuStudio distribution zip file (`emuStudio-xxx-release.zip`) and run it using command:

```
java -jar emuStudio.jar
```

## Using Vagrant

It is also possible to use [Vagrant](https://www.vagrantup.com/) in order to run emuStudio. This way does not
require to install JRE, but virtual box must be installed. Also it works only for custom releases, as described
above.

Assuming the release is prepared, run the script in the root directory of emuStudio:

```
> ./emuStudio-vagrant.sh
```

The script does the following:

1. It boots or resumes the already-prepared virtual machine with preinstalled Linux and Java using
   [Vagrant](https://www.vagrantup.com/).
2. Synchronizes `release/target/` subdirectory with the machine.
3. Then it unzips the release zip file into VM's `/emustudio/` directory
4. Finally, it runs emuStudio from the virtual machine (using SSH with X forwarding). 
