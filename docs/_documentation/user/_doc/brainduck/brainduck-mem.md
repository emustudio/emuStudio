---
layout: default
title: Memory
nav_order: 3
parent: BrainDuck
---

:imagepath: brainduck/images/

== Memory "brainduck-mem"

BrainDuck memory is used as a part of BrainDuck computer, which acts as the operating memory, holding both of brainfuck
program and data.

BrainDuck CPU reads/writes instructions from/to the memory. Memory updates its cells and notifies debugger in emuStudio
about the update.

The memory plug-in contains simple graphical window, a GUI, which provides a set of the following features:

- paginated view of memory cells, arranged into 16x16 table per page.
- cells are displayed in hexadecimal form and can be changed directly by user.
- there are hard-coded 256 pages, so the memory size is 256 * (16x16) = 64 kB

[[XMI]]
=== Installation and run

The BrainDuck memory can be run only as a part of emuStudio. It is installed in location
`mem/brainduck-mem.jar`.

[[XMG]]
=== Graphical user interface (GUI)

In the following screenshot, it is possible to see GUI of `brainduck-mem`.

image::{imagepath}/brainduck-mem.png[BrainDuck memory window]

- *A*: Shows actually displayed page. Can be edited manually by entering a number and pressing ENTER key
- *B*: By double-clicking on a memory cell, the cell editor is enabled and user can overwrite the content of the cell.
Supported number formats are decimal or hexadecimal. Hexadecimal number must begin with `0x` prefix.
- *C*: By clicking on button `Page down`, the page number is increased; button `Page up` decreases the page number.
