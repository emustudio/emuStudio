Intel 8080 CPU plug-in
-----------------------

This is another Java-based emulator of 8-bit Intel 8080 CPU. It is implemented as a plug-in for
[emuStudio](http://emustudio.sf.net), a software-based computer emulation platform\*.

This project has been the first attempt of creating the emuStudio (it was called emu8 in that time).
After time passed, the emulator was enhanced. The CPU emulator of the Intel 8080 processor was separated
to be a first emuStudio plug-in. Features include:

* Interpetation as emulation technique,
* Correct real timing,
* Ability to set clock frequency manually at run-time,
* Emulation of all instructions including interrupts,
* Disassembler implementation,
* Support of breakpoints,
* Ability of communication with up to 256 I/O devices,
* Status window shows all registers, flags, and run-time frequency.

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `cpu`**.

For example: `emuStudio/cpu/8080-cpu.jar`.

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://emustudio.sourceforge.net/downloads.html).

[![Build Status](https://travis-ci.org/vbmacher/8080-cpu.png)](https://travis-ci.org/vbmacher/8080-cpu)

License
-------

This project is released under GNU GPL v2 license.

* * *

\* You can find emuStudio repository at [GitHub](http://github.com/vbmacher/emuStudio).

