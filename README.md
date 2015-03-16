# Welcome to emuStudio
[![Build Status](https://travis-ci.org/vbmacher/emuStudio.png)](https://travis-ci.org/vbmacher/emuStudio)
[![Coverage Status](https://coveralls.io/repos/vbmacher/emuStudio/badge.png?branch=branch-0_39)](https://coveralls.io/r/vbmacher/emuStudio?branch=branch-0_39)

emuStudio is free and versatile platform for computer emulation. It is written in Java, which makes
it a naturally cross-platform application.

The description of plug-ins combinations is a description of a virtual computer. Plug-ins represent
computer components.

Plug-ins are developed separately, following so-called communication model. Anyone can take those
components and combine them into a working computer, just by drawing simple computer schema.

Main purpose of emuStudio is to support education process of mainly old hardware. The platform has
been used at Technical University of Košice (Slovakia) with very good responses of students and
teachers since 2007.

# Feature highlights

* Main module with Graphical User Interface (GUI)

* Simple computer schema editor

* Source code editor with support of syntax highlighting

* CPU debugger with basic and advanced abilities

* Operating memory and devices can have their own GUI windows

* Automatic emulation run from command line

* Several predefined computer sets and plug-ins

* Consistent API for development of custom plug-ins

# Compiling

The project is using Maven for managing dependencies and build process. Each module can be compiled separately,
but if you run the following command in the root directory, it will compile each module (results will be in particular
module directory):

```
mvn clean install
```

# Installation

Please follow instructions on [project's web page](http://emustudio.sourceforge.net/downloads.html).

# License

This project is released under GNU GPL v2 license.
