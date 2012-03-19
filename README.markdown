88-SIO Serial interface plug-in
-------------------------------


This project is an emulator of MITS 88-SIO device (serial card), used mainly in MITS
Altair8800 computers. The device has been a serial I/O interface card that is plugged into
S-BUS bus. It is implemented as a plug-in for [emuStudio](http://emustudio.sf.net), a software-based
computer emulation platform\*.

In the present time it can be used side by side with 
[Intel 8080](https://github.com/vbmacher/8080-cpu) or [Zilog Z80](https://github.com/vbmacher/z80-cpu)
CPU plug-ins. The card is controlled by two I/O ports attached to the CPU. 

In the emuStudio, this device is used as a mediator between LSI ADM-3A terminal and the CPU. The
terminal was connected to the MITS Altair8800 computer through serial RS-232 port of the 88-SIO device.

The Altair8800 used two versions of the 88-SIO. First (classical 88-SIO - this plug-in) used only one
physical port allowing to connect a single device. Second version (88-SIO-2) was enhanced to have two
physical ports, allowing to connect two devices at the same time.


Programming
-----------

The disk uses two ports - status port and data port. The plug-in was programmed to support almost all
capabilities of real device, according to a manual found at
[this](http://www.classiccmp.org/dunfield/s100c/mits/88sio_1.pdf) link. You can
find there exact instructions how to program it. Only interrupts support is not implemented.

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `devices`**.

For example: `emuStudio/devices/88-sio.jar`.

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://emustudio.sourceforge.net/downloads.html).

License
-------

This project is released under GNU GPL v2 license.

* * *

\* You can find emuStudio repository at [GitHub](http://github.com/vbmacher/emuStudio).

