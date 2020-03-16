LSI ADM-3A terminal plug-in
---------------------------

The project represents an emulator of [ADM-3A](http://en.wikipedia.org/wiki/ADM-3A)
terminal device from LSI company. It was used with old computers, e.g. MITS Altair8800.
The terminal was connected into RS-232 serial port (in emuStudio, it can be plugged into
MITS 88-SIO device). The project is implemented as a plug-in for [emuStudio](http://net.emustudio.sf.net),
a software-based computer emulation platform.

Some main properties of the product include:

* Support of half/full duplex modes
* ASCII characters only with host platform encoding (can be disadvantage)
* Anti-aliasing support
* Double buffering
* Realistic terminal window

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `devices`**.

For example: `emuStudio/devices/adm3a-terminal.jar`.

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://net.emustudio.sourceforge.net/downloads.html).

