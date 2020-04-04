---
layout: default
title: CPU z80-cpu
nav_order: 4
parent: MITS Altair8800
permalink: /altair8800/z80-cpu
---

# Zilog Z80 CPU emulator

It was possible to upgrade your Altair 8800 computer with a "better" 8-bit processor [Zilog Z80][z80]{:target="_blank"}. The processor was probably the most used 8-bit processor in 80's. It was backward compatible with 8080, and brought many enhancements. It was originally targeted for embedded systems, but very soon it become very popular and used for all kinds of computers - including desktop computers, arcade games, etc. Today the CPU is still used in some MP3 players, see e.g. [S1 MP3 Player][mp3]{:target="_blank"}.

Main features of the emulator include:

* Interpretation as emulation technique,
* Correct real timing of instructions,
* Ability to set clock frequency manually at run-time,
* Emulation of all instructions including interrupts,
* Disassembler implementation,
* Ability to "dump" instruction history to console at run-time,
* Support of breakpoints,
* Ability of communication with up to 256 I/O devices,
* Status window shows all registers, flags, and run-time frequency.

## Configuration file

The following table shows all the possible settings of Zilog Z80 CPU plugin:

|---
|Name              | Default value        | Valid values          | Description
|-|-|-|-
|`printCode`       | false                | true / false          | Whether the emulator should print executed instructions, and its internal state to console (dump)
|`printCodeUseCache`| false               | true / false          | If `printCode` is set to `true`, then a cache will be used which remembers already visited blocks of code so the instruction dump will not be bloated with infinite loops
|---

## Dumping executed instructions

The CPU offers a quite unique feature, which is the ability to dump executed instructions as a sequence to the console. When enabled, then each executed instruction - together with content of flags and registers values after the execution are printed. This feature might be extremely useful in two cases:

1. Reverse engineering of some unknown software
2. It allows to build tools for automatic checking of register values during the emulation, when performing automatic emulation.

In order to enable this feature, please see the section "Configuration file".

For example, let's take one of the examples which computes a reverse text:

```
; Print reversed text

org 1000

dec sp       ; stack initialization (0FFFFh)

ld hl,text1
call putstr  ; print text1
ld de,input  ; address for string input
call getline ; read from keyboard

ld bc,input

ld d,0      ; chars counter

char_loop:
ld a, (bc)
inc bc        ; bc = bc+1
cp 10       ; end of input?
jp z, char_end
cp 13
jp z, char_end

inc d        ; d =d+1
jp char_loop
char_end:

dec bc        ;  bc = bc-1
dec bc

call newline

char2_loop:
ld a, (bc)
call putchar

dec bc

dec d
jp z, char2_end

jp char2_loop
char2_end:

halt

include "include\getchar.inc"
include "include\getline.inc"
include "include\putstr.inc"
include "include\putchar.inc"
include "include\newline.inc"

text1: db "Reversed text ...",10,13,"Enter text: ",0
text2: db 10,13,"Reversed: ",0
input: ds 30
```

When the program is being run, and the dump instructions feature is turned on, on console you can see the following
output:

```
0000 | PC=03e8 |          dec SP |        3B  || regs=00 00 00 00 00 00 00 00  IX=0000 IY=0000 IFF=0 I=00 R=01 | flags=       | SP=ffff | PC=03e9
0001 | PC=03e9 |      ld HL, 485 |  21 85 04  || regs=00 00 00 00 04 85 00 00  IX=0000 IY=0000 IFF=0 I=00 R=02 | flags=       | SP=ffff | PC=03ec
0002 | PC=03ec |        call 46D |  CD 6D 04  || regs=00 00 00 00 04 85 00 00  IX=0000 IY=0000 IFF=0 I=00 R=03 | flags=       | SP=fffd | PC=046d
0002 | PC=046d |      ld A, (HL) |        7E  || regs=00 00 00 00 04 85 00 52  IX=0000 IY=0000 IFF=0 I=00 R=04 | flags=       | SP=fffd | PC=046e
0003 | PC=046e |          inc HL |        23  || regs=00 00 00 00 04 86 00 52  IX=0000 IY=0000 IFF=0 I=00 R=05 | flags=       | SP=fffd | PC=046f
0003 | PC=046f |            cp 0 |     FE 00  || regs=00 00 00 00 04 86 00 52  IX=0000 IY=0000 IFF=0 I=00 R=06 | flags=    N  | SP=fffd | PC=0471
0007 | PC=0471 |           ret Z |        C8  || regs=00 00 00 00 04 86 00 52  IX=0000 IY=0000 IFF=0 I=00 R=07 | flags=    N  | SP=fffd | PC=0472
0008 | PC=0472 |      out (11),A |     D3 11  || regs=00 00 00 00 04 86 00 52  IX=0000 IY=0000 IFF=0 I=00 R=08 | flags=    N  | SP=fffd | PC=0474
0008 | PC=0474 |          jp 46D |  C3 6D 04  || regs=00 00 00 00 04 86 00 52  IX=0000 IY=0000 IFF=0 I=00 R=09 | flags=    N  | SP=fffd | PC=046d
0009 | PC=046d |      ld A, (HL) |        7E  || regs=00 00 00 00 04 86 00 65  IX=0000 IY=0000 IFF=0 I=00 R=0a | flags=    N  | SP=fffd | PC=046e
0025 | Block from 0474 to 03EF; count=184
0025 | PC=03ef |      ld DE, 4B2 |  11 B2 04  || regs=00 00 04 b2 04 a5 00 00  IX=0000 IY=0000 IFF=0 I=00 R=42 | flags= Z  N  | SP=ffff | PC=03f2
0025 | PC=03f2 |        call 428 |  CD 28 04  || regs=00 00 04 b2 04 a5 00 00  IX=0000 IY=0000 IFF=0 I=00 R=43 | flags= Z  N  | SP=fffd | PC=0428
0025 | PC=0428 |         ld C, 0 |     0E 00  || regs=00 00 04 b2 04 a5 00 00  IX=0000 IY=0000 IFF=0 I=00 R=44 | flags= Z  N  | SP=fffd | PC=042a
0026 | PC=042a |        in A, 10 |     DB 10  || regs=00 00 04 b2 04 a5 00 02  IX=0000 IY=0000 IFF=0 I=00 R=45 | flags= Z  N  | SP=fffd | PC=042c
0026 | PC=042c |           and 1 |     E6 01  || regs=00 00 04 b2 04 a5 00 00  IX=0000 IY=0000 IFF=0 I=00 R=46 | flags= ZHP   | SP=fffd | PC=042e
0026 | PC=042e |       jp Z, 42A |  CA 2A 04  || regs=00 00 04 b2 04 a5 00 00  IX=0000 IY=0000 IFF=0 I=00 R=47 | flags= ZHP   | SP=fffd | PC=042a
0027 | PC=042a |        in A, 10 |     DB 10  || regs=00 00 04 b2 04 a5 00 02  IX=0000 IY=0000 IFF=0 I=00 R=48 | flags= ZHP   | SP=fffd | PC=042c
6323 | Block from 042E to 0431; count=1048716
6323 | PC=0431 |        in A, 11 |     DB 11  || regs=00 00 04 b2 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=54 | flags=  H    | SP=fffd | PC=0433
6323 | PC=0433 |            cp D |     FE 0D  || regs=00 00 04 b2 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=55 | flags=    N  | SP=fffd | PC=0435
6324 | PC=0435 |       jp Z, 461 |  CA 61 04  || regs=00 00 04 b2 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=56 | flags=    N  | SP=fffd | PC=0438
6324 | PC=0438 |            cp A |     FE 0A  || regs=00 00 04 b2 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=57 | flags=    N  | SP=fffd | PC=043a
6324 | PC=043a |       jp Z, 461 |  CA 61 04  || regs=00 00 04 b2 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=58 | flags=    N  | SP=fffd | PC=043d
6324 | PC=043d |            cp 8 |     FE 08  || regs=00 00 04 b2 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=59 | flags=    N  | SP=fffd | PC=043f
6324 | PC=043f |      jp NZ, 459 |  C2 59 04  || regs=00 00 04 b2 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=5a | flags=    N  | SP=fffd | PC=0459
6324 | PC=0459 |      out (11),A |     D3 11  || regs=00 00 04 b2 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=5b | flags=    N  | SP=fffd | PC=045b
6324 | PC=045b |      ld (DE), A |        12  || regs=00 00 04 b2 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=5c | flags=    N  | SP=fffd | PC=045c
6324 | PC=045c |          inc DE |        13  || regs=00 00 04 b3 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=5d | flags=    N  | SP=fffd | PC=045d
6325 | PC=045d |           inc C |        0C  || regs=00 01 04 b3 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=5e | flags=       | SP=fffd | PC=045e
6325 | PC=045e |          jp 42A |  C3 2A 04  || regs=00 01 04 b3 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=5f | flags=       | SP=fffd | PC=042a
6325 | PC=042a |        in A, 10 |     DB 10  || regs=00 01 04 b3 04 a5 00 02  IX=0000 IY=0000 IFF=0 I=00 R=60 | flags=       | SP=fffd | PC=042c
8683 | Block from 045E to 0461; count=440826
8683 | PC=0461 |         ld A, A |     3E 0A  || regs=00 04 04 b6 04 a5 00 0a  IX=0000 IY=0000 IFF=0 I=00 R=5a | flags= ZH N  | SP=fffd | PC=0463
8683 | PC=0463 |      ld (DE), A |        12  || regs=00 04 04 b6 04 a5 00 0a  IX=0000 IY=0000 IFF=0 I=00 R=5b | flags= ZH N  | SP=fffd | PC=0464
8683 | PC=0464 |          inc DE |        13  || regs=00 04 04 b7 04 a5 00 0a  IX=0000 IY=0000 IFF=0 I=00 R=5c | flags= ZH N  | SP=fffd | PC=0465
8683 | PC=0465 |         ld A, D |     3E 0D  || regs=00 04 04 b7 04 a5 00 0d  IX=0000 IY=0000 IFF=0 I=00 R=5d | flags= ZH N  | SP=fffd | PC=0467
8683 | PC=0467 |      ld (DE), A |        12  || regs=00 04 04 b7 04 a5 00 0d  IX=0000 IY=0000 IFF=0 I=00 R=5e | flags= ZH N  | SP=fffd | PC=0468
8684 | PC=0468 |          inc DE |        13  || regs=00 04 04 b8 04 a5 00 0d  IX=0000 IY=0000 IFF=0 I=00 R=5f | flags= ZH N  | SP=fffd | PC=0469
8684 | PC=0469 |         ld A, 0 |     3E 00  || regs=00 04 04 b8 04 a5 00 00  IX=0000 IY=0000 IFF=0 I=00 R=60 | flags= ZH N  | SP=fffd | PC=046b
8684 | PC=046b |      ld (DE), A |        12  || regs=00 04 04 b8 04 a5 00 00  IX=0000 IY=0000 IFF=0 I=00 R=61 | flags= ZH N  | SP=fffd | PC=046c
8684 | PC=046c |             ret |        C9  || regs=00 04 04 b8 04 a5 00 00  IX=0000 IY=0000 IFF=0 I=00 R=62 | flags= ZH N  | SP=ffff | PC=03f5
8684 | PC=03f5 |      ld BC, 4B2 |  01 B2 04  || regs=04 b2 04 b8 04 a5 00 00  IX=0000 IY=0000 IFF=0 I=00 R=63 | flags= ZH N  | SP=ffff | PC=03f8
8684 | PC=03f8 |         ld D, 0 |     16 00  || regs=04 b2 00 b8 04 a5 00 00  IX=0000 IY=0000 IFF=0 I=00 R=64 | flags= ZH N  | SP=ffff | PC=03fa
8684 | PC=03fa |      ld A, (BC) |        0A  || regs=04 b2 00 b8 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=65 | flags= ZH N  | SP=ffff | PC=03fb
8684 | PC=03fb |          inc BC |        03  || regs=04 b3 00 b8 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=66 | flags= ZH N  | SP=ffff | PC=03fc
8684 | PC=03fc |            cp A |     FE 0A  || regs=04 b3 00 b8 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=67 | flags=    N  | SP=ffff | PC=03fe
8684 | PC=03fe |       jp Z, 40A |  CA 0A 04  || regs=04 b3 00 b8 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=68 | flags=    N  | SP=ffff | PC=0401
8684 | PC=0401 |            cp D |     FE 0D  || regs=04 b3 00 b8 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=69 | flags=    N  | SP=ffff | PC=0403
8685 | PC=0403 |       jp Z, 40A |  CA 0A 04  || regs=04 b3 00 b8 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=6a | flags=    N  | SP=ffff | PC=0406
8685 | PC=0406 |           inc D |        14  || regs=04 b3 01 b8 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=6b | flags=       | SP=ffff | PC=0407
8685 | PC=0407 |          jp 3FA |  C3 FA 03  || regs=04 b3 01 b8 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=6c | flags=       | SP=ffff | PC=03fa
8685 | PC=03fa |      ld A, (BC) |        0A  || regs=04 b3 01 b8 04 a5 00 68  IX=0000 IY=0000 IFF=0 I=00 R=6d | flags=       | SP=ffff | PC=03fb
8685 | Block from 0407 to 040A; count=28
8685 | PC=040a |          dec BC |        0B  || regs=04 b6 04 b8 04 a5 00 0a  IX=0000 IY=0000 IFF=0 I=00 R=09 | flags= ZH N  | SP=ffff | PC=040b
8685 | PC=040b |          dec BC |        0B  || regs=04 b5 04 b8 04 a5 00 0a  IX=0000 IY=0000 IFF=0 I=00 R=0a | flags= ZH N  | SP=ffff | PC=040c
8685 | PC=040c |        call 47A |  CD 7A 04  || regs=04 b5 04 b8 04 a5 00 0a  IX=0000 IY=0000 IFF=0 I=00 R=0b | flags= ZH N  | SP=fffd | PC=047a
8685 | PC=047a |         ld A, A |     3E 0A  || regs=04 b5 04 b8 04 a5 00 0a  IX=0000 IY=0000 IFF=0 I=00 R=0c | flags= ZH N  | SP=fffd | PC=047c
8686 | PC=047c |        call 477 |  CD 77 04  || regs=04 b5 04 b8 04 a5 00 0a  IX=0000 IY=0000 IFF=0 I=00 R=0d | flags= ZH N  | SP=fffb | PC=0477
8686 | PC=0477 |      out (11),A |     D3 11  || regs=04 b5 04 b8 04 a5 00 0a  IX=0000 IY=0000 IFF=0 I=00 R=0e | flags= ZH N  | SP=fffb | PC=0479
8686 | PC=0479 |             ret |        C9  || regs=04 b5 04 b8 04 a5 00 0a  IX=0000 IY=0000 IFF=0 I=00 R=0f | flags= ZH N  | SP=fffd | PC=047f
8686 | PC=047f |         ld A, D |     3E 0D  || regs=04 b5 04 b8 04 a5 00 0d  IX=0000 IY=0000 IFF=0 I=00 R=10 | flags= ZH N  | SP=fffd | PC=0481
8686 | PC=0481 |        call 477 |  CD 77 04  || regs=04 b5 04 b8 04 a5 00 0d  IX=0000 IY=0000 IFF=0 I=00 R=11 | flags= ZH N  | SP=fffb | PC=0477
8686 | PC=0477 |      out (11),A |     D3 11  || regs=04 b5 04 b8 04 a5 00 0d  IX=0000 IY=0000 IFF=0 I=00 R=12 | flags= ZH N  | SP=fffb | PC=0479
8686 | Block from 0481 to 0484; count=2
8686 | PC=0484 |             ret |        C9  || regs=04 b5 04 b8 04 a5 00 0d  IX=0000 IY=0000 IFF=0 I=00 R=14 | flags= ZH N  | SP=ffff | PC=040f
8686 | PC=040f |      ld A, (BC) |        0A  || regs=04 b5 04 b8 04 a5 00 6a  IX=0000 IY=0000 IFF=0 I=00 R=15 | flags= ZH N  | SP=ffff | PC=0410
8686 | PC=0410 |        call 477 |  CD 77 04  || regs=04 b5 04 b8 04 a5 00 6a  IX=0000 IY=0000 IFF=0 I=00 R=16 | flags= ZH N  | SP=fffd | PC=0477
8686 | PC=0477 |      out (11),A |     D3 11  || regs=04 b5 04 b8 04 a5 00 6a  IX=0000 IY=0000 IFF=0 I=00 R=17 | flags= ZH N  | SP=fffd | PC=0479
8686 | Block from 0410 to 0413; count=2
8686 | PC=0413 |          dec BC |        0B  || regs=04 b4 04 b8 04 a5 00 6a  IX=0000 IY=0000 IFF=0 I=00 R=19 | flags= ZH N  | SP=ffff | PC=0414
8687 | PC=0414 |           dec D |        15  || regs=04 b4 03 b8 04 a5 00 6a  IX=0000 IY=0000 IFF=0 I=00 R=1a | flags=  H N  | SP=ffff | PC=0415
8687 | PC=0415 |       jp Z, 41B |  CA 1B 04  || regs=04 b4 03 b8 04 a5 00 6a  IX=0000 IY=0000 IFF=0 I=00 R=1b | flags=  H N  | SP=ffff | PC=0418
8687 | PC=0418 |          jp 40F |  C3 0F 04  || regs=04 b4 03 b8 04 a5 00 6a  IX=0000 IY=0000 IFF=0 I=00 R=1c | flags=  H N  | SP=ffff | PC=040f
8687 | PC=040f |      ld A, (BC) |        0A  || regs=04 b4 03 b8 04 a5 00 6f  IX=0000 IY=0000 IFF=0 I=00 R=1d | flags=  H N  | SP=ffff | PC=0410
8687 | Block from 0418 to 041B; count=23
8687 | PC=041b |            halt |        76  || regs=04 b1 00 b8 04 a5 00 61  IX=0000 IY=0000 IFF=0 I=00 R=34 | flags= ZH N  | SP=ffff | PC=041c
```

The dump format consists of lines, each line represents one instruction execution. The line is separated by `|` chars, splitting it into so-called sections. Sections before the sequence `||` represent state *before* instruction execution, and sections after it represent the state *after* instruction execution. Particular sections are described in the following table.

|---
|Column | Description
|-|-
| 1     | Timestamp from program start (seconds)
| 2     | Program counter before instruction execution
| 3     | Disassembled instruction
| 4     | Instruction opcodes
|       | Now follows the state *after* instruction execution
| 5     | Register values (`B`, `C`, `D`, `E`, `H`, `L`, reserved (always 0), `A`)
| 6     | Flags
| 7     | Stack pointer register (`SP`)
| 8     | Program counter after instruction execution
|---


[z80]: https://en.wikipedia.org/wiki/Zilog_Z80
[mp3]: https://en.wikipedia.org/wiki/S1_MP3_player
