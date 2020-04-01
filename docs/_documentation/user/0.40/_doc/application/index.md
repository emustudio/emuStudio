---
layout: default
title: emuStudio Application
nav_order: 1
has_children: true
permalink: /application/
---

# emuStudio Application

emuStudio is a desktop application which allows emulation of various computers. Besides emulation, it contains source code editor, which can be used to write and then compile programs then to be instantly emulated. Virtual computers, as the emulators are called, are represented by plugins of various type (compiler, memory, CPU and device), combined in a comuter configuration. The computer configuration can be opened during startup.

The application has also command-line interface, which allows executing automatic "load-compile-emulate" workflow, possibly without graphical interface. This workflow is called "automatic emulation" and has its specifics, discussed per virtual computer.

A logger is used by emuStudio which helps debugging of the application and plugins.   

## Installation and run

At first, please download emuStudio distribution. It is either a TAR or ZIP file in the form `emuStudio-[VERSION].zip`. For Linux/Mac environments, a TAR variant will be more suitable since it preserves file attributes and execution permissions. Unpack the file where you want to have emuStudio installed.

Before running, [Java 11][java11]{:target="_blank"} or later must be installed. Then, emuStudio can be run by executing the following script:

- On Linux / Mac
```
> ./emuStudio
```

- On Windows:
```
> emuStudio.bat
```

NOTE: Currently supported are Linux and Windows. Mac is NOT supported, but it might work to some extent.

## Command-line interface

emuStudio accepts several command line arguments. Their description is accessible with `--help` argument:

	$ ./emuStudio --help
	 --auto            : run the emulation automation (default: false)
     --config filename : load configuration with file name
     --help            : output this message (default: true)
     --input filename  : use the source code given by the file name
     --nogui           : try to not show GUI in automation (default: false)
     --waitmax X       : wait for emulation finish max X milliseconds (default: -1)


Most of these arguments are self-explanatory. Some of them have meaning only when emulation automation is turned on (`--nogui`, `--waitmax`).


[java11]: https://jdk.java.net/archive/
