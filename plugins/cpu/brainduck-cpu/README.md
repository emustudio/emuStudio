BrainDuck CPU plug-in
----------------------

BrainDuck is abstract computer architecture based on esoteric programming language 
[brainfuck](http://en.wikipedia.org/wiki/Brainfuck). This project represents Java-based
emulator the CPU. It is implemented as a plug-in for [emuStudio](http://net.emustudio.sf.net),
a software-based computer emulation platform.

This project has been created as the material for teaching how to create plug-ins in emuStudio.
Instructions of the CPU are following those of the brainfuck language (BF), but are renamed into more
readable form.

The following instructions are supported:

* `HALT`: stops the CPU execution
* `INC`: BF's `>`
* `DEC`: BF's `<`
* `INCV`: BF's `+`
* `DECV`: BF's `-`
* `PRINT`: BF's `.`
* `LOAD`: BF's `,`
* `LOOP`: BF's `[`
* `ENDL`: BF's `]`

For more information about the instructions, please visit [brainfuck](http://en.wikipedia.org/wiki/Brainfuck) page.

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `cpu`**.

For example: `emuStudio/cpu/brainduck-cpu.jar`.

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://net.emustudio.sourceforge.net/downloads.html).
