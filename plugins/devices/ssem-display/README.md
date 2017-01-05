SSEM CRT display plug-in
------------------------

This project is a CRT display for SSEM computer. SSEM was the first stored-program computer. More
information can be found at [this link](https://en.wikipedia.org/wiki/Manchester_Small-Scale_Experimental_Machine).
It is implemented as a plug-in for [emuStudio](http://emustudio.sf.net), a software-based
computer emulation platform.

SSEM uses Cathode-Ray-Tube for displaying the content of the memory. Both the display and memory were in fact
Williams-Killburn tubes, as well as all other stores in the computer. The display is just a grid of 32x32
"diodes", remarkably big, so it cannot be considered to a "terminal" or something similar.

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `devices`**.

For example: `emuStudio/devices/ssem-display.jar`.

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://emustudio.sourceforge.net/downloads.html).

