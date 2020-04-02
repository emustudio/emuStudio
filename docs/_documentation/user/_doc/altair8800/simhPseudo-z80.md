---
layout: default
title: Device simhPseudo-z80
nav_order: 9
parent: MITS Altair8800
permalink: /altair8800/simhPseudo-z80
---

# Virtual device "simhPseudo-z80"

Virtual device partially reimplemented from [simh][simh]{:target="_blank"} emulator. This device is used mainly for communication between CP/M 3 operating system for `simh` and emuStudio. Most of the original functionality is not implemented, but it is crucial for support of memory bank-switching.

## Programming

Z80 or 8080 programs communicate with the SIMH pseudo device via port `0xfe`. Programmers must apply the following principles:

1. For commands that do not require parameters and do not return results:

        ld  a,<cmd>
        out (0feh),a

   Special case is the reset command which needs to be send 128 times to make
   sure that the internal state is properly reset.

2. For commands that require parameters and do not return results:

        ld  a,<cmd>
        out (0feh),a
        ld  a,<p1>
        out (0feh),a
        ld  a,<p2>
        out (0feh),a
        ...

   Note: The calling program must send all parameter bytes. Otherwise
   the pseudo device is left in an undefined state.

3. For commands that do not require parameters and return results:

        ld  a,<cmd>
        out (0feh),a
        in  a,(0feh)    ; <A> contains first byte of result
        in  a,(0feh)    ; <A> contains second byte of result
        ...

   Note: The calling program must request all bytes of the result. Otherwise
   the pseudo device is left in an undefined state.

4. Commands requiring parameters and returning results do not exist currently.

[simh]: http://simh.trailing-edge.com/
