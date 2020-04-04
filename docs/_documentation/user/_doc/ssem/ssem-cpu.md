---
layout: default
title: CPU "ssem-cpu"
nav_order: 2
parent: SSEM
permalink: /ssem/ssem-cpu
---

# SSEM CPU emulator

SSEM is one of the first implementations of the von-Neumann design of a computer. It contained control unit, arithmetic-logic unit and I/O subsystem (CRT display).

Speed of CPU is around 700 instructions per second.

The architecture of our SSEM CPU emulator will look as follows (below is Display and Memory just to show how it is connected in overall):

![SSEM scheme]({{ site.baseurl }}/assets/ssem/ssem-scheme.svg)

## Status panel

The status panel is the interaction point between CPU and the user. With it, the user can be allowed to modify or view the internal status of the CPU emulator. This is very handy when learning or checking how it works, what the registers' values really are (and compare them with those shown on a display), etc. The status panel shows the following:

- CPU run state
- Internal state: registers or possibly portion of memory
- Speed

SSEM CPU status panel looks as follows:

![SSEM CPU Status panel GUI]({{ site.baseurl }}/assets/ssem/cpu-status-panel.png)

## Automatic emulation

The optional step is to change a behavior slightly when user runs the automatic emulation. The memory content is important enough to be put in a file in case of automatic emulation. It has just 32 rows. It can be useful to see the content of the accumulator and CI register after the emulation finishes as well. 

After each emulation "stop" - no matter the reason of stopping, if before the emulation was running, a "snapshot" of the emulator state is performed - registers `Acc`, `CI` and memory content are saved to the file, called `ssem.out`.


The emulator automation can be run as follows:

    ./emuStudio --config config/SSEMBaby.toml --nogui --auto --input examples/as-ssem/noodle-timer.ssem

the emulation will run without user interaction, and file `ssem.out` will be created with the following content:

```
ACC=0x3bfffe2
CI=0x58

   L L L L L 5 6 7 8 9 0 1 2 I I I 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
00                                                                 
01             * * * * *       *   * * * * * * * * * * * * * * * * 
02     * * *       *         * *               * * * *             
03     * * *       *           *               * * * *             
04 *   * * *       *             *             * * * *             
05                           * *           * * * * * * * *         
06 * * * * *     * * *         *               * * * *             
07 * *   * *       *             *             * * * *             
08                 *           * *             * * * *             
09   *   * *     * * *                 * * * * * * * * * * * *     
10 *   *                       *               * * * *             
11 * *   * *   *       *         *             * * * *             
12     * * *   * *   * *     * *               * * * *             
13     * * *   *   *   *       *           * * * * * * * *         
14 *   *       *       *     * *               * * * *             
15 *                           *               * * * *             
16 * *   * *   * * * * *         *             * * * *             
17     * * *   * * * *       * *       * * * * * * * * * * * *     
18     * * *   * * * * *       *               * * * *             
19 *                         * *               * * * *             
20   * * * *   * * * *           *             * * * *             
21             *       *       * *         * * * * * * * *         
22             * * * *       * * *             * * * *             
23             *     *                         * * * *             
24             *       *                       * * * *             
25                                 * * * * * * * * * * * * * * * * 
26   * *                                                           
27 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * 
28             *         * * *   *                                 
29                                             *       * * * * * * 
30   * * * *   * * * * *       *   * * * * * *         * * * * * * 
31         *   * *   * *                                         *
```

