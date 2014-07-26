Standard operating memory plug-in
----------------------------------

This project represents virtual computer's operating memory. It is implemented as a plug-in for
[emuStudio](http://emustudio.sf.net), a software-based computer emulation platform.

This plug-in can be used with any 'real' von-Neumann's CPUs that understand memory cells as linear
ordered bytes. Size of the memory is variable, and can be set-up during construction of virtual computer.
It supports ROM ranges (some parts of memory can be taken as read-only), bank switching technique, and DMA.

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `mem`**.

For example: `emuStudio/mem/standard-mem.jar`

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://emustudio.sourceforge.net/downloads.html).

