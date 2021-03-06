# ![emuStudio logo](logo.png "emuStudio logo") Welcome to emuStudio
![emuStudio Build](https://github.com/emustudio/emuStudio/workflows/emuStudio%20Build/badge.svg)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

[emuStudio](https://www.emustudio.net/) is a desktop application used for computer emulation and writing programs
for emulated computers. It extensible; it encourages developers to write their own computer emulators.

The main goal of emuStudio is to support the "compile-load-emulate" workflow, aiming at students or anyone to help to learn about older but important computers or even abstract machines.

emuStudio is very appropriate for use at schools, e.g. when students are doing first steps in assembler, or when they are taught about computer history. For example, emuStudio is used at the [Technical University of Košice](http://www.fei.tuke.sk/en)
since 2007.

## Available emulators

* [MITS Altair8800](https://www.emustudio.net/documentation/user/altair8800/)

* [Manchester SSEM](https://www.emustudio.net/documentation/user/ssem/)

* [Random Access Machine (RAM)](https://www.emustudio.net/documentation/user/ram/)

* [Random Access Stored Program (RASP)](https://www.emustudio.net/documentation/user/rasp/) 

* [BrainDuck (brainfuck interpreter)](https://www.emustudio.net/documentation/user/brainduck/)

## BIG THANKS

Big thanks goes to the one and only [simh](http://simh.trailing-edge.com/) project, which inspired me a lot, and helped
me as a student and emulator enthusiast when working on emuStudio. I wish emuStudio will reach it's simplicity and
emulators "richness" as the simh project has.

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
- Which rules needs to be followed

<a href="https://www.buymeacoffee.com/vbmacher" target="_blank"><img src="https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png" alt="Buy Me A Coffee" style="height: 41px !important;width: 174px !important;box-shadow: 0px 3px 2px 0px rgba(190, 190, 190, 0.5) !important;-webkit-box-shadow: 0px 3px 2px 0px rgba(190, 190, 190, 0.5) !important;" ></a>

### Related projects

There exist some additional projects, which are used by emuStudio, useful for contributors:
  
- [emuLib](https://github.com/emustudio/emuLib), a run-time library
- [edigen](https://github.com/emustudio/edigen), an emulator disassembler generator
- [cpu-testsuite](https://github.com/emustudio/cpu-testsuite), a JUnit-based test suite for comfortable testing of CPU
  plugins
