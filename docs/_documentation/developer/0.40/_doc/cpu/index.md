---
layout: default
title: Writing a CPU
nav_order: 4
has_children: true
permalink: /cpu/
---

# Writing a CPU

CPU plugins in emuStudio are not just plain emulators. They must cooperate with emuStudio and provide capabilities allowing debugging and some interaction.

A CPU plugin must implement:

Emulation "engine"
: It is the CPU emulator itself, and it should be implemented using some emulation technique. In Java there are not many options, so usually either interpretation or threaded dispatch are used, both described e.g. [here][interpretation]{:target="_blank"} or [here][bario]{:target="_blank"}.

Disassembler
: It will be used by emuStudio for creating the list of instructions in the debugger panel.

Java Swing GUI panel
: It should implement the visualization of CPU registers, possibly current frequency and CPU run state.

Both disassembler and GUI panel should be instantiated just once. It is good practice to instantiate disassembler during plugin [instantiation][instantiation] or [initialization][initialization], and GUI in the [CPU.getStatusPanel()][getStatusPanel]{:target="_blank"} method call. emuStudio application will call the method just once from Swing [Event dispatch thread][swingthread]{:target="_blank"}.

Programming a disassembler might be tedious and error-prone. Therefore we encourage to use a tool which can generate the disassembler from a specification file. The tool is called [Edigen][edigen]{:target="_blank"}, an abbreviation for _Emulator DIsassembler GENerator_. It can be nicely incorporated into Gradle build using [edigen-gradle-plugin][edigen-gradle]{:target="_blank"}.





[bario]: http://www.xsim.com/papers/Bario.2001.emubook.pdf
[interpretation]: http://cse.unl.edu/~witty/class/embedded/material/note/emulation.pdf
[instantiation]: {{ site.baseurl }}/plugin_basics/loading.html#plugin-instantiation
[initialization]: {{ site.baseurl }}/plugin_basics/loading.html#plugin-initialization
[getStatusPanel]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/cpu/CPU.html#getStatusPanel()
[swingthread]: https://docs.oracle.com/javase/tutorial/uiswing/concurrency/dispatch.html
[edigen]: https://github.com/emustudio/edigen
[edigen-gradle]: https://github.com/emustudio/edigen-gradle-plugin
