88-DISK plug-in
---------------

This project is an emulator of MITS 88-DISK device, used in MITS Altair8800 computers.
The device has been a disk controller that is plugged into S-BUS bus. It is implemented
as a plug-in for [emuStudio](http://emustudio.sf.net), a software-based computer emulation
platform.

In the present time it can be used side by side with `8080-cpu` or `z80-cpu` plug-ins.
The plug-in supports up to 16 disk drives that are controlled by three I/O ports
attached to the CPU. 

Programming
-----------

The disk uses three ports - status port, data port and control port for setting position. The plug-in
was programmed to support almost all capabilities of real device, according to a manual found
at [this](http://www.virtualaltair.com/virtualaltair.com/PDF/88dsk%20manual%20v2.pdf) link. You can
find there exact instructions how to program it. Only interrupts support is not implemented.

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `devices`**.

For example: `emuStudio/devices/88-disk.jar`.

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://emustudio.sourceforge.net/downloads.html).

