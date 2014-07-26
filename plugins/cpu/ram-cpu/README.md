Random Access Machine emulator
------------------------------

This is a Java-based emulator of Ranom Access Machine (RAM), used in theoretical informatics. It is implemented as a plug-in for
[emuStudio](http://emustudio.sf.net), a software-based computer emulation platform.

Random Access Machine (RAM) is abstract machine derived from Turing machine. It was created for easier computation of algorithms`
complexity, in comparison to Turing machine. Originally, RAM is of Harvard architecture, but it is implemented also in the emuStudio,
as an example of the possibility to implement non-von Neumann computers. However, it is more complex for now.

This plug-in can be used only with `ramc-ram` compiler and `ram-mem` program memory. For proper functionality, it requires also
three `abstractTape-ram` devices that represent input, output and register tapes (their purpose is set-up by the emulator at
runtime). The program tape is represented by mentioned `ram-mem` plug-in.

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `cpu`**.

For example: `emuStudio/cpu/ram-cpu.jar`.

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://emustudio.sourceforge.net/downloads.html).

