Program memory plug-in for Random Access Machine (RAM)
------------------------------------------------------

This project represents program memory (or program tape) of Random Access Machine, used in theoretical
informatics. It is implemented as a plug-in for [emuStudio](http://emustudio.sf.net), a software-based
computer emulation platform\*.

This plug-in can be used only with [ramc-ram](https://github.com/vbmacher/ramc-ram) compiler and
[ram-cpu](https://github.com/vbmacher/ram-cpu) virtual processor. 
Originally, RAM is of Harvard architecture, but it is implemented also in the emuStudio, as an example
of the possibility to implement non-von Neumann computers. However, it is more complex for now.

The plug-in has the following features:

* Ability to automatically compute program’s uniform complexity
* Cells are specific, of non-general type (each cell value has to be correct RAM instruction – data is
  stored elsewhere).

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `mem`**.

For example: `emuStudio/mem/ram-mem.jar`

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://emustudio.sourceforge.net/downloads.html).

License
-------

This project is released under GNU GPL v2 license.

* * *

\* You can find emuStudio repository at [GitHub](http://github.com/vbmacher/emuStudio).

