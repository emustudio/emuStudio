# Welcome to emuStudio
[![Build Status](https://travis-ci.org/vbmacher/emuStudio.svg?branch=develop)](https://travis-ci.org/vbmacher/emuStudio)

[emuStudio](www.emustudio.net) is a desktop application which allows to write programs and emulate computers.
In addition, it is easily extensible - acting as a framework it encourages developers to write their own computer
emulator. It is versatile, free, and written in Java.

The main goal is to support learning about older but important computers or abstract machines. 
It is also appropriate for using at schools; for example, emuStudio is used at
[Technical University of Košice](http://www.fei.tuke.sk/en) since 2007.

## Available emulators

* [MITS Altair8800](https://vbmacher.github.io/emuStudio/docuser/mits_altair_8800/index/)

* [Manchester SSEM](https://vbmacher.github.io/emuStudio/docuser/ssem/index/)

* [Random Access Machine (RAM)](https://vbmacher.github.io/emuStudio/docuser/ram/index/)

* [Random Access Stored Program (RASP)](https://vbmacher.github.io/emuStudio/docuser/rasp/index/) 

* [BrainDuck (brainfuck interpreter)](https://vbmacher.github.io/emuStudio/docuser/brainduck/index/)

## Related projects

There exist some additional projects, which are either incorporated in or used by emuStudio:
  
- [emuLib](https://github.com/vbmacher/emuLib), a run-time library
- [edigen](https://github.com/sulir/edigen), an emulator disassembler generator


## BIG THANKS

Big thanks goes to the one and only [simh](http://simh.trailing-edge.com/) project, which inspired me a lot, and helped
me as a student and emulator enthusiast when working on emuStudio. I wish emuStudio will reach it's simplicity and
emulators "richness" as the simh project has.  

# Getting started

At first, either compile or [download](https://vbmacher.github.io/emuStudio/download/) emuStudio.
The prerequisite is to have Java Runtime Environment (JRE) version 1.8 installed. 

Then, unzip the zip file (`emuStudio-xxx.zip`) and run it using command:

```
java -jar emuStudio.jar
```

NOTE: Currently supported are Linux and Windows. Mac is NOT supported, but it might work to some extent.

For more information, please read [user documentation](https://vbmacher.github.io/emuStudio/docs/).


# Contributing

Anyone can contribute. Before start, please read
[developer documentation](https://vbmacher.github.io/emuStudio/docdevel/emulator_tutorial/index/),
which includes information like:

- Which tools to use and how to set up the environment
- How to compile emuStudio and prepare local releases
- Which git branch to use
- Which rules needs to be followed

# License

This project is released under GNU GPL v2 license.
