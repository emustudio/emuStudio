---
layout: default
title: Original software
nav_order: 10
parent: MITS Altair8800
permalink: /altair8800/software
---

# Original software for Altair8800

Since Altair8800 virtual computer emulates a real machine, it's possible to use real software written for the computer. Several operating systems and programs can be run on Altair. There are many disk and memory images of those systems available online, but only some were tested and proved to work. Some of the available online sites are:

- [Peter Schorn][schorn]{:target="_blank"}
- [Altair clone][aclone]{:target="_blank"}
- [SIMH][simhf]{:target="_blank"}
- [DeRamp][deramp]{:target="_blank"}

If you want to manipulate with disk images, please follow this [link][cpmtools]{:target="_blank"}.

Most of the disk images were borrowed from great [simh][simh]{:target="_blank"} emulator. It's obvious that some images were modified for simh. On the other hand, it's not that obvious if the original images would actually work at all.

Tested and fully-functional images were:

- Operating system CP/M v2.2 and 3
- Altair DOS v1.0
- BASIC programming language in various versions

Disk / memory images for software for Altair8800 are available on many online sites, such as [here][asw1]{:target="_blank"} or [here][asw2]{:target="_blank"}.
Some manuals can be found e.g. [here][manuals]{:target="_blank"}.

The following subsections describe in short how to boot some of those systems, along with screen-shots how it looks.

## Boot ROM

Booting operating systems on Altair requires special ROM image to be loaded in operating memory. The purpose of a boot ROM is to load specific block of data from a device and then run it as if it was code. The code block is often called 'boot loader'. It is very small program which just loads either the whole or part of the operating system into memory and then jumps to it.

Originally, more boot ROMs existed. Different boot ROMs were used to load the code from different devices. In current implementation of emuStudio, there is only one boot ROM supported - so called 'disk boot loader' (or DBL), which loads operating system from MITS 88-DISK (through CPU ports).

The boot loader is already available in a file `examples/altair8800/boot.bin` of emuStudio installation.

Boot ROM must be loaded into memory at address `0xFF00` (hexadecimal). It is safe to jump to this address manually when operating system image file is mounted.

NOTE: All subsequent sections assume that the boot loader has been loaded in the operating memory.

## CP/M 2.2

During Altair8800 computer era, many operating systems, applications and programming languages have been developed. On of the most known operating systems is CP/M. It was written by Gary Kildall from Digital Research, Inc. At first it was mono-tasking and single-user operating system which didn't need more than 64kB of memory. Subsequent versions added multi-user variants and they were ported to 16-bit processors.

The combination of CP/M and computers with S-100 bus (8-bit computers sharing some similarities with Altair 8800) was big "industry standard", widely spread in 70's up to 80's years of twentieth century. The operating system took the burden of programming abilities from user, and this was one of the reasons why the demand for hardware and software was rapidly increased.

Tested image has name `altcpm.dsk`. It can be downloaded at [this link][altsw]{:target="_blank"}.

In order to run CP/M, please follow these steps:

1. Mount `altcpm.dsk` to drive `A:` in MITS 88-DISK.
2. In emuStudio jump to location `0xFF00`
3. Optionally, you can set CPU frequency to 2000 kHz, which was Intel 8080 original frequency.
4. Before starting emulation, show ADM-3A terminal
5. Run the emulation

When the steps are completed, CP/M should start (an informational message appears) and command line prompt will be displayed:

![Operating system CP/M 2.2]({{ site.baseurl}}/assets/altair8800/cpm22.png)

Command `dir` is working, `ls` is better `dir`. More information about CP/M commands can be found at [this link][cpm22]{:target="_blank"}.

## CP/M 3

Steps for running CP/M 3 operating systems are not that different from CP/M 2. The disk image file is called `cpm3.dsk` and can be downloaded at [this link][cpm3]{:target="_blank"}. CP/M 3 came with two versions: banked and non-banked. The image is the banked version of CP/M. Also, [simh][simh]{:target="_blank"} authors provided custom BIOS and custom boot loader.

Manual of CP/M 3 can be found at [this link][cpm3manual]{:target="_blank"}. For more information about [simh][simh]{:target="_blank"} version of Altair8800 and CP/M 3, click [here][simhmanual]{:target="_blank"}.

There are some requirements for the computer architecture, a bit different for CP/M 2.2.

### CPU

It is recommended to use Z80 version of the computer. CPU Intel 8080 will work for the operating system itself, but most provided applications require Z80.

### Operating memory

Also, the operating memory needs to be set for memory banks. The following parameters were borrowed from [simh][simh]{:target="_blank"} and were tested:

- 8 memory banks
- common address `C000h`

### Boot ROM

There exist specific version of boot loader (modified probably by [simh][simh]{:target="_blank"} authors) to load CP/M into banked memory. It is available in `examples/altair8800/mboot.bin` in your emuStudio installation. Before other steps, please load this image into operating memory at address `0xFF00` (hexadecimal).

### Steps for booting CP/M 3

Specific steps how to boot CP/M 3 in emuStudio follow:

1. Mount `cpm3.dsk` to drive `A:` in MITS 88-DISK.
2. In emuStudio jump to location `0xFF00`
3. Optionally, you can set CPU frequency to 2500 kHz, which was Zilog Z80 original frequency.
4. Before starting emulation, show ADM-3A terminal
5. Run the emulation

The following image shows the look right after the boot:

![Operating system CP/M 3 (banked version)]({{ site.baseurl }}/assets/altair8800/cpm3.png)

## Altair DOS v1.0

Steps for booting Altair DOS v1.0 follow:

1. Mount `altdos.dsk` to drive `A:` in MITS 88-DISK.
2. In emuStudio jump to location `0xFF00`
3. Optionally, you can set CPU frequency to 2000 kHz, which was Intel 8080 original frequency.
4. Before starting emulation, show ADM-3A terminal
5. Run the emulation

The system will start asking some questions. According to the [Altair manual][altairmanual]{:target="_blank"}, answers for emuStudio are: 

- `MEMORY SIZE?` -> 64 or ENTER (if memory ROM is at `0xFFFF`)
- `INTERRUPTS` -> N or just ENTER
- `HIGHEST DISK NUMBER?` -> 0 (if only 1 disk is mounted)
- `HOW MANY DISK FILES?` -> 3
- `HOW MANY RANDOM FILES?` -> 2

Basic commands you can use are e.g. `MNT 0` - to mount the drive, and then `DIR 0` to list the files.

If you want AltairDOS being able to automatically detect how much memory is installed on system, it is possible. The system does it by very nasty trick - testing if it can write to particular address
(ofcourse, maximum is 16-bits - i.e. 64K of memory). If the result is the same as it was before reading, it means that it reached the "end of memory". But when it fails to detect the ROM, it fails to determine the size, too, and the output will be `INSUFFICIENT MEMORY`.

The following image shows how it looks like:

![Operating system Altair DOS 1.0]({{ site.baseurl }}/assets/altair8800/altairdos.gif)

## BASIC

In this section will be presented how to boot MITS BASIC version 4.1. There is possible to boot also other versions, but the principle is always the same.

As it is written in [simh][simh]{:target="_blank"} manual: MITS BASIC 4.1 was the commonly used software for serious users of the Altair computer. It is a powerful (but slow) BASIC with some extended commands to allow it to access and manage the disk. There was no operating system it ran under.

After boot, you must mount the disk with `MOUNT 0`. Then, command `FILES` will show all files on the disk. In order to run a file, run command `RUN "file"`. Manual can be found at [this link][basic]{:target="_blank"}.

It is assumed you have either `examples/altair8800/boot.bin` or `examples/altair8800/mboot.bin` mounted in the operating memory.

Steps for booting BASIC follow:

1. Mount `mbasic.dsk` to drive `A:` in MITS 88-DISK.
2. In emuStudio jump to location `0xFF00`
3. Optionally, you can set CPU frequency to 2000 kHz, which was Intel 8080 original frequency.
4. Before starting emulation, show ADM-3A terminal
5. Run the emulation

The following image shows the look right after the boot:

![Altair 8800 Basic 4.1]({{ site.baseurl }}/assets/altair8800/mbasic.png)


[schorn]: http://schorn.ch/altair_4.php
[aclone]: http://altairclone.com/support.htm
[simhf]: http://www.classiccmp.org/cpmarchives/cpm/mirrors/www.schorn.ch/cpm/intro.php
[deramp]: https://deramp.com/downloads/altair/
[simh]: http://simh.trailing-edge.com/
[altsw]: http://schorn.ch/cpm/zip/altsw.zip
[cpm22]: http://www.classiccmp.org/dunfield/r/cpm22.pdf
[cpm3]: http://schorn.ch/cpm/zip/cpm3.zip
[cpm3manual]: http://www.cpm.z80.de/manuals/cpm3-usr.pdf
[simhmanual]: http://simh.trailing-edge.com/pdf/altairz80_doc.pdf
[altairmanual]: http://altairclone.com/downloads/manuals/Altair%20DOS%20User's%20Manual.pdf
[basic]: http://bitsavers.informatik.uni-stuttgart.de/pdf/mits/Altair_8800_BASIC_4.1_Reference_Jul77.pdf
[cpmtools]: http://www.autometer.de/unix4fun/z80pack/
[asw1]: http://schorn.ch/altair.html
[asw2]: http://www.classiccmp.org/cpmarchives/cpm/mirrors/www.schorn.ch/cpm/intro.php
[manuals]: http://altairclone.com/altair_manuals.htm
