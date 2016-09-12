Operating memory plug-in for Random Access Stored Program (RASP) machine
------------------------------------------------------------------------

This project is implemented as a plug-in for [emuStudio](http://emustudio.sf.net), a software-based
computer emulation platform. It is used as the operating memory for the RASP machine emulator plug-in. 

Supported features:
* during the emulation, user can view current content of the operating memory in a simple GUI window
* the memory cells' content is editable in the table in the GUI window
* it is also possible to load a compiled memory image from a file

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `mem`**.

For example: `emuStudio/mem/rasp-mem.jar`

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://emustudio.sourceforge.net/downloads.html).

