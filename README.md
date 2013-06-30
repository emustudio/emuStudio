BrainDuck text terminal plug-in
-------------------------------

This project is text terminal for BrainDuck architecture. BrainDuck is abstract
machine based on esoteric programming language [brainfuck](http://en.wikipedia.org/wiki/Brainfuck).
It is implemented as a plug-in for [emuStudio](http://emustudio.sf.net), a software-based
computer emulation platform\*.

The project has been created as teaching material for people who want to create plug-ins for emuStudio.
The device can be used only with [brain-cpu](https://github.com/vbmacher/brainduck-cpu), because the
device assumes special CPU context.

The terminal is implemented as simple as possible. It is in fact an output tape that can roll in only 'down'
direction. Therefore it's impossible to write chars to any screen position, as it is in classic terminals.
The terminal uses default system font, black color on white background.

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `devices`**.

For example: `emuStudio/devices/brainduck-terminal.jar`.

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://emustudio.sourceforge.net/downloads.html).

[![Build Status](https://travis-ci.org/vbmacher/brainduck-terminal.png)](https://travis-ci.org/vbmacher/brainduck-terminal)

License
-------

This project is released under GNU GPL v2 license.

* * *

\* You can find emuStudio repository at [GitHub](http://github.com/vbmacher/emuStudio).

