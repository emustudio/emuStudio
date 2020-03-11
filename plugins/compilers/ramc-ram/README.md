Compiler for Random Access Machine (RAM)
----------------------------------------

This project is a compiler for abstract machine RAM, used in theoretical informatics, written in Java.
It is implemented as a plug-in for [emuStudio](http://net.emustudio.sf.net), a software-based computer
emulation platform.

The compiler doesn't support compilation into a binary file, but outputs the result directly to
program memory of the emulator. It implements only parsing and semantic analysis.

The compiler supports the following instructions: `HALT`, `READ`, `WRITE`, `LOAD`, `STORE`, `ADD`, `SUB`,
`MUL`, `DIV`, `JMP`, `JGTZ`, and `JZ`. The input tape can be filled via `<input>` directive. Some examples
can be found in the repository.

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `compilers`**.

For example: `emuStudio/compilers/ramc-ram.jar`

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://net.emustudio.sourceforge.net/downloads.html).
