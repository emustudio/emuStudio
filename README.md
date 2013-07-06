Zilog Z80 Assembler plug-in
----------------------------
[![Build Status](https://travis-ci.org/vbmacher/as-z80.png)](https://travis-ci.org/vbmacher/as-z80)
[![Coverage Status](https://coveralls.io/repos/vbmacher/as-z80/badge.png?branch=branch-0_17)](https://coveralls.io/r/vbmacher/as-z80?branch=branch-0_17)


This project is my own version of assembler for Z80 processor, written in Java.
It is implemented as a plug-in for [emuStudio](http://emustudio.sf.net), a software-based computer
emulation platform\*.

The assembler syntax is inspired by my [Intel 8080 assembler](https://github.com/vbmacher/as-8080)
project and by instruction list found at [this](http://nemesis.lonestar.org/computers/tandy/software/apps/m4/qd/opcodes.html)
link. The assembler supports following features:

* Support of full instruction set,
* Support of unlimited nested macros,
* Support of conditional assembly,
* Ability to include other source files (using "libraries"),
* Ability of data definition and data reservation with various sizes,
* Ability to define labels of relative adresses and use them as forward references,
* Literals and expressions definition in various radixes,
* Binary output in Intel HEX format.

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `compilers`**.

For example: `emuStudio/compilers/as-z80.jar`

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://emustudio.sourceforge.net/downloads.html).

License
-------

This project is released under GNU GPL v2 license.

* * *

\* You can find emuStudio repository at [GitHub](https://github.com/vbmacher/emuStudio).
