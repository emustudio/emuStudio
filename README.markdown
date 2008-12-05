Welcome to emu8 Studio project page !
-------------------------------------

In 2006 I began to write my own CPU emulator, Intel 8080. I had fun, but the software lacked many
features. So I decided to enhance the software. Now it has grown to emulation platform
that can emulate arbitrary, not necessarily 8-bit computers. In present time it is implemented
only MITS Altair 8800 computer with Intel 8080 CPU, 64kB RAM, MITS 88-DISK controller with up to
16 disk drives and terminal device ([LSI ADM-3A][adm3a]), and more.
Plugins are available in other repositories.

The whole project is implemented in Java, what can be a surprise for programmers, because
Java is emulated itself and therefore performance of my emulator won't be amazing. I
know that, but.. I'm trying to develop it for fun and for learning purposes. With this
project I have learned a lot from programming and old computers. And I'm still having fun ;-)

It is based on plugins that can be developed independently. Besides emulation process,
the platform can be used as good source-code editor with compiler for selected platform.

Installation
------------

For istallation, copy distribution somewhere where you want. Then add library
called `emu_ifaces`, you can find it in my repos.


Directory tree should be:

`emuStudio
  |
  +- compilers -> here should be compiler plugins located
  +- cpu       -> here should be CPU plugins located
  +- devices   -> here should be device plugins located
  +- mem       -> here should be memory plugins located
  +- lib       -> here should be libraries located
  +- config    -> here is a place for emuStudio config files`

For correct functionality of compilers plugins, also add run-time library of
[CUP parser generator][cup]. The library you can download from:
[CUP 11a beta 20060608 runtime][cup_runtime].


[adm3a]:       http://www.tentacle.franken.de/adm3a/
[cup]:         http://www2.cs.tum.edu/projects/cup
[cup_runtime]: http://www2.cs.tum.edu/projects/cup/java-cup-11a-runtime.jar
