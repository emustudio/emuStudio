---
layout: default
title: MITS Altair8800
nav_order: 1
has_children: true
permalink: /altair8800/
---

# MITS Altair8800

Computer MITS Altair 8800 was named after a planet in one of the first episodes of Star Trek series. Having Intel 8080 CPU inside, with 256 bytes of memory, no display and keyboard is this computer, when comparing to the present era, absolutely ridiculous. His author, Ed Roberts, called the invention "personal computer", which is now very common term. As Wikipedia states:

> The Altair is widely recognized as the spark that ignited the microcomputer revolution.
>> Wikipedia, Altair 8800

![MITS Altair8800 with LSI ADM-3A terminal and floppy drive]({{ site.baseurl }}/assets/altair8800/altair8800.png)

Altair 8800 is one of the oldest commercially available computers overall. Ed Roberts (founder and CEO of MITS corporation) was selling these machines by classic mail directly from the factory.

Various enthusiasts understood the power of Altair and started to develop software and hardware for the computer. Those people saw a freedom in Altair - some kind of a release from batch tasks ran on mainframe systems, maintained by elite. The phenomenon of a computer which could be put on the kitchen table allowed to make enormous money by two smart university students. In 1975, Paul Allen and Bill Gates wrote a trimmed version of BASIC programming language, called Altair BASIC, which pushed them directly to foundation of Microsoft corporation.

Basic configuration of MITS Altair 8800 was:

|---
| Item | Notes
|-|-
|Processor        | Intel 8080 or 8080a
|Speed            | 2 MHz
|RAM              | from 256 bytes to 64 kB
|ROM              | optional; usually EPROM Intel 1702 with 256 bytes (They were used for various bootloaders)
|Storage          | optional; paper tapes, cassette tapes or 5.25" or 8" floppy disks (MITS 88-DISK)
|Extensions       | at first 16 slots, later 18 slots
|Bus              | famous [S-100][s100]{:target="_blank"}
|Video            | none
|I/O              | optional; parallel or serial board (MITS 88-SIO)
|Original software| [Altair DOS][ados]{:target="_blank"}, [CP/M][cpm]{:target="_blank"}, [Altair BASIC][basic]{:target="_blank"}
|---

## Altair8800 for emuStudio

In emuStudio, there exist two variants of the computer, varying in CPU. Either Intel 8080 CPU, or Zilog Z80 CPU is used. Some behavior was inspired by [simh] emulator. Abstract schema for emuStudio (Intel 8080):

![Abstract schema of MITS Altair8800 (with Intel 8080)]({{ site.baseurl }}/assets/altair8800/altairscheme.png){:class="img-responsive" width="400px"}

Abstract schema for emuStudio (Zilog Z80):

![Abstract schema of MITS Altair8800 (with Zilog Z80)]({{ site.baseurl }}/assets/altair8800/altairz80.png){:class="img-responsive" width="526px"}

Each plugin is described in further sections.


[simh]: http://simh.trailing-edge.com/
[s100]: https://en.wikipedia.org/wiki/S-100_bus
[ados]: http://altairclone.com/downloads/manuals/Altair%20DOS%20User's%20Manual.pdf
[cpm]: http://www.classiccmp.org/dunfield/r/cpm22.pdf
[basic]: https://en.wikipedia.org/wiki/Altair_BASIC
