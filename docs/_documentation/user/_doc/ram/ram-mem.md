---
layout: default
title: Memory "ram-mem"
nav_order: 4
parent: RAM
permalink: /ram/ram-mem
---

# Program memory ("ram-mem")

RAM memory is used as a part of RAM simulator, which acts as the "program memory", holding just the program.

RAM CPU reads instructions from this memory. The instructions can be written here only by compiling the source code, or loading already compiled binary image.

The memory plugin contains simple graphical window, a GUI, which provides a set of the following features:

- It computes time and space complexity of the program
- It shows the memory content (the "program") as the list of disassembled instructions

## Graphical user interface (GUI)

The memory GUI can be seen in the following picture.

![RAM memory window]({{ site.baseurl }}/assets/ram/ram-mem.png)

- *A*: Opens already compiled program into memory. Previous program will be dismissed.
- *B*: Clears memory.
- *C*: Shows uniform time complexity for the actual program.
- *D*: Shows uniform space complexity for the actual program.

Uniform time complexity means maximum number of instructions based on the input `N`. Uniform space complexity means maximum number of used registers.
