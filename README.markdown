Welcome to emuStudio !
-------------------------

In 2006 I began to write my own CPU emulator, Intel 8080. It was created as a school
project. The software lacked many features and I had continued to enhance the software.

Now it has grown to emulation platform that can emulate almost arbitrary, not necessarily
only 8-bit computers. 

The versatility of the platform lies of the emulator's structure. Each emulated component
is implemented as independent plug-in (they are available in the other repositories on GitHub).

The whole project is implemented in Java (probably that is not happy surprise for performance
expectations). But... At first, the *main purpose of the platform is to support education process*.
The platform has been used at Technical University of Kosice (Slovakia) in great success since
year 2007.

Second, I'm trying to develop it for fun. With this project I have learned a lot about programming
and about the hardware insights. I'm still trying to improve and enhance it (including the performance),
and I'm still having fun ;-)

Once more, "universal" means that it is able to emulate almost arbitrary computer, based on von Neumann
architecture. The emuStudio categorizes all computer components into four types:

* Compilers
* CPUs
* Memories
* Devices

Each computer component is implemented as separate plug-in. The plug-ins can be developed independently.

Besides the emulation itself, the platform can be used as a good source-code editor with compiler for
selected CPU.

Installation
------------

For istallation, first download some distribution copy from the Download page.

Unpack it, and everything is ready.

Directory tree will look like this:

    emuStudio
      |
      +- compilers -> there are all compiler plug-ins
      +- cpu       -> there are all CPU plug-ins
      +- devices   -> there are all device plug-ins
      +- mem       -> there are all memory plug-ins
      +- lib       -> there are libraries needed (emuLib and [CUP parser generator runtime][cup_runtime])
      +- config    -> here is a place for emuStudio abstract schemas (virtual computer configurations)

[adm3a]:       http://www.tentacle.franken.de/adm3a/
[cup]:         http://www2.cs.tum.edu/projects/cup
[cup_runtime]: http://www2.cs.tum.edu/projects/cup/java-cup-11a-runtime.jar

License
-------

This project is released under GNU GPL v2 license.

Related projects
----------------

* emuLib
* as-8080
* as-z80
* ramc-ram
* brainc-brainduck
* 8080-cpu
* z80-cpu
* ram-cpu
* brainduck-cpu
* standard-mem
* ram-mem
* brainduck-mem
* 88-disk
* 88-sio
* adm3A-terminal
* simhPseudo-z80
* abstractTape-ram
* brainduck-terminal
