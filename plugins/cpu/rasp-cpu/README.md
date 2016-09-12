Random Access Stored Program (RASP) machine emulator
----------------------------------------------------

RASP machine emulator is implemented as a plug-in for [emuStudio](http://emustudio.sf.net), a software-based computer emulation platform. RASP machine is a von Neumann equivalent of Random Access Machine (RAM), which is a Harvard one. It is supposed to serve as a study supporting tool for courses oriented to computer architectures.

This plug-in can be used only with `raspc-rasp` compiler and `rasp-mem` operating memory. As a form of input/output devices, input and output tapes can be attached. A tape is represented by a `abstractTape-ram` device (abstract tape is borrowed from the RAM machine emulator).

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `cpu`**.

For example: `emuStudio/cpu/rasp-cpu.jar`.

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://emustudio.sourceforge.net/downloads.html).
