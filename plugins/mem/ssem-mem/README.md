SSEM operating memory plug-in
------------------------------

This module is a main store of SSEM "Baby" machine. It is implemented as a plug-in for
[emuStudio](http://net.emustudio.sf.net), a software-based computer emulation platform.

Main features include:

- 32 memory cells, each has size 32 bits
- GUI window which shows and allows to edit the cells (in either binary or hex)

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `mem`**.

For example: `emuStudio/mem/ssem-mem.jar`

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://net.emustudio.sourceforge.net/downloads.html).

