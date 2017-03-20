# Welcome to emuStudio
[![Build Status](https://travis-ci.org/vbmacher/emuStudio.svg?branch=develop)](https://travis-ci.org/vbmacher/emuStudio)

emuStudio is a desktop application which allows to write programs and emulate computers.
In addition, it is easily extensible - acting as a framework it encourages developers to write their own computer
emulators. It is versatile, free, and written in Java.

The main goal is to support learning about older but important computers or abstract machines. 
I think it is also appropriate for schools; for example, emuStudio is used at
[Technical University of Košice](http://www.fei.tuke.sk/en) since 2007.

## Available emulators

* [MITS Altair8800](https://vbmacher.github.io/emuStudio/docuser/mits_altair_8800/index/)

* [Manchester SSEM](https://vbmacher.github.io/emuStudio/docuser/ssem/index/)

* [Random Access Machine (RAM)](https://vbmacher.github.io/emuStudio/docuser/ram/index/)

* [Random Access Stored Program (RASP)](https://vbmacher.github.io/emuStudio/docuser/rasp/index/) 

* [BrainDuck (brainfuck interpreter)](https://vbmacher.github.io/emuStudio/docuser/brainduck/index/)


## BIG THANKS

Big thanks goes to the one and only [simh](http://simh.trailing-edge.com/) project, which inspired me a lot, and helped
me as a student and emulator enthusiast when working on emuStudio. I wish emuStudio will reach it's simplicity and
emulators "richness" as the simh project has.  

# Supported host platforms

Currently supported are Linux and Windows. Mac is NOT supported, but it might work to some extent.

# License

This project is released under GNU GPL v2 license.

# Contributing

Anyone can contribute. Before start, please read
[developer documentation](https://vbmacher.github.io/emuStudio/docdevel/emulator_tutorial/index/),
which includes information like:

- Which tools to use and how to set up the environment
- How to compile emuStudio
- How to prepare local release


# Running / installation

There are two ways of how to run emuStudio. In both cases, a prerequisite is to have a full release of emuStudio.
You can either download it from [project's web site](https://vbmacher.github.io/emuStudio/download/),
or you can prepare it according to the previous section.

## The "classic" way

The requirement is to have installed Java Runtime Environment (JRE) 1.8.

Unzip the emuStudio distribution zip file (`emuStudio-xxx.zip`) and run it using command:

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
