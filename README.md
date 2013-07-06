Intel 8080 Assembler plug-in
----------------------------
[![Build Status](https://travis-ci.org/vbmacher/as-8080.png)](https://travis-ci.org/vbmacher/as-8080)
[![Coverage Status](https://coveralls.io/repos/vbmacher/as-8080/badge.png?branch=branch-0_31)](https://coveralls.io/r/vbmacher/as-8080?branch=branch-0_31)

This project is light modified (enhanced) "clone" of original Intel's 8080 assembler, written in Java.
It is implemented as a plug-in for [emuStudio](http://emustudio.sf.net), a software-based computer
emulation platform\*.

The assembler syntax is taken from manual found at [this](http://www.classiccmp.org/dunfield/r/8080asm.pdf)
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

For example: `emuStudio/compilers/as-8080.jar`

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://emustudio.sourceforge.net/downloads.html).

License
-------

This project is released under GNU GPL v2 license.

* * *

\* You can find emuStudio repository at [GitHub](http://github.com/vbmacher/emuStudio).
