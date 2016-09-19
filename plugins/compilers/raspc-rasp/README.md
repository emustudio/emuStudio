Compiler for Random Access Stored Program (RASP) machine.
---------------------------------------------------------

This project is a compiler for abstract machine RASP, similarly to RAM - used in theoretical informatics. It is written in Java. It is implemented as a plug-in for [emuStudio](http://emustudio.sf.net), a software-based computer emulation platform. 

The result of the compilation is loaded into the operating memory of the RASP emulator and also serialised into a file with `*.bin` extension.

All necessary information can be found in the documentation of the plug-in (included in the emuStudio documentation). Also some example programs are attached to this compiler, in the `/examples` subdirectory of emuStudio.

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `compilers`**.

For example: `emuStudio/compilers/raspc-rasp.jar`

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://emustudio.sourceforge.net/downloads.html).
