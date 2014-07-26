BrainDuck compiler plug-in
---------------------------

This is a compiler for my own CPU architecture called BrainDuck (`brainduck-cpu`),
written in Java. It is implemented as a plug-in for [emuStudio](http://emustudio.sf.net), a software-based
computer emulation platform\*.

BrainDuck is abstract architecture derived from [brainfuck](http://en.wikipedia.org/wiki/Brainfuck), esoteric
programming language. It was created to demonstrate the ability of emuStudio to emulate custom computer architectures.
The output of the compiler is in Intel HEX format.

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `compilers`**.

For example: `emuStudio/compilers/brainc-brainduck.jar`

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://emustudio.sourceforge.net/downloads.html).

License
-------

This project is released under GNU GPL v2 license.

* * *

\* You can find emuStudio repository at [GitHub](https://github.com/vbmacher/emuStudio).
