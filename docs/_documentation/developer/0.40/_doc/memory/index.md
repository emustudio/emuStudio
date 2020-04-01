---
layout: default
title: Writing a memory
nav_order: 5
permalink: /memory/
---

# Writing a memory

In emuStudio, plugin root class must either implement [Memory][memory]{:target="_blank"} interface, or can extend more bloat-free [AbstractMemory][abstractMemory]{:target="_blank"} class.
 
Generally, a memory in an emulator is usually implemented as an array of integers. Indexes to the array represent addresses, and values are the memory cell values. In emuStudio, this kind of implementation is reflected by memory context. Memory context should be a class which either implements [MemoryContext][memoryContext]{:target="_blank"} interface, or extends [AbstractMemoryContext][abstractMemoryContext]{:target="_blank"} class. The latter provide additional functionality - management of memory "listeners".

A memory listener (implementing [Memory.MemoryListener][memoryListener]{:target="_blank"} interface) can observe memory value changes on all address range. But when the emulation is in running state, emuStudio turns off the memory notifications to speed up the emulation.  



[memory]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/memory/Memory.html
[memoryContext]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/memory/MemoryContext.html
[abstractMemory]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/memory/AbstractMemory.html
[abstractMemoryContext]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/memory/AbstractMemoryContext.html
[memoryListener]: http://localhost:4000/documentation/developer/0.40/emulib_javadoc/net/emustudio/emulib/plugins/memory/Memory.MemoryListener.html
