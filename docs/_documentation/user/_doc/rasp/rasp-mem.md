---
layout: default
title: Memory "rasp-mem"
nav_order: 3
parent: RASP
permalink: /rasp/rasp-mem
---

# Memory "rasp-mem"

RASP memory plug-in serves as the main store (operating memory) for the RASP virtual computer. As already mentioned in the introduction of this manual, RASP is an example of von-Neumann architecture, which implies that both program and data reside in the same memory module.

After compilation of a RASP source code file the compiled program is loaded into here. During the process of the emulation, the CPU plug-in reads instructions an their operands and writes results of the operations from/to the memory. 

## Graphical user interface (GUI)

During the emulation, user can view current content of the operating memory in a simple GUI window:

![Memory GUI window]({{ site.baseurl }}/assets/rasp/memory_window.png)

It is also possible to load a compiled memory image from a file by clicking on the OPEN icon:

![OPEN icon]({{ site.baseurl }}/assets/rasp/document-open.png)

After that, you can choose the binary file you want to load.

By clicking on the CLEAN icon, you can clean the entire memory content:

![CLEAN icon]({{ site.baseurl }}/assets/rasp/edit-delete.png)

The table with memory cells content is editable. By double-clicking on a row you can simply edit the value. You confirm your changes by the ENTER key. 
 
WARNING: If you edit a cell that contains an instruction, you edit its operation code, e.g. if a cell contains the `ADD =` instruction (operation code 7) and you change the cell to 9, the operation code will be overwritten. As a result, the instruction changes to `SUB =`. It means that you cannot write number 9 as a data item here, only as an operation code.
