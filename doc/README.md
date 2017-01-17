# emuStudio documentation

This module contains all the official documentation for emuStudio. Besides user manual it contains also a set of
tutorials for plug-in development.

All the documentation is written in Asciidoc format. 

## How to "compile" the documentation?

Before installation, please install Graphviz on your computer. The `dot` program must be in path.
Graphviz can be downloaded from http://www.graphviz.org/Download.php

Then, run the following command:

```
mvn clean install -P doc
```

In `doc/target` subdirectory, you can find the documentation.

## Documentation organization

The documentation is organized in several subdirectories:

- `src/tutorials` - tutorials for developing plugins for emuStudio. People reading these tutorials should be Java
                    developers.
- `src/user-manual` - user's manual. All official computers and plug-ins should be documented from the user
                      perspective. Here should be also placed all programming examples used in emulated machines.
- `src/roadmap` - this is a "scratchpad" directory where new ideas and roadmap is written for the next development of
                  emuStudio. Files placed there should contain decisions, strategies, motivations and explanations
                  of "how,why,when" of emuStudio. The directory is not named "RFC" or similarly, because the files
                  are allowed to be written informally.

## How to contribute

Anyone can contribute.
 
It's suggested to follow a predefined structure template of particular document type, which
follows. User manual is written in different style and contains different type of sections than developer's tutorial.

### Extending user manual

User manual should document only official plug-ins and main-module, including some general behavior of emuStudio as
a whole. This means, things not related to emuStudio directly should not be documented. For example, emulation history
is not part of emuStudio. These things can be mentioned in text, but with link to the location with more detailed information.

#### Main module

Regarding main module, the following sections are important, in order:

- Main description - what is emuStudio, explaining the purpose. Next, for who it is for. Next, what can be done with
                     emuStudio - list of features, and general description of how it works.
- User interface - description of various windows used in emuStudio. Starting with windows which appears first, like
                   the open computer dialog. Then, logically explain each "panel" - source code editor, and emulator,
                   and the descriptions of related subwindows as their subsections.
- Command line - description of command line commands
- Automatic emulation - explain the purpose. And show example of using.
- Logger configuration - how to turn on/off or customize logger. Also, how to access log file.

#### Plug-ins 

Description of a plug-in usually should not be standalone, but put in a bigger document describing the
whole computer. Description of each computer should be put in a separate directory, e.g. `altair8800/`, `brainduck`, etc.
The description should focus on the interactive part of the emulation, and do not describe what's going under the hood
in much detail.

The description should start with some introduction:

- How the computer is related to the computer history?
- Is it abstract or real?
- The purpose of the computer
- Comparison of features which it has as the emulator for emuStudio with the features of real computer

Then, every plug-in should be described, starting from compiler - in the form of the "programming language" tutorial.

It is important - keep the information useful. Do not try hard to put any information if you think it is too small.
Some plug-ins are quite clear and don't seem to interact much with user, which is OK. For example usually it's the
CPU plug-in.

Programming examples should follow, if the plug-in allows programming. For example, both MITS 88-DISK and MITS 88-SIO
are programmable devices.

Then a very important section should be devoted to automatic emulation. More specifically:

- How the plug-in will behave if emuStudio will run in automatic emulation mode?
- Where can user find output files if the output is redirected to a file?
- What is the behavior if the automatic emulation is run more times in a row? Will the files be overridden or appended? 
- Can be output file names changed?

The last section should talk about debugging of the plug-in. For example:

- List of known bugs
- How to report a bug
- How to do some analysis when something does not work


### Extending tutorials for developing plug-ins

Developing of plug-ins is in the form of tutorials. The official guide is a bunch of tutorials, specific for particular
plug-in type. The tutorials are oriented towads building single computer - 
https://en.wikipedia.org/wiki/Manchester_Small-Scale_Experimental_Machine[SSEM (Small-Scale Experimental Machine)].
It is important to keep the focus.
 
Also, keep the order of the sections and particular tutorials as when a developer would really proceed. It is true that
every developer might do things in different order and this is for a debate. For this reason, the current order is given
as follows:

- Compiler
- Memory
- CPU
- Device(s) 
