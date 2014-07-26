SIMH Pseudo-device plug-in
--------------------------

The aim of this project is to mimic functionality of pseudo device that is implemented
inside [simh](http://simh.trailing-edge.com/) emulator. It is used for the interaction
between emulated programs and the emuStudio (eg. CP/M 3 operating system assembled for
the SIMH emulator uses this device). It is implemented as a plug-in for
[emuStudio](http://emustudio.sf.net), a software-based computer emulation platform\*.

In the present time it can be used side by side with `8080-cpu` or `z80-cpu` plug-ins.

Features
--------

The device offers the following commands (original pseudo-device offers much more):

* get/set the current time in milliseconds, in various formats;
* memory bank switching.

Programming
-----------

Z80 or 8080 programs communicate with the SIMH pseudo device via port `0xfe`. Programmers
must apply the following principles:

1. For commands that do not require parameters and do not return results:

        ld  a,<cmd>
        out (0feh),a

   Special case is the reset command which needs to be send 128 times to make
   sure that the internal state is properly reset.

2. For commands that require parameters and do not return results:

        ld  a,<cmd>
        out (0feh),a
        ld  a,<p1>
        out (0feh),a
        ld  a,<p2>
        out (0feh),a
        ...

   Note: The calling program must send all parameter bytes. Otherwise
   the pseudo device is left in an undefined state.

3. For commands that do not require parameters and return results:

        ld  a,<cmd>
        out (0feh),a
        in  a,(0feh)    ; <A> contains first byte of result
        in  a,(0feh)    ; <A> contains second byte of result
        ...

   Note: The calling program must request all bytes of the result. Otherwise
   the pseudo device is left in an undefined state.

4. Commands requiring parameters and returning results do not exist currently.

The list of commands and their parameters is/will be in documentation.

Installation
------------

The easiest way how to install the plug-in is to use whole emuStudio distribution release. This plug-in is
included in each release and it will be included in the future as well. However, if you still want to install
a snapshot version, follow this rule: 

**Put the compiled jar file into subdirectory of emuStudio called `devices`**.

For example: `emuStudio/devices/simhPseudo-z80.jar`.

Now you can use the plug-in in abstract schema editor to construct virtual computers. The emuStudio
will not recognize the plug-in until restart. Don't forget to check the compatibility with chosen
distribution.

For more information, please visit [emuStudio home page](http://emustudio.sourceforge.net/downloads.html).

License
-------

This project is released under GNU GPL v2 license.

* * *

\* You can find emuStudio repository at [GitHub](http://github.com/vbmacher/emuStudio).

