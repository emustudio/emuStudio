BrainDuck operating memory plug-in
-----------------------------------

This project represents operating memory of abstract computer architecture called BrainDuck.
It is implemented as a plug-in for [emuStudio](http://emustudio.sf.net), a software-based
computer emulation platform.

The project has been created as teaching material for people who wants to create plug-ins for
emuStudio. It is intended to be as simple as possible. It can be used for other architectures
as well - if the CPU doesn't need specialized memory context.

The memory uses memory cells that are linearly ordered. Size of the memory is 64kB,
and cannot be set-up manually. Size of a single cell is of java `Short` type.

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `mem`**.

For example: `emuStudio/mem/brainduck-mem.jar`

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://emustudio.sourceforge.net/downloads.html).

