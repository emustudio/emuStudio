# emuStudio application

The application is implemented using Java and Swing for GUI.

# Features

* Graphical User Interface (GUI)

* Debugger with interactive disassembler

* Simple computer schema editor

* Source code editor with support of syntax highlighting

* Automatic emulation run from command line

# Installation

The simplest way how to install main module is to use whole distribution. For that case, please follow
the instructions at [project's web page](http://net.emustudio.sourceforge.net/downloads.html).

The hacker ways follows. At first, compile the project:

```
emuStudio/            > cd main-module
emuStudio/main-module/> mvn clean install
```

Then, overwrite the `emuStudio.jar` file found in `target/` subdirectory in the root directory where
emuStudio distribution is unpacked.

# License

This project is released under GNU GPL v2 license.
