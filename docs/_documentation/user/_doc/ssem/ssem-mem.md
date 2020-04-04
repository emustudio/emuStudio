---
layout: default
title: Memory "ssem-mem"
nav_order: 3
parent: SSEM
permalink: /ssem/ssem-mem
---

# Memory "ssem-mem"

SSEM used the world's first random-access memory called Williams or [Williams-Kilburn][tube]{:target="_blank"} tube. Used principle was the same as in standard Cathode-Ray-Tubes (CRTs). Original [EDSAC][edsac]{:target="_blank"} computer (which introduced the von Neumann architecture) did not have random-access memory.

SSEM memory had 32 memory cells (called words), each had size of 32 bits. The memory could contain instructions and data. So, one SSEM instruction perfectly fits in the single memory word.

SSEM had 32 so-called "lines", which represented cells in memory. Each line, or a cell, was 4 bytes long. 

## Graphical user interface (GUI)

Since emuStudio is interactive application, GUIs are a natural thing. The memory GUI looks as follows:

![SSEM Memory GUI sample look]({{ site.baseurl }}/assets/ssem/ssem-memory.png)

As you can see in the picture, a row represents single SSEM memory cell - 32 scattered bits, and the last few columns show both the number the bits represent, and a raw ASCII value of the 4-byte sequence of data.

Edits of cells is also possible, by pointing to a bit, and pressing either 1 or 0 - possibly a DELETE key, committing the change immediately. This works for the value itself, and for the data column as well.

Movement around cells is possible with arrow keys.


[tube]: https://en.wikipedia.org/wiki/Manchester_Small-Scale_Experimental_Machine#Williams-Kilburn_tube
[edsac]: https://en.wikipedia.org/wiki/EDSAC
