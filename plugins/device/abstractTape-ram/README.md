Abstract tape for Random Access Machine
----------------------------------------

This project represents an 'abstract tape' that is used in abstract machines, such as Random
Access Machine (RAM), or Turing machine. It is implemented as a plug-in for
[emuStudio](http://net.emustudio.sf.net), a software-based computer emulation platform.

In the present time, only RAM machine emulator exists within the emuStudio (`ram-cpu`).
The device then represents either input, output or register tape of the RAM machine (it
needs all three tapes).

The program tape is represented by `ram-mem` plug-in. The RAM emulator (`ram-cpu`)
assigns the function of the tape during initialization process (user is not responsible of this).

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `devices`**.

For example: `emuStudio/devices/abstractTape-ram.jar`.

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://net.emustudio.sourceforge.net/downloads.html).

